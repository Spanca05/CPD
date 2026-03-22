import os
import re
import pandas as pd
import matplotlib.pyplot as plt


DOC_DIR = os.path.dirname(os.path.abspath(__file__))
os.chdir(DOC_DIR)
MD_FILE = os.path.join(DOC_DIR, "performance_values.md")


def parse_markdown_tables(filepath):
    with open(filepath, "r", encoding="utf-8") as f:
        lines = f.readlines()

    contexts = []
    current_step = None
    current_lang = None
    current_table = []

    for line in lines:
        stripped = line.strip()

        if stripped.startswith("## Step 1"):
            current_step = "Step 1"
            current_lang = None
        elif stripped.startswith("## Step 2"):
            current_step = "Step 2"
            current_lang = None
        elif stripped.startswith("## Step 3"):
            current_step = "Step 3"
            current_lang = None
        elif stripped == "### C++":
            current_lang = "C++"
        elif stripped == "### Java":
            current_lang = "Java"

        if stripped.startswith("|"):
            current_table.append(stripped)
        else:
            if current_table:
                contexts.append((current_step, current_lang, current_table))
                current_table = []

    if current_table:
        contexts.append((current_step, current_lang, current_table))

    parsed = []
    for step, lang, table_lines in contexts:
        if len(table_lines) < 2:
            continue

        headers = [h.strip() for h in table_lines[0].strip("|").split("|")]
        rows = []

        for row in table_lines[2:]:
            values = [v.strip() for v in row.strip("|").split("|")]
            if len(values) == len(headers):
                rows.append(values)

        df = pd.DataFrame(rows, columns=headers)
        parsed.append((step, lang, df))

    return parsed


def matrix_size_to_n(size_str):
    match = re.match(r"(\d+)[×x](\d+)", str(size_str).strip())
    if not match:
        raise ValueError(f"Invalid matrix size: {size_str}")
    return int(match.group(1))


def clean_numeric_series(series):
    return pd.to_numeric(series.astype(str).str.replace(",", "", regex=False), errors="coerce")


def prepare_df(df, step):
    df = df.copy()

    if "Matrix Size" in df.columns:
        df["N"] = df["Matrix Size"].apply(matrix_size_to_n)

    numeric_cols = [c for c in df.columns if c not in ["Matrix Size"]]
    for col in numeric_cols:
        if col == "Block Size":
            df[col] = clean_numeric_series(df[col]).astype("Int64")
        else:
            df[col] = clean_numeric_series(df[col])

    if "Time (s)" in df.columns and "N" in df.columns:
        df["GFLOPS"] = (2 * (df["N"] ** 3)) / (df["Time (s)"] * 1e9)

    return df


def save_line_plot(x1, y1, label1, x2=None, y2=None, label2=None,
                   xlabel="", ylabel="", title="", filename="plot.png"):
    plt.figure(figsize=(8, 5))
    plt.plot(x1, y1, marker="o", label=label1)
    if x2 is not None and y2 is not None:
        plt.plot(x2, y2, marker="o", label=label2)
    plt.xlabel(xlabel)
    plt.ylabel(ylabel)
    plt.title(title)
    plt.grid(True)
    plt.legend()
    plt.tight_layout()
    plt.savefig(filename)
    plt.close()


def save_multi_series_block_plot(df, ycol, ylabel, title, filename):
    plt.figure(figsize=(8, 5))

    for n in sorted(df["N"].dropna().unique()):
        subset = df[df["N"] == n].sort_values("Block Size")
        plt.plot(
            subset["Block Size"],
            subset[ycol],
            marker="o",
            label=f"N={int(n)}"
        )

    plt.xlabel("Block Size")
    plt.ylabel(ylabel)
    plt.title(title)
    plt.grid(True)
    plt.legend()
    plt.tight_layout()
    plt.savefig(filename)
    plt.close()


def extract_table_from_section(filepath, section_heading, language=None):
    with open(filepath, "r", encoding="utf-8") as f:
        lines = f.readlines()

    start_idx = None
    for idx, line in enumerate(lines):
        if line.strip() == section_heading:
            start_idx = idx
            break

    if start_idx is None:
        return None

    search_idx = start_idx + 1
    if language is not None:
        lang_tag = f"### {language}"
        while search_idx < len(lines):
            stripped = lines[search_idx].strip()
            if stripped.startswith("## ") and search_idx > start_idx:
                return None
            if stripped == lang_tag:
                search_idx += 1
                break
            search_idx += 1

    table_start = None
    for idx in range(search_idx, len(lines)):
        stripped = lines[idx].strip()
        if stripped.startswith("## ") and idx > start_idx:
            break
        if stripped.startswith("|"):
            table_start = idx
            break

    if table_start is None:
        return None

    table_lines = []
    for idx in range(table_start, len(lines)):
        stripped = lines[idx].strip()
        if not stripped.startswith("|"):
            break
        table_lines.append(stripped)

    if len(table_lines) < 2:
        return None

    headers = [h.strip() for h in table_lines[0].strip("|").split("|")]
    rows = []
    for row in table_lines[2:]:
        values = [v.strip() for v in row.strip("|").split("|")]
        if len(values) == len(headers):
            rows.append(values)

    if not rows:
        return None

    return pd.DataFrame(rows, columns=headers)


