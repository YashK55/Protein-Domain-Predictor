import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.util.List;

/**
 * Renders one pipeline-step graph: nodes on a circular layout plus edges,
 * with interactive hover tracking to view information of graph elements.
 */
public class GraphPanel extends JPanel {

    public static final class Snapshot {
        final String title;
        final String subtitle;
        final int nodeCount;
        final List<int[]> edges;
        final String[] nodeLabels;
        final Color[] nodeColors;
        final Color[] edgeColors;

        public Snapshot(String title, String subtitle, int nodeCount,
                         List<int[]> edges, String[] nodeLabels,
                         Color[] nodeColors, Color[] edgeColors) {
            this.title = title;
            this.subtitle = subtitle;
            this.nodeCount = nodeCount;
            this.edges = edges;
            this.nodeLabels = nodeLabels;
            this.nodeColors = nodeColors;
            this.edgeColors = edgeColors;
        }
    }

    private static final Color DEFAULT_NODE = new Color(99, 102, 241); // Indigo
    private static final Color DEFAULT_EDGE = new Color(255, 255, 255, 45); // Light transparent gray

    private Snapshot snapshot;
    private int hoveredNode = -1;

    public GraphPanel() {
        setBackground(new Color(11, 15, 25)); // Slate background
        setPreferredSize(new Dimension(600, 420));

        MouseAdapter hoverAdapter = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int oldHovered = hoveredNode;
                hoveredNode = -1;

                if (snapshot != null && snapshot.nodeCount > 0) {
                    int cx = getWidth() / 2;
                    int cy = getHeight() / 2 + 14;
                    int radius = Math.max(30, Math.min(getWidth(), getHeight()) / 2 - 60);
                    int n = snapshot.nodeCount;
                    int nodeSize = n <= 40 ? 16 : n <= 120 ? 9 : 5;

                    for (int i = 0; i < n; i++) {
                        double angle = 2 * Math.PI * i / n - Math.PI / 2;
                        double x = cx + radius * Math.cos(angle);
                        double y = cy + radius * Math.sin(angle);
                        double dx = e.getX() - x;
                        double dy = e.getY() - y;
                        if (dx * dx + dy * dy <= (nodeSize / 2.0 + 4) * (nodeSize / 2.0 + 4)) {
                            hoveredNode = i;
                            break;
                        }
                    }
                }

                if (hoveredNode != oldHovered) {
                    repaint();
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (hoveredNode != -1) {
                    hoveredNode = -1;
                    repaint();
                }
            }
        };

