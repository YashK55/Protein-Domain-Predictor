import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class PredictionPanel extends JPanel implements PipelinePanel.PipelineListener {

    public interface PredictionListener {
        void onBackToHome();
    }

    private final PipelinePanel pipelinePanel;
    private final GraphPanel graphPanel;
    
    private final JPanel canvasContainer;
    private final JPanel subStepContainer;
    
    private final JLabel metadataLbl;
    private final JLabel methodLbl;
    
    private final JDialog logDialog;
    private final JTextPane logTextPane;
    private final JScrollPane logScroll;
    private final JPanel dialogFooter;
    private final AppTheme.RoundedButton closeDialogBtn;
    
    private final JToggleButton themeToggle;
    
    private final List<GraphPanel.Snapshot> snapshots = new ArrayList<>();
    
    private final JPanel bottomPanel;
    private final JPanel metadataCol;
    private final JPanel actionsPanel;
    private final JPanel vizToolbar;
    
    private int currentStep = -1;

    public PredictionPanel(JFrame parentFrame, GraphPanel graphPanel, Runnable themeSwitcher, PredictionListener listener) {
        this.graphPanel = graphPanel;

        // Initialize final variables first
        canvasContainer = new JPanel(new BorderLayout());
        logDialog = new JDialog(parentFrame, "Prediction Details / Logs", false);
        logTextPane = new JTextPane();
        logScroll = new JScrollPane(logTextPane);
        dialogFooter = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 8));
        closeDialogBtn = new AppTheme.RoundedButton("Close", true);

        setBackground(AppTheme.getBackground());
        setLayout(new BorderLayout(12, 12));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        // 1. Pipeline Panel (Left)
        pipelinePanel = new PipelinePanel(this);
        pipelinePanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.getBorder(), 1),
                BorderFactory.createEmptyBorder(12, 0, 12, 0)
        ));
        add(pipelinePanel, BorderLayout.WEST);

        // 2. Center Panel (Visualization & Toolbar)
        JPanel centerPanel = new JPanel(new BorderLayout(0, 8));
        centerPanel.setBackground(AppTheme.getBackground());

        // Visualization Toolbar (Top of Center Panel)
        vizToolbar = new JPanel(new BorderLayout());
        vizToolbar.setBackground(AppTheme.getSurface());
        vizToolbar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.getBorder(), 1),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)
        ));

        // Title/Label on Left of Toolbar
        JLabel titleLabel = AppTheme.createLabel("Graph Canvas", AppTheme.BODY_BOLD, AppTheme.TEXT);
        vizToolbar.add(titleLabel, BorderLayout.WEST);

        // Dynamic Sub-step container (Middle)
        subStepContainer = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
        subStepContainer.setBackground(AppTheme.getSurface());
        vizToolbar.add(subStepContainer, BorderLayout.CENTER);

        // Theme Toggle on Right
        themeToggle = AppTheme.createThemeToggle(themeSwitcher);
        vizToolbar.add(themeToggle, BorderLayout.EAST);

        centerPanel.add(vizToolbar, BorderLayout.NORTH);

        // Canvas container (Holds GraphPanel)
        canvasContainer.setBackground(AppTheme.getCanvasBackground());
        canvasContainer.setBorder(BorderFactory.createLineBorder(AppTheme.getBorder(), 1));
        canvasContainer.add(graphPanel, BorderLayout.CENTER);
        
        centerPanel.add(canvasContainer, BorderLayout.CENTER);
        add(centerPanel, BorderLayout.CENTER);

        // 3. Bottom Panel (Metadata, Methods, Action Buttons)
        bottomPanel = new JPanel(new BorderLayout(12, 6));
        bottomPanel.setBackground(AppTheme.getSurface());
        bottomPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.getBorder(), 1),
                BorderFactory.createEmptyBorder(10, 16, 10, 16)
        ));

        metadataCol = new JPanel(new GridLayout(2, 1, 0, 2));
        metadataCol.setBackground(AppTheme.getSurface());

        metadataLbl = AppTheme.createLabel("Ready. Waiting for prediction...", AppTheme.BODY_BOLD, AppTheme.TEXT);
        metadataCol.add(metadataLbl);

        methodLbl = AppTheme.createLabel("Method: Contact Graph → Louvain → Line Graph → MAD → Boundary Optimization", AppTheme.SMALL, AppTheme.TEXT_MUTED);
        metadataCol.add(methodLbl);

        bottomPanel.add(metadataCol, BorderLayout.CENTER);

        actionsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actionsPanel.setBackground(AppTheme.getSurface());

        AppTheme.RoundedButton backBtn = new AppTheme.RoundedButton(" Back to Home", false);
        backBtn.setIcon(new AppTheme.ArrowIcon());
        backBtn.setFont(AppTheme.SMALL_BOLD);
        backBtn.addActionListener(e -> {
            if (listener != null) listener.onBackToHome();
        });
        actionsPanel.add(backBtn);

        AppTheme.RoundedButton showLogsBtn = new AppTheme.RoundedButton("Show Logs", false);
        showLogsBtn.setFont(AppTheme.SMALL_BOLD);
        showLogsBtn.addActionListener(e -> {
            logDialog.setLocationRelativeTo(parentFrame);
            logDialog.setVisible(true);
        });
        actionsPanel.add(showLogsBtn);

        bottomPanel.add(actionsPanel, BorderLayout.EAST);
        add(bottomPanel, BorderLayout.SOUTH);

        // 4. Log JDialog (Collapsible Logs Dialog)
        logDialog.setSize(650, 480);
        logDialog.setLayout(new BorderLayout());
        logDialog.setBackground(AppTheme.getBackground());

        logTextPane.setContentType("text/html");
        logTextPane.setEditable(false);
        
        AppTheme.styleScrollPane(logScroll);
        logDialog.add(logScroll, BorderLayout.CENTER);

        dialogFooter.setBackground(AppTheme.getBackground());
        dialogFooter.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, AppTheme.getBorder()));
        
        closeDialogBtn.setFont(AppTheme.SMALL_BOLD);
        closeDialogBtn.addActionListener(e -> logDialog.setVisible(false));
        dialogFooter.add(closeDialogBtn);
        logDialog.add(dialogFooter, BorderLayout.SOUTH);
    }

    public void applyTheme() {
        setBackground(AppTheme.getBackground());
        vizToolbar.setBackground(AppTheme.getSurface());
        vizToolbar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.getBorder(), 1),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)
        ));
        subStepContainer.setBackground(AppTheme.getSurface());

        pipelinePanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.getBorder(), 1),
                BorderFactory.createEmptyBorder(12, 0, 12, 0)
        ));

        canvasContainer.setBackground(AppTheme.getCanvasBackground());
        canvasContainer.setBorder(BorderFactory.createLineBorder(AppTheme.getBorder(), 1));

        bottomPanel.setBackground(AppTheme.getSurface());
        bottomPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.getBorder(), 1),
                BorderFactory.createEmptyBorder(10, 16, 10, 16)
        ));
        metadataCol.setBackground(AppTheme.getSurface());
        actionsPanel.setBackground(AppTheme.getSurface());

        logDialog.setBackground(AppTheme.getBackground());
        AppTheme.styleScrollPane(logScroll);
        dialogFooter.setBackground(AppTheme.getBackground());
        dialogFooter.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, AppTheme.getBorder()));

        // Repaint all
        pipelinePanel.repaint();
        graphPanel.repaint();
        
        // Refresh toggles
        updateToggleButtonsForeground();
        
        // Update theme toggle label
        AppTheme.updateThemeToggle(themeToggle);

        revalidate();
        repaint();
    }

    public void reset() {
        snapshots.clear();
        pipelinePanel.reset();
        graphPanel.show(null);
        subStepContainer.removeAll();
        subStepContainer.revalidate();
        subStepContainer.repaint();
        metadataLbl.setText("Ready. Waiting for prediction...");
        logTextPane.setText("");
        currentStep = -1;
    }

    public void addSnapshot(GraphPanel.Snapshot snapshot) {
        snapshots.add(snapshot);
        int stageIndex = mapSnapshotToStage(snapshot);
        if (stageIndex != -1) {
            pipelinePanel.setStepStatus(stageIndex, PipelinePanel.StepStatus.COMPLETED);
            
            // Auto-select latest completed step in the pipeline
            if (currentStep < stageIndex) {
                currentStep = stageIndex;
                pipelinePanel.setSelectedStep(stageIndex);
                updateSubStepToolbar(stageIndex);
            } else if (pipelinePanel.getSelectedStep() == stageIndex) {
                updateSubStepToolbar(stageIndex);
            }
        }
    }

    public void setStepStatus(int stageIndex, PipelinePanel.StepStatus status) {
        pipelinePanel.setStepStatus(stageIndex, status);
        if (status == PipelinePanel.StepStatus.ACTIVE) {
            pipelinePanel.setSelectedStep(stageIndex);
            updateSubStepToolbar(stageIndex);
        }
    }

    public void setMetadataText(String text) {
        SwingUtilities.invokeLater(() -> metadataLbl.setText(text));
    }

    public JTextPane getLogTextPane() {
        return logTextPane;
    }

    @Override
    public void onStepSelected(int stepIndex) {
        updateSubStepToolbar(stepIndex);
    }

    private int mapSnapshotToStage(GraphPanel.Snapshot snapshot) {
        String title = snapshot.title.toLowerCase();
        if (title.contains("protein graph")) return 1; // Major Stage 2 (Graph Prediction)
        if (title.contains("louvain") || title.contains("coarse-grained")) return 1; // Major Stage 2 (Graph Prediction)
        if (title.contains("line graph") || title.contains("mad")) return 2; // Major Stage 3 (Domain Detection)
        if (title.contains("label") || title.contains("remove negative")) return 2; // Major Stage 3 (Domain Detection)
        if (title.contains("rigidity") || title.contains("final")) return 3; // Major Stage 4 (Results)
        return -1;
    }

    private GraphPanel.Snapshot findSnapshotForStageAndSubStep(int stageIndex, int subIdx) {
        for (GraphPanel.Snapshot snap : snapshots) {
            if (mapSnapshotToStage(snap) == stageIndex) {
                int resolvedSubIdx = mapSnapshotToSubStepIndex(snap);
                if (resolvedSubIdx == subIdx) {
                    return snap;
                }
            }
        }
        return null;
    }

    private int mapSnapshotToSubStepIndex(GraphPanel.Snapshot snapshot) {
        String title = snapshot.title.toLowerCase();
        if (title.contains("protein graph")) return 0;
        if (title.contains("louvain communities")) return 1;
        if (title.contains("coarse-grained graph")) return 2;
        if (title.contains("line graph")) return 0;
        if (title.contains("mad outlier")) return 1;
        if (title.contains("label inference")) return 2;
        if (title.contains("remove negative")) return 3;
        if (title.contains("rigidity check")) return 0;
        if (title.contains("final domains")) return 1;
        return -1;
    }

    private void updateToggleButtonsForeground() {
        for (Component c : subStepContainer.getComponents()) {
            if (c instanceof JToggleButton) {
                JToggleButton btn = (JToggleButton) c;
                if (!btn.isEnabled()) {
                    btn.setForeground(AppTheme.getTextMuted());
                } else if (btn.isSelected()) {
                    btn.setForeground(Color.WHITE); // White text on selected indigo accent background
                } else {
                    btn.setForeground(AppTheme.getText());
                }
            }
        }
    }

    private void updateSubStepToolbar(int stageIndex) {
        subStepContainer.removeAll();
        
        String[] titles;
        if (stageIndex == 1) {
            titles = new String[]{"Contact Graph", "Louvain Partitioning", "Coarse Graph"};
        } else if (stageIndex == 2) {
            titles = new String[]{"Line Graph", "Hinge Detection", "Boundary Opt.", "Split Coarse Graph"};
        } else if (stageIndex == 3) {
            titles = new String[]{"Rigidity Check", "Final Domains"};
        } else {
            titles = new String[0]; // Stage 0 (Structure) has no sub-steps
        }

        if (titles.length > 0) {
            JLabel prompt = AppTheme.createLabel("Sub-steps: ", AppTheme.SMALL_BOLD, AppTheme.TEXT_MUTED);
            subStepContainer.add(prompt);

            ButtonGroup bg = new ButtonGroup();
            
            for (int i = 0; i < titles.length; i++) {
                final int subIdx = i;
                final GraphPanel.Snapshot snap = findSnapshotForStageAndSubStep(stageIndex, subIdx);
                
                JToggleButton btn = new JToggleButton(titles[i]) {
                    private boolean hovered = false;
                    {
                        addMouseListener(new java.awt.event.MouseAdapter() {
                            @Override
                            public void mouseEntered(java.awt.event.MouseEvent e) {
                                hovered = true;
                                repaint();
                            }
                            @Override
                            public void mouseExited(java.awt.event.MouseEvent e) {
                                hovered = false;
                                repaint();
                            }
                        });
                    }

                    @Override
                    protected void paintComponent(Graphics g) {
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        if (!isEnabled()) {
                            g2.setColor(AppTheme.getThemeMode() == AppTheme.ThemeMode.DARK ? new Color(30, 41, 67, 100) : new Color(226, 232, 240, 100));
                        } else if (isSelected()) {
                            g2.setColor(AppTheme.getAccent());
                        } else {
                            boolean isLight = AppTheme.getThemeMode() == AppTheme.ThemeMode.LIGHT;
                            g2.setColor(hovered 
                                ? (isLight ? new Color(226, 232, 240) : AppTheme.getSurfaceLight()) 
                                : AppTheme.getSurface());
                        }
                        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                        g2.setColor(AppTheme.getBorder());
                        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 6, 6);
                        g2.dispose();
                        super.paintComponent(g);
                    }
                };
                
                btn.setContentAreaFilled(false);
                btn.setFocusPainted(false);
                btn.setFont(AppTheme.SMALL_BOLD);
                btn.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
                btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

                if (snap == null) {
                    btn.setEnabled(false);
                    btn.setForeground(AppTheme.getTextMuted());
                } else {
                    btn.setEnabled(true);
                    btn.setForeground(AppTheme.getText());
                    btn.addActionListener(e -> {
                        graphPanel.show(snap);
                        updateToggleButtonsForeground();
                    });
                    
                    bg.add(btn);
                }
                
                subStepContainer.add(btn);
            }
            
            // Automatically select the last available sub-step to show the latest progress!
            JToggleButton lastAvailable = null;
            for (Component c : subStepContainer.getComponents()) {
                if (c instanceof JToggleButton && c.isEnabled()) {
                    lastAvailable = (JToggleButton) c;
                }
            }
            if (lastAvailable != null) {
                lastAvailable.setSelected(true);
                // Trigger action
                for (java.awt.event.ActionListener al : lastAvailable.getActionListeners()) {
                    al.actionPerformed(null);
                }
            }
            updateToggleButtonsForeground();
        } else {
            // Stage 0: Show empty state on graph
            graphPanel.show(null);
        }
        
        subStepContainer.revalidate();
        subStepContainer.repaint();
    }
}
