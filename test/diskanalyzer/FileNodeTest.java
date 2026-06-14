package diskanalyzer;

import org.junit.Test;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/*
 * ============================================================================
 * CONCEPT: Unit Testing (Week 15)
 * ============================================================================
 * These JUnit test cases verify the FileNode Abstract Data Type.
 *
 * Testing strategy:
 *   - Constructor: valid inputs, null file, negative depth
 *   - Getters: verify correct values after construction
 *   - addChild: valid add, add to file node (should throw), null child
 *   - getChildren: returns unmodifiable list (immutability check)
 *   - computeSize: files, empty directories, nested directories
 *   - getTotalEntryCount: recursive count verification
 *   - Representation Invariant: verify checkRep via assertions
 *
 * Uses JUnit's TemporaryFolder rule to create real file system structures
 * for testing, ensuring tests don't leave files behind.
 * ============================================================================
 */

/**
 * Unit tests for {@link FileNode}.
 *
 * <p><b>CONCEPT: Unit Testing (Week 15)</b> – Tests the ADT's operations,
 * preconditions, rep invariant, and immutability guarantees.</p>
 */
public class FileNodeTest {

    /**
     * JUnit TemporaryFolder creates a temp directory that is automatically
     * cleaned up after each test. This prevents test pollution.
     */
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private File testDir;
    private File testFile;

    @Before
    public void setUp() throws IOException {
        // Create a test directory structure:
        //   testDir/
        //     subDir/
        //       nested.txt (100 bytes)
        //     file1.txt (50 bytes)
        //     file2.txt (200 bytes)
        testDir = tempFolder.newFolder("testDir");

        testFile = new File(testDir, "file1.txt");
        createFileWithSize(testFile, 50);

        File file2 = new File(testDir, "file2.txt");
        createFileWithSize(file2, 200);

        File subDir = new File(testDir, "subDir");
        subDir.mkdir();

        File nested = new File(subDir, "nested.txt");
        createFileWithSize(nested, 100);
    }

    /**
     * Helper to create a file with approximately the given size.
     */
    private void createFileWithSize(File file, int sizeBytes) throws IOException {
        try (FileWriter writer = new FileWriter(file)) {
            char[] chars = new char[sizeBytes];
            java.util.Arrays.fill(chars, 'A');
            writer.write(chars);
        }
    }

    // ========================================================================
    // Test: Constructor — valid inputs
    // ========================================================================

    @Test
    public void testConstructorWithDirectory() {
        FileNode node = new FileNode(testDir, 0);
        assertEquals(testDir.getAbsolutePath(), node.getPath());
        assertEquals("testDir", node.getName());
        assertTrue(node.isDirectory());
        assertEquals(0, node.getDepth());
    }

    @Test
    public void testConstructorWithFile() {
        FileNode node = new FileNode(testFile, 1);
        assertEquals(testFile.getAbsolutePath(), node.getPath());
        assertEquals("file1.txt", node.getName());
        assertFalse(node.isDirectory());
        assertEquals(1, node.getDepth());
        assertTrue("File size should be > 0", node.getSize() > 0);
    }

    @Test
    public void testDefaultDepthConstructor() {
        FileNode node = new FileNode(testDir);
        assertEquals(0, node.getDepth());
    }

