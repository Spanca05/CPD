# Parallel and Distributed Computing Projects

Two projects developed for the Parallel and Distributed Computing course at FEUP (University of Porto), 2025/2026.

| Project | Topic | Stack |
|---|---|---|
| [Matrix Multiplication Performance](#1-matrix-multiplication-performance) | Performance analysis, parallel computing | C++ · Java · OpenMP |
| [Secure Chat](#2-secure-chat) | Concurrent client-server system | Java 21 · TLS |

---

## 1. Matrix Multiplication Performance

Study of how memory access patterns and parallelism affect performance, using matrix multiplication in C++ and Java.

- **Three algorithms:** standard (i-j-k), line-by-line (i-k-j) and block multiplication
- **Single-core evaluation:** execution time and L1/L2 cache misses for matrices from 1024×1024 to 3072×3072
- **Multi-core evaluation:** several parallel versions with OpenMP, compared on matrices up to 8192×8192 with 4 to 24 threads
- **Key result:** changing the loop order to improve cache locality made the C++ version about **5× faster** on a single core

Code in [`assign1/src`](assign1/src) · results in [`assign1/doc/performance_values.md`](assign1/doc/performance_values.md)

---

## 2. Secure Chat

Client-server chat application in Java, with chat rooms, authentication and AI-assisted rooms.

- **Concurrency:** each client is handled on a **virtual thread**, with thread-safe lists, maps and queues implemented from scratch
- **Security:** all traffic encrypted with **TLS**; passwords hashed with **PBKDF2 + salt** and compared in constant time
- **Sessions:** token-based sessions with expiry and reconnection (users get back to the room they were in)
- **Custom protocol:** text-based command protocol with its own parser
- **AI rooms:** the conversation history is sent to a local language model (Ollama) asynchronously, through a queue that prevents overloading it

How to run: see [`assign2/README.md`](assign2/README.md)

---

## Authors
Vasco Vieira · Leonardo Carvalho
