package diskanalyzer;

import java.io.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/*
 * ============================================================================
 * CONCEPT: Synchronization / Locks (Week 13)
 * ============================================================================
 * The CacheManager uses a ReadWriteLock to protect the shared cache map.
 * This is necessary because:
 *   - The scan runs on a background thread (SwingWorker)
 *   - The UI reads cache data on the Event Dispatch Thread (EDT)
 *   - Multiple scans could run concurrently
 *
 * A ReadWriteLock allows:
 *   - Multiple concurrent readers (UI checking cache) — no contention
 *   - Exclusive writer access (scan updating cache) — blocks readers
 *
 * This is MORE efficient than a simple synchronized block because reads
 * (which are more frequent) don't block each other.
 *
 * CONCEPT: Refactoring (Week 15)
 * ============================================================================
 * Single Responsibility Principle: CacheManager handles ONLY caching.
 * It does not know about the UI, scanning logic, or file formatting.
 * This separation means:
 *   - Cache strategy can be changed without affecting other classes
 *   - Cache can be tested independently
 *   - Cache persistence (to disk) can be added without modifying scanners
 *
 * CONCEPT: Exception Handling (Week 15)
 * ============================================================================
 * Serialization/deserialization can fail due to:
 *   - Corrupt cache files
 *   - Class version mismatches (serialVersionUID)
 *   - Disk full errors
 * All failures are handled gracefully — a failed cache load simply means
 * the directory will be re-scanned (cache miss, not a crash).
 * ============================================================================
 */

/**
 * Thread-safe cache manager for storing and retrieving scan results.
 *
 * <p>Provides in-memory caching of FileNode trees keyed by directory path.
 * Optionally persists cache to disk using Java serialization.</p>
 *
 * <p>Thread safety is ensured using a {@link ReadWriteLock}, allowing
 * concurrent reads with exclusive writes.</p>
 */
public class CacheManager {

    private static final Logger LOGGER = Logger.getLogger(CacheManager.class.getName());

    // ========================================================================
    // CONCEPT: Synchronization / Locks (Week 13)
    // ReadWriteLock provides fine-grained concurrency control:
    //   - Read lock: allows multiple threads to read cache simultaneously
    //   - Write lock: ensures exclusive access when modifying cache
    // ========================================================================

    /** Read-write lock for thread-safe cache access. */
    private final ReadWriteLock cacheLock = new ReentrantReadWriteLock();

    /**
     * In-memory cache: maps directory path → cached FileNode tree.
     *
     * <p><b>CONCEPT: Synchronization (Week 13)</b> – Although ConcurrentHashMap
     * is itself thread-safe for individual operations, we use the ReadWriteLock
     * for compound operations (check-then-act) that need atomicity.</p>
     */
    private final Map<String, FileNode> cache = new ConcurrentHashMap<>();

    /** Directory where cache files are stored on disk. */
    private final File cacheDirectory;

    /**
     * Constructs a CacheManager that stores cache files in the given directory.
     *
     * <p><b>Precondition:</b> {@code cacheDir} is not null.</p>
     * <p><b>Postcondition:</b> Cache directory exists (created if needed).</p>
     *
     * @param cacheDir the directory for persistent cache files
     */
    public CacheManager(File cacheDir) {
        // CONCEPT: Design by Contract (Week 4)
        if (cacheDir == null) {
            throw new IllegalArgumentException("Precondition violated: cacheDir must not be null");
        }
        this.cacheDirectory = cacheDir;

        // Create cache directory if it doesn't exist
        if (!cacheDirectory.exists()) {
            boolean created = cacheDirectory.mkdirs();
            if (!created) {
                LOGGER.warning("Could not create cache directory: " + cacheDir.getAbsolutePath());
            }
        }
    }

    /**
     * Default constructor using a "scan_cache" directory in the current working directory.
     */
    public CacheManager() {
        this(new File(System.getProperty("user.home"), ".diskanalyzer_cache"));
    }

    // ========================================================================
    // CONCEPT: Synchronization / Locks (Week 13) - Read lock for cache lookup
    // ========================================================================

    /**
     * Retrieves a cached FileNode tree for the given path.
     *
     * <p><b>CONCEPT: Synchronization (Week 13)</b> – Acquires a READ lock
     * so multiple threads can check the cache concurrently without blocking
     * each other. Only write operations will block readers.</p>
     *
     * <p><b>Precondition:</b> {@code path} is not null.</p>
     * <p><b>Postcondition:</b> Returns the cached FileNode or null if not found.</p>
     *
     * @param path the directory path to look up
     * @return the cached FileNode tree, or null if not in cache
     */
    public FileNode get(String path) {
        if (path == null) {
            return null;
        }

        // Acquire READ lock — allows concurrent reads
        cacheLock.readLock().lock();
        try {
            FileNode cached = cache.get(path);
            if (cached != null) {
                LOGGER.info("Cache HIT for: " + path);
                return cached;
            }
        } finally {
            // ALWAYS release lock in finally block to prevent deadlocks
            cacheLock.readLock().unlock();
        }

        // Cache miss — try to load from disk
        LOGGER.info("Cache MISS for: " + path);
        return loadFromDisk(path);
    }

