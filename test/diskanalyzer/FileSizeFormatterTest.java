package diskanalyzer;

import org.junit.Test;
import static org.junit.Assert.*;

/*
 * ============================================================================
 * CONCEPT: Unit Testing (Week 15)
 * ============================================================================
 * These JUnit test cases verify the FileSizeFormatter utility class.
 *
 * Testing strategy:
 *   - Boundary values: 0 bytes, exactly 1 KB, exactly 1 MB, etc.
 *   - Typical values: various sizes in each unit range
 *   - Edge cases: negative input (should throw exception)
 *   - Partition testing: one test per unit range (B, KB, MB, GB, TB)
 *
 * Why unit testing matters:
 *   - Catches bugs early before they propagate to the UI
 *   - Documents expected behavior (tests are executable specifications)
 *   - Enables safe refactoring (change code, run tests, verify correctness)
 *   - Regression prevention (ensures fixes don't break later)
 *
 * To run these tests:
 *   javac -cp .:junit-4.13.2.jar:hamcrest-core-1.3.jar \
 *         src/diskanalyzer/*.java test/diskanalyzer/*Test.java
 *   java -cp .:junit-4.13.2.jar:hamcrest-core-1.3.jar:src:test \
 *         org.junit.runner.JUnitCore diskanalyzer.FileSizeFormatterTest
 * ============================================================================
 */

/**
 * Unit tests for {@link FileSizeFormatter}.
 *
 * <p><b>CONCEPT: Unit Testing (Week 15)</b> – Each test method verifies
 * a specific behavior of the formatter, using boundary values, typical
 * inputs, and error cases.</p>
 */
public class FileSizeFormatterTest {

    // ========================================================================
    // Test: format() — Zero bytes
    // ========================================================================

    @Test
    public void testFormatZeroBytes() {
        assertEquals("0 B", FileSizeFormatter.format(0));
    }

    // ========================================================================
    // Test: format() — Bytes range (0–1023)
    // ========================================================================

    @Test
    public void testFormatBytes() {
        assertEquals("1 B", FileSizeFormatter.format(1));
        assertEquals("512 B", FileSizeFormatter.format(512));
        assertEquals("1023 B", FileSizeFormatter.format(1023));
    }

    // ========================================================================
    // Test: format() — Kilobytes range (1024–1048575)
    // ========================================================================

    @Test
    public void testFormatExactlyOneKB() {
        assertEquals("1.00 KB", FileSizeFormatter.format(1024));
    }

    @Test
    public void testFormatKilobytes() {
        assertEquals("1.50 KB", FileSizeFormatter.format(1536));
        assertEquals("10.00 KB", FileSizeFormatter.format(10240));
        assertEquals("999.99 KB", FileSizeFormatter.format(1023990));
    }

    // ========================================================================
    // Test: format() — Megabytes range
    // ========================================================================

    @Test
    public void testFormatExactlyOneMB() {
        assertEquals("1.00 MB", FileSizeFormatter.format(1048576));
    }

    @Test
    public void testFormatMegabytes() {
        assertEquals("5.00 MB", FileSizeFormatter.format(5 * 1048576L));
        assertEquals("100.00 MB", FileSizeFormatter.format(100 * 1048576L));
    }

    // ========================================================================
    // Test: format() — Gigabytes range
    // ========================================================================

    @Test
    public void testFormatExactlyOneGB() {
        assertEquals("1.00 GB", FileSizeFormatter.format(1073741824L));
    }

    @Test
    public void testFormatGigabytes() {
        assertEquals("2.50 GB", FileSizeFormatter.format((long) (2.5 * 1073741824L)));
    }

    // ========================================================================
    // Test: format() — Terabytes range
    // ========================================================================

    @Test
    public void testFormatExactlyOneTB() {
        assertEquals("1.00 TB", FileSizeFormatter.format(1099511627776L));
    }

    // ========================================================================
    // Test: format() — Negative input (should throw)
    // ========================================================================

    @Test(expected = IllegalArgumentException.class)
    public void testFormatNegativeThrows() {
        // CONCEPT: Design by Contract (Week 4) — precondition violation
        FileSizeFormatter.format(-1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFormatLargeNegativeThrows() {
        FileSizeFormatter.format(-1000000);
    }

    // ========================================================================
    // Test: formatCompact()
    // ========================================================================

    @Test
    public void testFormatCompactZero() {
        assertEquals("0 B", FileSizeFormatter.formatCompact(0));
    }

    @Test
    public void testFormatCompactKB() {
        assertEquals("1.5 KB", FileSizeFormatter.formatCompact(1536));
    }

    @Test
    public void testFormatCompactMB() {
        assertEquals("1.0 MB", FileSizeFormatter.formatCompact(1048576));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFormatCompactNegativeThrows() {
        FileSizeFormatter.formatCompact(-1);
    }

    // ========================================================================
    // Test: percentage()
    // ========================================================================

    @Test
    public void testPercentageFull() {
        assertEquals(100.0, FileSizeFormatter.percentage(100, 100), 0.01);
    }

    @Test
    public void testPercentageHalf() {
        assertEquals(50.0, FileSizeFormatter.percentage(50, 100), 0.01);
    }

    @Test
    public void testPercentageZeroPart() {
        assertEquals(0.0, FileSizeFormatter.percentage(0, 100), 0.01);
    }

    @Test
    public void testPercentageZeroWhole() {
        // When whole is 0, should return 0 (not divide by zero)
        assertEquals(0.0, FileSizeFormatter.percentage(50, 0), 0.01);
    }

    @Test
    public void testPercentageSmall() {
        assertEquals(10.0, FileSizeFormatter.percentage(10, 100), 0.01);
    }
}
