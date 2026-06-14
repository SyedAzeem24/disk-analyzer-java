package diskanalyzer;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
// import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/*
 * ╔═════════════════════════════════════════════════════════════════════════╗
 * ║                     DISK USAGE ANALYZER — DiskAnalyzer.java             ║
 * ║                     Software Construction & Design Project              ║
 * ╠═════════════════════════════════════════════════════════════════════════╣
 * ║                                                                         ║
 * ║  CONCEPT INDEX — Where each course concept is demonstrated:             ║
 * ║                                                                         ║
 * ║  1. Recursion (Week 6):                                                 ║
 * ║     → FileNode.java: computeSize(), getTotalEntryCount(), sortBySize()  ║
 * ║     → LocalFileSystemScanner.java: scanDirectory() recursive traversal  ║
 * ║                                                                         ║
 * ║  2. Abstract Data Types (Week 7):                                       ║
 * ║     → FileNode.java: encapsulated data with public operations only      ║
 * ║                                                                         ║
 * ║  3. Representation Invariants & Abstraction Function (Week 9):          ║
 * ║     → FileNode.java: checkRep() method, RI & AF in class Javadoc        ║
 * ║                                                                         ║
 * ║  4. Immutability (Week 5):                                              ║
 * ║     → FileNode.java: final fields (path, name, isDirectory, depth)      ║
 * ║     → FileNode.java: unmodifiable children list via getChildren()       ║
 * ║     → FileSizeFormatter.java: stateless, all-static utility class       ║
 * ║                                                                         ║
 * ║  5. Design by Contract (Week 4):                                        ║
 * ║     → FileNode.java: preconditions/postconditions on all methods        ║
 * ║     → FileSizeFormatter.java: format() pre/postconditions               ║
 * ║     → ScanWorker.java: constructor preconditions                        ║
 * ║                                                                         ║
 * ║  6. Concurrency (Week 12):                                              ║
 * ║     → ScanWorker.java: SwingWorker background thread                    ║
 * ║     → ScanWorker.java: doInBackground(), process(), done()              ║
 * ║                                                                         ║
 * ║  7. Synchronization / Locks (Week 13):                                  ║
 * ║     → CacheManager.java: ReadWriteLock for thread-safe cache            ║
 * ║     → ScanWorker.java: volatile filesScanned counter                    ║
 * ║                                                                         ║
 * ║  8. Refactoring (Week 15):                                              ║
 * ║     → 7 classes, each with Single Responsibility:                       ║
 * ║       DiskAnalyzer (UI), FileNode (data), ScanWorker (concurrency),     ║
 * ║       FileSizeFormatter (formatting), DiskUsageCellRenderer (rendering),║
 * ║       CacheManager (caching), LocalFileSystemScanner (scanning)         ║
 * ║                                                                         ║
 * ║  9. Exception Handling (Week 15):                                       ║
 * ║     → LocalFileSystemScanner.java: permission & I/O error handling      ║
 * ║     → CacheManager.java: serialization error handling                   ║
 * ║     → DiskAnalyzer.java: delete & export error handling                 ║
 * ║     → ScanWorker.java: done() method error handling                     ║
 * ║                                                                         ║
 * ║ 10. Coding Standards (Week 2):                                          ║
 * ║     → All files: PascalCase classes, camelCase methods, UPPER_SNAKE     ║
 * ║       constants, meaningful names, consistent indentation               ║
 * ║                                                                         ║
 * ║ 11. Managing Complexity (Week 3):                                       ║
 * ║     → Layered architecture: Data → Format → Render → UI                 ║
 * ║     → FileSystemScanner interface decouples scanning from UI            ║
 * ║     → ScanCallback decouples worker from UI                             ║
 * ║                                                                         ║
 * ║ 12. Anticipating Change (Week 3):                                       ║
 * ║     → FileSystemScanner.java: interface for pluggable scan strategies   ║
 * ║     → ScanWorker depends on interface, not concrete scanner             ║
 * ║                                                                         ║
 * ║ 13. Unit Testing (Week 15):                                             ║
 * ║     → test/diskanalyzer/FileSizeFormatterTest.java                      ║
 * ║     → test/diskanalyzer/FileNodeTest.java                               ║
 * ║                                                                         ║
 * ║ 14. Version Control (Week 14):                                          ║
 * ║     → .gitignore with Git workflow documentation                        ║
 * ║                                                                         ║
 * ╚═════════════════════════════════════════════════════════════════════════╝
 */

