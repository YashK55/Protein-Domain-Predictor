import javax.swing.*;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;
import java.awt.*;

public class ReferencesPanel extends JPanel {

    public interface BackListener {
        void onBack();
    }

    private final JTextPane refPane;
    private final JScrollPane scrollPane;
    private final JLabel titleLbl;
    private final AppTheme.RoundedButton backBtn;
    private final JToggleButton themeToggle;
    private final JPanel headerPanel;

    public ReferencesPanel(Runnable themeSwitcher, BackListener listener) {
        setBackground(AppTheme.getBackground());
        setLayout(new BorderLayout(0, 12));
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        // Header Panel using GridBagLayout to prevent overlaps
        headerPanel = new JPanel(new GridBagLayout());
        headerPanel.setBackground(AppTheme.getBackground());

        backBtn = new AppTheme.RoundedButton(" Back to Home", false);
        backBtn.setIcon(new AppTheme.ArrowIcon());
        backBtn.setFont(AppTheme.SMALL_BOLD);
        backBtn.addActionListener(e -> {
            if (listener != null) listener.onBack();
        });

        titleLbl = AppTheme.createLabel("References & Methodology", AppTheme.TITLE, AppTheme.TEXT);
        titleLbl.setHorizontalAlignment(SwingConstants.CENTER);

        themeToggle = AppTheme.createThemeToggle(themeSwitcher);

        GridBagConstraints hc = new GridBagConstraints();
        hc.gridy = 0;
        
        hc.gridx = 0;
        hc.weightx = 0.0;
        hc.anchor = GridBagConstraints.WEST;
        headerPanel.add(backBtn, hc);

        hc.gridx = 1;
        hc.weightx = 1.0;
        hc.anchor = GridBagConstraints.CENTER;
        hc.fill = GridBagConstraints.HORIZONTAL;
        headerPanel.add(titleLbl, hc);

        hc.gridx = 2;
        hc.weightx = 0.0;
        hc.anchor = GridBagConstraints.EAST;
        hc.fill = GridBagConstraints.NONE;
        headerPanel.add(themeToggle, hc);

        add(headerPanel, BorderLayout.NORTH);

        // Content pane
        refPane = new JTextPane();
        refPane.setEditable(false);
        
        scrollPane = new JScrollPane(refPane);
        AppTheme.styleScrollPane(scrollPane);
        add(scrollPane, BorderLayout.CENTER);

        // Load content initially
        loadContent();
    }

    public void applyTheme() {
        setBackground(AppTheme.getBackground());
        headerPanel.setBackground(AppTheme.getBackground());
        
        titleLbl.repaint();
        backBtn.repaint();
        AppTheme.styleScrollPane(scrollPane);

        // Update theme toggle label
        AppTheme.updateThemeToggle(themeToggle);

        // Reload content with new CSS colors
        loadContent();

        revalidate();
        repaint();
    }

    private void loadContent() {
        boolean isDark = AppTheme.getThemeMode() == AppTheme.ThemeMode.DARK;
        String bodyBg = isDark ? "#0b111e" : "#ffffff";
        String bodyFg = isDark ? "#94a3b8" : "#475569";
        String headerColor = isDark ? "#818cf8" : "#4f46e5";
        String boldColor = isDark ? "#f8fafc" : "#0f172a";
        String borderColor = isDark ? "#263455" : "#e2e8f0";

        HTMLEditorKit kit = new HTMLEditorKit();
        refPane.setEditorKit(kit);
        HTMLDocument refDoc = (HTMLDocument) refPane.getDocument();
        StyleSheet refSheet = refDoc.getStyleSheet();
        
        refSheet.addRule("body { font-family: sans-serif; font-size: 13px; background-color: " + bodyBg + "; color: " + bodyFg + "; padding: 24px; line-height: 1.6; }");
        refSheet.addRule("h2 { color: " + headerColor + "; border-bottom: 1px solid " + borderColor + "; padding-bottom: 6px; font-size: 16px; font-weight: bold; margin-top: 24px; }");
        refSheet.addRule("p, li { margin-bottom: 8px; color: " + bodyFg + "; }");
        refSheet.addRule("b { color: " + boldColor + "; }");
        refSheet.addRule("i { color: " + bodyFg + "; }");
        refSheet.addRule("a { color: #6366f1; text-decoration: none; }");
        
        refPane.setText("<html><body>" +
                "<h2>Reference Publication</h2>" +
                "<p><b>A graph-based algorithm for detecting rigid domains in protein structures.</b><br>" +
                "Dang TKL, Nguyen T, Habeck M, Gültas M, Waack S.<br>" +
                "<i>BMC Bioinformatics. 2021;22:66.</i><br>" +
                "DOI: <a href='https://doi.org/10.1186/s12859-021-03966-3'>10.1186/s12859-021-03966-3</a></p>" +
                
                "<h2>Pipeline Methodology</h2>" +
                "<ol>" +
                "<li><b>C-Alpha Extraction</b>: Reads backbone C-alpha coordinates from multi-conformation structures.</li>" +
                "<li><b>Sequence Alignment</b>: Maps residue correspondences across inputs using Needleman-Wunsch dynamic programming.</li>" +
                "<li><b>Distance Matrices</b>: Computes intra-structure distance mappings.</li>" +
                "<li><b>Multi-Structure Protein Graph</b>: Builds contact maps with a 7.5 Å cutoff threshold.</li>" +
                "<li><b>Louvain C-G Partitioning</b>: Divides structure nodes into O(1) cached dense communities.</li>" +
                "<li><b>Line Graph Mapping</b>: Models join connectivity and extracts distance variance metrics.</li>" +
                "<li><b>MAD Hinge Selection</b>: Identifies flexible outlier joints using Median Absolute Deviation.</li>" +
                "<li><b>Local Boundary Inference</b>: Infers binary domain boundaries using localized Viterbi updates.</li>" +
                "<li><b>Rigidity & Merging Check</b>: Refines domains based on RMSD thresholds and merges equivalent segments.</li>" +
                "</ol>" +
                "</body></html>");
    }
}
