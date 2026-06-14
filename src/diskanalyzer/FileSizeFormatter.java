package diskanalyzer;

/*
 * ============================================================================
 * CONCEPT: Coding Standards (Week 2)
 * ============================================================================
 * This utility class follows Java naming conventions:
 *   - Class name: PascalCase (FileSizeFormatter)
 *   - Method names: camelCase (format, parse)
 *   - Constants: UPPER_SNAKE_CASE (UNITS, FACTOR)
 *   - Meaningful names that describe purpose
 *
 * CONCEPT: Managing Complexity (Week 3)
 * ============================================================================
 * This class is a pure utility with NO dependencies on UI or scanning logic.
 * It handles ONE responsibility: converting between byte counts and
 * human-readable size strings. This separation means:
 *   - It can be unit-tested in isolation
 *   - It can be reused in any context (UI labels, CSV export, logging)
 *   - Changes to formatting don't affect scanning or display logic
 *
 * CONCEPT: Refactoring (Week 15)
 * ============================================================================
 * Single Responsibility Principle: This class does ONE thing — format sizes.
 * Extracted from what could have been inline formatting in the UI layer.
 * ============================================================================
 */

/**
 * Utility class for formatting file sizes into human-readable strings
 * and parsing them back to byte values.
 *
 * <p>Converts raw byte counts to strings like "1.5 KB", "320 MB", "2.1 GB".
 * Also provides the inverse operation (parsing formatted strings back to bytes).</p>
 *
 * <p>This class cannot be instantiated (private constructor) and provides
 * only static methods — a standard Java utility class pattern.</p>
 */
public final class FileSizeFormatter {

    // ========================================================================
    // CONCEPT: Coding Standards (Week 2) - Constants in UPPER_SNAKE_CASE
    // ========================================================================

    /** Size unit labels in ascending order of magnitude. */
    private static final String[] UNITS = {"B", "KB", "MB", "GB", "TB", "PB"};

    /** Factor between adjacent units (1 KB = 1024 B). */
    private static final long FACTOR = 1024L;

    /**
     * Private constructor prevents instantiation.
     *
     * <p><b>CONCEPT: Coding Standards (Week 2)</b> – Utility classes should
     * have a private constructor to prevent accidental instantiation.</p>
     */
    private FileSizeFormatter() {
        throw new UnsupportedOperationException("Utility class — do not instantiate");
    }

    // ========================================================================
    // CONCEPT: Design by Contract (Week 4) - Preconditions & postconditions
    // ========================================================================

    /**
     * Formats a byte count into a human-readable string.
     *
     * <p><b>Precondition:</b> {@code bytes} >= 0.</p>
     * <p><b>Postcondition:</b> Returns a non-null, non-empty string in the
     * format "X.XX UNIT" (e.g., "1.50 KB") or "0 B" for zero bytes.</p>
     *
     * <p>Examples:</p>
     * <ul>
     *   <li>{@code format(0)}       → "0 B"</li>
     *   <li>{@code format(512)}     → "512 B"</li>
     *   <li>{@code format(1024)}    → "1.00 KB"</li>
     *   <li>{@code format(1536)}    → "1.50 KB"</li>
     *   <li>{@code format(1048576)} → "1.00 MB"</li>
     * </ul>
     *
     * @param bytes the number of bytes (must be >= 0)
     * @return a formatted string like "1.50 KB"
     * @throws IllegalArgumentException if bytes is negative
     */
    public static String format(long bytes) {
        // CONCEPT: Design by Contract (Week 4) - Precondition check
        if (bytes < 0) {
            throw new IllegalArgumentException(
                "Precondition violated: bytes must be >= 0, got " + bytes);
        }

        // Special case: zero bytes
        if (bytes == 0) {
            return "0 B";
        }

        // Find the appropriate unit by dividing by 1024 repeatedly
        int unitIndex = 0;
        double value = (double) bytes;

        while (value >= FACTOR && unitIndex < UNITS.length - 1) {
            value /= FACTOR;
            unitIndex++;
        }

        // Format: no decimals for bytes, 2 decimals for larger units
        String result;
        if (unitIndex == 0) {
            result = String.format("%d %s", bytes, UNITS[0]);
        } else {
            result = String.format("%.2f %s", value, UNITS[unitIndex]);
        }

        // CONCEPT: Design by Contract (Week 4) - Postcondition check
        assert result != null && !result.isEmpty()
            : "Postcondition violated: formatted result is null or empty";

        return result;
    }

    /**
     * Formats bytes into a compact string (1 decimal place) for tree labels.
     *
     * <p><b>Precondition:</b> {@code bytes} >= 0.</p>
     * <p><b>Postcondition:</b> Returns a compact non-null, non-empty string.</p>
     *
     * @param bytes the number of bytes
     * @return compact formatted string like "1.5 KB"
     */
    public static String formatCompact(long bytes) {
        if (bytes < 0) {
            throw new IllegalArgumentException(
                "Precondition violated: bytes must be >= 0, got " + bytes);
        }

        if (bytes == 0) {
            return "0 B";
        }

        int unitIndex = 0;
        double value = (double) bytes;

        while (value >= FACTOR && unitIndex < UNITS.length - 1) {
            value /= FACTOR;
            unitIndex++;
        }

        if (unitIndex == 0) {
            return String.format("%d B", bytes);
        } else {
            return String.format("%.1f %s", value, UNITS[unitIndex]);
        }
    }

    /**
     * Calculates the percentage that {@code part} is of {@code whole}.
     *
     * <p><b>Precondition:</b> both values >= 0, {@code whole} > 0.</p>
     * <p><b>Postcondition:</b> result is in [0.0, 100.0].</p>
     *
     * @param part  the part value
     * @param whole the whole value (must be > 0)
     * @return the percentage (0.0 to 100.0)
     */
    public static double percentage(long part, long whole) {
        // CONCEPT: Design by Contract (Week 4)
        if (whole <= 0) {
            return 0.0;
        }
        if (part < 0) {
            throw new IllegalArgumentException("Precondition violated: part must be >= 0");
        }

        double pct = (double) part / whole * 100.0;

        // Clamp to [0, 100]
        return Math.min(100.0, Math.max(0.0, pct));
    }
}