        addMouseListener(hoverAdapter);
        addMouseMotionListener(hoverAdapter);
    }

    public void show(Snapshot s) {
        this.snapshot = s;
        this.hoveredNode = -1;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();

        if (snapshot == null || snapshot.nodeCount == 0) {
            g.setColor(new Color(156, 163, 175));
            g.drawString("No graph for this step yet.", 20, 30);
            return;
        }

        int cx = width / 2;
        int cy = height / 2 + 14;
        int radius = Math.max(30, Math.min(width, height) / 2 - 60);

        int n = snapshot.nodeCount;
        double[] xs = new double[n];
        double[] ys = new double[n];

        for (int i = 0; i < n; i++) {
            double angle = 2 * Math.PI * i / n - Math.PI / 2;
            xs[i] = cx + radius * Math.cos(angle);
            ys[i] = cy + radius * Math.sin(angle);
        }

        // Draw edges
        for (int i = 0; i < snapshot.edges.size(); i++) {
            int[] e = snapshot.edges.get(i);
            if (e[0] < 0 || e[0] >= n || e[1] < 0 || e[1] >= n) continue;

            if (hoveredNode != -1) {
                // Focus highlight: if edge connects to hovered node, paint cyan. Otherwise, draw faint.
                if (e[0] == hoveredNode || e[1] == hoveredNode) {
                    g.setColor(new Color(6, 182, 212, 220)); // Bright cyan
                    g.setStroke(new BasicStroke(2.0f));
                    g.drawLine((int) xs[e[0]], (int) ys[e[0]], (int) xs[e[1]], (int) ys[e[1]]);
                } else {
                    g.setColor(new Color(255, 255, 255, 10)); // Very dim edge
                    g.setStroke(new BasicStroke(1.0f));
                    g.drawLine((int) xs[e[0]], (int) ys[e[0]], (int) xs[e[1]], (int) ys[e[1]]);
                }
            } else {
                Color ec = (snapshot.edgeColors != null && i < snapshot.edgeColors.length && snapshot.edgeColors[i] != null)
                        ? snapshot.edgeColors[i] : DEFAULT_EDGE;
                g.setColor(ec);
                g.setStroke(new BasicStroke(1.0f));
                g.drawLine((int) xs[e[0]], (int) ys[e[0]], (int) xs[e[1]], (int) ys[e[1]]);
            }
        }

        g.setStroke(new BasicStroke(1.0f)); // reset stroke

        int nodeSize = n <= 40 ? 16 : n <= 120 ? 9 : 5;
        boolean showLabels = n <= 60 && snapshot.nodeLabels != null;

        // Draw nodes
        for (int i = 0; i < n; i++) {
            Color nc = (snapshot.nodeColors != null && i < snapshot.nodeColors.length && snapshot.nodeColors[i] != null)
                    ? snapshot.nodeColors[i] : DEFAULT_NODE;

            if (i == hoveredNode) {
                // Glowing outer ring for hovered node
                g.setColor(new Color(nc.getRed(), nc.getGreen(), nc.getBlue(), 80));
                g.fill(new Ellipse2D.Double(xs[i] - (nodeSize + 8) / 2.0, ys[i] - (nodeSize + 8) / 2.0, nodeSize + 8, nodeSize + 8));
            }

            g.setColor(nc);
            Ellipse2D circle = new Ellipse2D.Double(
                    xs[i] - nodeSize / 2.0, ys[i] - nodeSize / 2.0, nodeSize, nodeSize);
            g.fill(circle);

            // Node border outline
            g.setColor(i == hoveredNode ? Color.WHITE : new Color(255, 255, 255, 80));
            g.draw(circle);

            if (showLabels && snapshot.nodeLabels[i] != null) {
                g.setColor(i == hoveredNode ? Color.WHITE : new Color(156, 163, 175));
                g.setFont(g.getFont().deriveFont(10f));
                g.drawString(snapshot.nodeLabels[i], (float) xs[i] + nodeSize, (float) ys[i]);
            }
        }

        g.setColor(Color.WHITE);
        g.setFont(g.getFont().deriveFont(Font.BOLD, 15f));
        g.drawString(snapshot.title, 15, 22);

        if (snapshot.subtitle != null) {
            g.setFont(g.getFont().deriveFont(Font.PLAIN, 12f));
            g.setColor(new Color(156, 163, 175));
            g.drawString(snapshot.subtitle, 15, 40);
        }

        // Draw floating information card for hovered element
        if (hoveredNode != -1) {
            int connections = 0;
            for (int[] edge : snapshot.edges) {
                if (edge[0] == hoveredNode || edge[1] == hoveredNode) {
                    connections++;
                }
            }

            int cardW = 190;
            int cardH = 75;
            int cardX = 15;
            int cardY = height - cardH - 15;

            // Semi-transparent panel
            g.setColor(new Color(17, 24, 39, 230));
            g.fillRoundRect(cardX, cardY, cardW, cardH, 10, 10);
            g.setColor(new Color(255, 255, 255, 30));
            g.drawRoundRect(cardX, cardY, cardW - 1, cardH - 1, 10, 10);

            // Text entries
            g.setColor(Color.WHITE);
            g.setFont(g.getFont().deriveFont(Font.BOLD, 11f));
            g.drawString("Residue ID: " + (hoveredNode + 1), cardX + 12, cardY + 20);

            g.setFont(g.getFont().deriveFont(Font.PLAIN, 10f));
            g.setColor(new Color(156, 163, 175));
            g.drawString("Group: " + snapshot.nodeLabels[hoveredNode], cardX + 12, cardY + 38);
            g.drawString("Connections: " + connections + " edges", cardX + 12, cardY + 54);
        }
    }

    /** Deterministic, well-spread color for a palette index. */
    public static Color paletteColor(int index) {
        float hue = (float) ((index * 0.61803398875) % 1.0);
        return Color.getHSBColor(hue, 0.55f, 0.85f);
    }
}
