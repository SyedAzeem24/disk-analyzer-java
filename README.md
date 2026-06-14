
```markdown
# Disk Usage Analyzer

> A visual storage management tool built in pure Java that helps you understand and reclaim disk space.

##  Overview

**Disk Usage Analyzer** is a desktop application that scans any directory on your computer, calculates file and folder sizes recursively, and presents the data in a hierarchical tree view with visual progress bars. It helps you quickly identify large files and folders that are wasting disk space.

This project was developed as part of a **Software Construction & Development** course to demonstrate key software engineering concepts including recursion, concurrency, abstract data types, design by contract, and refactoring.

---

## ✨ Features

|         Feature           |                       Description                            |
|---------------------------|--------------------------------------------------------------|
|  **Directory Scanner**    | Recursively scans any selected folder                        |
|  **Size Visualization**   | Shows folder sizes with progress bars relative to parent     |
|  **Tree View**            | Expand/collapse hierarchy using Swing `JTree`                |
|  **Background Scanning**  | UI stays responsive with `SwingWorker`                       |
|  **Cancel Operation**     | Stop long-running scans anytime                              |
|  **Delete Files**         | Remove files/folders directly from the app with confirmation |
|  **Export to CSV**        | Save scan results for analysis in Excel                      |
|  **Cache Results**        | Avoid re-scanning the same folder twice                      |
|  **Human-Readable Sizes** | Displays B, KB, MB, GB with proper formatting                |

---

##  Tech Stack
|   Component  |               Technology                    |
|--------------|---------------------------------------------|
| Language     | Java 11+ (pure, no external libraries)      |
| UI Framework | Swing (built into JDK)                      |
| File I/O     | `java.io.File`, `java.nio.file.FileVisitor` |
| Concurrency  | `SwingWorker`, `ExecutorService`            |
| Testing      | JUnit 5                                     |
| Build        | Compile with `javac` or any IDE             |

---

### Prerequisites

- **Java JDK 11 or higher** installed
- (Optional) Git for cloning the repository

### Installation

```bash
# Clone the repository
git clone https://github.com/yourusername/DiskUsageAnalyzer.git

# Navigate to the project folder
cd DiskUsageAnalyzer

# Compile all Java files
javac src/*.java

# Run the application
java src/DiskAnalyzer
```

### Or Run the Pre-built JAR

```bash
java -jar DiskAnalyzer.jar
```

---

##  Usage Guide

### Step 1: Launch the Application

```bash
java src/DiskAnalyzer
```

### Step 2: Select a Folder to Scan

Click the **"Select Folder to Scan"** button and choose a directory (e.g., `C:\Users\YourName\Documents` or `/home/yourname/Downloads`).

### Step 3: Scan Progress

- A progress bar shows scan completion
- The tree populates as folders are discovered
- You can click **"Cancel"** to stop mid-scan

### Step 4: Navigate the Tree

- Click the **▶** arrow to expand a folder
- Click the **▼** arrow to collapse
- Folder sizes appear in human-readable format (e.g., "1.2 GB")

### Step 5: Delete a File/Folder

- Right-click on any item OR select it and click **"Delete"**
- Confirm deletion in the popup dialog

### Step 6: Export Results

Click **"Export CSV"** to save the scan results. Open the CSV in Excel or any spreadsheet app.

---

##  Running Tests

```bash
# Compile tests
javac -cp junit-platform-console-standalone.jar test/*.java

# Run tests
java -jar junit-platform-console-standalone.jar --class-path src:test --scan-class-path
```