/**
 * Main application frame for the Disk Usage Analyzer.
 *
 * <p>
 * Provides the graphical user interface for:
 * <ul>
 * <li>Selecting a folder to scan via JFileChooser</li>
 * <li>Displaying the file tree with sizes and progress bars</li>
 * <li>Background scanning with cancel support</li>
 * <li>Deleting files/folders with confirmation</li>
 * <li>Exporting results to CSV</li>
 * </ul>
 *
 * <p>
 * <b>CONCEPT: Managing Complexity (Week 3)</b> – This class handles
 * ONLY UI concerns. All data logic is in FileNode, scanning in
 * ScanWorker/LocalFileSystemScanner, formatting in FileSizeFormatter,
 * rendering in DiskUsageCellRenderer, and caching in CacheManager.
 * </p>
 *
 * <p>
 * <b>CONCEPT: Refactoring (Week 15)</b> – Clean separation of concerns
 * across 7+ classes, each with a single responsibility.
 * </p>
 */
public class DiskAnalyzer extends JFrame implements ScanWorker.ScanCallback {

    private static final Logger LOGGER = Logger.getLogger(DiskAnalyzer.class.getName());

    // ========================================================================
    // CONCEPT: Coding Standards (Week 2) - Named constants
    // ========================================================================
    private static final String APP_TITLE = "Disk Usage Analyzer";
    private static final int DEFAULT_WIDTH = 1100;
    private static final int DEFAULT_HEIGHT = 700;

    // ========================================================================
    // UI Components
    // ========================================================================
    private JTree fileTree;
    private DefaultTreeModel treeModel;
    private DefaultMutableTreeNode rootTreeNode;
    private JLabel statusLabel;
    private JProgressBar scanProgressBar;
    private JButton scanButton;
    private JButton cancelButton;
    private JButton deleteButton;
    private JButton exportButton;
    private JButton clearCacheButton;
    private JTextField pathField;
    private JButton browseButton;

    // ========================================================================
    // Application components
    // ========================================================================
    private final FileSystemScanner scanner;
    private final CacheManager cacheManager;
    private ScanWorker currentWorker;
    private FileNode currentScanResult;

    // ========================================================================
    // Constructor and UI Setup
    // ========================================================================

    /**
     * Constructs the DiskAnalyzer main window.
     *
     * <p>
     * <b>CONCEPT: Anticipating Change (Week 3)</b> – The scanner is injected
     * as a FileSystemScanner interface, not a concrete class. This allows
     * swapping scanner implementations without changing this class.
     * </p>
     */
    public DiskAnalyzer() {
        super(APP_TITLE);

        // CONCEPT: Anticipating Change (Week 3) - Depend on interface
        this.scanner = new LocalFileSystemScanner();
        this.cacheManager = new CacheManager();

        initializeLookAndFeel();
        initializeUI();
        setupEventHandlers();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
        setLocationRelativeTo(null); // Center on screen
        setMinimumSize(new Dimension(800, 500));
    }