def save_requested_comparison_plot(md_file):
    series_specs = [
        ("## Step 1 - Standard Multiplication (i-j-k)", "C++", "Standard Multiplication (i-j-k)"),
        ("## Step 2 - Line-by-Line Multiplication (i-k-j)", "C++", "Line-by-Line Multiplication (i-k-j)"),
        ("## Step 1 - Standard Multiplication (i-j-k) V1", None, "Standard Multiplication (i-j-k) V1"),
        ("## Step 2 - Standard Multiplication (i-j-k) V2", None, "Standard Multiplication (i-j-k) V2"),
        ("## Step 3 - Line-by-Line Multiplication (i-k-j) V1", None, "Line-by-Line Multiplication (i-k-j) V1"),
        ("## Step 4 - Line-by-Line Multiplication (i-k-j) V2", None, "Line-by-Line Multiplication (i-k-j) V2"),
    ]

    plt.figure(figsize=(10, 6))
    plotted = 0

    for section_heading, language, label in series_specs:
        df = extract_table_from_section(md_file, section_heading, language)
        if df is None or "Matrix Size" not in df.columns or "Time (s)" not in df.columns:
            continue

        prepared = prepare_df(df, step="comparison")
        prepared = prepared.sort_values("N")
        plt.plot(prepared["N"], prepared["Time (s)"], marker="o", label=label)
        plotted += 1

    if plotted == 0:
        return

    plt.xlabel("Matrix Size (N)")
    plt.ylabel("Time (s)")
    plt.title("Time vs Matrix Size - Requested Algorithm Comparison")
    plt.grid(True)
    plt.legend()
    plt.tight_layout()
    plt.savefig("requested_time_comparison.png")
    plt.close()


def save_requested_gflops_plot(md_file):
    series_specs = [
        ("## Step 1 - Standard Multiplication (i-j-k)", "C++", "Standard Multiplication (i-j-k)"),
        ("## Step 2 - Line-by-Line Multiplication (i-k-j)", "C++", "Line-by-Line Multiplication (i-k-j)"),
        ("## Step 1 - Standard Multiplication (i-j-k) V1", None, "Standard Multiplication (i-j-k) V1"),
        ("## Step 2 - Standard Multiplication (i-j-k) V2", None, "Standard Multiplication (i-j-k) V2"),
        ("## Step 3 - Line-by-Line Multiplication (i-k-j) V1", None, "Line-by-Line Multiplication (i-k-j) V1"),
        ("## Step 4 - Line-by-Line Multiplication (i-k-j) V2", None, "Line-by-Line Multiplication (i-k-j) V2"),
    ]

    plt.figure(figsize=(10, 6))
    plotted = 0

    for section_heading, language, label in series_specs:
        df = extract_table_from_section(md_file, section_heading, language)
        if df is None or "Matrix Size" not in df.columns or "Time (s)" not in df.columns:
            continue

        prepared = prepare_df(df, step="comparison")
        prepared = prepared.sort_values("N")
        plt.plot(prepared["N"], prepared["GFLOPS"], marker="o", label=label)
        plotted += 1

    if plotted == 0:
        return

    plt.xlabel("Matrix Size (N)")
    plt.ylabel("GFlops")
    plt.title("GFlops vs Matrix Size - Requested Algorithm Comparison")
    plt.grid(True)
    plt.legend()
    plt.tight_layout()
    plt.savefig("requested_gflops_comparison.png")
    plt.close()


