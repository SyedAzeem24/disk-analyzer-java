package diskanalyzer;

import javax.swing.*;
import java.io.File;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/*
 * ============================================================================
 * CONCEPT: Concurrency (Week 12)
 * ============================================================================
 * ScanWorker extends SwingWorker to perform directory scanning on a
 * BACKGROUND THREAD, keeping the UI responsive.
 *
 * Why concurrency is needed here:
 *   - Scanning a large directory tree can take seconds or minutes
 *   - If done on the Event Dispatch Thread (EDT), the UI would freeze
 *   - SwingWorker provides a clean way to:
 *       1. Run work in the background (doInBackground)
 *       2. Publish progress updates safely to the EDT (publish/process)
 *       3. Handle completion on the EDT (done)
 *       4. Support cancellation (cancel/isCancelled)
 *
 * Thread model:
 *   - doInBackground() runs on a WORKER THREAD (not EDT)
 *   - process() runs on the EDT (safe to update UI)
 *   - done() runs on the EDT (safe to update UI)
 *
 * CONCEPT: Synchronization / Locks (Week 13)
 * ============================================================================
 * The ScanWorker accesses shared resources:
 *   - CacheManager (thread-safe via ReadWriteLock — see CacheManager.java)
 *   - Progress data shared between worker thread and EDT
 *
 * The publish/process mechanism in SwingWorker is itself a thread-safe
 * producer-consumer pattern: the worker thread produces progress strings,
 * and the EDT consumes them for display.
 * ============================================================================
 */

/**
 * Background worker that scans a directory tree using SwingWorker.
 *
 * <p><b>CONCEPT: Concurrency (Week 12)</b> – Extends {@link SwingWorker}
 * to scan directories on a background thread while keeping the Swing UI
 * responsive. Progress updates are published safely to the EDT.</p>
 *
 * <p>Type parameters:</p>
 * <ul>
 *   <li>{@code FileNode} — the result type (returned by doInBackground)</li>
 *   <li>{@code String} — the intermediate result type (progress messages)</li>
 * </ul>
 */
public class ScanWorker extends SwingWorker<FileNode, String> {

    private static final Logger LOGGER = Logger.getLogger(ScanWorker.class.getName());

    /** The root directory to scan. */
    private final File rootDirectory;

    /**
     * The scanner implementation to use.
     *
     * <p><b>CONCEPT: Anticipating Change (Week 3)</b> – We depend on the
     * FileSystemScanner INTERFACE, not a concrete class. This means we can
     * swap in any scanner implementation without modifying this class.</p>
     */
    private final FileSystemScanner scanner;

    /** Cache manager for storing/retrieving scan results. */
    private final CacheManager cacheManager;

    /** Callback to update the UI when scanning completes or fails. */
    private final ScanCallback callback;

    /** Whether to use cached results if available. */
    private final boolean useCache;

    // ========================================================================
    // CONCEPT: Synchronization / Locks (Week 13)
    // The filesScanned counter is accessed from the worker thread (incremented)
    // and potentially read from the EDT (for status display).
    // We use volatile to ensure visibility across threads without a full lock.
    // ========================================================================

    /** Number of files scanned so far (volatile for cross-thread visibility). */
    private volatile int filesScanned = 0;

    /**
     * Callback interface for scan completion.
     *
     * <p><b>CONCEPT: Managing Complexity (Week 3)</b> – The callback decouples
     * ScanWorker from the UI. ScanWorker doesn't know about JTree, JFrame, or
     * any UI components. It just calls the callback when done.</p>
     */
    public interface ScanCallback {
        /**
         * Called on the EDT when scanning completes successfully.
         *
         * @param result the root FileNode of the scanned tree
         */
        void onScanComplete(FileNode result);

        /**
         * Called on the EDT when scanning fails.
         *
         * @param error the exception that caused the failure
         */
        void onScanFailed(Exception error);

        /**
         * Called on the EDT when scanning is cancelled by the user.
         */
        void onScanCancelled();

        /**
         * Called on the EDT with progress updates during scanning.
         *
         * @param currentPath  the path currently being scanned
         * @param filesScanned total files scanned so far
         */
        void onScanProgress(String currentPath, int filesScanned);
    }

    /**
     * Constructs a new ScanWorker.
     *
     * <p><b>Precondition:</b> All parameters except cacheManager are non-null.
     * rootDirectory must exist and be a directory.</p>
     *
     * @param rootDirectory the directory to scan
     * @param scanner       the scanner implementation
     * @param cacheManager  cache manager (may be null to disable caching)
     * @param callback      UI callback for results
     * @param useCache      whether to check cache before scanning
     */
    public ScanWorker(File rootDirectory, FileSystemScanner scanner,
                      CacheManager cacheManager, ScanCallback callback,
                      boolean useCache) {
        // CONCEPT: Design by Contract (Week 4)
        if (rootDirectory == null || scanner == null || callback == null) {
            throw new IllegalArgumentException("Precondition violated: required parameters must not be null");
        }

        this.rootDirectory = rootDirectory;
        this.scanner = scanner;
        this.cacheManager = cacheManager;
        this.callback = callback;
        this.useCache = useCache;
    }