    /**
     * Sets the Look and Feel to a modern cross-platform style.
     */
    private void initializeLookAndFeel() {
        try {
            // Try FlatLaf-style or Nimbus for modern appearance
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());

                    // Customize Nimbus colors for a dark theme
                    UIManager.put("control", new Color(40, 42, 48));
                    UIManager.put("nimbusBase", new Color(30, 32, 38));
                    UIManager.put("nimbusFocus", new Color(70, 130, 200));
                    UIManager.put("nimbusLightBackground", new Color(45, 47, 53));
                    UIManager.put("text", new Color(220, 222, 228));
                    UIManager.put("nimbusSelectionBackground", new Color(45, 85, 145));
                    UIManager.put("info", new Color(50, 52, 58));
                    break;
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Could not set Look and Feel", e);
        }
    }

    /**
     * Initializes all UI components and layout.
     */
    private void initializeUI() {
        setLayout(new BorderLayout(0, 0));

        // --- Top toolbar panel ---
        JPanel toolbarPanel = createToolbarPanel();
        add(toolbarPanel, BorderLayout.NORTH);

        // --- Center: File tree ---
        JPanel treePanel = createTreePanel();
        add(treePanel, BorderLayout.CENTER);

        // --- Bottom: Status bar ---
        JPanel statusPanel = createStatusPanel();
        add(statusPanel, BorderLayout.SOUTH);
    }

    /**
     * Creates the toolbar panel with path field, browse, scan, cancel buttons.
     */
    private JPanel createToolbarPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 0));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 12, 8, 12));
        panel.setBackground(new Color(35, 37, 42));

        // Left: path input
        JPanel pathPanel = new JPanel(new BorderLayout(6, 0));
        pathPanel.setOpaque(false);

        JLabel pathLabel = new JLabel("  Path: ");
        pathLabel.setForeground(new Color(180, 185, 195));
        pathLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        pathPanel.add(pathLabel, BorderLayout.WEST);

        pathField = new JTextField(30);
        pathField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        pathField.setToolTipText("Enter or browse to a directory to scan");
        pathField.setText(System.getProperty("user.home"));
        pathPanel.add(pathField, BorderLayout.CENTER);

        browseButton = createStyledButton("Browse...", new Color(80, 85, 95));
        pathPanel.add(browseButton, BorderLayout.EAST);

        panel.add(pathPanel, BorderLayout.CENTER);

        // Right: action buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        buttonPanel.setOpaque(false);

        scanButton = createStyledButton("Scan", new Color(40, 120, 80));
        cancelButton = createStyledButton("Cancel", new Color(160, 60, 60));
        deleteButton = createStyledButton("Delete", new Color(140, 55, 55));
        exportButton = createStyledButton("Export CSV", new Color(60, 100, 150));
        clearCacheButton = createStyledButton("Clear Cache", new Color(100, 85, 60));

        cancelButton.setEnabled(false);
        deleteButton.setEnabled(false);
        exportButton.setEnabled(false);

        buttonPanel.add(scanButton);
        buttonPanel.add(cancelButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(exportButton);
        buttonPanel.add(clearCacheButton);

        panel.add(buttonPanel, BorderLayout.EAST);

        return panel;
    }

    /**
     * Creates a styled button with the given text and background color.
     */
    private JButton createStyledButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        button.setFocusPainted(false);
        button.setBackground(bgColor);
        button.setForeground(new Color(230, 232, 238));
        button.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Hover effect
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (button.isEnabled()) {
                    button.setBackground(bgColor.brighter());
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(bgColor);
            }
        });

        return button;
    }

    /**
     * Creates the tree panel with a scrollable JTree.
     */
    private JPanel createTreePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(40, 42, 48));

        // Initialize tree with empty root
        rootTreeNode = new DefaultMutableTreeNode("Select a directory to scan");
        treeModel = new DefaultTreeModel(rootTreeNode);
        fileTree = new JTree(treeModel);
        fileTree.setCellRenderer(new DiskUsageCellRenderer());
        fileTree.setRowHeight(28);
        fileTree.setBackground(new Color(40, 42, 48));
        fileTree.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        fileTree.setShowsRootHandles(true);

        JScrollPane scrollPane = new JScrollPane(fileTree);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(new Color(40, 42, 48));

        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    /**
     * Creates the status bar at the bottom of the window.
     */
    private JPanel createStatusPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 0));
        panel.setBorder(BorderFactory.createEmptyBorder(6, 12, 8, 12));
        panel.setBackground(new Color(32, 34, 40));

        statusLabel = new JLabel("Ready — Select a folder and click Scan to begin.");
        statusLabel.setForeground(new Color(150, 155, 168));
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        scanProgressBar = new JProgressBar();
        scanProgressBar.setIndeterminate(false);
        scanProgressBar.setStringPainted(true);
        scanProgressBar.setString("");
        scanProgressBar.setPreferredSize(new Dimension(200, 18));
        scanProgressBar.setVisible(false);

        panel.add(statusLabel, BorderLayout.CENTER);
        panel.add(scanProgressBar, BorderLayout.EAST);

        return panel;
    }

    // ========================================================================
    // Event Handlers
    // ========================================================================

    /**
     * Sets up all event handlers for UI components.
     */
    private void setupEventHandlers() {
        browseButton.addActionListener(e -> browseForDirectory());
        scanButton.addActionListener(e -> startScan());
        cancelButton.addActionListener(e -> cancelScan());
        deleteButton.addActionListener(e -> deleteSelected());
        exportButton.addActionListener(e -> exportToCsv());
        clearCacheButton.addActionListener(e -> clearCache());

        // Enable delete when a tree node is selected
        fileTree.addTreeSelectionListener(e -> {
            boolean hasSelection = fileTree.getSelectionPath() != null;
            deleteButton.setEnabled(hasSelection);
        });

        // Allow pressing Enter in path field to start scan
        pathField.addActionListener(e -> startScan());
    }

    /**
     * Opens a JFileChooser for directory selection.
     */
    private void browseForDirectory() {
        JFileChooser chooser = new JFileChooser(FileSystemView.getFileSystemView().getHomeDirectory());
        chooser.setDialogTitle("Select Directory to Analyze");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            pathField.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    // ========================================================================
    // CONCEPT: Concurrency (Week 12) - Starting a background scan
    // ========================================================================

    /**
     * Starts a background scan of the selected directory.
     *
     * <p>
     * <b>CONCEPT: Concurrency (Week 12)</b> – Creates a ScanWorker
     * (SwingWorker subclass) and calls execute() to start scanning on
     * a background thread. The UI remains responsive during the scan.
     * </p>
     *
     * <p>
     * <b>CONCEPT: Exception Handling (Week 15)</b> – Validates the path
     * before starting the scan, showing error dialogs for invalid paths.
     * </p>
     */
    private void startScan() {
        String path = pathField.getText().trim();

        // CONCEPT: Exception Handling (Week 15) - Validate input
        if (path.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Please enter or browse to a directory path.",
                    "No Path Specified", JOptionPane.WARNING_MESSAGE);
            return;
        }

        File rootDir = new File(path);
        if (!rootDir.exists()) {
            JOptionPane.showMessageDialog(this,
                    "The specified path does not exist:\n" + path,
                    "Invalid Path", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (!rootDir.isDirectory()) {
            JOptionPane.showMessageDialog(this,
                    "The specified path is not a directory:\n" + path,
                    "Not a Directory", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Cancel any existing scan
        if (currentWorker != null && !currentWorker.isDone()) {
            currentWorker.cancel(true);
        }

        // Update UI state
        scanButton.setEnabled(false);
        cancelButton.setEnabled(true);
        deleteButton.setEnabled(false);
        exportButton.setEnabled(false);
        scanProgressBar.setVisible(true);
        scanProgressBar.setIndeterminate(true);
        scanProgressBar.setString("Scanning...");
        statusLabel.setText("Scanning: " + path);

        // Clear existing tree
        rootTreeNode.removeAllChildren();
        rootTreeNode.setUserObject("Scanning...");
        treeModel.reload();

        // CONCEPT: Concurrency (Week 12) - Create and start SwingWorker
        currentWorker = new ScanWorker(rootDir, scanner, cacheManager, this, true);
        currentWorker.execute(); // Starts doInBackground() on worker thread
    }

    /**
     * Cancels the currently running scan.
     *
     * <p>
     * <b>CONCEPT: Concurrency (Week 12)</b> – Calls cancel(true) on the
     * SwingWorker, which sets the cancelled flag that the scanner checks
     * between processing each directory entry.
     * </p>
     */
    private void cancelScan() {
        if (currentWorker != null && !currentWorker.isDone()) {
            currentWorker.cancel(true);
            statusLabel.setText("Cancelling scan...");
        }
    }

    // ========================================================================
    // CONCEPT: Concurrency (Week 12) - ScanCallback implementations
    // All callback methods run on the EDT (safe to update UI)
    // ========================================================================

    /**
     * Called on the EDT when scanning completes successfully.
     *
     * <p>
     * <b>CONCEPT: Concurrency (Week 12)</b> – This callback runs on the
     * Event Dispatch Thread, so it's safe to update Swing components.
     * </p>
     */
    @Override
    public void onScanComplete(FileNode result) {
        this.currentScanResult = result;

        // Build the JTree from the FileNode tree
        rootTreeNode = buildTreeNode(result);
        treeModel = new DefaultTreeModel(rootTreeNode);
        fileTree.setModel(treeModel);

        // Expand the root node
        fileTree.expandRow(0);

        // Update UI state
        scanButton.setEnabled(true);
        cancelButton.setEnabled(false);
        exportButton.setEnabled(true);
        scanProgressBar.setVisible(false);
        statusLabel.setText(String.format("Scan complete — %s total (%d items)",
                FileSizeFormatter.format(result.getSize()),
                result.getTotalEntryCount()));
    }

    @Override
    public void onScanFailed(Exception error) {
        scanButton.setEnabled(true);
        cancelButton.setEnabled(false);
        scanProgressBar.setVisible(false);
        statusLabel.setText("Scan failed: " + error.getMessage());

        // CONCEPT: Exception Handling (Week 15) - Show error to user
        JOptionPane.showMessageDialog(this,
                "Scan failed:\n" + error.getMessage(),
                "Scan Error", JOptionPane.ERROR_MESSAGE);
    }

    @Override
    public void onScanCancelled() {
        scanButton.setEnabled(true);
        cancelButton.setEnabled(false);
        scanProgressBar.setVisible(false);
        statusLabel.setText("Scan cancelled by user.");
    }

    @Override
    public void onScanProgress(String currentPath, int filesScanned) {
        // Truncate long paths for display
        String displayPath = currentPath;
        if (displayPath.length() > 80) {
            displayPath = "..." + displayPath.substring(displayPath.length() - 77);
        }
        statusLabel.setText(String.format("Scanning (%d items): %s", filesScanned, displayPath));
        scanProgressBar.setString(filesScanned + " items");
    }

    // ========================================================================
    // CONCEPT: Recursion (Week 6) - Recursive tree node construction
    // ========================================================================

    /**
     * Recursively builds a JTree DefaultMutableTreeNode from a FileNode.
     *
     * <p>
     * <b>CONCEPT: Recursion (Week 6)</b> – This method recursively
     * converts the FileNode tree into a JTree tree structure.
     * </p>
     *
     * <p>
     * <b>Base case:</b> A file node (leaf) — create a tree node with no children.
     * </p>
     * <p>
     * <b>Recursive case:</b> A directory — create a tree node, then
     * recursively create and add child tree nodes.
     * </p>
     *
     * @param fileNode the FileNode to convert
     * @return a DefaultMutableTreeNode for the JTree
     */
    private DefaultMutableTreeNode buildTreeNode(FileNode fileNode) {
        DefaultMutableTreeNode treeNode = new DefaultMutableTreeNode(fileNode);

        if (fileNode.isDirectory()) {
            for (FileNode child : fileNode.getChildren()) {
                // RECURSION: build subtree for each child
                treeNode.add(buildTreeNode(child));
            }
        }

        return treeNode;
    }

    // ========================================================================
    // Delete functionality
    // ========================================================================

    /**
     * Deletes the selected file or folder after user confirmation.
     *
     * <p>
     * <b>CONCEPT: Exception Handling (Week 15)</b> – Deletion can fail
     * due to permissions, locked files, or system restrictions. All errors
     * are caught and displayed to the user.
     * </p>
     */
    private void deleteSelected() {
        TreePath selectedPath = fileTree.getSelectionPath();
        if (selectedPath == null) {
            return;
        }

        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) selectedPath.getLastPathComponent();
        Object userObj = selectedNode.getUserObject();

        if (!(userObj instanceof FileNode)) {
            return;
        }

        FileNode fileNode = (FileNode) userObj;
        File file = fileNode.toFile();

        // Confirmation dialog
        String message = String.format(
                "Are you sure you want to permanently delete:\n\n%s\n\nSize: %s\nType: %s",
                file.getAbsolutePath(),
                FileSizeFormatter.format(fileNode.getSize()),
                fileNode.isDirectory() ? "Directory (and ALL contents)" : "File");

        int result = JOptionPane.showConfirmDialog(this,
                message, "Confirm Delete",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (result != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            boolean deleted;
            if (file.isDirectory()) {
                deleted = deleteDirectoryRecursively(file);
            } else {
                deleted = file.delete();
            }

            if (deleted) {
                // Remove from tree
                DefaultMutableTreeNode parent = (DefaultMutableTreeNode) selectedNode.getParent();
                if (parent != null) {
                    selectedNode.removeFromParent();
                    treeModel.reload(parent);
                }

                // Invalidate cache since file structure changed
                if (currentScanResult != null) {
                    cacheManager.invalidate(currentScanResult.getPath());
                }

                statusLabel.setText("Deleted: " + file.getAbsolutePath());
            } else {
                JOptionPane.showMessageDialog(this,
                        "Could not delete: " + file.getAbsolutePath() +
                                "\n\nThe file may be in use or you may lack permissions.",
                        "Delete Failed", JOptionPane.ERROR_MESSAGE);
            }
        } catch (SecurityException e) {
            // CONCEPT: Exception Handling (Week 15)
            LOGGER.log(Level.WARNING, "Permission denied during delete", e);
            JOptionPane.showMessageDialog(this,
                    "Permission denied:\n" + e.getMessage(),
                    "Delete Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Recursively deletes a directory and all its contents.
     *
     * <p>
     * <b>CONCEPT: Recursion (Week 6)</b> – Must delete all children before
     * the directory itself can be deleted (post-order traversal).
     * </p>
     *
     * @param directory the directory to delete
     * @return true if deletion was successful
     */
    private boolean deleteDirectoryRecursively(File directory) {
        File[] contents = directory.listFiles();
        if (contents != null) {
            for (File file : contents) {
                if (file.isDirectory()) {
                    // RECURSION: delete subdirectory first
                    deleteDirectoryRecursively(file);
                } else {
                    file.delete();
                }
            }
        }
        return directory.delete();
    }

    // ========================================================================
    // CSV Export
    // ========================================================================

    /**
     * Exports the current scan results to a CSV file.
     *
     * <p>
     * <b>CONCEPT: Exception Handling (Week 15)</b> – File writing can fail
     * due to disk full, permission errors, etc. All errors are handled.
     * </p>
     */
    private void exportToCsv() {
        if (currentScanResult == null) {
            JOptionPane.showMessageDialog(this,
                    "No scan results to export. Please scan a directory first.",
                    "Nothing to Export", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Export Scan Results to CSV");
        chooser.setSelectedFile(new File("disk_usage_report.csv"));

        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File csvFile = chooser.getSelectedFile();

        // CONCEPT: Exception Handling (Week 15) - try-with-resources
        try (PrintWriter writer = new PrintWriter(
                new BufferedWriter(new FileWriter(csvFile)))) {

            // CSV header
            writer.println("Path,Name,Type,Size (Bytes),Size (Formatted),Depth");

            // CONCEPT: Recursion (Week 6) - Recursive CSV writing
            writeCsvNode(writer, currentScanResult);

            statusLabel.setText("Exported to: " + csvFile.getAbsolutePath());
            JOptionPane.showMessageDialog(this,
                    "Report exported successfully to:\n" + csvFile.getAbsolutePath(),
                    "Export Complete", JOptionPane.INFORMATION_MESSAGE);

        } catch (IOException e) {
            // CONCEPT: Exception Handling (Week 15)
            LOGGER.log(Level.SEVERE, "Failed to export CSV", e);
            JOptionPane.showMessageDialog(this,
                    "Failed to export CSV:\n" + e.getMessage(),
                    "Export Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Recursively writes a FileNode and its children to CSV.
     *
     * <p>
     * <b>CONCEPT: Recursion (Week 6)</b>
     * </p>
     */
    private void writeCsvNode(PrintWriter writer, FileNode node) {
        // Escape commas and quotes in path/name for CSV safety
        String escapedPath = "\"" + node.getPath().replace("\"", "\"\"") + "\"";
        String escapedName = "\"" + node.getName().replace("\"", "\"\"") + "\"";

        writer.printf("%s,%s,%s,%d,%s,%d%n",
                escapedPath,
                escapedName,
                node.isDirectory() ? "Directory" : "File",
                node.getSize(),
                FileSizeFormatter.format(node.getSize()),
                node.getDepth());

        // RECURSION: write children
        if (node.isDirectory()) {
            for (FileNode child : node.getChildren()) {
                writeCsvNode(writer, child);
            }
        }
    }

    // ========================================================================
    // Cache management
    // ========================================================================

    /**
     * Clears all cached scan results.
     */
    private void clearCache() {
        int result = JOptionPane.showConfirmDialog(this,
                "Clear all cached scan results?\n\n" +
                        "Cached entries: " + cacheManager.size(),
                "Clear Cache", JOptionPane.YES_NO_OPTION);

        if (result == JOptionPane.YES_OPTION) {
            cacheManager.clearAll();
            statusLabel.setText("Cache cleared.");
        }
    }

    // ========================================================================
    // CONCEPT: Version Control (Week 14)
    // ========================================================================
    // This project uses Git for version control.
    // See .gitignore for the Git workflow documentation.
    //
    // Recommended Git commands for this project:
    // git init
    // git add .
    // git commit -m "Initial commit: Disk Usage Analyzer"
    // git branch -M main
    // git remote add origin <repository-url>
    // git push -u origin main
    //
    // For feature development:
    // git checkout -b feature/new-feature-name
    // ... make changes ...
    // git add .
    // git commit -m "Add feature: description"
    // git checkout main
    // git merge feature/new-feature-name
    // ========================================================================

    /**
     * Application entry point.
     *
     * <p>
     * <b>CONCEPT: Concurrency (Week 12)</b> – Uses SwingUtilities.invokeLater()
     * to ensure the UI is created on the Event Dispatch Thread, which is
     * required by Swing's threading model.
     * </p>
     */
    public static void main(String[] args) {
        // Enable assertions for rep invariant checking
        ClassLoader.getSystemClassLoader().setDefaultAssertionStatus(true);

        // CONCEPT: Concurrency (Week 12) - Create UI on the EDT
        SwingUtilities.invokeLater(() -> {
            DiskAnalyzer analyzer = new DiskAnalyzer();
            analyzer.setVisible(true);
        });
    }
}
