package diskanalyzer;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/*
 * ============================================================================
 * CONCEPT: Anticipating Change (Week 3)
 * ============================================================================
 * This class is a CONCRETE implementation of the FileSystemScanner interface.
 * It handles scanning the local file system using java.io.File.
 *
 * If we later need to scan:
 *   - A remote file system (FTP, SSH) → implement RemoteFileSystemScanner
 *   - Inside archives (ZIP, TAR) → implement ArchiveFileSystemScanner
 *   - Cloud storage (S3, GCS) → implement CloudFileSystemScanner
 *
 * None of those changes would require modifying THIS class or the UI code,
 * because they all implement the same FileSystemScanner interface.
 *
 * CONCEPT: Recursion (Week 6)
 * ============================================================================
 * The scanDirectory() method uses RECURSION to traverse the file system tree.
 * Each call to scanDirectory() processes one directory and recursively calls
 * itself for each subdirectory, building up the FileNode tree.
 *
 * CONCEPT: Exception Handling (Week 15)
 * ============================================================================
 * File system operations can fail due to:
 *   - Permission denied (SecurityException)
 *   - Broken symbolic links
 *   - Files deleted during scanning
 *   - Disk I/O errors
 * This class handles all of these gracefully, logging warnings and continuing
 * the scan rather than crashing.
 * ============================================================================
 */

/**
 * Scans the local file system to build a FileNode tree.
 *
 * <p>Implements {@link FileSystemScanner} for local disk traversal using
 * recursive directory walking. Handles permission errors and broken
 * symlinks gracefully.</p>
 */
public class LocalFileSystemScanner implements FileSystemScanner {

    private static final Logger LOGGER = Logger.getLogger(LocalFileSystemScanner.class.getName());

    /** Counter for files scanned (used for progress reporting). */
    private int filesScanned;

    // ========================================================================
    // CONCEPT: Design by Contract (Week 4) - Preconditions on scan()
    // ========================================================================

    /**
     * Scans the given root directory and returns a FileNode tree.
     *
     * <p><b>Precondition:</b> {@code root} is not null, exists, and is a directory.</p>
     * <p><b>Postcondition:</b> Returns a complete FileNode tree with all sizes
     * computed, sorted by size descending. Returns null if cancelled.</p>
     *
     * @param root     the root directory to scan
     * @param listener optional progress listener
     * @return the FileNode tree, or null if cancelled
     * @throws IOException if a critical I/O error prevents scanning
     */
    @Override
    public FileNode scan(File root, ScanProgressListener listener) throws IOException {
        // CONCEPT: Design by Contract (Week 4) - Precondition enforcement
        if (root == null) {
            throw new IllegalArgumentException("Precondition violated: root must not be null");
        }
        if (!root.exists()) {
            throw new IOException("Root path does not exist: " + root.getAbsolutePath());
        }
        if (!root.isDirectory()) {
            throw new IOException("Root path is not a directory: " + root.getAbsolutePath());
        }

        filesScanned = 0;

        // CONCEPT: Recursion (Week 6) - Start recursive traversal
        FileNode rootNode = scanDirectory(root, 0, listener);

        if (rootNode != null) {
            // Compute sizes recursively from leaves to root
            rootNode.computeSize();
            // Sort children by size (largest first) - also recursive
            rootNode.sortBySize();
        }

        return rootNode;
    }

    // ========================================================================
    // CONCEPT: Recursion (Week 6) - Recursive directory traversal
    // ========================================================================

    /**
     * Recursively scans a directory and builds a FileNode subtree.
     *
     * <p><b>CONCEPT: Recursion (Week 6)</b></p>
     * <ul>
     *   <li><b>Base case:</b> The entry is a file → create a leaf FileNode.</li>
     *   <li><b>Recursive case:</b> The entry is a directory → create a FileNode,
     *       then recursively scan each child entry and add it.</li>
     * </ul>
     *
     * <p><b>CONCEPT: Exception Handling (Week 15)</b> – Each file operation
     * is wrapped in try-catch to handle permission errors without aborting
     * the entire scan.</p>
     *
     * @param directory the current directory to scan
     * @param depth     current depth from root (0 = root)
     * @param listener  progress listener (may be null)
     * @return the FileNode for this directory, or null if cancelled
     */
    private FileNode scanDirectory(File directory, int depth, ScanProgressListener listener) {
        // Check cancellation before processing each directory
        if (listener != null && listener.isCancelled()) {
            return null; // Scan was cancelled by user
        }

        FileNode dirNode = new FileNode(directory, depth);
        filesScanned++;

        // Report progress
        if (listener != null) {
            listener.onProgress(directory.getAbsolutePath(), filesScanned);
        }

        // ====================================================================
        // CONCEPT: Exception Handling (Week 15)
        // File.listFiles() can return null if:
        //   - The path is not a directory
        //   - An I/O error occurs
        //   - Permission is denied (SecurityException)
        // We handle all cases gracefully.
        // ====================================================================
        File[] entries;
        try {
            entries = directory.listFiles();
        } catch (SecurityException e) {
            // CONCEPT: Exception Handling (Week 15) - Permission denied
            LOGGER.log(Level.WARNING,
                "Permission denied accessing directory: " + directory.getAbsolutePath(), e);
            return dirNode; // Return empty directory node
        }

        if (entries == null) {
            // Could not list directory (permission error or I/O error)
            LOGGER.warning("Cannot list directory (possible permission error): "
                + directory.getAbsolutePath());
            return dirNode;
        }

        // Process each entry in the directory
        for (File entry : entries) {
            // Check cancellation between entries for responsiveness
            if (listener != null && listener.isCancelled()) {
                return null;
            }

            try {
                if (entry.isDirectory()) {
                    // RECURSIVE CASE: Scan subdirectory
                    FileNode childDir = scanDirectory(entry, depth + 1, listener);
                    if (childDir == null) {
                        return null; // Cancelled
                    }
                    dirNode.addChild(childDir);
                } else if (entry.isFile()) {
                    // BASE CASE: Create leaf node for file
                    FileNode fileNode = new FileNode(entry, depth + 1);
                    dirNode.addChild(fileNode);
                    filesScanned++;

                    if (listener != null) {
                        listener.onProgress(entry.getAbsolutePath(), filesScanned);
                    }
                }
                // Skip special files (symlinks to nowhere, device files, etc.)
            } catch (SecurityException e) {
                // CONCEPT: Exception Handling (Week 15)
                // Skip entries we can't access rather than crashing
                LOGGER.log(Level.WARNING,
                    "Permission denied accessing: " + entry.getAbsolutePath(), e);
            } catch (Exception e) {
                // CONCEPT: Exception Handling (Week 15)
                // Catch any unexpected errors to keep scanning
                LOGGER.log(Level.WARNING,
                    "Error processing: " + entry.getAbsolutePath(), e);
            }
        }

        return dirNode;
    }
}
