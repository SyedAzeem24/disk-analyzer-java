package diskanalyzer;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;

/*
 * ============================================================================
 * CONCEPT: Refactoring (Week 15)
 * ============================================================================
 * Single Responsibility Principle: This class handles ONLY the visual
 * rendering of tree nodes. It does not contain any scanning logic, data
 * model code, or event handling. This clean separation means:
 *   - Visual appearance can be changed without touching data or logic
 *   - Different renderers could be swapped in (e.g., pie chart renderer)
 *   - The renderer can be tested visually in isolation
 *
 * CONCEPT: Managing Complexity (Week 3)
 * ============================================================================
 * Abstraction layers in this project:
 *   Layer 1 (Data):    FileNode — raw data about files and sizes
 *   Layer 2 (Format):  FileSizeFormatter — converts bytes to readable strings
 *   Layer 3 (Render):  DiskUsageCellRenderer — visual presentation (THIS CLASS)
 *   Layer 4 (UI):      DiskAnalyzer — user interaction and layout
 *
 * Each layer only depends on the layer(s) below it, never above.
 * ============================================================================
 */

/**
 * Custom tree cell renderer that displays file/folder nodes with:
 * <ul>
 *   <li>File/folder icons</li>
 *   <li>Name and formatted size</li>
 *   <li>A visual progress bar showing size relative to parent</li>
 *   <li>Color coding based on size percentage</li>
 * </ul>
 *
 * <p><b>CONCEPT: Refactoring (Week 15)</b> – This renderer is a separate class
 * with a single responsibility: rendering tree nodes visually.</p>
 */
public class DiskUsageCellRenderer extends DefaultTreeCellRenderer {

    // ========================================================================
    // CONCEPT: Coding Standards (Week 2) - Named constants, not magic numbers
    // ========================================================================

    /** Width of the progress bar in pixels. */
    private static final int BAR_WIDTH = 120;

    /** Height of the progress bar in pixels. */
    private static final int BAR_HEIGHT = 14;

    /** Padding between label text and progress bar. */
    private static final int PADDING = 8;

    /** Threshold percentage for "large" items (displayed in warm colors). */
    private static final double LARGE_THRESHOLD = 50.0;

    /** Threshold percentage for "medium" items. */
    private static final double MEDIUM_THRESHOLD = 20.0;

    // Color palette for size visualization
    private static final Color COLOR_LARGE  = new Color(220, 60, 60);    // Red for large
    private static final Color COLOR_MEDIUM = new Color(230, 160, 40);   // Amber for medium
    private static final Color COLOR_SMALL  = new Color(60, 160, 100);   // Green for small
    private static final Color COLOR_BAR_BG = new Color(60, 63, 70);     // Dark background
    private static final Color COLOR_TEXT    = new Color(220, 222, 228);  // Light text
    private static final Color COLOR_SIZE    = new Color(150, 155, 168);  // Dimmer size text
    private static final Color COLOR_BG_SELECTED   = new Color(45, 85, 145);
    private static final Color COLOR_BG_UNSELECTED = new Color(40, 42, 48);

    /** Custom panel that renders the node with label + progress bar. */
    private final NodePanel nodePanel = new NodePanel();

    /**
     * Returns the component used to render a tree cell.
     *
     * <p>This method is called by JTree for EACH visible node whenever
     * the tree needs to be painted. We return a custom panel that includes
     * the node name, formatted size, and a proportional progress bar.</p>
     */
    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value,
            boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {

        // Extract FileNode from the tree node
        FileNode fileNode = null;
        if (value instanceof DefaultMutableTreeNode) {
            Object userObj = ((DefaultMutableTreeNode) value).getUserObject();
            if (userObj instanceof FileNode) {
                fileNode = (FileNode) userObj;
            }
        }

        if (fileNode == null) {
            // Fallback to default rendering for non-FileNode nodes
            return super.getTreeCellRendererComponent(tree, value, selected,
                expanded, leaf, row, hasFocus);
        }

        // Calculate percentage relative to parent
        double percentage = 0.0;
        DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) value;
        DefaultMutableTreeNode parentTreeNode =
            (DefaultMutableTreeNode) treeNode.getParent();

