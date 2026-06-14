package diskanalyzer;

import java.io.File;

/*
 * ============================================================================
 * CONCEPT: Anticipating Change (Week 3)
 * ============================================================================
 * This interface defines the contract for scanning a file system tree.
 * By programming to an interface rather than a concrete class, we can:
 *
 *   1. Swap in different scanning strategies (e.g., local disk, network share,
 *      cloud storage, ZIP archives) without changing the UI or model code.
 *   2. Create mock implementations for testing without touching real files.
 *   3. Add new scanning modes (e.g., filtered scan, depth-limited scan)
 *      by implementing this interface.
 *
 * This is the Strategy pattern — the scanning algorithm is decoupled from
 * the code that uses it. If a future requirement adds "scan a remote server"
 * or "scan inside ZIP files," we just add a new implementation.
 *
 * CONCEPT: Managing Complexity (Week 3)
 * ============================================================================
 * This interface sits between the UI layer and the file system. It defines
 * a clear abstraction boundary:
 *   - The UI knows about FileSystemScanner (the interface)
 *   - The UI does NOT know about java.io.File traversal details
 *   - This reduces coupling and makes the system easier to maintain
 * ============================================================================
 */

/**
 * Interface for scanning a file system tree and building a FileNode hierarchy.
 *
 * <p>Implementations of this interface handle the details of traversing
 * a directory structure, computing sizes, and reporting progress.
 * The caller (e.g., ScanWorker) only depends on this interface,
 * not on any specific scanning implementation.</p>
 *
 * <p><b>CONCEPT: Anticipating Change (Week 3)</b> – Future file sources
 * (remote, archived, virtual) can be added by implementing this interface
 * without modifying existing code (Open/Closed Principle).</p>
 */
public interface FileSystemScanner {

    /**
     * Callback interface for receiving scan progress updates.
     *
     * <p>Decouples the scanner from any specific progress reporting
     * mechanism (UI progress bar, console output, logging, etc.).</p>
     */
    interface ScanProgressListener {

        /**
         * Called when a file or directory is being scanned.
         *
         * @param currentPath the path currently being scanned
         * @param filesScanned the total number of files scanned so far
         */
        void onProgress(String currentPath, int filesScanned);

        /**
         * Called to check if the scan has been cancelled.
         *
         * @return true if the scan should be stopped
         */
        boolean isCancelled();
    }

    /**
     * Scans the given root directory and returns a FileNode tree.
     *
     * <p><b>Precondition:</b> {@code root} is not null and exists on disk.</p>
     * <p><b>Postcondition:</b> Returns a non-null FileNode tree with computed
     * sizes for all directories, or null if the scan was cancelled.</p>
     *
     * @param root     the directory to scan
     * @param listener progress listener (may be null if no progress needed)
     * @return a FileNode tree rooted at the given directory, or null if cancelled
     * @throws java.io.IOException if an I/O error occurs during scanning
     */
    FileNode scan(File root, ScanProgressListener listener) throws java.io.IOException;
}