    // ========================================================================
    // CONCEPT: Synchronization / Locks (Week 13) - Write lock for cache update
    // ========================================================================

    /**
     * Stores a FileNode tree in the cache.
     *
     * <p><b>CONCEPT: Synchronization (Week 13)</b> – Acquires a WRITE lock
     * for exclusive access. While the write lock is held, no other thread
     * can read or write the cache, ensuring consistency.</p>
     *
     * <p><b>Precondition:</b> {@code path} and {@code node} are not null.</p>
     * <p><b>Postcondition:</b> The node is stored in memory and optionally on disk.</p>
     *
     * @param path the directory path (key)
     * @param node the FileNode tree to cache (value)
     */
    public void put(String path, FileNode node) {
        if (path == null || node == null) {
            return;
        }

        // Acquire WRITE lock — exclusive access
        cacheLock.writeLock().lock();
        try {
            cache.put(path, node);
            LOGGER.info("Cached scan result for: " + path);
        } finally {
            // ALWAYS release lock in finally block
            cacheLock.writeLock().unlock();
        }

        // Save to disk (outside lock to minimize lock duration)
        saveToDisk(path, node);
    }

    /**
     * Removes a cached entry for the given path.
     *
     * <p><b>CONCEPT: Synchronization (Week 13)</b> – Uses write lock.</p>
     *
     * @param path the directory path to remove from cache
     */
    public void invalidate(String path) {
        if (path == null) {
            return;
        }

        cacheLock.writeLock().lock();
        try {
            cache.remove(path);
            LOGGER.info("Invalidated cache for: " + path);
        } finally {
            cacheLock.writeLock().unlock();
        }

        // Delete from disk
        File cacheFile = getCacheFile(path);
        if (cacheFile.exists()) {
            boolean deleted = cacheFile.delete();
            if (!deleted) {
                LOGGER.warning("Could not delete cache file: " + cacheFile.getAbsolutePath());
            }
        }
    }

    /**
     * Clears all cached entries (in memory and on disk).
     */
    public void clearAll() {
        cacheLock.writeLock().lock();
        try {
            cache.clear();
        } finally {
            cacheLock.writeLock().unlock();
        }

        // Delete all cache files
        File[] cacheFiles = cacheDirectory.listFiles((dir, name) -> name.endsWith(".cache"));
        if (cacheFiles != null) {
            for (File f : cacheFiles) {
                f.delete();
            }
        }
        LOGGER.info("Cleared all cache entries");
    }

    /**
     * Returns the number of cached entries.
     *
     * @return number of entries in the in-memory cache
     */
    public int size() {
        cacheLock.readLock().lock();
        try {
            return cache.size();
        } finally {
            cacheLock.readLock().unlock();
        }
    }

    // ========================================================================
    // CONCEPT: Exception Handling (Week 15) - Serialization error handling
    // ========================================================================

    /**
     * Saves a FileNode tree to disk using Java serialization.
     *
     * <p><b>CONCEPT: Exception Handling (Week 15)</b> – Serialization can fail
     * due to disk errors or non-serializable objects. Failures are logged but
     * do not crash the application — the data simply won't be persisted.</p>
     */
    private void saveToDisk(String path, FileNode node) {
        File cacheFile = getCacheFile(path);
        try (ObjectOutputStream oos = new ObjectOutputStream(
                new BufferedOutputStream(new FileOutputStream(cacheFile)))) {
            oos.writeObject(node);
            LOGGER.fine("Saved cache to disk: " + cacheFile.getAbsolutePath());
        } catch (IOException e) {
            // CONCEPT: Exception Handling (Week 15) - Non-critical failure
            LOGGER.log(Level.WARNING, "Failed to save cache to disk for: " + path, e);
            // Cache save failure is non-critical; the scan result is still in memory
        }
    }

    /**
     * Loads a FileNode tree from disk.
     *
     * <p><b>CONCEPT: Exception Handling (Week 15)</b> – Deserialization can fail
     * due to corrupt files or class version mismatches. Returns null on failure.</p>
     */
    private FileNode loadFromDisk(String path) {
        File cacheFile = getCacheFile(path);
        if (!cacheFile.exists()) {
            return null;
        }

        try (ObjectInputStream ois = new ObjectInputStream(
                new BufferedInputStream(new FileInputStream(cacheFile)))) {
            FileNode node = (FileNode) ois.readObject();
            LOGGER.info("Loaded cache from disk: " + path);

            // Put in memory cache for faster subsequent access
            cacheLock.writeLock().lock();
            try {
                cache.put(path, node);
            } finally {
                cacheLock.writeLock().unlock();
            }

            return node;
        } catch (IOException | ClassNotFoundException e) {
            // CONCEPT: Exception Handling (Week 15) - Corrupt cache file
            LOGGER.log(Level.WARNING, "Failed to load cache from disk: " + path, e);
            // Delete corrupt cache file
            cacheFile.delete();
            return null;
        }
    }

    /**
     * Generates the cache file path for a given directory path.
     * Uses a hash of the path to create a safe filename.
     */
    private File getCacheFile(String path) {
        // Use hash to create a safe filename (paths may contain special chars)
        String safeFileName = Integer.toHexString(path.hashCode()) + ".cache";
        return new File(cacheDirectory, safeFileName);
    }
}
