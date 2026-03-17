import re
import pandas as pd
import matplotlib.pyplot as plt


MD_FILE = "performance_values.md"


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

    print("Graphs generated successfully.")


if __name__ == "__main__":
    main()