    // ========================================================================
    // CONCEPT: Concurrency (Week 12) - Background thread execution
    // ========================================================================

    /**
     * Performs the directory scan on a BACKGROUND THREAD.
     *
     * <p><b>CONCEPT: Concurrency (Week 12)</b> – This method runs on a
     * worker thread, NOT the Event Dispatch Thread. This prevents the UI
     * from freezing during long scans. It is automatically called when
     * {@code execute()} is invoked on this SwingWorker.</p>
     *
     * <p><b>CONCEPT: Synchronization (Week 13)</b> – Cache access is
     * thread-safe via CacheManager's ReadWriteLock.</p>
     *
     * @return the root FileNode of the scanned tree
     * @throws Exception if scanning fails
     */
    @Override
    protected FileNode doInBackground() throws Exception {
        String rootPath = rootDirectory.getAbsolutePath();

        // Check cache first (if enabled)
        if (useCache && cacheManager != null) {
            // CONCEPT: Synchronization (Week 13) - Thread-safe cache read
            FileNode cached = cacheManager.get(rootPath);
            if (cached != null) {
                publish("Loaded from cache: " + rootPath);
                return cached;
            }
        }

        publish("Starting scan: " + rootPath);

        // Create a progress listener that bridges to SwingWorker's publish/process
        FileSystemScanner.ScanProgressListener progressListener =
            new FileSystemScanner.ScanProgressListener() {

                @Override
                public void onProgress(String currentPath, int scanned) {
                    filesScanned = scanned;
                    // CONCEPT: Concurrency (Week 12) - publish() sends data
                    // from worker thread to EDT via the process() method
                    if (scanned % 50 == 0) { // Throttle updates to every 50 files
                        publish("Scanning (" + scanned + " items): " + currentPath);
                    }
                }

                @Override
                public boolean isCancelled() {
                    // CONCEPT: Concurrency (Week 12) - Check SwingWorker cancellation
                    return ScanWorker.this.isCancelled();
                }
            };

        // Perform the scan (this is the long-running operation)
        FileNode result = scanner.scan(rootDirectory, progressListener);

        // Cache the result for future use
        if (result != null && cacheManager != null) {
            // CONCEPT: Synchronization (Week 13) - Thread-safe cache write
            cacheManager.put(rootPath, result);
            publish("Scan complete. " + filesScanned + " items scanned and cached.");
        }

        return result;
    }

    // ========================================================================
    // CONCEPT: Concurrency (Week 12) - EDT-safe progress updates
    // ========================================================================

    /**
     * Processes intermediate progress results on the EDT.
     *
     * <p><b>CONCEPT: Concurrency (Week 12)</b> – This method runs on the
     * Event Dispatch Thread, making it safe to update UI components.
     * SwingWorker batches published strings and delivers them here.</p>
     *
     * @param chunks list of progress messages from the worker thread
     */
    @Override
    protected void process(List<String> chunks) {
        // Only use the most recent progress message (avoid UI flooding)
        if (!chunks.isEmpty()) {
            String latestMessage = chunks.get(chunks.size() - 1);
            callback.onScanProgress(latestMessage, filesScanned);
        }
    }

    // ========================================================================
    // CONCEPT: Concurrency (Week 12) - Completion handling on EDT
    // CONCEPT: Exception Handling (Week 15) - Error handling for async results
    // ========================================================================

    /**
     * Called on the EDT when doInBackground() completes.
     *
     * <p><b>CONCEPT: Concurrency (Week 12)</b> – This runs on the EDT,
     * so it's safe to update the UI tree model, status bar, etc.</p>
     *
     * <p><b>CONCEPT: Exception Handling (Week 15)</b> – Uses try-catch around
     * {@code get()} to handle exceptions thrown in doInBackground().</p>
     */
    @Override
    protected void done() {
        // Check if cancelled first
        if (isCancelled()) {
            callback.onScanCancelled();
            return;
        }

        try {
            // CONCEPT: Exception Handling (Week 15)
            // get() re-throws any exception from doInBackground()
            FileNode result = get();
            if (result != null) {
                callback.onScanComplete(result);
            } else {
                callback.onScanCancelled();
            }
        } catch (java.util.concurrent.CancellationException e) {
            callback.onScanCancelled();
        } catch (java.util.concurrent.ExecutionException e) {
            // CONCEPT: Exception Handling (Week 15) - Unwrap the cause
            Throwable cause = e.getCause();
            LOGGER.log(Level.SEVERE, "Scan failed", cause);
            callback.onScanFailed(
                cause instanceof Exception ? (Exception) cause : new Exception(cause));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            callback.onScanCancelled();
        }
    }

    /**
     * Returns the number of files scanned so far.
     *
     * @return the current scan count
     */
    public int getFilesScanned() {
        return filesScanned;
    }
}
