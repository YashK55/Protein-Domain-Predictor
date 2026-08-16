import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * Orbit-controlled viewer for superimposed CA backbone traces. Includes
 * interactive coordinate picking to inspect residues and domain bounds.
 */
public class Structure3DPanel extends JPanel {

    public static final class Trace {
        final String label;
        final List<double[]> points;

        public Trace(String label, List<double[]> points) {
            this.label = label;
            this.points = points;
        }
    }

    private List<Trace> traces = List.of();
    private Color[] residueColors = new Color[0];

    private double rotX = -0.4;
    private double rotY = 0.6;
    private double scale = 1.0;
    private double[] center = {0, 0, 0};

    private int lastX, lastY;

    private int hoveredPointIndex = -1;
    private int hoveredTraceIndex = -1;

    public Structure3DPanel() {
        setBackground(AppTheme.getCanvasBackground());
        setPreferredSize(new Dimension(700, 600));

        MouseAdapter mouseHandler = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                lastX = e.getX();
                lastY = e.getY();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                int dx = e.getX() - lastX;
                int dy = e.getY() - lastY;
                lastX = e.getX();
                lastY = e.getY();

                rotY += dx * 0.01;
                rotX += dy * 0.01;
                repaint();
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                int oldPt = hoveredPointIndex;
                int oldTr = hoveredTraceIndex;
                hoveredPointIndex = -1;
                hoveredTraceIndex = -1;

                if (!traces.isEmpty()) {
                    int cx = getWidth() / 2;
                    int cy = getHeight() / 2;
                    double cosX = Math.cos(rotX), sinX = Math.sin(rotX);
                    double cosY = Math.cos(rotY), sinY = Math.sin(rotY);

                    double bestDist = 12.0; // Picking radius threshold in pixels

                    for (int tIndex = 0; tIndex < traces.size(); tIndex++) {
                        Trace t = traces.get(tIndex);
                        for (int pIndex = 0; pIndex < t.points.size(); pIndex++) {
                            double[] p = t.points.get(pIndex);
                            double x = p[0] - center[0], y = p[1] - center[1], z = p[2] - center[2];

                            double x1 = x * cosY + z * sinY;
                            double z1 = -x * sinY + z * cosY;
                            double y1 = y * cosX - z1 * sinX;

                            int px = cx + (int) (x1 * scale);
                            int py = cy - (int) (y1 * scale);

                            double dx = e.getX() - px;
                            double dy = e.getY() - py;
                            double dist = Math.sqrt(dx * dx + dy * dy);

                            if (dist < bestDist) {
                                bestDist = dist;
                                hoveredPointIndex = pIndex;
                                hoveredTraceIndex = tIndex;
                            }
                        }
                    }
                }

                if (hoveredPointIndex != oldPt || hoveredTraceIndex != oldTr) {
                    repaint();
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (hoveredPointIndex != -1) {
                    hoveredPointIndex = -1;
                    hoveredTraceIndex = -1;
                    repaint();
                }
            }
        };

        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);

        addMouseWheelListener(e -> {
            scale *= Math.pow(1.1, -e.getWheelRotation());
            scale = Math.max(0.05, Math.min(50, scale));
            repaint();
        });
    }

    public List<Trace> getTraces() {
        return traces;
    }

    public Color[] getResidueColors() {
        return residueColors;
    }

    public void setData(List<Trace> traces, Color[] residueColors) {
        this.traces = traces;
        this.residueColors = residueColors;
        this.hoveredPointIndex = -1;
        this.hoveredTraceIndex = -1;
        resetCamera();
    }

    public void resetCamera() {
        double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE;
        double minY = Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        double minZ = Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;

        double sx = 0, sy = 0, sz = 0;
        int count = 0;

        for (Trace t : traces) {
            for (double[] p : t.points) {
                sx += p[0]; sy += p[1]; sz += p[2];
                count++;
                minX = Math.min(minX, p[0]); maxX = Math.max(maxX, p[0]);
                minY = Math.min(minY, p[1]); maxY = Math.max(maxY, p[1]);
                minZ = Math.min(minZ, p[2]); maxZ = Math.max(maxZ, p[2]);
            }
        }

        if (count > 0) {
            center = new double[]{sx / count, sy / count, sz / count};
            double extent = Math.max(maxX - minX, Math.max(maxY - minY, maxZ - minZ));
            scale = extent > 1e-6 ? 300.0 / extent : 1.0;
        }

        rotX = -0.4;
        rotY = 0.6;

        repaint();
    }

    @Override
    protected void paintComponent(Graphics g0) {
        // Dynamically set background
        setBackground(AppTheme.getCanvasBackground());
        super.paintComponent(g0);

        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();

        boolean isLight = AppTheme.getThemeMode() == AppTheme.ThemeMode.LIGHT;

        if (traces.isEmpty()) {
            g.setColor(AppTheme.getText());
            g.setFont(AppTheme.BODY_BOLD);
            FontMetrics fm = g.getFontMetrics();
            String line1 = "No 3D Superposition";
            String line2 = "Complete the required analysis steps to enable 3D visualization.";
            
            int w1 = fm.stringWidth(line1);
            g.drawString(line1, (width - w1) / 2, height / 2 - 12);
            
            g.setFont(AppTheme.BODY);
            g.setColor(AppTheme.getTextMuted());
            FontMetrics fm2 = g.getFontMetrics();
            int w2 = fm2.stringWidth(line2);
            g.drawString(line2, (width - w2) / 2, height / 2 + 12);
            return;
        }

        int cx = width / 2;
        int cy = height / 2;

        double cosX = Math.cos(rotX), sinX = Math.sin(rotX);
        double cosY = Math.cos(rotY), sinY = Math.sin(rotY);

        g.setStroke(new BasicStroke(2.0f));

        int highlightPx = -1;
        int highlightPy = -1;

        for (int tIndex = 0; tIndex < traces.size(); tIndex++) {
            Trace t = traces.get(tIndex);
            int n = t.points.size();
            int[] px = new int[n];
            int[] py = new int[n];

            for (int i = 0; i < n; i++) {
                double[] p = t.points.get(i);
                double x = p[0] - center[0], y = p[1] - center[1], z = p[2] - center[2];

                double x1 = x * cosY + z * sinY;
                double z1 = -x * sinY + z * cosY;
                double y1 = y * cosX - z1 * sinX;

                px[i] = cx + (int) (x1 * scale);
                py[i] = cy - (int) (y1 * scale);

                if (tIndex == hoveredTraceIndex && i == hoveredPointIndex) {
                    highlightPx = px[i];
                    highlightPy = py[i];
                }
            }

            for (int i = 0; i < n - 1; i++) {
                Color c = (residueColors != null && i < residueColors.length && residueColors[i] != null)
                        ? residueColors[i] : AppTheme.getTextMuted();
                g.setColor(c);
                g.drawLine(px[i], py[i], px[i + 1], py[i + 1]);
            }
        }

        // Draw selection ring for hovered 3D point
        if (highlightPx != -1 && highlightPy != -1) {
            g.setColor(isLight ? new Color(0, 0, 0, 100) : new Color(255, 255, 255, 100));
            g.setStroke(new BasicStroke(1.5f));
            g.drawOval(highlightPx - 7, highlightPy - 7, 14, 14);
            g.setColor(isLight ? new Color(6, 143, 170) : Color.CYAN);
            g.fillOval(highlightPx - 3, highlightPy - 3, 6, 6);
        }

        g.setStroke(new BasicStroke(1.0f)); // reset stroke

        g.setColor(AppTheme.getText());
        g.setFont(AppTheme.BODY_BOLD);
        g.drawString("Superimposed structures (drag to rotate, scroll to zoom)", 15, 22);

        // Draw floating information card for hovered 3D element
        if (hoveredPointIndex != -1 && hoveredTraceIndex != -1) {
            int cardW = 210;
            int cardH = 75;
            int cardX = 15;
            int cardY = height - cardH - 15;

            // Semi-transparent panel
            g.setColor(AppTheme.getSurface());
            g.fillRoundRect(cardX, cardY, cardW, cardH, 10, 10);
            g.setColor(AppTheme.getBorder());
            g.drawRoundRect(cardX, cardY, cardW - 1, cardH - 1, 10, 10);

            // Text entries
            g.setColor(AppTheme.getText());
            g.setFont(AppTheme.BODY_BOLD);
            g.drawString("Conformer: " + traces.get(hoveredTraceIndex).label, cardX + 12, cardY + 20);

            g.setFont(AppTheme.SMALL);
            g.setColor(AppTheme.getTextMuted());
            g.drawString("Residue ID: " + (hoveredPointIndex + 1), cardX + 12, cardY + 38);

            Color rc = (residueColors != null && hoveredPointIndex < residueColors.length) ? residueColors[hoveredPointIndex] : null;
            String classification = "Flexible/Hinge";
            if (rc != null) {
                for (int d = 0; d < 10; d++) {
                    if (rc.equals(GraphPanel.paletteColor(d))) {
                        classification = "Domain " + (d + 1);
                        break;
                    }
                }
            }
            g.drawString("Domain: " + classification, cardX + 12, cardY + 54);
        }
    }
}