        if (parentTreeNode != null && parentTreeNode.getUserObject() instanceof FileNode) {
            FileNode parentFileNode = (FileNode) parentTreeNode.getUserObject();
            if (parentFileNode.getSize() > 0) {
                percentage = FileSizeFormatter.percentage(
                    fileNode.getSize(), parentFileNode.getSize());
            }
        } else {
            // Root node — 100% of itself
            percentage = 100.0;
        }

        // Configure the custom panel
        nodePanel.configure(fileNode, percentage, selected);
        return nodePanel;
    }

    // ========================================================================
    // Inner class for custom rendering panel
    // ========================================================================

    /**
     * Custom panel that renders a single tree node with:
     * - An icon (folder/file)
     * - The node name and formatted size
     * - A progress bar showing relative size
     */
    private static class NodePanel extends JPanel {

        private String displayName = "";
        private String displaySize = "";
        private double percentage = 0.0;
        private boolean isDirectory = false;
        private boolean isSelected = false;

        NodePanel() {
            setOpaque(true);
            setFont(new Font("Segoe UI", Font.PLAIN, 13));
        }

        /**
         * Configures this panel for the given FileNode.
         */
        void configure(FileNode node, double pct, boolean selected) {
            this.displayName = node.getName();
            this.displaySize = FileSizeFormatter.formatCompact(node.getSize());
            this.percentage = pct;
            this.isDirectory = node.isDirectory();
            this.isSelected = selected;
        }

        @Override
        public Dimension getPreferredSize() {
            FontMetrics fm = getFontMetrics(getFont());
            int textWidth = fm.stringWidth(displayName + "  " + displaySize);
            int iconWidth = 22; // icon space
            int totalWidth = iconWidth + textWidth + PADDING + BAR_WIDTH + PADDING * 2;
            int height = Math.max(fm.getHeight() + 8, BAR_HEIGHT + 6);
            return new Dimension(totalWidth, height);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

            int w = getWidth();
            int h = getHeight();

            // Background
            g2.setColor(isSelected ? COLOR_BG_SELECTED : COLOR_BG_UNSELECTED);
            g2.fillRect(0, 0, w, h);

            FontMetrics fm = g2.getFontMetrics(getFont());
            int textY = (h + fm.getAscent() - fm.getDescent()) / 2;
            int x = 4;

            // Draw icon
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            String icon = isDirectory ? "\uD83D\uDCC1" : "\uD83D\uDCC4";
            g2.setColor(COLOR_TEXT);
            g2.drawString(icon, x, textY);
            x += 22;

            // Draw name
            g2.setFont(getFont());
            g2.setColor(COLOR_TEXT);
            g2.drawString(displayName, x, textY);
            x += fm.stringWidth(displayName) + 6;

            // Draw size
            g2.setColor(COLOR_SIZE);
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            g2.drawString(displaySize, x, textY);
            x += g2.getFontMetrics().stringWidth(displaySize) + PADDING;

            // Draw progress bar
            int barX = x;
            int barY = (h - BAR_HEIGHT) / 2;

            // Bar background
            g2.setColor(COLOR_BAR_BG);
            g2.fillRoundRect(barX, barY, BAR_WIDTH, BAR_HEIGHT, 6, 6);

            // Bar fill
            int fillWidth = (int) (BAR_WIDTH * percentage / 100.0);
            if (fillWidth > 0) {
                Color barColor = getBarColor(percentage);
                g2.setColor(barColor);
                g2.fillRoundRect(barX, barY, fillWidth, BAR_HEIGHT, 6, 6);
            }

            // Percentage text inside bar
            g2.setColor(new Color(255, 255, 255, 200));
            g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
            String pctText = String.format("%.1f%%", percentage);
            FontMetrics pctFm = g2.getFontMetrics();
            int pctX = barX + (BAR_WIDTH - pctFm.stringWidth(pctText)) / 2;
            int pctY = barY + (BAR_HEIGHT + pctFm.getAscent() - pctFm.getDescent()) / 2;
            g2.drawString(pctText, pctX, pctY);

            g2.dispose();
        }

        /**
         * Returns the appropriate color based on percentage.
         * Large percentages get warm colors (red), small get cool (green).
         */
        private Color getBarColor(double pct) {
            if (pct >= LARGE_THRESHOLD) {
                return COLOR_LARGE;
            } else if (pct >= MEDIUM_THRESHOLD) {
                return COLOR_MEDIUM;
            } else {
                return COLOR_SMALL;
            }
        }
    }
}
