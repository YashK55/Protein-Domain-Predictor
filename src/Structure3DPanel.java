import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * Orbit-controlled viewer for superimposed CA backbone traces. Each trace is
 * one conformer, already rotated/translated into a common reference frame;
 * segments are colored per residue (typically by detected domain) so that
 * rigid domains visually overlay while flexible/hinge regions fan out.
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

    public Structure3DPanel() {
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(700, 600));

        MouseAdapter drag = new MouseAdapter() {
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
        };

        addMouseListener(drag);
        addMouseMotionListener(drag);

        addMouseWheelListener(e -> {
            scale *= Math.pow(1.1, -e.getWheelRotation());
            scale = Math.max(0.05, Math.min(50, scale));
            repaint();
        });
    }

    public void setData(List<Trace> traces, Color[] residueColors) {
        this.traces = traces;
        this.residueColors = residueColors;

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
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (traces.isEmpty()) {
            g.setColor(Color.GRAY);
            g.drawString("Run an analysis first, then click Show Superimposition.", 20, 30);
            return;
        }

        int cx = getWidth() / 2;
        int cy = getHeight() / 2;

        double cosX = Math.cos(rotX), sinX = Math.sin(rotX);
        double cosY = Math.cos(rotY), sinY = Math.sin(rotY);

        g.setStroke(new BasicStroke(2f));

        for (Trace t : traces) {
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
            }

            for (int i = 0; i < n - 1; i++) {
                Color c = (residueColors != null && i < residueColors.length && residueColors[i] != null)
                        ? residueColors[i] : Color.DARK_GRAY;
                g.setColor(c);
                g.drawLine(px[i], py[i], px[i + 1], py[i + 1]);
            }
        }

        g.setColor(Color.BLACK);
        g.setFont(g.getFont().deriveFont(Font.BOLD, 14f));
        g.drawString("Superimposed structures (drag to rotate, scroll to zoom)", 15, 22);
    }
}
