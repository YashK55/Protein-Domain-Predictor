import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class PipelinePanel extends JPanel {

    public interface PipelineListener {
        void onStepSelected(int stepIndex);
    }

    public enum StepStatus {
        PENDING, ACTIVE, COMPLETED, ERROR
    }

    private final String[] stepTitles = {
            "Structure",
            "Graph Analysis",
            "Domain Detection",
            "Results"
    };

    private final StepStatus[] stepStatuses = new StepStatus[4];
    private int selectedStep = -1;
    private final PipelineListener listener;

    private static final int START_Y = 55;
    private static final int STEP_HEIGHT = 68;
    private static final int LEFT_MARGIN = 24;

    public PipelinePanel(PipelineListener listener) {
        this.listener = listener;
        setPreferredSize(new Dimension(240, 400));
        
        // Initialize statuses
        for (int i = 0; i < 4; i++) {
            stepStatuses[i] = StepStatus.PENDING;
        }

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int y = e.getY();
                int x = e.getX();
                if (x >= LEFT_MARGIN - 15) {
                    int step = (y - START_Y) / STEP_HEIGHT;
                    if (step >= 0 && step < 4) {
                        // Allow selecting steps that are COMPLETED or ACTIVE
                        if (stepStatuses[step] == StepStatus.COMPLETED || stepStatuses[step] == StepStatus.ACTIVE) {
                            setSelectedStep(step);
                            if (PipelinePanel.this.listener != null) {
                                PipelinePanel.this.listener.onStepSelected(step);
                            }
                        }
                    }
                }
            }
        });
    }

    public void setStepStatus(int stepIndex, StepStatus status) {
        if (stepIndex >= 0 && stepIndex < 4) {
            stepStatuses[stepIndex] = status;
            repaint();
        }
    }

    public int getSelectedStep() {
        return selectedStep;
    }

    public void setSelectedStep(int stepIndex) {
        this.selectedStep = stepIndex;
        repaint();
    }

    public void reset() {
        for (int i = 0; i < 4; i++) {
            stepStatuses[i] = StepStatus.PENDING;
        }
        selectedStep = -1;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g0) {
        // Set background dynamically
        setBackground(AppTheme.getSurface());
        super.paintComponent(g0);

        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Sidebar title
        g.setFont(AppTheme.SMALL_BOLD);
        g.setColor(AppTheme.getTextMuted());
        g.drawString("ANALYSIS WORKFLOW", LEFT_MARGIN, 25);

        // Draw connecting vertical line
        g.setColor(AppTheme.getBorder());
        g.setStroke(new BasicStroke(2.0f));
        int cx = LEFT_MARGIN + 10;
        int lineStartY = START_Y + 16;
        int lineEndY = START_Y + 3 * STEP_HEIGHT + 16;
        g.drawLine(cx, lineStartY, cx, lineEndY);

        // Draw each step
        for (int i = 0; i < 4; i++) {
            int cy = START_Y + i * STEP_HEIGHT + 16;
            StepStatus status = stepStatuses[i];
            boolean isSelected = (i == selectedStep);

            // 1. Draw Step Card Background if Selected
            if (isSelected) {
                g.setColor(AppTheme.getThemeMode() == AppTheme.ThemeMode.DARK ? new Color(255, 255, 255, 10) : new Color(0, 0, 0, 15));
                g.fillRoundRect(cx + 25, cy - 22, getWidth() - cx - 35, 48, 8, 8);
            }

            // 2. Draw circle indicator
            if (status == StepStatus.PENDING) {
                g.setColor(AppTheme.getBackground());
                g.fillOval(cx - 8, cy - 8, 16, 16);
                g.setColor(AppTheme.getBorder());
                g.setStroke(new BasicStroke(2.0f));
                g.drawOval(cx - 8, cy - 8, 16, 16);
            } else if (status == StepStatus.ACTIVE) {
                // Outer glow ring
                g.setColor(new Color(99, 102, 241, 70));
                g.fillOval(cx - 11, cy - 11, 22, 22);
                
                g.setColor(AppTheme.getAccent());
                g.fillOval(cx - 7, cy - 7, 14, 14);
            } else if (status == StepStatus.COMPLETED) {
                g.setColor(AppTheme.getSuccess());
                g.fillOval(cx - 8, cy - 8, 16, 16);
                // Draw tiny checkmark inside
                g.setColor(Color.WHITE);
                g.setStroke(new BasicStroke(1.5f));
                g.drawLine(cx - 4, cy, cx - 1, cy + 3);
                g.drawLine(cx - 1, cy + 3, cx + 4, cy - 3);
            } else if (status == StepStatus.ERROR) {
                g.setColor(AppTheme.getError());
                g.fillOval(cx - 8, cy - 8, 16, 16);
                // Draw X
                g.setColor(Color.WHITE);
                g.setStroke(new BasicStroke(1.5f));
                g.drawLine(cx - 3, cy - 3, cx + 3, cy + 3);
                g.drawLine(cx + 3, cy - 3, cx - 3, cy + 3);
            }

            // 3. Draw Labels (Number & Step Title)
            String stepNum = "0" + (i + 1);
            g.setFont(AppTheme.BODY_BOLD);
            g.setColor(isSelected ? AppTheme.getText() : AppTheme.getTextMuted());
            g.drawString(stepNum, cx + 35, cy - 3);

            g.setFont(isSelected ? AppTheme.BODY_BOLD : AppTheme.BODY);
            g.drawString(stepTitles[i], cx + 58, cy - 3);

            // 4. Draw Status Sub-label
            g.setFont(AppTheme.SMALL_BOLD);
            if (status == StepStatus.PENDING) {
                g.setColor(AppTheme.getTextMuted());
                g.drawString("○ Pending", cx + 35, cy + 14);
            } else if (status == StepStatus.ACTIVE) {
                g.setColor(AppTheme.getAccent());
                g.drawString("● Current", cx + 35, cy + 14);
            } else if (status == StepStatus.COMPLETED) {
                g.setColor(AppTheme.getSuccess());
                g.drawString("✓ Complete", cx + 35, cy + 14);
            } else if (status == StepStatus.ERROR) {
                g.setColor(AppTheme.getError());
                g.drawString("× Error", cx + 35, cy + 14);
            }
        }
    }
}