def save_requested_speedup_efficiency_plots(md_file, threads=4):
    std_seq = extract_table_from_section(md_file, "## Step 1 - Standard Multiplication (i-j-k)", "C++")
    line_seq = extract_table_from_section(md_file, "## Step 2 - Line-by-Line Multiplication (i-k-j)", "C++")
    std_v1 = extract_table_from_section(md_file, "## Step 1 - Standard Multiplication (i-j-k) V1")
    std_v2 = extract_table_from_section(md_file, "## Step 2 - Standard Multiplication (i-j-k) V2")
    line_v1 = extract_table_from_section(md_file, "## Step 3 - Line-by-Line Multiplication (i-k-j) V1")
    line_v2 = extract_table_from_section(md_file, "## Step 4 - Line-by-Line Multiplication (i-k-j) V2")

    required = [std_seq, line_seq, std_v1, std_v2, line_v1, line_v2]
    if any(df is None for df in required):
        return

    std_seq = prepare_df(std_seq, step="comparison")[ ["N", "Time (s)"] ].rename(columns={"Time (s)": "T_seq_std"})
    line_seq = prepare_df(line_seq, step="comparison")[ ["N", "Time (s)"] ].rename(columns={"Time (s)": "T_seq_line"})
    std_v1 = prepare_df(std_v1, step="comparison")[ ["N", "Time (s)"] ].rename(columns={"Time (s)": "T_std_v1"})
    std_v2 = prepare_df(std_v2, step="comparison")[ ["N", "Time (s)"] ].rename(columns={"Time (s)": "T_std_v2"})
    line_v1 = prepare_df(line_v1, step="comparison")[ ["N", "Time (s)"] ].rename(columns={"Time (s)": "T_line_v1"})
    line_v2 = prepare_df(line_v2, step="comparison")[ ["N", "Time (s)"] ].rename(columns={"Time (s)": "T_line_v2"})

    merged = std_seq.merge(line_seq, on="N", how="inner")
    merged = merged.merge(std_v1, on="N", how="inner")
    merged = merged.merge(std_v2, on="N", how="inner")
    merged = merged.merge(line_v1, on="N", how="inner")
    merged = merged.merge(line_v2, on="N", how="inner")
    merged = merged.sort_values("N")

    merged["Speedup Standard V1"] = merged["T_seq_std"] / merged["T_std_v1"]
    merged["Speedup Standard V2"] = merged["T_seq_std"] / merged["T_std_v2"]
    merged["Speedup Line V1"] = merged["T_seq_line"] / merged["T_line_v1"]
    merged["Speedup Line V2"] = merged["T_seq_line"] / merged["T_line_v2"]

    plt.figure(figsize=(10, 6))
    plt.plot(merged["N"], merged["Speedup Standard V1"], marker="o", label="Standard V1")
    plt.plot(merged["N"], merged["Speedup Standard V2"], marker="o", label="Standard V2")
    plt.plot(merged["N"], merged["Speedup Line V1"], marker="o", label="Line-by-Line V1")
    plt.plot(merged["N"], merged["Speedup Line V2"], marker="o", label="Line-by-Line V2")
    plt.xlabel("Matrix Size (N)")
    plt.ylabel("Speedup")
    plt.title("Speedup vs Matrix Size (4 Threads)")
    plt.grid(True)
    plt.legend()
    plt.tight_layout()
    plt.savefig("requested_speedup_comparison.png")
    plt.close()

    merged["Efficiency Standard V1"] = merged["Speedup Standard V1"] / threads
    merged["Efficiency Standard V2"] = merged["Speedup Standard V2"] / threads
    merged["Efficiency Line V1"] = merged["Speedup Line V1"] / threads
    merged["Efficiency Line V2"] = merged["Speedup Line V2"] / threads

    plt.figure(figsize=(10, 6))
    plt.plot(merged["N"], merged["Efficiency Standard V1"], marker="o", label="Standard V1")
    plt.plot(merged["N"], merged["Efficiency Standard V2"], marker="o", label="Standard V2")
    plt.plot(merged["N"], merged["Efficiency Line V1"], marker="o", label="Line-by-Line V1")
    plt.plot(merged["N"], merged["Efficiency Line V2"], marker="o", label="Line-by-Line V2")
    plt.axhline(1.0, color="gray", linestyle="--", linewidth=1)
    plt.xlabel("Matrix Size (N)")
    plt.ylabel("Efficiency")
    plt.title("Efficiency vs Matrix Size (4 Threads)")
    plt.grid(True)
    plt.legend()
    plt.tight_layout()
    plt.savefig("requested_efficiency_comparison.png")
    plt.close()


def load_assignment_series(md_file):
    """Load only the six assignment series (N=1024..3072) with explicit labels."""
    specs = [
        ("Standard Multiplication (i-j-k)", "## Step 1 - Standard Multiplication (i-j-k)", "C++"),
        ("Line-by-Line Multiplication (i-k-j)", "## Step 2 - Line-by-Line Multiplication (i-k-j)", "C++"),
        ("Standard Multiplication (i-j-k) V1", "## Step 1 - Standard Multiplication (i-j-k) V1", None),
        ("Standard Multiplication (i-j-k) V2", "## Step 2 - Standard Multiplication (i-j-k) V2", None),
        ("Line-by-Line Multiplication (i-k-j) V1", "## Step 3 - Line-by-Line Multiplication (i-k-j) V1", None),
        ("Line-by-Line Multiplication (i-k-j) V2", "## Step 4 - Line-by-Line Multiplication (i-k-j) V2", None),
    ]

    frames = {}
    for label, heading, lang in specs:
        raw = extract_table_from_section(md_file, heading, lang)
        if raw is None:
            continue
        df = prepare_df(raw, step="assignment").sort_values("N")
        df = df[df["N"].isin([1024, 1536, 2048, 2560, 3072])]

        # Use Time/Thread when available for non-scaling views (time and throughput plots).
        if "Time/Thread (s)" in df.columns:
            effective_time = df["Time/Thread (s)"]
        else:
            effective_time = df["Time (s)"]

        temp = df[["N", "Time (s)"]].copy()
        temp["Effective Time (s)"] = effective_time
        temp["GFLOPS Effective"] = (2 * (temp["N"] ** 3)) / (temp["Effective Time (s)"] * 1e9)
        frames[label] = temp

    return frames


