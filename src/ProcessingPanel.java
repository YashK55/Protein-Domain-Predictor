import javax.swing.*;
import java.awt.*;

public class ProcessingPanel extends JPanel {

    public enum StepState {
        PENDING, ACTIVE, COMPLETED
    }

    private final JLabel[] stepIndicators = new JLabel[4];
    private final JLabel[] stepLabels = new JLabel[4];
    
    private final JLabel structuresValue;
    private final JLabel residuesValue;
    private final JLabel statusValue;
    
    private final JPanel stepsCard;
    private final JPanel infoPanel;
    private final JLabel titleLbl;
    private final JLabel structuresLbl;
    private final JLabel residuesLbl;
    private final JLabel statusLbl;

    public ProcessingPanel() {
        setBackground(AppTheme.getBackground());
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createEmptyBorder(40, 40, 40, 40));

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = GridBagConstraints.RELATIVE;
        c.insets = new Insets(10, 0, 10, 0);
        c.anchor = GridBagConstraints.CENTER;

        // Title
        titleLbl = AppTheme.createLabel("Preparing Structures", AppTheme.TITLE, AppTheme.TEXT);
        add(titleLbl, c);

        c.insets = new Insets(20, 0, 20, 0);

        // Steps Card
        stepsCard = new JPanel(new GridBagLayout());
        stepsCard.setBackground(AppTheme.getSurface());
        stepsCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.getBorder(), 1),
                BorderFactory.createEmptyBorder(20, 30, 20, 30)
        ));

        GridBagConstraints sc = new GridBagConstraints();
        sc.anchor = GridBagConstraints.WEST;
        sc.insets = new Insets(10, 10, 10, 10);

        String[] stepTexts = {
                "Loading PDB structures",
                "Reading residues",
                "Detecting available chains",
                "Preparing analysis"
        };

        for (int i = 0; i < 4; i++) {
            sc.gridx = 0;
            sc.gridy = i;
            stepIndicators[i] = new JLabel("○") {
                // Ensure correct color mapping on theme update
                @Override
                public Color getForeground() {
                    // Check if it is a pending indicator or checkmark
                    String t = getText();
                    if (t.equals("○")) return AppTheme.getTextMuted();
                    if (t.equals("✓")) return AppTheme.getSuccess();
                    return AppTheme.getAccent(); // active
                }
            };
            stepIndicators[i].setFont(new Font("SansSerif", Font.BOLD, 16));
            stepsCard.add(stepIndicators[i], sc);

            sc.gridx = 1;
            stepLabels[i] = AppTheme.createLabel(stepTexts[i], AppTheme.BODY, AppTheme.TEXT_MUTED);
            stepsCard.add(stepLabels[i], sc);
        }

        // Set card width preferred size
        stepsCard.setPreferredSize(new Dimension(380, 200));
        add(stepsCard, c);

        // Info Card
        c.insets = new Insets(10, 0, 10, 0);
        infoPanel = new JPanel(new GridLayout(3, 2, 20, 8));
        infoPanel.setBackground(AppTheme.getBackground());
        infoPanel.setPreferredSize(new Dimension(280, 90));

        structuresLbl = AppTheme.createLabel("Structures:", AppTheme.BODY_BOLD, AppTheme.TEXT_MUTED);
        infoPanel.add(structuresLbl);
        structuresValue = AppTheme.createLabel("—", AppTheme.BODY, AppTheme.TEXT);
        infoPanel.add(structuresValue);

        residuesLbl = AppTheme.createLabel("Residues:", AppTheme.BODY_BOLD, AppTheme.TEXT_MUTED);
        infoPanel.add(residuesLbl);
        residuesValue = AppTheme.createLabel("—", AppTheme.BODY, AppTheme.TEXT);
        infoPanel.add(residuesValue);

        statusLbl = AppTheme.createLabel("Status:", AppTheme.BODY_BOLD, AppTheme.TEXT_MUTED);
        infoPanel.add(statusLbl);
        statusValue = AppTheme.createLabel("Preparing", AppTheme.BODY, AppTheme.getAccent());
        infoPanel.add(statusValue);

        add(infoPanel, c);
    }

    public void applyTheme() {
        setBackground(AppTheme.getBackground());
        
        stepsCard.setBackground(AppTheme.getSurface());
        stepsCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.getBorder(), 1),
                BorderFactory.createEmptyBorder(20, 30, 20, 30)
        ));

        infoPanel.setBackground(AppTheme.getBackground());

        titleLbl.repaint();
        structuresLbl.repaint();
        structuresValue.repaint();
        residuesLbl.repaint();
        residuesValue.repaint();
        statusLbl.repaint();
        
        // Status value is indigo accent
        statusValue.setForeground(AppTheme.getAccent());
        statusValue.repaint();

        for (int i = 0; i < 4; i++) {
            stepIndicators[i].repaint();
            stepLabels[i].repaint();
        }

        revalidate();
        repaint();
    }

    public void updateStep(int stepIndex, StepState state) {
        if (stepIndex < 0 || stepIndex >= 4) return;

        SwingUtilities.invokeLater(() -> {
            switch (state) {
                case PENDING:
                    stepIndicators[stepIndex].setText("○");
                    stepLabels[stepIndex].setForeground(AppTheme.getTextMuted());
                    break;
                case ACTIVE:
                    stepIndicators[stepIndex].setText("●");
                    stepLabels[stepIndex].setForeground(AppTheme.getText());
                    break;
                case COMPLETED:
                    stepIndicators[stepIndex].setText("✓");
                    stepLabels[stepIndex].setForeground(AppTheme.getTextMuted());
                    break;
            }
            repaint();
        });
    }

    public void setDetails(String structures, String residues, String status) {
        SwingUtilities.invokeLater(() -> {
            structuresValue.setText(structures);
            residuesValue.setText(residues);
            statusValue.setText(status);
            repaint();
        });
    }

    public void reset() {
        for (int i = 0; i < 4; i++) {
            updateStep(i, StepState.PENDING);
        }
        setDetails("—", "—", "Preparing");
    }
}
