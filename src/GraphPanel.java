import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.util.List;

/**
 * Renders one pipeline-step graph: nodes on a circular layout plus edges,
 * with optional per-node/per-edge colors and labels.
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

    private static final Color DEFAULT_NODE = new Color(70, 130, 180);
    private static final Color DEFAULT_EDGE = new Color(120, 120, 120, 140);

    private Snapshot snapshot;

    public GraphPanel() {
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(600, 420));
    }

    public void show(Snapshot s) {
        this.snapshot = s;
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
            g.setColor(Color.GRAY);
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

        for (int i = 0; i < snapshot.edges.size(); i++) {
            int[] e = snapshot.edges.get(i);
            if (e[0] < 0 || e[0] >= n || e[1] < 0 || e[1] >= n) continue;

            Color ec = (snapshot.edgeColors != null && i < snapshot.edgeColors.length)
                    ? snapshot.edgeColors[i] : DEFAULT_EDGE;
            g.setColor(ec);
            g.drawLine((int) xs[e[0]], (int) ys[e[0]], (int) xs[e[1]], (int) ys[e[1]]);
        }

        int nodeSize = n <= 40 ? 16 : n <= 120 ? 9 : 5;
        boolean showLabels = n <= 60 && snapshot.nodeLabels != null;

        for (int i = 0; i < n; i++) {
            Color nc = (snapshot.nodeColors != null && i < snapshot.nodeColors.length && snapshot.nodeColors[i] != null)
                    ? snapshot.nodeColors[i] : DEFAULT_NODE;

            g.setColor(nc);
            Ellipse2D circle = new Ellipse2D.Double(
                    xs[i] - nodeSize / 2.0, ys[i] - nodeSize / 2.0, nodeSize, nodeSize);
            g.fill(circle);
            g.setColor(Color.DARK_GRAY);
            g.draw(circle);

            if (showLabels && snapshot.nodeLabels[i] != null) {
                g.setFont(g.getFont().deriveFont(10f));
                g.drawString(snapshot.nodeLabels[i], (float) xs[i] + nodeSize, (float) ys[i]);
            }
        }

        g.setColor(Color.BLACK);
        g.setFont(g.getFont().deriveFont(Font.BOLD, 15f));
        g.drawString(snapshot.title, 15, 22);

        if (snapshot.subtitle != null) {
            g.setFont(g.getFont().deriveFont(Font.PLAIN, 12f));
            g.setColor(Color.DARK_GRAY);
            g.drawString(snapshot.subtitle, 15, 40);
        }
    }

    /** Deterministic, well-spread color for a palette index (community/domain id, etc). */
    public static Color paletteColor(int index) {
        float hue = (float) ((index * 0.61803398875) % 1.0);
        return Color.getHSBColor(hue, 0.55f, 0.85f);
    }
}
