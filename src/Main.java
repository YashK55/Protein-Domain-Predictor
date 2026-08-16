import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;
import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Main {

    private JFrame frame;
    private final JTextField pdbIdsField = new RoundTextField("4AKE,1AKE");
    private final JTextField chainField = new RoundTextField("A");
    private final JTextPane output = new JTextPane();
    private final JLabel status = new JLabel("Ready");

    private final DefaultListModel<String> stepListModel = new DefaultListModel<>();
    private final JList<String> stepList = new JList<>(stepListModel);
    private final GraphPanel graphPanel = new GraphPanel();
    private final List<GraphPanel.Snapshot> snapshots = new ArrayList<>();
    private final JButton superimposeButton = new RoundButton("Show 3D Superposition");

    private final Structure3DPanel structure3DPanel = new Structure3DPanel();
    private final CardLayout vizLayout = new CardLayout();
    private final JPanel vizContainer = new JPanel(vizLayout);
    private final JPanel legendContainer = new JPanel(new BorderLayout());
    private boolean showingSuperimposition = false;

    private static final class DomainSummary {
        final Color color;
        final String label;

        DomainSummary(Color color, String label) {
            this.color = color;
            this.label = label;
        }
    }

    private volatile Analyzer.Result lastResult;
    private volatile List<Structure> lastStructures;
    private volatile Color[] domainColors;
    private volatile List<DomainSummary> domainSummaries;

    // Custom modern anti-aliased rounded button widget
    public static class RoundButton extends JButton {
        public RoundButton(String text) {
            super(text);
            setContentAreaFilled(false);
            setFocusPainted(false);
            setForeground(Color.WHITE);
            setFont(new Font("sans-serif", Font.BOLD, 12));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (!isEnabled()) {
                g2.setColor(new Color(55, 65, 81, 100));
            } else if (getModel().isPressed()) {
                g2.setColor(new Color(45, 55, 72));
            } else if (getModel().isRollover()) {
                g2.setColor(new Color(55, 65, 81));
            } else {
                g2.setColor(new Color(31, 41, 55));
            }
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
            g2.setColor(new Color(255, 255, 255, 25));
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    // Custom modern anti-aliased rounded text input widget
    public static class RoundTextField extends JTextField {
        public RoundTextField(String text) {
            super(text);
            setOpaque(false);
            setBackground(new Color(17, 24, 39)); // Charcoal dark
            setForeground(Color.WHITE);
            setCaretColor(Color.WHITE);
            setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
            setFont(new Font("sans-serif", Font.PLAIN, 12));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getBackground());
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
            g2.setColor(new Color(255, 255, 255, 20));
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Main().createWindow());
    }

    private void createWindow() {
        frame = new JFrame("Protein Domain Predictor");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1150, 800);
        frame.setLocationRelativeTo(null);

        // Load window icon from root directory logo.jpg if it exists
        File iconFile = new File("logo.jpg");
        if (iconFile.exists()) {
            try {
                Image icon = Toolkit.getDefaultToolkit().getImage(iconFile.getAbsolutePath());
                frame.setIconImage(icon);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Initialize JTabbedPane with custom dark theme colors
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setBackground(new Color(17, 24, 39));
        tabbedPane.setForeground(new Color(243, 244, 246));
        tabbedPane.setBorder(null);

        // ---------------- TAB 1: ANALYZER WORKSPACE ----------------
        JPanel analyzerTab = new JPanel(new BorderLayout());
        analyzerTab.setBackground(new Color(11, 15, 25));

        JPanel top = new JPanel(new GridBagLayout());
        top.setBackground(new Color(11, 15, 25));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 6, 6, 6);
        c.fill = GridBagConstraints.HORIZONTAL;

        c.gridx = 0; c.gridy = 0;
        top.add(createStyledLabel("PDB IDs:"), c);

        c.gridx = 1; c.weightx = 1;
        top.add(pdbIdsField, c);

        c.gridx = 2; c.weightx = 0;
        top.add(createStyledLabel("Default Chain:"), c);

        c.gridx = 3;
        chainField.setColumns(3);
        top.add(chainField, c);

        JButton analyzeIds = new RoundButton("Analyze PDB IDs");
        c.gridx = 4;
        top.add(analyzeIds, c);

        JButton chooseFiles = new RoundButton("Analyze PDB Files");
        c.gridx = 5;
        top.add(chooseFiles, c);

        superimposeButton.setEnabled(false);
        c.gridx = 6;
        top.add(superimposeButton, c);

        JButton clearBtn = new RoundButton("Clear View");
        c.gridx = 7;
        top.add(clearBtn, c);

        // Configure JTextPane for dark, styled HTML logging
        output.setContentType("text/html");
        output.setEditable(false);
        HTMLEditorKit kit = new HTMLEditorKit();
        output.setEditorKit(kit);
        HTMLDocument doc = (HTMLDocument) output.getDocument();
        StyleSheet sheet = doc.getStyleSheet();
        sheet.addRule("body { font-family: monospace; font-size: 12px; background-color: #030712; color: #E5E7EB; padding: 10px; line-height: 1.4; }");
        sheet.addRule("h2 { color: #818CF8; margin: 12px 0 6px 0; font-size: 14px; font-weight: bold; border-bottom: 1px solid rgba(255,255,255,0.08); padding-bottom: 2px; }");
        sheet.addRule("hr { border: 0; border-top: 1px solid rgba(255, 255, 255, 0.08); margin: 10px 0; }");
        sheet.addRule(".success { color: #10B981; }");
        sheet.addRule(".error { color: #EF4444; font-weight: bold; }");
        sheet.addRule(".step { color: #A5B4FC; font-weight: bold; }");
        sheet.addRule(".param { color: #9CA3AF; }");
        
        output.setText("<html><body><font color='#9CA3AF'>Ready. Enter PDB IDs (e.g. 4AKE,1AKE) or choose files to begin.</font><br></body></html>");

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setBackground(new Color(11, 15, 25));
        status.setForeground(new Color(156, 163, 175));
        bottom.add(status, BorderLayout.WEST);
        bottom.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));

        // Style the steps JList cells with modern cell padding and backgrounds
        stepList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        stepList.setBackground(new Color(17, 24, 39));
        stepList.setForeground(new Color(156, 163, 175));
        stepList.setSelectionBackground(new Color(99, 102, 241, 60));
        stepList.setSelectionForeground(Color.WHITE);
        stepList.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        
        stepList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                label.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
                label.setOpaque(true);
                if (isSelected) {
                    label.setBackground(new Color(99, 102, 241, 60));
                    label.setForeground(Color.WHITE);
                } else {
                    label.setBackground(new Color(17, 24, 39));
                    label.setForeground(new Color(156, 163, 175));
                }
                return label;
            }
        });

        stepList.addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            int index = stepList.getSelectedIndex();
            if (index >= 0 && index < snapshots.size()) {
                graphPanel.show(snapshots.get(index));
            }
        });

        // Wrap stepScroll list in a panel to make JScrollPane scrollbars flush
        JScrollPane stepScroll = new JScrollPane(stepList);
        stepScroll.setBorder(null);
        stepScroll.setBackground(new Color(17, 24, 39));
        stepScroll.getViewport().setBackground(new Color(17, 24, 39));

        JPanel stepPanel = new JPanel(new BorderLayout());
        stepPanel.setBackground(new Color(11, 15, 25));
        stepPanel.add(stepScroll, BorderLayout.CENTER);
        styleTitledBorder(stepPanel, "Pipeline steps");
        stepPanel.setPreferredSize(new Dimension(220, 0));

        // Viz Container holds the 2D step graph / 3D superposition
        JPanel vizPanel = new JPanel(new BorderLayout());
        vizPanel.setBackground(new Color(11, 15, 25));
        vizPanel.add(vizContainer, BorderLayout.CENTER);
        styleTitledBorder(vizPanel, "Visualization canvas");

        vizContainer.setBackground(new Color(11, 15, 25));
        vizContainer.add(graphPanel, "GRAPH");

        JPanel superimposePanel = new JPanel(new BorderLayout());
        superimposePanel.setBackground(new Color(11, 15, 25));
        superimposePanel.add(structure3DPanel, BorderLayout.CENTER);
        
        legendContainer.setPreferredSize(new Dimension(220, 0));
        legendContainer.setBackground(new Color(11, 15, 25));
        superimposePanel.add(legendContainer, BorderLayout.EAST);

        vizContainer.add(superimposePanel, "SUPERIMPOSITION");

        // Wrap output log scrollpane in a panel for flush borders
        JScrollPane outputScroll = new JScrollPane(output);
        outputScroll.setBorder(null);

        JPanel outputPanel = new JPanel(new BorderLayout());
        outputPanel.setBackground(new Color(11, 15, 25));
        outputPanel.add(outputScroll, BorderLayout.CENTER);
        styleTitledBorder(outputPanel, "Terminal log");

        JSplitPane rightSplit = new JSplitPane(
                JSplitPane.VERTICAL_SPLIT, vizPanel, outputPanel);
        rightSplit.setResizeWeight(0.6);
        rightSplit.setBorder(null);
        rightSplit.setBackground(new Color(11, 15, 25));

        JSplitPane mainSplit = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT, stepPanel, rightSplit);
        mainSplit.setResizeWeight(0.0);
        mainSplit.setBorder(null);
        mainSplit.setBackground(new Color(11, 15, 25));

        analyzerTab.add(top, BorderLayout.NORTH);
        analyzerTab.add(mainSplit, BorderLayout.CENTER);
        analyzerTab.add(bottom, BorderLayout.SOUTH);

        tabbedPane.addTab("Analyzer Workspace", analyzerTab);

        // ---------------- TAB 2: REFERENCES & METHODOLOGY ----------------
        JTextPane refPane = new JTextPane();
        refPane.setContentType("text/html");
        refPane.setEditable(false);
        refPane.setEditorKit(kit); // reuse kit
        HTMLDocument refDoc = (HTMLDocument) refPane.getDocument();
        StyleSheet refSheet = refDoc.getStyleSheet();
        refSheet.addRule("body { font-family: sans-serif; font-size: 13px; background-color: #030712; color: #E5E7EB; padding: 24px; line-height: 1.5; }");
        refSheet.addRule("h2 { color: #818CF8; border-bottom: 1px solid rgba(255,255,255,0.08); padding-bottom: 4px; font-size: 16px; font-weight: bold; margin-top: 24px; }");
        refSheet.addRule("p, li { margin-bottom: 8px; }");
        refSheet.addRule("a { color: #6366F1; text-decoration: none; }");
        
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
        
        JScrollPane refScroll = new JScrollPane(refPane);
        refScroll.setBorder(null);
        tabbedPane.addTab("References & Methodology", refScroll);

        // ---------------- TAB 3: ABOUT & CHANGELOG ----------------
        JTextPane aboutPane = new JTextPane();
        aboutPane.setContentType("text/html");
        aboutPane.setEditable(false);
        aboutPane.setEditorKit(kit); // reuse kit
        HTMLDocument aboutDoc = (HTMLDocument) aboutPane.getDocument();
        StyleSheet aboutSheet = aboutDoc.getStyleSheet();
        aboutSheet.addRule("body { font-family: sans-serif; font-size: 13px; background-color: #030712; color: #E5E7EB; padding: 24px; line-height: 1.5; }");
        aboutSheet.addRule("h2 { color: #818CF8; border-bottom: 1px solid rgba(255,255,255,0.08); padding-bottom: 4px; font-size: 16px; font-weight: bold; margin-top: 24px; }");
        aboutSheet.addRule("h3 { color: #A5B4FC; font-size: 14px; font-weight: bold; margin-top: 14px; }");
        aboutSheet.addRule("li { margin-bottom: 6px; }");

        aboutPane.setText("<html><body>" +
                "<h2>Protein Domain Predictor</h2>" +
                "<p><b>Version:</b> 2.0.0 (Standard Swing Release)<br>" +
                "<b>Type:</b> Academic Student Project & Research Tool.</p>" +
                
                "<h3>Development Credits</h3>" +
                "<ul>" +
                "<li>Created and formatted for structural biology alignment and domain decomposition workflows.</li>" +
                "<li>Visualized using Circular layouts and 3D orthographic coordinate projection.</li>" +
                "</ul>" +
                
                "<h2>Changelog</h2>" +
                "<ul>" +
                "<li><b>v2.0.0 (Current)</b>: Added JTabbedPane documentation tabs. Styled JTextPane log with HTML/CSS dark theme. Added 'Clear View' panel reset button.</li>" +
                "<li><b>v1.9.0</b>: Optimized Louvain partition sizing calculations from O(N) to O(1) in Analyzer.java. Implemented step-wise chain selection scanner.</li>" +
                "<li><b>v1.8.0</b>: Reverted application back to Swing-only build, eliminating bulky JavaFX WebView configuration requirements.</li>" +
                "<li><b>v1.7.0</b>: Integrated 3D superposition coordinate display directly into Main window CardLayout instead of JDialog.</li>" +
                "<li><b>v1.0.0</b>: Ported rigid-body graph algorithm pipeline from original Python Dang et al. codebase.</li>" +
                "</ul>" +
                "</body></html>");

        JScrollPane aboutScroll = new JScrollPane(aboutPane);
        aboutScroll.setBorder(null);
        tabbedPane.addTab("About & Changelog", aboutScroll);

        frame.add(tabbedPane, BorderLayout.CENTER);

        // Bind Action Listeners
        analyzeIds.addActionListener(e -> analyzeIds());
        chooseFiles.addActionListener(e -> analyzeFiles(frame));
        superimposeButton.addActionListener(e -> toggleSuperimposition());
        clearBtn.addActionListener(e -> {
            output.setText("<html><body><font color='#9CA3AF'>Ready. Enter PDB IDs or choose files to begin.</font><br></body></html>");
            clearSteps();
            status.setText("Ready");
        });

        frame.setVisible(true);
    }

    private void styleTitledBorder(JComponent component, String title) {
        component.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(255, 255, 255, 15), 1),
                title,
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font("sans-serif", Font.BOLD, 12),
                new Color(129, 140, 248) // Indigo title
        ));
    }

    private JLabel createStyledLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(new Color(243, 244, 246));
        label.setFont(new Font("sans-serif", Font.BOLD, 12));
        return label;
    }

    private void analyzeIds() {
        output.setText("<html><body></body></html>");
        clearSteps();
        superimposeButton.setEnabled(false);
        status.setText("Downloading PDBs...");

        new Thread(() -> {
            try {
                String raw = pdbIdsField.getText().trim();
                if (raw.isEmpty()) {
                    throw new IllegalArgumentException(
                            "Enter at least two PDB IDs, e.g. 4AKE,1AKE");
                }

                String defaultChain = chainField.getText().trim();
                char defaultChainChar = defaultChain.isEmpty() ? 'A' : defaultChain.charAt(0);

                List<String> pdbIds = new ArrayList<>();
                List<File> files = new ArrayList<>();
                Path data = Path.of("data");

                for (String token : raw.split(",")) {
                    token = token.trim();
                    if (token.isEmpty()) continue;

                    String id = token.split("_")[0].trim().toUpperCase();
                    pdbIds.add(id);

                    append("Downloading " + id + " PDB file...\n");
                    File f = downloadPdbFile(id, data);
                    files.add(f);
                }

                // Detect chains in each file
                List<List<Character>> allAvailableChains = new ArrayList<>();
                for (File f : files) {
                    allAvailableChains.add(detectChains(f));
                }

                // Prompt user for chain selection on EDT
                final List<Character> selectedChains = new ArrayList<>();
                SwingUtilities.invokeAndWait(() -> {
                    List<Character> choices = promptChainSelection(pdbIds, allAvailableChains);
                    if (choices != null) {
                        selectedChains.addAll(choices);
                    }
                });

                if (selectedChains.isEmpty()) {
                    append("Analysis cancelled by user.\n");
                    setStatus("Cancelled");
                    return;
                }

                status.setText("Analyzing structures...");
                List<Structure> structures = new ArrayList<>();
                for (int i = 0; i < files.size(); i++) {
                    String id = pdbIds.get(i);
                    char chain = selectedChains.get(i);
                    File file = files.get(i);

                    append("Loading " + id + " chain " + chain + "...\n");
                    Structure s = new Structure(id, chain);
                    s.loadPDB(file.getAbsolutePath());
                    structures.add(s);
                }

                runAnalysis(structures);

            } catch (Exception ex) {
                append("\nERROR: " + ex.getMessage() + "\n");
                ex.printStackTrace();
                setStatus("Error");
            }
        }).start();
    }

    private void analyzeFiles(JFrame frame) {
        JFileChooser chooser = new JFileChooser(new File("data"));
        chooser.setMultiSelectionEnabled(true);
        chooser.setFileFilter(new FileNameExtensionFilter("PDB files", "pdb", "ent"));

        if (chooser.showOpenDialog(frame) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File[] files = chooser.getSelectedFiles();

        if (files.length < 2) {
            JOptionPane.showMessageDialog(
                    frame,
                    "Select at least two PDB files.",
                    "Multi-structure analysis",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        output.setText("<html><body></body></html>");
        clearSteps();
        superimposeButton.setEnabled(false);
        status.setText("Scanning chains...");

        new Thread(() -> {
            try {
                List<String> fileNames = new ArrayList<>();
                List<List<Character>> allAvailableChains = new ArrayList<>();
                for (File f : files) {
                    fileNames.add(stripExtension(f.getName()));
                    allAvailableChains.add(detectChains(f));
                }

                // Prompt user for chain selection
                final List<Character> selectedChains = new ArrayList<>();
                SwingUtilities.invokeAndWait(() -> {
                    List<Character> choices = promptChainSelection(fileNames, allAvailableChains);
                    if (choices != null) {
                        selectedChains.addAll(choices);
                    }
                });

                if (selectedChains.isEmpty()) {
                    append("Analysis cancelled by user.\n");
                    setStatus("Cancelled");
                    return;
                }

                status.setText("Analyzing files...");
                List<Structure> structures = new ArrayList<>();

                for (int i = 0; i < files.length; i++) {
                    File file = files[i];
                    char chain = selectedChains.get(i);
                    String name = fileNames.get(i);

                    append("Reading " + file.getName() + " chain " + chain + "...\n");
                    Structure s = new Structure(name, chain);
                    s.loadPDB(file.getAbsolutePath());
                    structures.add(s);
                }

                runAnalysis(structures);

            } catch (Exception ex) {
                append("\nERROR: " + ex.getMessage() + "\n");
                ex.printStackTrace();
                setStatus("Error");
            }
        }).start();
    }

    private void runAnalysis(List<Structure> structures) {
        Analyzer analyzer = new Analyzer(this::append, this::onSnapshot);

        append("\n============================================\n");
        append("PROTEIN DOMAIN PREDICTOR\n");
        append("Graph-based rigid-domain estimation\n");
        append("============================================\n");

        append("\nParameters:\n");
        append("  Contact cutoff = 7.5 Å\n");
        append("  Rigidity threshold = 3.5 Å\n");
        append("  Merging threshold = 1.0\n");

        Analyzer.Result result = analyzer.analyze(
                structures,
                7.5,
                3.5,
                1.0
        );

        lastResult = result;
        lastStructures = structures;

        append("\n============================================\n");
        append("FINAL RESULT\n");
        append("============================================\n");

        List<DomainSummary> summaries = new ArrayList<>();
        Color[] colors = new Color[result.alignedResidues.get(0).size()];

        for (int d = 0; d < result.domains.size(); d++) {

            List<Integer> residues = domainResidues(result, d);
            if (residues.isEmpty()) continue;

            Color color = GraphPanel.paletteColor(d);
            for (int index : residues) colors[index] = color;

            String hexColor = String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
            append("\n<span style='color: " + hexColor + "; font-weight: bold;'>DOMAIN " + (d + 1) + "</span>\n");
            append("Residues: "
                    + (residues.get(0) + 1)
                    + " - "
                    + (residues.get(residues.size() - 1) + 1)
                    + "\n");

            append("Count: " + residues.size() + "\n");
            append("Residue list: ");

            for (int i = 0; i < residues.size(); i++) {
                int index = residues.get(i);
                Residue r = result.alignedResidues.get(0).get(index);

                append(r.number
                        + (r.insertionCode == ' '
                            ? "" : String.valueOf(r.insertionCode))
                        + r.name
                        + (i == residues.size() - 1 ? "" : ", "));
            }

            append("\n");

            Residue first = result.alignedResidues.get(0).get(residues.get(0));
            Residue last = result.alignedResidues.get(0).get(residues.get(residues.size() - 1));
            summaries.add(new DomainSummary(color,
                    "Domain " + (d + 1) + ": residues " + first.number + "-" + last.number
                            + " (" + residues.size() + " aa)"));
        }

        domainColors = colors;
        domainSummaries = summaries;

        append("\nAnalysis complete.\n");
        setStatus("Complete");
        SwingUtilities.invokeLater(() -> superimposeButton.setEnabled(true));
    }

    private List<Integer> domainResidues(Analyzer.Result result, int domainIndex) {
        List<Integer> communityGroup = result.domains.get(domainIndex);
        List<Integer> residues = new ArrayList<>();

        for (int community : communityGroup) {
            residues.addAll(result.communities.get(community));
        }

        residues.sort(Integer::compareTo);
        return residues;
    }

    private String stripExtension(String name) {
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }

    private File downloadPdbFile(String pdbId, Path downloadDirectory) throws IOException, InterruptedException {
        Files.createDirectories(downloadDirectory);
        String id = pdbId.toUpperCase();
        Path target = downloadDirectory.resolve(id.toLowerCase() + ".pdb");

        if (!Files.exists(target)) {
            String url = "https://files.rcsb.org/download/" + id + ".pdb";
            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(url))
                    .GET()
                    .build();

            java.net.http.HttpResponse<String> response =
                    client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new IOException("RCSB download failed for " + id
                        + " (HTTP " + response.statusCode() + ")");
            }

            Files.writeString(target, response.body());
        }
        return target.toFile();
    }

    private List<Character> detectChains(File file) {
        List<Character> chains = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if ((line.startsWith("ATOM") || line.startsWith("HETATM")) && line.length() >= 22) {
                    char chain = line.charAt(21);
                    if (chain != ' ' && !chains.contains(chain)) {
                        chains.add(chain);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (chains.isEmpty()) {
            chains.add('A'); // default fallback
        }
        chains.sort(Character::compareTo);
        return chains;
    }

    private List<Character> promptChainSelection(List<String> labels, List<List<Character>> availableChains) {
        JPanel panel = new JPanel(new GridLayout(0, 2, 10, 10));
        panel.setBackground(new Color(11, 15, 25));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        List<JComboBox<Character>> combos = new ArrayList<>();

        for (int i = 0; i < labels.size(); i++) {
            JLabel label = createStyledLabel("Select chain for " + labels.get(i) + ":");
            panel.add(label);

            List<Character> chains = availableChains.get(i);
            JComboBox<Character> combo = new JComboBox<>(chains.toArray(new Character[0]));
            combo.setBackground(new Color(17, 24, 39));
            combo.setForeground(Color.WHITE);
            if (chains.contains('A')) {
                combo.setSelectedItem('A');
            }
            combos.add(combo);
            panel.add(combo);
        }

        int option = JOptionPane.showConfirmDialog(
                frame,
                panel,
                "Select Chain Conformations",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (option == JOptionPane.OK_OPTION) {
            List<Character> selected = new ArrayList<>();
            for (JComboBox<Character> combo : combos) {
                selected.add((Character) combo.getSelectedItem());
            }
            return selected;
        }
        return null;
    }

    private void append(String text) {
        SwingUtilities.invokeLater(() -> {
            try {
                String html;
                if (text.startsWith("STEP") || text.startsWith("\nSTEP")) {
                    html = "<br><span class='step'>" + escapeHtml(text.trim()) + "</span><br>";
                } else if (text.startsWith("ERROR") || text.startsWith("\nERROR")) {
                    html = "<span class='error'>" + escapeHtml(text.trim()) + "</span><br>";
                } else if (text.equals("\n============================================\n")) {
                    html = "<hr>";
                } else if (text.trim().equals("PROTEIN DOMAIN PREDICTOR") || text.trim().equals("FINAL RESULT")) {
                    html = "<h2>" + text.trim() + "</h2>";
                } else if (text.startsWith("Graph-based") || text.startsWith("Analysis complete") || text.startsWith("Parameters:") || text.startsWith("  Contact") || text.startsWith("  Rigidity") || text.startsWith("  Merging")) {
                    html = "<span class='param'>" + escapeHtml(text.trim()) + "</span><br>";
                } else {
                    html = escapeHtml(text).replace("\n", "<br>");
                }

                HTMLDocument doc = (HTMLDocument) output.getDocument();
                HTMLEditorKit kit = (HTMLEditorKit) output.getEditorKit();
                kit.insertHTML(doc, doc.getLength(), html, 0, 0, null);
                output.setCaretPosition(doc.getLength());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private String escapeHtml(String s) {
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private void setStatus(String text) {
        SwingUtilities.invokeLater(() -> status.setText(text));
    }

    private void clearSteps() {
        stepListModel.clear();
        snapshots.clear();
        graphPanel.show(null);
        showingSuperimposition = false;
        vizLayout.show(vizContainer, "GRAPH");
        superimposeButton.setText("Show 3D Superposition");
    }

    private void onSnapshot(GraphPanel.Snapshot snapshot) {
        SwingUtilities.invokeLater(() -> {
            snapshots.add(snapshot);

            String label;
            String title = snapshot.title;
            if (title.contains(" - ")) {
                String[] parts = title.split(" - ", 2);
                label = "<html><body style='padding: 2px;'><font color='#818CF8'><b>" + parts[0] + "</b></font> - <font color='#CCCCCC'>" + parts[1] + "</font></body></html>";
            } else {
                label = "<html><body style='padding: 2px;'><font color='#818CF8'><b>" + title + "</b></font></body></html>";
            }

            stepListModel.addElement(label);
            stepList.setSelectedIndex(stepListModel.size() - 1);
            graphPanel.show(snapshot);
        });
    }

    private void toggleSuperimposition() {
        showingSuperimposition = !showingSuperimposition;
        if (showingSuperimposition) {
            updateSuperimpositionData();
            vizLayout.show(vizContainer, "SUPERIMPOSITION");
            superimposeButton.setText("Show Step Graph");
        } else {
            vizLayout.show(vizContainer, "GRAPH");
            superimposeButton.setText("Show 3D Superposition");
        }
    }

    private void updateSuperimpositionData() {
        Analyzer.Result result = lastResult;
        List<Structure> structures = lastStructures;
        if (result == null || structures == null) return;

        List<List<Residue>> aligned = result.alignedResidues;
        List<Residue> reference = aligned.get(0);

        List<Structure3DPanel.Trace> traces = new ArrayList<>();

        for (int k = 0; k < aligned.size(); k++) {
            List<Residue> mobile = aligned.get(k);
            Superposition.Transform transform =
                    k == 0 ? null : Superposition.fit(mobile, reference);

            List<double[]> points = new ArrayList<>();
            for (Residue r : mobile) {
                points.add(transform == null
                        ? new double[]{r.x, r.y, r.z}
                        : transform.apply(r.x, r.y, r.z));
            }

            traces.add(new Structure3DPanel.Trace(
                    structures.get(k).pdbId + "_" + structures.get(k).chain, points));
        }

        structure3DPanel.setData(traces, domainColors);

        legendContainer.removeAll();
        legendContainer.add(buildLegendPanel(structures), BorderLayout.CENTER);
        legendContainer.revalidate();
        legendContainer.repaint();
    }

    private JPanel buildLegendPanel(List<Structure> structures) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setPreferredSize(new Dimension(220, 0));

        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(255, 255, 255, 15), 1),
                "Legend",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font("sans-serif", Font.BOLD, 12),
                new Color(129, 140, 248)
        ));
        panel.setBackground(new Color(11, 15, 25));

        JLabel titleLbl = new JLabel("Structures overlaid:");
        titleLbl.setForeground(Color.WHITE);
        titleLbl.setFont(new Font("sans-serif", Font.BOLD, 11));
        panel.add(titleLbl);

        for (Structure s : structures) {
            JLabel nameLbl = new JLabel("  " + s.pdbId + " chain " + s.chain);
            nameLbl.setForeground(new Color(156, 163, 175));
            panel.add(nameLbl);
        }

        panel.add(Box.createVerticalStrut(12));

        JLabel domainTitleLbl = new JLabel("Domains:");
        domainTitleLbl.setForeground(Color.WHITE);
        domainTitleLbl.setFont(new Font("sans-serif", Font.BOLD, 11));
        panel.add(domainTitleLbl);

        List<DomainSummary> summaries = domainSummaries;
        if (summaries != null) {
            for (DomainSummary ds : summaries) {
                JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
                row.setBackground(new Color(11, 15, 25));

                JLabel swatch = new JLabel();
                swatch.setOpaque(true);
                swatch.setBackground(ds.color);
                swatch.setPreferredSize(new Dimension(16, 16));

                row.add(swatch);
                JLabel swatchLabel = new JLabel(ds.label);
                swatchLabel.setForeground(new Color(156, 163, 175));
                row.add(swatchLabel);
                panel.add(row);
            }
        }

        panel.add(Box.createVerticalGlue());
        return panel;
    }
}
