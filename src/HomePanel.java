import javax.swing.*;
import java.awt.*;
import java.io.File;

public class HomePanel extends JPanel {

    public interface HomeListener {
        void onPredictIds(String pdbIds);
        void onOpenFiles();
        void onViewReferences();
        void onViewAbout();
    }

    private final AppTheme.RoundedTextField pdbInputField;
    private final JPanel card;
    private final JLabel titleLbl;
    private final JLabel subtitleLbl;
    private final JLabel descLbl;
    private final JLabel inputPrompt;
    private final JLabel orLbl;
    private final JPanel footer;
    
    private final AppTheme.RoundedButton predictBtn;
    private final AppTheme.RoundedButton loadFilesBtn;
    private final AppTheme.RoundedButton refBtn;
    private final AppTheme.RoundedButton aboutBtn;
    private final JToggleButton themeToggle;
    private final JPanel topBar;

    public HomePanel(Runnable themeSwitcher, HomeListener listener) {
        setBackground(AppTheme.getBackground());
        setLayout(new BorderLayout());

        // Top bar for global Theme Toggle
        topBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 16, 12));
        topBar.setBackground(AppTheme.getBackground());
        themeToggle = AppTheme.createThemeToggle(themeSwitcher);
        topBar.add(themeToggle);
        add(topBar, BorderLayout.NORTH);

        // Center Content using GridBagLayout for alignment
        JPanel centerWrapper = new JPanel(new GridBagLayout());
        centerWrapper.setOpaque(false);

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = GridBagConstraints.RELATIVE;
        c.insets = new Insets(6, 0, 6, 0);
        c.anchor = GridBagConstraints.CENTER;
        c.fill = GridBagConstraints.NONE;

        // 1. Logo
        JLabel logoLabel = new JLabel();
        try {
            java.net.URL logoUrl = HomePanel.class.getResource("/Logo.png");
            ImageIcon origIcon = null;
            if (logoUrl != null) {
                origIcon = new ImageIcon(logoUrl);
            } else {
                File logoFile = new File("Logo.png");
                if (logoFile.exists()) {
                    origIcon = new ImageIcon(logoFile.getAbsolutePath());
                }
            }
            if (origIcon != null && origIcon.getIconWidth() > 0) {
                Image scaled = origIcon.getImage().getScaledInstance(110, 110, Image.SCALE_SMOOTH);
                logoLabel.setIcon(new ImageIcon(scaled));
            }
        } catch (Exception e) {
            // fall back to no icon
        }
        centerWrapper.add(logoLabel, c);

        // Spacer
        c.insets = new Insets(10, 0, 4, 0);

        // 2. Title
        titleLbl = AppTheme.createLabel("Protein Domain Predictor", AppTheme.TITLE, AppTheme.TEXT);
        centerWrapper.add(titleLbl, c);

        c.insets = new Insets(2, 0, 14, 0);

        // 3. Subtitle
        subtitleLbl = AppTheme.createLabel("Graph-based structural domain prediction", AppTheme.SUBTITLE, AppTheme.TEXT_MUTED);
        centerWrapper.add(subtitleLbl, c);

        // 4. Description text
        c.insets = new Insets(2, 0, 20, 0);
        descLbl = AppTheme.createLabel("Predict protein structural domains and identify potential rigid domain boundaries.", AppTheme.BODY, AppTheme.TEXT_MUTED);
        centerWrapper.add(descLbl, c);

        // 5. Card container for Inputs
        card = new JPanel(new GridBagLayout());
        card.setBackground(AppTheme.getSurface());
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.getBorder(), 1),
                BorderFactory.createEmptyBorder(20, 32, 20, 32)
        ));
        
        GridBagConstraints cc = new GridBagConstraints();
        cc.gridx = 0;
        cc.gridy = GridBagConstraints.RELATIVE;
        cc.fill = GridBagConstraints.HORIZONTAL;
        cc.insets = new Insets(6, 0, 6, 0);
        cc.weightx = 1.0;

        inputPrompt = AppTheme.createLabel("Enter PDB IDs (comma-separated)", AppTheme.SMALL_BOLD, AppTheme.TEXT_MUTED);
        card.add(inputPrompt, cc);

        pdbInputField = new AppTheme.RoundedTextField("4AKE,1AKE", "e.g., 4AKE,1AKE");
        pdbInputField.setPreferredSize(new Dimension(320, 38));
        card.add(pdbInputField, cc);

        cc.insets = new Insets(10, 0, 6, 0);
        predictBtn = new AppTheme.RoundedButton("Predict Domains", true);
        predictBtn.addActionListener(e -> {
            if (listener != null) {
                listener.onPredictIds(pdbInputField.getText().trim());
            }
        });
        card.add(predictBtn, cc);

        cc.insets = new Insets(6, 0, 6, 0);
        cc.fill = GridBagConstraints.NONE;
        cc.anchor = GridBagConstraints.CENTER;
        orLbl = AppTheme.createLabel("— OR —", AppTheme.SMALL_BOLD, AppTheme.TEXT_MUTED);
        card.add(orLbl, cc);

        cc.insets = new Insets(4, 0, 4, 0);
        cc.fill = GridBagConstraints.HORIZONTAL;
        loadFilesBtn = new AppTheme.RoundedButton("Open PDB Files", false);
        loadFilesBtn.addActionListener(e -> {
            if (listener != null) {
                listener.onOpenFiles();
            }
        });
        card.add(loadFilesBtn, cc);

        c.insets = new Insets(0, 0, 30, 0);
        centerWrapper.add(card, c);

        // 6. Footer Links
        footer = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        footer.setOpaque(false);

        refBtn = new AppTheme.RoundedButton("References & Methodology", false);
        refBtn.setFont(AppTheme.SMALL_BOLD);
        refBtn.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
        refBtn.addActionListener(e -> {
            if (listener != null) {
                listener.onViewReferences();
            }
        });

        aboutBtn = new AppTheme.RoundedButton("About & Changelog", false);
        aboutBtn.setFont(AppTheme.SMALL_BOLD);
        aboutBtn.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
        aboutBtn.addActionListener(e -> {
            if (listener != null) {
                listener.onViewAbout();
            }
        });

        footer.add(refBtn);
        footer.add(aboutBtn);

        c.insets = new Insets(10, 0, 10, 0);
        centerWrapper.add(footer, c);

        add(centerWrapper, BorderLayout.CENTER);
    }
    
    public void applyTheme() {
        setBackground(AppTheme.getBackground());
        topBar.setBackground(AppTheme.getBackground());
        
        card.setBackground(AppTheme.getSurface());
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.getBorder(), 1),
                BorderFactory.createEmptyBorder(20, 32, 20, 32)
        ));

        // Update title/labels (they'll resolve text/textmuted colors on repaint)
        titleLbl.repaint();
        subtitleLbl.repaint();
        descLbl.repaint();
        inputPrompt.repaint();
        orLbl.repaint();

        // Repaint custom buttons/text field
        predictBtn.repaint();
        loadFilesBtn.repaint();
        refBtn.repaint();
        aboutBtn.repaint();
        pdbInputField.repaint();

        // Update theme toggle label
        AppTheme.updateThemeToggle(themeToggle);

        revalidate();
        repaint();
    }

    public void setPdbText(String text) {
        pdbInputField.setText(text);
    }
}
