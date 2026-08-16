import javax.swing.*;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;
import java.awt.*;

public class AboutPanel extends JPanel {

    public interface BackListener {
        void onBack();
    }

    private final JTextPane aboutPane;
    private final JScrollPane scrollPane;
    private final JLabel titleLbl;
    private final AppTheme.RoundedButton backBtn;
    private final JToggleButton themeToggle;
    private final JPanel headerPanel;

    public AboutPanel(Runnable themeSwitcher, BackListener listener) {
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

        titleLbl = AppTheme.createLabel("About & Changelog", AppTheme.TITLE, AppTheme.TEXT);
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
        aboutPane = new JTextPane();
        aboutPane.setEditable(false);
        
        scrollPane = new JScrollPane(aboutPane);
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
        String subHeaderColor = isDark ? "#a5b4fc" : "#312e81";
        String borderColor = isDark ? "#263455" : "#e2e8f0";

        HTMLEditorKit kit = new HTMLEditorKit();
        aboutPane.setEditorKit(kit);
        HTMLDocument aboutDoc = (HTMLDocument) aboutPane.getDocument();
        StyleSheet aboutSheet = aboutDoc.getStyleSheet();
        
        aboutSheet.addRule("body { font-family: sans-serif; font-size: 13px; background-color: " + bodyBg + "; color: " + bodyFg + "; padding: 24px; line-height: 1.6; }");
        aboutSheet.addRule("h2 { color: " + headerColor + "; border-bottom: 1px solid " + borderColor + "; padding-bottom: 6px; font-size: 16px; font-weight: bold; margin-top: 24px; }");
        aboutSheet.addRule("h3 { color: " + subHeaderColor + "; font-size: 14px; font-weight: bold; margin-top: 14px; }");
        aboutSheet.addRule("p, li { margin-bottom: 6px; color: " + bodyFg + "; }");
        aboutSheet.addRule("b { color: " + boldColor + "; }");

        aboutPane.setText("<html><body>" +
                "<h2>Protein Domain Predictor</h2>" +
                "<p><b>Version:</b> 2.2.0 (High Contrast Dark/Light UI Redesign)<br>" +
                "<b>Type:</b> Academic Student Project & Research Tool.</p>" +
                
                "<h3>Academic Assignment</h3>" +
                "<p><b>Course:</b> Assignment for Java Programming<br>" +
                "<b>Instructor:</b> Monal Pissude<br>" +
                "<b>Students:</b> Yash Katekhaye, Sujit Mohanty, Aniruddha Naik</p>" +
                
                "<h3>Development Credits</h3>" +
                "<ul>" +
                "<li>Created and formatted for structural biology alignment and domain decomposition workflows.</li>" +
                "<li>Visualized using Circular layouts and 3D orthographic coordinate projection.</li>" +
                "</ul>" +
                
                "<h2>Changelog</h2>" +
                "<ul>" +
                "<li><b>v2.2.0 (Current)</b>: Added dynamic light and dark theme switching with dynamic contrast canvases. Re-engineered progress sidebar to map 4 major workflow steps, fixing index offsets. Shifted visual layout so detailed snapshots appear contextually in the header. Added singular/plural metadata formatting.</li>" +
                "<li><b>v2.1.0</b>: Redesigned workflow with a modern centered Home screen, a clean guided step progress stage, an interactive Chain Selection view, and a high-focus Analysis Workspace with a vertical pipeline sidebar and a metadata-driven bottom panel. Replaced massive console output with a collapsible/dialog styled log view.</li>" +
                "<li><b>v2.0.0</b>: Added JTabbedPane documentation tabs. Styled JTextPane log with HTML/CSS dark theme. Added 'Clear View' panel reset button.</li>" +
                "<li><b>v1.9.0</b>: Optimized Louvain partition sizing calculations from O(N) to O(1) in Analyzer.java. Implemented step-wise chain selection scanner.</li>" +
                "<li><b>v1.8.0</b>: Reverted application back to Swing-only build, eliminating bulky JavaFX WebView configuration requirements.</li>" +
                "<li><b>v1.7.0</b>: Integrated 3D superposition coordinate display directly into Main window CardLayout instead of JDialog.</li>" +
                "<li><b>v1.0.0</b>: Ported rigid-body graph algorithm pipeline from original Python Dang et al. codebase.</li>" +
                "</ul>" +
                "</body></html>");
    }
}