def save_assignment_graphs(md_file, threads=4):
    """Generate the four required assignment graphs with explicit formulas."""
    data = load_assignment_series(md_file)
    required = [
        "Standard Multiplication (i-j-k)",
        "Line-by-Line Multiplication (i-k-j)",
        "Standard Multiplication (i-j-k) V1",
        "Standard Multiplication (i-j-k) V2",
        "Line-by-Line Multiplication (i-k-j) V1",
        "Line-by-Line Multiplication (i-k-j) V2",
    ]

    if any(k not in data for k in required):
        return

    # 1) Time vs N (all six)
    plt.figure(figsize=(10, 6))
    for label in required:
        df = data[label]
        plt.plot(df["N"], df["Effective Time (s)"], marker="o", linewidth=2, label=label)
    plt.xlabel("Matrix Size (N)")
    plt.ylabel("Time (s)")
    plt.title("Time vs Matrix Size (Time/Thread for Parallel Series)")
    plt.grid(True, alpha=0.3)
    plt.legend(fontsize=8)
    plt.tight_layout()
    plt.savefig("requested_time_comparison.png")
    plt.close()

    # 2) GFlops vs N (all six)
    plt.figure(figsize=(10, 6))
    for label in required:
        df = data[label]
        plt.plot(df["N"], df["GFLOPS Effective"], marker="o", linewidth=2, label=label)
    plt.xlabel("Matrix Size (N)")
    plt.ylabel("GFlops")
    plt.title("GFlops vs Matrix Size (Using Time/Thread for Parallel Series)")
    plt.grid(True, alpha=0.3)
    plt.legend(fontsize=8)
    plt.tight_layout()
    plt.savefig("requested_gflops_comparison.png")
    plt.close()

    seq_std = data["Standard Multiplication (i-j-k)"].rename(columns={"Time (s)": "T_seq_std"})[["N", "T_seq_std"]]
    seq_line = data["Line-by-Line Multiplication (i-k-j)"].rename(columns={"Time (s)": "T_seq_line"})[["N", "T_seq_line"]]
    p_std_v1 = data["Standard Multiplication (i-j-k) V1"].rename(columns={"Time (s)": "T_std_v1"})[["N", "T_std_v1"]]
    p_std_v2 = data["Standard Multiplication (i-j-k) V2"].rename(columns={"Time (s)": "T_std_v2"})[["N", "T_std_v2"]]
    p_line_v1 = data["Line-by-Line Multiplication (i-k-j) V1"].rename(columns={"Time (s)": "T_line_v1"})[["N", "T_line_v1"]]
    p_line_v2 = data["Line-by-Line Multiplication (i-k-j) V2"].rename(columns={"Time (s)": "T_line_v2"})[["N", "T_line_v2"]]

    merged = seq_std.merge(seq_line, on="N", how="inner")
    merged = merged.merge(p_std_v1, on="N", how="inner")
    merged = merged.merge(p_std_v2, on="N", how="inner")
    merged = merged.merge(p_line_v1, on="N", how="inner")
    merged = merged.merge(p_line_v2, on="N", how="inner")
    merged = merged.sort_values("N")

    merged["Standard V1"] = merged["T_seq_std"] / merged["T_std_v1"]
    merged["Standard V2"] = merged["T_seq_std"] / merged["T_std_v2"]
    merged["Line-by-Line V1"] = merged["T_seq_line"] / merged["T_line_v1"]
    merged["Line-by-Line V2"] = merged["T_seq_line"] / merged["T_line_v2"]

    # 3) Speedup vs N (four parallel variants)
    plt.figure(figsize=(10, 6))
    for col in ["Standard V1", "Standard V2", "Line-by-Line V1", "Line-by-Line V2"]:
        plt.plot(merged["N"], merged[col], marker="o", linewidth=2, label=col)
    plt.xlabel("Matrix Size (N)")
    plt.ylabel("Speedup")
    plt.title("Speedup vs Matrix Size (4 Threads)")
    plt.grid(True, alpha=0.3)
    plt.legend()
    plt.tight_layout()
    plt.savefig("requested_speedup_comparison.png")
    plt.close()

    # 4) Efficiency vs N (four parallel variants)
    plt.figure(figsize=(10, 6))
    for col in ["Standard V1", "Standard V2", "Line-by-Line V1", "Line-by-Line V2"]:
        plt.plot(merged["N"], merged[col] / threads, marker="o", linewidth=2, label=col)
    plt.axhline(1.0, color="gray", linestyle="--", linewidth=1)
    plt.xlabel("Matrix Size (N)")
    plt.ylabel("Efficiency")
    plt.title("Efficiency vs Matrix Size (4 Threads)")
    plt.grid(True, alpha=0.3)
    plt.legend()
    plt.tight_layout()
    plt.savefig("requested_efficiency_comparison.png")
    plt.close()


