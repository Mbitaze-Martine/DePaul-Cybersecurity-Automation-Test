📘 File Retrieval Engine (Multithreaded Indexing System)
Author: YourName
Course: Programming Assignment 3 – Multithreaded File Retrieval
Date: October 2025
Language: Java (JDK 17)
Build Tool: Apache Maven 3.9.6

🧾 Overview

This project implements a multithreaded file retrieval and indexing engine capable of efficiently processing and indexing large text datasets from the Gutenberg Project.
The system builds an inverted index from thousands of .txt files and allows keyword-based search queries using AND operators (up to three terms).

The main objectives of the assignment are:

To evaluate indexing performance using 1, 2, 4, and 8 worker threads.

To measure execution time and throughput (MB/s).

To ensure thread-safe updates of the global index.

To support multi-term AND search queries returning the top 10 relevant documents.

⚙️ Project Structure
AppEngine/
│
├── pom.xml                     # Maven build configuration
├── target/                     # Compiled JAR files
├── src/
│   ├── main/java/frengine/
│   │   ├── AppInterface.java   # CLI entry point for the engine
│   │   ├── ProcessingEngine.java  # Thread management and file crawling
│   │   ├── IndexStore.java     # Thread-safe global inverted index
│   │   └── FileWorker.java     # Local index creation and merging
│   └── test/                   # Optional unit tests (if any)
│
├── dataset1/                   # Gutenberg Dataset 1
├── dataset2/                   # Gutenberg Dataset 2
├── dataset3/                   # Gutenberg Dataset 3
│
└── README.md                   # Project documentation (this file)

💡 Features

🧩 Recursive directory crawling (includes all subfolders)

⚙️ Configurable number of worker threads (1–8)

🔒 Thread-safe index updates using ReentrantReadWriteLock

⚡ High throughput via local index creation and batch merging

🔍 Multi-term AND search support (up to three terms)

📊 Performance metrics: time (s), throughput (MB/s), number of files

🧰 Requirements
Tool	Version	Purpose
Java	17 or higher	Compilation & Execution
Maven	3.8+	Build automation
OS	Windows / Linux / macOS	Compatible with all
🧱 Installation and Setup
Step 1 — Clone or Copy Project

Copy the project folder AppEngine into your workspace directory (e.g., D:\AppEngine).

Step 2 — Verify Java and Maven

Run in PowerShell or CMD:

java -version
mvn -version


Ensure Java ≥ 17 and Maven ≥ 3.8 are installed.

Step 3 — Build the Project

In the project root:

mvn clean package


Expected output:

[INFO] BUILD SUCCESS


This creates an executable JAR file at:

target\filere_eng-1.0-SNAPSHOT-jar-with-dependencies.jar

🚀 Usage Instructions
Step 1 — Run the Program
java -jar target\filere_eng-1.0-SNAPSHOT-jar-with-dependencies.jar <num_threads>


Example:

java -jar target\filere_eng-1.0-SNAPSHOT-jar-with-dependencies.jar 4

Step 2 — Index a Dataset

At the > prompt, type:

index D:\AppEngine\dataset3\dataset3


The engine will recursively crawl all 8 subfolders and build an inverted index.

Example Output:

Indexed 6095 files (2045.81 MB) with 4 threads in 92.938 s -> 22.013 MB/s
Indexing finished. Time: 92.938 s, Throughput: 22.013 MB/s, Files: 6095

Step 3 — Perform a Search

You can search for up to three terms using AND:

search Universal AND Service


Example Output:

Top results:
1. [2468] D:\AppEngine\dataset3\dataset3\folder6\Document27348.txt
2. [2299] D:\AppEngine\dataset3\dataset3\folder6\Document27560.txt
3. [1780] D:\AppEngine\dataset3\dataset3\folder6\Document27559.txt
...

Step 4 — Quit Program
quit

📊 Performance Evaluation (Example Results)
Dataset	Threads	Files	Size (MB)	Time (s)	Throughput (MB/s)
Dataset1	1	3021	812.34	122.45	6.63
Dataset1	4	3021	812.34	39.72	20.44
Dataset3	1	6095	2045.81	284.84	7.18
Dataset3	4	6095	2045.81	92.93	22.01
Dataset3	8	6095	2045.81	82.70	24.74

💡 Throughput increases nearly linearly up to 4 threads and stabilizes beyond that due to I/O and synchronization limits.

🧠 Technical Explanation

Partitioning Strategy:
Files are distributed evenly among threads using round-robin assignment.

Local Indexes:
Each worker thread builds one local index and merges it into the global index once at the end.

Thread Safety:
The global index in IndexStore uses ReentrantReadWriteLock for controlled concurrent access.

Performance Measurement:
Wall-clock time is measured using System.nanoTime(), and throughput is computed as dataset size divided by elapsed time.

🔒 Thread Safety Design
Mechanism	Description
ReentrantReadWriteLock	Allows concurrent reads, single-threaded writes
Local indexing	Reduces global contention
Atomic updates	Prevents race conditions
Encapsulation	All shared data confined to IndexStore
📦 Dataset Information

Each dataset consists of 8 subfolders with multiple .txt documents:

dataset1/
  ├── folder1/
  ├── folder2/
  ...
dataset3/
  ├── folder1/
  ├── folder2/
  ...
  └── folder8/


All files are plain-text versions of public domain books from the Gutenberg Project.

🧪 Example Evaluation Command Series

To perform a full 12-run evaluation:

# Dataset 1
java -jar target\filere_eng-1.0-SNAPSHOT-jar-with-dependencies.jar 1
> index D:\AppEngine\dataset1
java -jar ... 2
java -jar ... 4
java -jar ... 8

# Dataset 2
java -jar ... 1
java -jar ... 2
java -jar ... 4
java -jar ... 8

# Dataset 3
java -jar ... 1
java -jar ... 2
java -jar ... 4
java -jar ... 8


Collect time (s) and throughput (MB/s) from the program’s printed output.

🧩 Known Limitations

Performance may plateau beyond 4–8 threads due to disk I/O limits.

Requires sufficient RAM for large datasets.

Currently designed for local indexing (not distributed systems).

🧠 Future Enhancements

Implement dynamic work scheduling to improve load balancing.

Integrate ConcurrentHashMap for finer-grained updates.

Add parallel index merging to reduce locking overhead.

Extend to distributed environments (e.g., Hadoop, Spark, or Chameleon Cloud).

📚 References

Dean, J. and Ghemawat, S. (2008) MapReduce: Simplified Data Processing on Large Clusters. Communications of the ACM, 51(1), pp.107–113.

Baeza-Yates, R. and Ribeiro-Neto, B. (2011) Modern Information Retrieval. Addison-Wesley.

Manning, C.D., Raghavan, P. and Schütze, H. (2008) Introduction to Information Retrieval. Cambridge University Press.

Zobel, J. and Moffat, A. (2006) Inverted Files for Text Search Engines. ACM Computing Surveys, 38(2), Article 6.

Oracle (2022) ReentrantReadWriteLock (Java Platform SE 17).

Adoptium (2025) Eclipse Temurin OpenJDK 17 Distribution