    // ========================================================================
    // Test: Constructor — precondition violations (Design by Contract)
    // ========================================================================

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorNullFileThrows() {
        // CONCEPT: Design by Contract (Week 4) — null file violates precondition
        new FileNode(null, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorNegativeDepthThrows() {
        // CONCEPT: Design by Contract (Week 4) — negative depth violates precondition
        new FileNode(testDir, -1);
    }

    // ========================================================================
    // Test: addChild() — valid and invalid cases
    // ========================================================================

    @Test
    public void testAddChildToDirectory() {
        FileNode parent = new FileNode(testDir, 0);
        FileNode child = new FileNode(testFile, 1);
        parent.addChild(child);
        assertEquals(1, parent.getChildCount());
        assertEquals(child, parent.getChildAt(0));
    }

    @Test(expected = IllegalStateException.class)
    public void testAddChildToFileThrows() {
        // CONCEPT: Design by Contract (Week 4) — cannot add child to file
        FileNode fileNode = new FileNode(testFile, 0);
        FileNode child = new FileNode(testDir, 1);
        fileNode.addChild(child);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddNullChildThrows() {
        FileNode parent = new FileNode(testDir, 0);
        parent.addChild(null);
    }

    // ========================================================================
    // Test: getChildren() — immutability check
    // ========================================================================

    @Test(expected = UnsupportedOperationException.class)
    public void testGetChildrenReturnsUnmodifiableList() {
        // CONCEPT: Immutability (Week 5) — external code cannot modify children
        FileNode parent = new FileNode(testDir, 0);
        FileNode child = new FileNode(testFile, 1);
        parent.addChild(child);

        List<FileNode> children = parent.getChildren();
        children.add(new FileNode(testFile, 2)); // Should throw!
    }

    // ========================================================================
    // Test: computeSize() — recursive size computation
    // ========================================================================

    @Test
    public void testComputeSizeForFile() {
        // CONCEPT: Recursion (Week 6) — base case
        FileNode fileNode = new FileNode(testFile, 0);
        long size = fileNode.computeSize();
        assertTrue("File size should be > 0", size > 0);
    }

    @Test
    public void testComputeSizeForDirectoryWithChildren() {
        // CONCEPT: Recursion (Week 6) — recursive case
        FileNode parent = new FileNode(testDir, 0);

        FileNode child1 = new FileNode(testFile, 1);
        parent.addChild(child1);

        // Create another file node
        File file2 = new File(testDir, "file2.txt");
        FileNode child2 = new FileNode(file2, 1);
        parent.addChild(child2);

        long totalSize = parent.computeSize();
        assertEquals("Directory size should equal sum of children",
            child1.getSize() + child2.getSize(), totalSize);
    }

    @Test
    public void testComputeSizeEmptyDirectory() {
        File emptyDir;
        try {
            emptyDir = tempFolder.newFolder("emptyDir");
        } catch (IOException e) {
            fail("Could not create temp directory");
            return;
        }
        FileNode dirNode = new FileNode(emptyDir, 0);
        assertEquals(0, dirNode.computeSize());
    }

    // ========================================================================
    // Test: getTotalEntryCount() — recursive counting
    // ========================================================================

    @Test
    public void testGetTotalEntryCountSingleFile() {
        FileNode fileNode = new FileNode(testFile, 0);
        assertEquals(1, fileNode.getTotalEntryCount());
    }

    @Test
    public void testGetTotalEntryCountWithChildren() {
        // CONCEPT: Recursion (Week 6) — recursive counting
        FileNode parent = new FileNode(testDir, 0);
        parent.addChild(new FileNode(testFile, 1));
        parent.addChild(new FileNode(new File(testDir, "file2.txt"), 1));

        // parent + 2 children = 3
        assertEquals(3, parent.getTotalEntryCount());
    }

    @Test
    public void testGetTotalEntryCountNested() {
        FileNode root = new FileNode(testDir, 0);
        FileNode subDir = new FileNode(new File(testDir, "subDir"), 1);
        FileNode nested = new FileNode(new File(testDir, "subDir/nested.txt"), 2);
        subDir.addChild(nested);
        root.addChild(subDir);
        root.addChild(new FileNode(testFile, 1));

        // root + subDir + nested + file1 = 4
        assertEquals(4, root.getTotalEntryCount());
    }

    // ========================================================================
    // Test: getChildAt() — bounds checking
    // ========================================================================

    @Test(expected = IndexOutOfBoundsException.class)
    public void testGetChildAtNegativeIndex() {
        FileNode parent = new FileNode(testDir, 0);
        parent.addChild(new FileNode(testFile, 1));
        parent.getChildAt(-1);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testGetChildAtOutOfBounds() {
        FileNode parent = new FileNode(testDir, 0);
        parent.addChild(new FileNode(testFile, 1));
        parent.getChildAt(5); // Only 1 child, index 5 is out of bounds
    }

    // ========================================================================
    // Test: toString()
    // ========================================================================

    @Test
    public void testToStringContainsName() {
        FileNode node = new FileNode(testFile, 0);
        String str = node.toString();
        assertTrue("toString should contain file name",
            str.contains("file1.txt"));
    }

    // ========================================================================
    // Test: equals() and hashCode()
    // ========================================================================

    @Test
    public void testEqualsSamePath() {
        FileNode node1 = new FileNode(testFile, 0);
        FileNode node2 = new FileNode(testFile, 1); // different depth, same path
        assertEquals(node1, node2); // equality is based on path
    }

    @Test
    public void testNotEqualsDifferentPath() {
        FileNode node1 = new FileNode(testFile, 0);
        FileNode node2 = new FileNode(testDir, 0);
        assertNotEquals(node1, node2);
    }

    @Test
    public void testHashCodeConsistentWithEquals() {
        FileNode node1 = new FileNode(testFile, 0);
        FileNode node2 = new FileNode(testFile, 1);
        assertEquals(node1.hashCode(), node2.hashCode());
    }

    // ========================================================================
    // Test: sortBySize()
    // ========================================================================

    @Test
    public void testSortBySize() {
        FileNode parent = new FileNode(testDir, 0);

        // Add children with known sizes
        FileNode small = new FileNode(testFile, 1);               // ~50 bytes
        FileNode large = new FileNode(new File(testDir, "file2.txt"), 1); // ~200 bytes

        parent.addChild(small);
        parent.addChild(large);

        parent.sortBySize();

        // After sorting, largest should be first
        assertTrue("Largest child should be first after sorting",
            parent.getChildAt(0).getSize() >= parent.getChildAt(1).getSize());
    }
}