def save_part2_cache_miss_graphs(md_file):
    """Plot Part 2 cache misses by matrix size for Steps 1-4 only."""
    specs = [
        ("Standard Multiplication (i-j-k) V1", "## Step 1 - Standard Multiplication (i-j-k) V1"),
        ("Standard Multiplication (i-j-k) V2", "## Step 2 - Standard Multiplication (i-j-k) V2"),
        ("Line-by-Line Multiplication (i-k-j) V1", "## Step 3 - Line-by-Line Multiplication (i-k-j) V1"),
        ("Line-by-Line Multiplication (i-k-j) V2", "## Step 4 - Line-by-Line Multiplication (i-k-j) V2"),
    ]

    series = {}
    for label, heading in specs:
        raw = extract_table_from_section(md_file, heading)
        if raw is None:
            continue
        df = prepare_df(raw, step="part2_cache").sort_values("N")
        df = df[df["N"].isin([1024, 1536, 2048, 2560, 3072])]
        if "L1 Misses" in df.columns and "L2 Misses" in df.columns:
            series[label] = df[["N", "L1 Misses", "L2 Misses"]]

    if not series:
        return

    plt.figure(figsize=(10, 6))
    for label, df in series.items():
        plt.plot(df["N"], df["L1 Misses"], marker="o", linewidth=2, label=label)
    plt.xlabel("Matrix Size (N)")
    plt.ylabel("L1 Misses")
    plt.title("Part 2 Cache Misses (L1) vs Matrix Size (Steps 1-4)")
    plt.grid(True, alpha=0.3)
    plt.legend(fontsize=8)
    plt.tight_layout()
    plt.savefig("requested_part2_l1_misses_comparison.png")
    plt.close()

    plt.figure(figsize=(10, 6))
    for label, df in series.items():
        plt.plot(df["N"], df["L2 Misses"], marker="o", linewidth=2, label=label)
    plt.xlabel("Matrix Size (N)")
    plt.ylabel("L2 Misses")
    plt.title("Part 2 Cache Misses (L2) vs Matrix Size (Steps 1-4)")
    plt.grid(True, alpha=0.3)
    plt.legend(fontsize=8)
    plt.tight_layout()
    plt.savefig("requested_part2_l2_misses_comparison.png")
    plt.close()


def _load_step_thread_series(md_file, section_heading):
    """Load a thread-scaling table with columns Number of Threads and Time (s)."""
    raw = extract_table_from_section(md_file, section_heading)
    if raw is None:
        return None

    if "Number of Threads" not in raw.columns or "Time (s)" not in raw.columns:
        return None

    df = raw.copy()
    df["Threads"] = clean_numeric_series(df["Number of Threads"])
    df["Time (s)"] = clean_numeric_series(df["Time (s)"])
    if "Time/Thread (s)" in df.columns:
        df["Time/Thread (s)"] = clean_numeric_series(df["Time/Thread (s)"])
    df = df.dropna(subset=["Threads", "Time (s)"]).sort_values("Threads")
    if df.empty:
        return None

    return df


def save_step5_v3_thread_scaling_graphs(md_file):
    """Create GFLOPS and speedup graphs for the current OnMultLineParallel_3 thread sweep."""
    heading = "## Step 5 - Line-by-Line Multiplication (i-k-j) V3"
    df = _load_step_thread_series(md_file, heading)
    if df is None:
        return

    n = 8192
    df["GFLOPS"] = (2 * (n ** 3)) / (df["Time (s)"] * 1e9)

    base = df[df["Threads"] == 4]
    if base.empty:
        return
    t4 = float(base.iloc[0]["Time (s)"])
    df["Speedup_vs_4t"] = t4 / df["Time (s)"]

    plt.figure(figsize=(9, 5))
    plt.plot(df["Threads"], df["GFLOPS"], marker="o", linewidth=2)
    plt.xlabel("Number of Threads")
    plt.ylabel("GFlops")
    plt.title("OnMultLineParallel_3 (N=8192): GFlops vs Threads")
    plt.grid(True, alpha=0.3)
    plt.tight_layout()
    plt.savefig("requested_step5_v3_gflops_vs_threads.png")
    plt.close()

    if "Time/Thread (s)" in df.columns:
        valid = df.dropna(subset=["Time/Thread (s)"]).copy()
        if not valid.empty:
            valid["GFLOPS_time_per_thread"] = (2 * (n ** 3)) / (valid["Time/Thread (s)"] * 1e9)

            plt.figure(figsize=(9, 5))
            plt.plot(valid["Threads"], valid["GFLOPS_time_per_thread"], marker="o", linewidth=2)
            plt.xlabel("Number of Threads")
            plt.ylabel("GFlops")
            plt.title("OnMultLineParallel_3 (N=8192): GFlops vs Threads (using Time/Thread)")
            plt.grid(True, alpha=0.3)
            plt.tight_layout()
            plt.savefig("requested_step5_v3_gflops_vs_threads_time_per_thread.png")
            plt.close()

    plt.figure(figsize=(9, 5))
    plt.plot(df["Threads"], df["Speedup_vs_4t"], marker="o", linewidth=2)
    plt.axhline(1.0, color="gray", linestyle="--", linewidth=1)
    plt.xlabel("Number of Threads")
    plt.ylabel("Speedup (vs 4 threads)")
    plt.title("OnMultLineParallel_3 (N=8192): Speedup vs Threads")
    plt.grid(True, alpha=0.3)
    plt.tight_layout()
    plt.savefig("requested_step5_v3_speedup_vs_threads.png")
    plt.close()

    if "Time/Thread (s)" in df.columns:
        valid = df.dropna(subset=["Time/Thread (s)"]).copy()
        if not valid.empty:
            base_tp = valid[valid["Threads"] == 4]
            if not base_tp.empty:
                t4_tp = float(base_tp.iloc[0]["Time/Thread (s)"])
                valid["Speedup_vs_4t_time_per_thread"] = t4_tp / valid["Time/Thread (s)"]

                plt.figure(figsize=(9, 5))
                plt.plot(valid["Threads"], valid["Speedup_vs_4t_time_per_thread"], marker="o", linewidth=2)
                plt.axhline(1.0, color="gray", linestyle="--", linewidth=1)
                plt.xlabel("Number of Threads")
                plt.ylabel("Speedup (vs 4 threads)")
                plt.title("OnMultLineParallel_3 (N=8192): Speedup vs Threads (using Time/Thread)")
                plt.grid(True, alpha=0.3)
                plt.tight_layout()
                plt.savefig("requested_step5_v3_speedup_vs_threads_time_per_thread.png")
                plt.close()

                if "Time/Thread (s)" in df.columns:
                    valid = df.dropna(subset=["Time/Thread (s)"]).copy()
                    if not valid.empty:
                        plt.figure(figsize=(9, 5))
                        plt.plot(valid["Threads"], valid["Time (s)"], marker="o", linewidth=2, label="Raw Time (s)")
                        plt.plot(valid["Threads"], valid["Time/Thread (s)"], marker="o", linewidth=2, label="Time/Thread (s)")
                        plt.xlabel("Number of Threads")
                        plt.ylabel("Time (s)")
                        plt.title("OnMultLineParallel_3 (N=8192): Raw Time vs Time/Thread")
                        plt.grid(True, alpha=0.3)
                        plt.legend()
                        plt.tight_layout()
                        plt.savefig("requested_step5_v3_time_vs_time_per_thread.png")
                        plt.close()


