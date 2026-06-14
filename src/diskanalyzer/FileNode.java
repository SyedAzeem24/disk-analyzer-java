package diskanalyzer;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/*
 * ============================================================================
 * CONCEPT: Abstract Data Type (Week 7)
 * ============================================================================
 * FileNode is an Abstract Data Type that represents a node in a file system
 * tree. It encapsulates:
 *   - The file/directory path
 *   - Whether it is a directory
 *   - Its size in bytes
 *   - Its children (if directory)
 *
 * The ADT provides well-defined operations (getters, addChild, computeSize)
 * and hides the internal representation from clients. Clients interact with
 * FileNode only through its public interface, never accessing raw fields.
 * ============================================================================
 *
 * CONCEPT: Representation Invariant (Week 9)
 * ============================================================================
 * Rep Invariant (RI):
 *   1. path != null && path is a valid file system path
 *   2. name != null && name.length() > 0
 *   3. size >= 0
 *   4. children != null (never null, empty list for files)
 *   5. If isDirectory == false, then children.isEmpty() == true
 *   6. No child in children is null
 *   7. depth >= 0
 *
 * The checkRep() method verifies the RI after construction and mutation.
 * ============================================================================
 *
 * CONCEPT: Abstraction Function (Week 9)
 * ============================================================================
 * Abstraction Function (AF):
 *   AF(this) = a file system entry E where:
 *     - E.path       = this.path       (absolute path on disk)
 *     - E.name       = this.name       (display name of file/folder)
 *     - E.isDir      = this.isDirectory (true if E is a directory)
 *     - E.totalSize  = this.size       (total size in bytes, including children)
 *     - E.contents   = this.children   (sub-entries if E is a directory)
 *     - E.depth      = this.depth      (nesting level from scan root)
 *
 * This maps the concrete representation (Java fields) to the abstract notion
 * of a file system entry with a known size and hierarchical structure.
 * ============================================================================
 *
 * CONCEPT: Immutability (Week 5)
 * ============================================================================
 * Immutable fields (declared final, set only in constructor):
 *   - path, name, isDirectory, depth
 * These represent intrinsic properties of a file that never change.
 *
 * Mutable fields (must be mutable by design):
 *   - size: computed lazily via recursive traversal; updated after scanning
 *   - children: populated incrementally as subdirectories are scanned
 *
 * Defensive copying is used for getChildren() to prevent external mutation
 * of the internal children list (returns an unmodifiable view).
 * ============================================================================
 */

/**
 * FileNode: An Abstract Data Type representing a file or directory in the
 * file system, along with its computed size and child entries.
 *
 * <p>This class is designed to be the core data model of the Disk Usage
 * Analyzer. It forms a tree structure where each directory node contains
 * its children as FileNode instances.</p>
 *
 * @see #checkRep() for the representation invariant
 */
public class FileNode implements Serializable {

    // ========================================================================
    // CONCEPT: Immutability (Week 5) - final fields cannot be reassigned
    // ========================================================================
    private static final long serialVersionUID = 1L;

    /** Absolute path of this file/directory (immutable). */
    private final String path;

    /** Display name of this file/directory (immutable). */
    private final String name;

    /** Whether this node represents a directory (immutable). */
    private final boolean isDirectory;

    /** Depth from the scan root, 0 = root (immutable). */
    private final int depth;

    // ========================================================================
    // CONCEPT: Immutability (Week 5) - Mutable fields with justification
    // These fields MUST be mutable because:
    //   - size is computed lazily after recursive scanning
    //   - children are added incrementally during traversal
    // ========================================================================

    /** Total size in bytes (mutable: computed after scanning). */
    private long size;

    /** Child nodes (mutable: populated during scanning). */
    private final List<FileNode> children;

    // ========================================================================
    // CONCEPT: Design by Contract (Week 4) - Constructor preconditions
    // ========================================================================

    /**
     * Constructs a new FileNode.
     *
     * <p><b>Precondition:</b> {@code file} is not null and represents a valid
     * file system path. {@code depth} >= 0.</p>
     *
     * <p><b>Postcondition:</b> A new FileNode is created with the correct path,
     * name, directory flag, depth, zero initial size, and an empty children list.
     * The representation invariant holds.</p>
     *
     * @param file  the File object to wrap (must not be null)
     * @param depth the depth from the scan root (must be >= 0)
     * @throws IllegalArgumentException if file is null or depth is negative
     */
    public FileNode(File file, int depth) {
        // CONCEPT: Design by Contract (Week 4) - Checking preconditions
        if (file == null) {
            throw new IllegalArgumentException("Precondition violated: file must not be null");
        }
        if (depth < 0) {
            throw new IllegalArgumentException("Precondition violated: depth must be >= 0, got " + depth);
        }

        this.path = file.getAbsolutePath();
        this.name = file.getName().isEmpty() ? file.getAbsolutePath() : file.getName();
        this.isDirectory = file.isDirectory();
        this.depth = depth;
        this.size = file.isFile() ? file.length() : 0;
        this.children = new ArrayList<>();

        // CONCEPT: Representation Invariant (Week 9) - Verify after construction
        checkRep();
    }

