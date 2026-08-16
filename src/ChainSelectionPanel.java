import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class ChainSelectionPanel extends JPanel {

    public interface ChainSelectionListener {
        void onChainSelected(char chain);
        void onBack();
    }

    private final JPanel chainsContainer;
    private final ButtonGroup buttonGroup;
    private final List<JRadioButton> radioButtons = new ArrayList<>();
    private final AppTheme.RoundedButton continueBtn;
    private final AppTheme.RoundedButton backBtn;
    private final JToggleButton themeToggle;
    
    private final JPanel card;
    private final JLabel titleLbl;
    private final JLabel subtitleLbl;
    private final JLabel selectPrompt;
    private final JScrollPane scrollPane;
    private final JPanel topBar;
    private final JPanel headerPanel;
    private final JPanel footerPanel;

    public ChainSelectionPanel(Runnable themeSwitcher, ChainSelectionListener listener) {
        setBackground(AppTheme.getBackground());
        setLayout(new BorderLayout(0, 24));
        setBorder(BorderFactory.createEmptyBorder(12, 40, 40, 40));

        // Top bar for global Theme Toggle
        topBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        topBar.setOpaque(false);
        themeToggle = AppTheme.createThemeToggle(themeSwitcher);
        topBar.add(themeToggle);
        add(topBar, BorderLayout.NORTH);

        // Header Section
        headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setBackground(AppTheme.getBackground());
        
        titleLbl = AppTheme.createLabel("Select Chain", AppTheme.TITLE, AppTheme.TEXT);
        titleLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        headerPanel.add(titleLbl);
        
        headerPanel.add(Box.createVerticalStrut(8));
        
        subtitleLbl = AppTheme.createLabel("Choose the chain to predict domains for across the loaded structures.", AppTheme.SUBTITLE, AppTheme.TEXT_MUTED);
        subtitleLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        headerPanel.add(subtitleLbl);

        // Wrap header in another panel to pack it cleanly
        JPanel headerWrapper = new JPanel(new BorderLayout());
        headerWrapper.setOpaque(false);
        headerWrapper.add(headerPanel, BorderLayout.CENTER);
        
        // Add header to centerWrapper, or manage layout
        // Let's place header in North of the main content center wrapper
        JPanel centerWrapper = new JPanel(new BorderLayout(0, 16));
        centerWrapper.setOpaque(false);
        centerWrapper.add(headerWrapper, BorderLayout.NORTH);

        // Center Section: Custom Card containing chains
        card = new JPanel(new BorderLayout(0, 16));
        card.setBackground(AppTheme.getSurface());
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.getBorder(), 1),
                BorderFactory.createEmptyBorder(24, 40, 24, 40)
        ));

        selectPrompt = AppTheme.createLabel("Available Chains", AppTheme.BODY_BOLD, AppTheme.TEXT);
        card.add(selectPrompt, BorderLayout.NORTH);

        chainsContainer = new JPanel();
        chainsContainer.setLayout(new BoxLayout(chainsContainer, BoxLayout.Y_AXIS));
        chainsContainer.setBackground(AppTheme.getSurface());
        
        buttonGroup = new ButtonGroup();

        scrollPane = new JScrollPane(chainsContainer);
        scrollPane.setBorder(null);
        scrollPane.setBackground(AppTheme.getSurface());
        scrollPane.getViewport().setBackground(AppTheme.getSurface());
        card.add(scrollPane, BorderLayout.CENTER);

        // Limit the size of the card panel to prevent it from occupying the entire screen
        JPanel cardWrapper = new JPanel(new GridBagLayout());
        cardWrapper.setOpaque(false);
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.BOTH;
        gc.weightx = 1.0;
        gc.weighty = 1.0;
        card.setPreferredSize(new Dimension(420, 280));
        cardWrapper.add(card, gc);

        centerWrapper.add(cardWrapper, BorderLayout.CENTER);
        add(centerWrapper, BorderLayout.CENTER);

        // Footer Section: Action Buttons
        footerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        footerPanel.setBackground(AppTheme.getBackground());

        backBtn = new AppTheme.RoundedButton(" Back", false);
        backBtn.setIcon(new AppTheme.ArrowIcon());
        backBtn.addActionListener(e -> {
            if (listener != null) listener.onBack();
        });
        footerPanel.add(backBtn);

        continueBtn = new AppTheme.RoundedButton("Continue to Prediction", true);
        continueBtn.setEnabled(false);
        continueBtn.addActionListener(e -> {
            if (listener != null) {
                for (JRadioButton rb : radioButtons) {
                    if (rb.isSelected()) {
                        char chain = rb.getActionCommand().charAt(0);
                        listener.onChainSelected(chain);
                        break;
                    }
                }
            }
        });
        footerPanel.add(continueBtn);

        add(footerPanel, BorderLayout.SOUTH);
    }

    public void applyTheme() {
        setBackground(AppTheme.getBackground());
        headerPanel.setBackground(AppTheme.getBackground());
        footerPanel.setBackground(AppTheme.getBackground());

        card.setBackground(AppTheme.getSurface());
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.getBorder(), 1),
                BorderFactory.createEmptyBorder(24, 40, 24, 40)
        ));

        chainsContainer.setBackground(AppTheme.getSurface());
        scrollPane.setBackground(AppTheme.getSurface());
        scrollPane.getViewport().setBackground(AppTheme.getSurface());

        titleLbl.repaint();
        subtitleLbl.repaint();
        selectPrompt.repaint();
        backBtn.repaint();
        continueBtn.repaint();

        for (JRadioButton rb : radioButtons) {
            rb.setBackground(AppTheme.getSurface());
            if (rb.isSelected()) {
                rb.setForeground(AppTheme.getText());
            } else {
                rb.setForeground(AppTheme.getTextMuted());
            }
            rb.repaint();
        }

        // Update theme toggle label
        AppTheme.updateThemeToggle(themeToggle);

        revalidate();
        repaint();
    }

    public void setChains(List<Character> chains, List<Integer> residueCounts) {
        chainsContainer.removeAll();
        buttonGroup.clearSelection();
        radioButtons.clear();
        continueBtn.setEnabled(false);

        if (chains == null || chains.isEmpty()) {
            JLabel emptyLbl = AppTheme.createLabel("No common chains found across loaded structures.", AppTheme.BODY, AppTheme.getError());
            emptyLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
            chainsContainer.add(emptyLbl);
        } else {
            for (int i = 0; i < chains.size(); i++) {
                char chain = chains.get(i);
                int count = i < residueCounts.size() ? residueCounts.get(i) : 0;
                
                String labelText = "Chain " + chain + "  (" + count + " residues)";
                JRadioButton rb = new JRadioButton(labelText) {
                    @Override
                    public Color getForeground() {
                        if (isSelected()) return AppTheme.getText();
                        return AppTheme.getTextMuted();
                    }
                };
                rb.setActionCommand(String.valueOf(chain));
                rb.setFont(AppTheme.BODY_BOLD);
                rb.setBackground(AppTheme.getSurface());
                rb.setFocusPainted(false);
                rb.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
                rb.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

                rb.addActionListener(e -> {
                    continueBtn.setEnabled(true);
                    for (JRadioButton button : radioButtons) {
                        button.repaint();
                    }
                });

                buttonGroup.add(rb);
                radioButtons.add(rb);
                chainsContainer.add(rb);
                chainsContainer.add(Box.createVerticalStrut(4));
            }

            // Automatically select the first one (usually chain A)
            if (!radioButtons.isEmpty()) {
                radioButtons.get(0).setSelected(true);
                continueBtn.setEnabled(true);
            }
        }

        chainsContainer.revalidate();
        chainsContainer.repaint();
    }
}