def save_step5_directive_comparison_graphs(md_file):
    """
    Compare thread scaling across directive variants if tables exist.
    Expected headings (if you add them to performance_values.md):
      - ## Step 5 - Line-by-Line Multiplication (i-k-j) V3
      - ## Step 6 - Line-by-Line Multiplication (i-k-j) V3 - for simd
      - ## Step 7 - Line-by-Line Multiplication (i-k-j) V3 - collapse(2)
    """
    candidates = [
        ("OnMultLineParallel_3", "## Step 5 - Line-by-Line Multiplication (i-k-j) V3"),
        ("for simd", "## Step 6 - Line-by-Line Multiplication (i-k-j) V3 - for simd"),
        ("collapse(2)", "## Step 7 - Line-by-Line Multiplication (i-k-j) V3 - collapse(2)"),
    ]

    loaded = []
    n = 8192
    for label, heading in candidates:
        df = _load_step_thread_series(md_file, heading)
        if df is None:
            continue
        df = df.copy()
        df["GFLOPS"] = (2 * (n ** 3)) / (df["Time (s)"] * 1e9)
        base = df[df["Threads"] == 4]
        if base.empty:
            continue
        t4 = float(base.iloc[0]["Time (s)"])
        df["Speedup_vs_4t"] = t4 / df["Time (s)"]
        loaded.append((label, df))

    if not loaded:
        return

    plt.figure(figsize=(10, 6))
    for label, df in loaded:
        plt.plot(df["Threads"], df["GFLOPS"], marker="o", linewidth=2, label=label)
    plt.xlabel("Number of Threads")
    plt.ylabel("GFlops")
    plt.title("N=8192: Directive Comparison (GFlops vs Threads)")
    plt.grid(True, alpha=0.3)
    plt.legend()
    plt.tight_layout()
    plt.savefig("requested_step5_directives_gflops_vs_threads.png")
    plt.close()

    plt.figure(figsize=(10, 6))
    for label, df in loaded:
        plt.plot(df["Threads"], df["Speedup_vs_4t"], marker="o", linewidth=2, label=label)
    plt.axhline(1.0, color="gray", linestyle="--", linewidth=1)
    plt.xlabel("Number of Threads")
    plt.ylabel("Speedup (vs 4 threads)")
    plt.title("N=8192: Directive Comparison (Speedup vs Threads)")
    plt.grid(True, alpha=0.3)
    plt.legend()
    plt.tight_layout()
    plt.savefig("requested_step5_directives_speedup_vs_threads.png")
    plt.close()