    /**
     * Overloaded constructor for creating a FileNode at the root level (depth 0).
     *
     * <p><b>Precondition:</b> {@code file} is not null.</p>
     * <p><b>Postcondition:</b> A root-level FileNode (depth=0) is created.</p>
     *
     * @param file the File object to wrap
     */
    public FileNode(File file) {
        this(file, 0);
    }

    // ========================================================================
    // CONCEPT: Representation Invariant (Week 9) - checkRep method
    // ========================================================================

    /**
     * Verifies the representation invariant.
     * Called after construction and after any mutation to ensure internal
     * consistency of the data structure.
     *
     * @throws AssertionError if any part of the rep invariant is violated
     */
    private void checkRep() {
        assert path != null : "RI violated: path is null";
        assert name != null && !name.isEmpty() : "RI violated: name is null or empty";
        assert size >= 0 : "RI violated: size is negative (" + size + ")";
        assert children != null : "RI violated: children list is null";
        assert depth >= 0 : "RI violated: depth is negative (" + depth + ")";

        // If not a directory, must have no children
        if (!isDirectory) {
            assert children.isEmpty() : "RI violated: file node has children";
        }

        // No child should be null
        for (FileNode child : children) {
            assert child != null : "RI violated: null child in children list";
        }
    }

    // ========================================================================
    // CONCEPT: Abstract Data Type (Week 7) - Public operations / interface
    // ========================================================================

    /**
     * Returns the absolute path of this file/directory.
     *
     * <p><b>Postcondition:</b> returned value is non-null and non-empty.</p>
     *
     * @return the absolute path string
     */
    public String getPath() {
        return path;
    }

    /**
     * Returns the display name of this file/directory.
     *
     * <p><b>Postcondition:</b> returned value is non-null and non-empty.</p>
     *
     * @return the file/directory name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns whether this node is a directory.
     *
     * @return true if this node represents a directory
     */
    public boolean isDirectory() {
        return isDirectory;
    }

    /**
     * Returns the depth from the scan root.
     *
     * <p><b>Postcondition:</b> returned value >= 0.</p>
     *
     * @return the depth level
     */
    public int getDepth() {
        return depth;
    }

    /**
     * Returns the total size in bytes.
     * For directories, this is the sum of all descendant file sizes.
     *
     * <p><b>Postcondition:</b> returned value >= 0.</p>
     *
     * @return size in bytes
     */
    public long getSize() {
        return size;
    }

    /**
     * Sets the total size of this node.
     *
     * <p><b>Precondition:</b> {@code size} >= 0.</p>
     * <p><b>Postcondition:</b> this.size == size, rep invariant holds.</p>
     *
     * @param size the new size in bytes
     */
    public void setSize(long size) {
        // CONCEPT: Design by Contract (Week 4) - Precondition check
        assert size >= 0 : "Precondition violated: size must be >= 0, got " + size;
        this.size = size;
        checkRep();
    }

    /**
     * Returns an unmodifiable view of this node's children.
     *
     * <p><b>CONCEPT: Immutability (Week 5)</b> – Defensive copying via
     * unmodifiable wrapper prevents external code from mutating the
     * internal children list.</p>
     *
     * <p><b>Postcondition:</b> returned list is non-null; modifications to it
     * will throw UnsupportedOperationException.</p>
     *
     * @return unmodifiable list of child FileNodes
     */
    public List<FileNode> getChildren() {
        // Return unmodifiable view to preserve encapsulation
        return Collections.unmodifiableList(children);
    }

    /**
     * Adds a child node to this directory node.
     *
     * <p><b>Precondition:</b> this.isDirectory() == true, child != null.</p>
     * <p><b>Postcondition:</b> child is in this.children, rep invariant holds.</p>
     *
     * @param child the child FileNode to add
     * @throws IllegalStateException    if this node is not a directory
     * @throws IllegalArgumentException if child is null
     */
    public void addChild(FileNode child) {
        // CONCEPT: Design by Contract (Week 4) - Precondition enforcement
        if (!isDirectory) {
            throw new IllegalStateException(
                "Precondition violated: cannot add child to a file node (not a directory)");
        }
        if (child == null) {
            throw new IllegalArgumentException("Precondition violated: child must not be null");
        }

        children.add(child);

        // CONCEPT: Representation Invariant (Week 9) - Verify after mutation
        checkRep();
    }

    /**
     * Returns the number of direct children.
     *
     * <p><b>Postcondition:</b> returned value >= 0.</p>
     *
     * @return number of children
     */
    public int getChildCount() {
        return children.size();
    }

    /**
     * Returns the child at the given index.
     *
     * <p><b>Precondition:</b> 0 <= index < getChildCount().</p>
     * <p><b>Postcondition:</b> returned child is non-null.</p>
     *
     * @param index the index of the child
     * @return the child FileNode
     * @throws IndexOutOfBoundsException if index is out of range
     */
    public FileNode getChildAt(int index) {
        // CONCEPT: Design by Contract (Week 4) - bounds checking
        if (index < 0 || index >= children.size()) {
            throw new IndexOutOfBoundsException(
                "Precondition violated: index " + index + " out of range [0, " + children.size() + ")");
        }
        return children.get(index);
    }

    // ========================================================================
    // CONCEPT: Recursion (Week 6) - Recursive size computation
    // ========================================================================

    /**
     * Recursively computes the total size of this node.
     * For files, returns the file size.
     * For directories, returns the sum of all descendant sizes.
     *
     * <p><b>CONCEPT: Recursion (Week 6)</b> – This method demonstrates
     * recursive computation: the size of a directory is defined as the sum
     * of the sizes of its children, where each child's size is computed
     * recursively.</p>
     *
     * <p><b>Base case:</b> A file node (leaf) returns its own size.</p>
     * <p><b>Recursive case:</b> A directory sums the computeSize() of each child.</p>
     *
     * <p><b>Postcondition:</b> this.size is updated to the total computed size,
     * and the rep invariant holds.</p>
     *
     * @return the total size in bytes
     */
    public long computeSize() {
        if (!isDirectory) {
            // BASE CASE: file node - size is already set from file.length()
            return size;
        }

        // RECURSIVE CASE: directory - sum children sizes
        long totalSize = 0;
        for (FileNode child : children) {
            totalSize += child.computeSize(); // RECURSION: each child computes its own size
        }
        this.size = totalSize;

        // CONCEPT: Representation Invariant (Week 9) - Verify after mutation
        checkRep();

        // CONCEPT: Design by Contract (Week 4) - Postcondition
        assert this.size >= 0 : "Postcondition violated: computed size is negative";

        return this.size;
    }

    // ========================================================================
    // CONCEPT: Recursion (Week 6) - Recursive count of all descendants
    // ========================================================================

    /**
     * Recursively counts the total number of files and directories
     * under this node (including this node itself).
     *
     * <p><b>CONCEPT: Recursion (Week 6)</b> – Another example of recursion,
     * counting descendants in a tree structure.</p>
     *
     * <p><b>Base case:</b> A file node returns 1 (itself).</p>
     * <p><b>Recursive case:</b> A directory returns 1 + sum of all children's counts.</p>
     *
     * @return total count of entries in this subtree
     */
    public int getTotalEntryCount() {
        if (!isDirectory || children.isEmpty()) {
            // BASE CASE
            return 1;
        }

        // RECURSIVE CASE
        int count = 1; // count this directory itself
        for (FileNode child : children) {
            count += child.getTotalEntryCount(); // RECURSION
        }
        return count;
    }

    /**
     * Sorts children by size in descending order (largest first).
     * Also recursively sorts all descendants.
     *
     * <p><b>CONCEPT: Recursion (Week 6)</b> – Recursively sorts the entire tree.</p>
     */
    public void sortBySize() {
        children.sort((a, b) -> Long.compare(b.getSize(), a.getSize()));
        for (FileNode child : children) {
            child.sortBySize(); // RECURSION
        }
    }

    /**
     * Returns a File object for this node's path.
     *
     * @return a new File pointing to this node's path
     */
    public File toFile() {
        return new File(path);
    }

    @Override
    public String toString() {
        return name + " [" + FileSizeFormatter.format(size) + "]";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof FileNode)) return false;
        FileNode other = (FileNode) obj;
        return Objects.equals(path, other.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path);
    }
}