def save_step5_comparison_raw_vs_normalized_metrics(md_file):
    """Compare speedup and GFlops calculated from raw time vs time/thread basis."""
    heading = "## Step 5 - Line-by-Line Multiplication (i-k-j) V3"
    df = _load_step_thread_series(md_file, heading)
    if df is None:
        return

    n = 8192
    
    # Calculate metrics using raw time
    df["GFLOPS_raw"] = (2 * (n ** 3)) / (df["Time (s)"] * 1e9)
    base = df[df["Threads"] == 4]
    if base.empty:
        return
    t4 = float(base.iloc[0]["Time (s)"])
    df["Speedup_raw"] = t4 / df["Time (s)"]

    # Calculate metrics using time/thread (if available)
    if "Time/Thread (s)" in df.columns:
        valid = df.dropna(subset=["Time/Thread (s)"]).copy()
        if not valid.empty:
            valid["GFLOPS_normalized"] = (2 * (n ** 3)) / (valid["Time/Thread (s)"] * 1e9)
            base_tp = valid[valid["Threads"] == 4]
            if not base_tp.empty:
                t4_tp = float(base_tp.iloc[0]["Time/Thread (s)"])
                valid["Speedup_normalized"] = t4_tp / valid["Time/Thread (s)"]

                # Create comparison graph for speedup
                plt.figure(figsize=(9, 5))
                plt.plot(valid["Threads"], valid["Speedup_raw"], marker="o", linewidth=2, label="Speedup (Raw Time)")
                plt.plot(valid["Threads"], valid["Speedup_normalized"], marker="s", linewidth=2, label="Speedup (Time/Thread)")
                plt.axhline(1.0, color="gray", linestyle="--", linewidth=1)
                plt.xlabel("Number of Threads")
                plt.ylabel("Speedup")
                plt.title("OnMultLineParallel_3 (N=8192): Speedup Comparison (Raw vs Time/Thread)")
                plt.grid(True, alpha=0.3)
                plt.legend()
                plt.tight_layout()
                plt.savefig("requested_step5_comparison_speedup.png")
                plt.close()

                # Create comparison graph for GFlops
                plt.figure(figsize=(9, 5))
                plt.plot(valid["Threads"], valid["GFLOPS_raw"], marker="o", linewidth=2, label="GFlops (Raw Time)")
                plt.plot(valid["Threads"], valid["GFLOPS_normalized"], marker="s", linewidth=2, label="GFlops (Time/Thread)")
                plt.xlabel("Number of Threads")
                plt.ylabel("GFlops")
                plt.title("OnMultLineParallel_3 (N=8192): GFlops Comparison (Raw vs Time/Thread)")
                plt.grid(True, alpha=0.3)
                plt.legend()
                plt.tight_layout()
                plt.savefig("requested_step5_comparison_gflops.png")
                plt.close()


def main():
    parsed = parse_markdown_tables(MD_FILE)

    data = {}
    for step, lang, df in parsed:
        prepared = prepare_df(df, step)
        data[(step, lang)] = prepared

    # Step 1 and Step 2
    cpp_s1 = data.get(("Step 1", "C++"))
    java_s1 = data.get(("Step 1", "Java"))
    cpp_s2 = data.get(("Step 2", "C++"))
    java_s2 = data.get(("Step 2", "Java"))
    cpp_s3 = data.get(("Step 3", "C++"))

    # Step 1 plots
    if cpp_s1 is not None and java_s1 is not None:
        save_line_plot(
            cpp_s1["N"], cpp_s1["Time (s)"], "C++",
            java_s1["N"], java_s1["Time (s)"], "Java",
            xlabel="Matrix Size (N)",
            ylabel="Time (s)",
            title="Step 1 - Standard Multiplication: Time",
            filename="step1_time.png"
        )

        save_line_plot(
            cpp_s1["N"], cpp_s1["GFLOPS"], "C++",
            java_s1["N"], java_s1["GFLOPS"], "Java",
            xlabel="Matrix Size (N)",
            ylabel="GFLOPS",
            title="Step 1 - Standard Multiplication: Performance",
            filename="step1_gflops.png"
        )

        save_line_plot(
            cpp_s1["N"], cpp_s1["L1 Misses"], "C++",
            java_s1["N"], java_s1["L1 Misses"], "Java",
            xlabel="Matrix Size (N)",
            ylabel="L1 Misses",
            title="Step 1 - Standard Multiplication: L1 Misses",
            filename="step1_l1.png"
        )

        save_line_plot(
            cpp_s1["N"], cpp_s1["L2 Misses"], "C++",
            java_s1["N"], java_s1["L2 Misses"], "Java",
            xlabel="Matrix Size (N)",
            ylabel="L2 Misses",
            title="Step 1 - Standard Multiplication: L2 Misses",
            filename="step1_l2.png"
        )

    # Step 2 plots
    if cpp_s2 is not None and java_s2 is not None:
        save_line_plot(
            cpp_s2["N"], cpp_s2["Time (s)"], "C++",
            java_s2["N"], java_s2["Time (s)"], "Java",
            xlabel="Matrix Size (N)",
            ylabel="Time (s)",
            title="Step 2 - Line-by-Line Multiplication: Time",
            filename="step2_time.png"
        )

        save_line_plot(
            cpp_s2["N"], cpp_s2["GFLOPS"], "C++",
            java_s2["N"], java_s2["GFLOPS"], "Java",
            xlabel="Matrix Size (N)",
            ylabel="GFLOPS",
            title="Step 2 - Line-by-Line Multiplication: Performance",
            filename="step2_gflops.png"
        )

        save_line_plot(
            cpp_s2["N"], cpp_s2["L1 Misses"], "C++",
            java_s2["N"], java_s2["L1 Misses"], "Java",
            xlabel="Matrix Size (N)",
            ylabel="L1 Misses",
            title="Step 2 - Line-by-Line Multiplication: L1 Misses",
            filename="step2_l1.png"
        )

        save_line_plot(
            cpp_s2["N"], cpp_s2["L2 Misses"], "C++",
            java_s2["N"], java_s2["L2 Misses"], "Java",
            xlabel="Matrix Size (N)",
            ylabel="L2 Misses",
            title="Step 2 - Line-by-Line Multiplication: L2 Misses",
            filename="step2_l2.png"
        )

    # Step 1 vs Step 2 speedup
    if cpp_s1 is not None and cpp_s2 is not None:
        cpp_speedup = pd.DataFrame({
            "N": cpp_s1["N"],
            "Speedup": cpp_s1["Time (s)"].values / cpp_s2["Time (s)"].values
        })

        if java_s1 is not None and java_s2 is not None:
            java_speedup = pd.DataFrame({
                "N": java_s1["N"],
                "Speedup": java_s1["Time (s)"].values / java_s2["Time (s)"].values
            })

            save_line_plot(
                cpp_speedup["N"], cpp_speedup["Speedup"], "C++",
                java_speedup["N"], java_speedup["Speedup"], "Java",
                xlabel="Matrix Size (N)",
                ylabel="Speedup (Step 1 / Step 2)",
                title="Speedup of Line-by-Line over Standard Multiplication",
                filename="step12_speedup.png"
            )
        else:
            save_line_plot(
                cpp_speedup["N"], cpp_speedup["Speedup"], "C++",
                xlabel="Matrix Size (N)",
                ylabel="Speedup (Step 1 / Step 2)",
                title="Speedup of Line-by-Line over Standard Multiplication",
                filename="step12_speedup.png"
            )

    # Step 1 vs Step 2 comparison (C++)
    if cpp_s1 is not None and cpp_s2 is not None:
        save_line_plot(
            cpp_s1["N"], cpp_s1["Time (s)"], "Step 1",
            cpp_s2["N"], cpp_s2["Time (s)"], "Step 2",
            xlabel="Matrix Size (N)",
            ylabel="Time (s)",
            title="C++ - Step 1 vs Step 2: Time",
            filename="cpp_step1_vs_step2_time.png"
        )

        save_line_plot(
            cpp_s1["N"], cpp_s1["L1 Misses"], "Step 1",
            cpp_s2["N"], cpp_s2["L1 Misses"], "Step 2",
            xlabel="Matrix Size (N)",
            ylabel="L1 Misses",
            title="C++ - Step 1 vs Step 2: L1 Misses",
            filename="cpp_step1_vs_step2_l1.png"
        )

        save_line_plot(
            cpp_s1["N"], cpp_s1["L2 Misses"], "Step 1",
            cpp_s2["N"], cpp_s2["L2 Misses"], "Step 2",
            xlabel="Matrix Size (N)",
            ylabel="L2 Misses",
            title="C++ - Step 1 vs Step 2: L2 Misses",
            filename="cpp_step1_vs_step2_l2.png"
        )

    # Step 1 vs Step 2 comparison (Java)
    if java_s1 is not None and java_s2 is not None:
        save_line_plot(
            java_s1["N"], java_s1["Time (s)"], "Step 1",
            java_s2["N"], java_s2["Time (s)"], "Step 2",
            xlabel="Matrix Size (N)",
            ylabel="Time (s)",
            title="Java - Step 1 vs Step 2: Time",
            filename="java_step1_vs_step2_time.png"
        )

        save_line_plot(
            java_s1["N"], java_s1["L1 Misses"], "Step 1",
            java_s2["N"], java_s2["L1 Misses"], "Step 2",
            xlabel="Matrix Size (N)",
            ylabel="L1 Misses",
            title="Java - Step 1 vs Step 2: L1 Misses",
            filename="java_step1_vs_step2_l1.png"
        )

        save_line_plot(
            java_s1["N"], java_s1["L2 Misses"], "Step 1",
            java_s2["N"], java_s2["L2 Misses"], "Step 2",
            xlabel="Matrix Size (N)",
            ylabel="L2 Misses",
            title="Java - Step 1 vs Step 2: L2 Misses",
            filename="java_step1_vs_step2_l2.png"
        )

    # Step 3 plots
    if cpp_s3 is not None:
        save_multi_series_block_plot(
            cpp_s3, "Time (s)",
            ylabel="Time (s)",
            title="Step 3 - Block Multiplication: Time vs Block Size",
            filename="step3_time_vs_block.png"
        )

        save_multi_series_block_plot(
            cpp_s3, "GFLOPS",
            ylabel="GFLOPS",
            title="Step 3 - Block Multiplication: Performance vs Block Size",
            filename="step3_gflops_vs_block.png"
        )

        save_multi_series_block_plot(
            cpp_s3, "L1 Misses",
            ylabel="L1 Misses",
            title="Step 3 - Block Multiplication: L1 Misses vs Block Size",
            filename="step3_l1_vs_block.png"
        )

        save_multi_series_block_plot(
            cpp_s3, "L2 Misses",
            ylabel="L2 Misses",
            title="Step 3 - Block Multiplication: L2 Misses vs Block Size",
            filename="step3_l2_vs_block.png"
        )

    # Use an explicit assignment-focused pipeline for the requested comparison graphs.
    save_assignment_graphs(MD_FILE, threads=4)
    save_part2_cache_miss_graphs(MD_FILE)
    save_step5_v3_thread_scaling_graphs(MD_FILE)
    save_step5_directive_comparison_graphs(MD_FILE)
    save_step5_comparison_raw_vs_normalized_metrics(MD_FILE)

    print("Graphs generated successfully.")


if __name__ == "__main__":
    main()