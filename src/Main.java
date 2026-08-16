import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;
import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Main {

    private JFrame frame;
    private CardLayout mainLayout;
    private JPanel mainContainer;

    private HomePanel homePanel;
    private ReferencesPanel referencesPanel;
    private AboutPanel aboutPanel;
    private ProcessingPanel processingPanel;
    private ChainSelectionPanel chainSelectionPanel;
    private AnalysisPanel analysisPanel;

    private GraphPanel graphPanel;
    private Structure3DPanel structure3DPanel;

    // Loaded structures and results cache
    private List<File> loadedFiles;
    private List<String> loadedPdbNames;
    private List<Character> commonChains;
    private List<Integer> commonChainResidueCounts;

    private volatile Analyzer.Result lastResult;
    private volatile List<Structure> lastStructures;
    private volatile Color[] domainColors;
    private volatile List<DomainSummary> domainSummaries;

    private static final class DomainSummary {
        final Color color;
        final String label;

        DomainSummary(Color color, String label) {
            this.color = color;
            this.label = label;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Main().createWindow());
    }

    private void createWindow() {
        frame = new JFrame("Protein Domain Predictor");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1280, 800);
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

        mainLayout = new CardLayout();
        mainContainer = new JPanel(mainLayout);
        mainContainer.setBackground(AppTheme.getBackground());

        graphPanel = new GraphPanel();
        structure3DPanel = new Structure3DPanel();

        // 1. Theme Switcher Callback
        Runnable themeSwitcher = () -> {
            SwingUtilities.invokeLater(() -> {
                homePanel.applyTheme();
                referencesPanel.applyTheme();
                aboutPanel.applyTheme();
                processingPanel.applyTheme();
                chainSelectionPanel.applyTheme();
                analysisPanel.applyTheme();
                mainContainer.setBackground(AppTheme.getBackground());
                frame.repaint();
            });
        };

        // 2. Initialize views
        homePanel = new HomePanel(themeSwitcher, new HomePanel.HomeListener() {
            @Override
            public void onAnalyzeIds(String pdbIds) {
                loadPdbIdsWorkflow(pdbIds);
            }

            @Override
            public void onOpenFiles() {
                loadPdbFilesWorkflow();
            }

            @Override
            public void onViewReferences() {
                mainLayout.show(mainContainer, "REFERENCES");
            }

            @Override
            public void onViewAbout() {
                mainLayout.show(mainContainer, "ABOUT");
            }
        });

        referencesPanel = new ReferencesPanel(themeSwitcher, () -> mainLayout.show(mainContainer, "HOME"));
        aboutPanel = new AboutPanel(themeSwitcher, () -> mainLayout.show(mainContainer, "HOME"));
        processingPanel = new ProcessingPanel();
        
        chainSelectionPanel = new ChainSelectionPanel(themeSwitcher, new ChainSelectionPanel.ChainSelectionListener() {
            @Override
            public void onChainSelected(char chain) {
                runAnalysisWorkflow(chain);
            }

            @Override
            public void onBack() {
                mainLayout.show(mainContainer, "HOME");
            }
        });

        analysisPanel = new AnalysisPanel(frame, graphPanel, structure3DPanel, themeSwitcher, new AnalysisPanel.AnalysisListener() {
            @Override
            public void onBackToHome() {
                mainLayout.show(mainContainer, "HOME");
            }

            @Override
            public void onToggle3D(boolean show3D) {
                // Pre-populated data does not need toggled loading
            }
        });

        mainContainer.add(homePanel, "HOME");
        mainContainer.add(referencesPanel, "REFERENCES");
        mainContainer.add(aboutPanel, "ABOUT");
        mainContainer.add(processingPanel, "PROCESSING");
        mainContainer.add(chainSelectionPanel, "CHAIN_SELECTION");
        mainContainer.add(analysisPanel, "ANALYSIS");

        frame.add(mainContainer);
        mainLayout.show(mainContainer, "HOME");
        frame.setVisible(true);
    }

    private void loadPdbIdsWorkflow(String raw) {
        if (raw.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Enter at least two PDB IDs, e.g. 4AKE,1AKE", "Input Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        processingPanel.reset();
        mainLayout.show(mainContainer, "PROCESSING");

        new Thread(() -> {
            try {
                processingPanel.updateStep(0, ProcessingPanel.StepState.ACTIVE);
                processingPanel.setDetails("—", "—", "Downloading PDBs...");

                List<String> pdbIds = new ArrayList<>();
                List<File> files = new ArrayList<>();
                Path data = Path.of("data");

                for (String token : raw.split(",")) {
                    token = token.trim();
                    if (token.isEmpty()) continue;

                    String id = token.split("_")[0].trim().toUpperCase();
                    pdbIds.add(id);

                    processingPanel.setDetails(String.valueOf(files.size()), "—", "Downloading " + id + "...");
                    File f = downloadPdbFile(id, data);
                    files.add(f);
                }

                if (files.size() < 2) {
                    throw new IllegalArgumentException("Please enter at least two valid PDB IDs.");
                }

                processingPanel.updateStep(0, ProcessingPanel.StepState.COMPLETED);
                processingPanel.updateStep(1, ProcessingPanel.StepState.ACTIVE);
                processingPanel.setDetails(String.valueOf(files.size()), "—", "Scanning residues...");

                // Detect chains in each file
                List<List<Character>> allAvailableChains = new ArrayList<>();
                for (File f : files) {
                    allAvailableChains.add(detectChains(f));
                }

                processingPanel.updateStep(1, ProcessingPanel.StepState.COMPLETED);
                processingPanel.updateStep(2, ProcessingPanel.StepState.ACTIVE);
                processingPanel.setDetails(String.valueOf(files.size()), "—", "Finding common chains...");

                List<Character> common = findCommonChains(allAvailableChains);
                List<Integer> residueCounts = new ArrayList<>();
                
                if (!files.isEmpty() && !common.isEmpty()) {
                    for (char ch : common) {
                        residueCounts.add(countResiduesInChain(files.get(0), ch));
                    }
                }

                processingPanel.updateStep(2, ProcessingPanel.StepState.COMPLETED);
                processingPanel.updateStep(3, ProcessingPanel.StepState.ACTIVE);
                processingPanel.setDetails(String.valueOf(files.size()), "—", "Ready for selection");

                loadedFiles = files;
                loadedPdbNames = pdbIds;
                commonChains = common;
                commonChainResidueCounts = residueCounts;

                SwingUtilities.invokeLater(() -> {
                    chainSelectionPanel.setChains(common, residueCounts);
                    mainLayout.show(mainContainer, "CHAIN_SELECTION");
                });

            } catch (Exception ex) {
                ex.printStackTrace();
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(frame, "Error loading structures: " + ex.getMessage(), "Loading Error", JOptionPane.ERROR_MESSAGE);
                    mainLayout.show(mainContainer, "HOME");
                });
            }
        }).start();
    }

    private void loadPdbFilesWorkflow() {
        JFileChooser chooser = new JFileChooser(new File("data"));
        chooser.setMultiSelectionEnabled(true);
        chooser.setFileFilter(new FileNameExtensionFilter("PDB files", "pdb", "ent"));

        if (chooser.showOpenDialog(frame) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File[] files = chooser.getSelectedFiles();
        if (files.length < 2) {
            JOptionPane.showMessageDialog(frame, "Select at least two PDB files.", "Multi-structure analysis", JOptionPane.WARNING_MESSAGE);
            return;
        }

        processingPanel.reset();
        mainLayout.show(mainContainer, "PROCESSING");

        new Thread(() -> {
            try {
                processingPanel.updateStep(0, ProcessingPanel.StepState.COMPLETED);
                processingPanel.updateStep(1, ProcessingPanel.StepState.ACTIVE);
                processingPanel.setDetails(String.valueOf(files.length), "—", "Scanning local files...");

                List<String> fileNames = new ArrayList<>();
                List<List<Character>> allAvailableChains = new ArrayList<>();
                List<File> fileList = new ArrayList<>();

                for (File f : files) {
                    fileList.add(f);
                    fileNames.add(stripExtension(f.getName()));
                    allAvailableChains.add(detectChains(f));
                }

                processingPanel.updateStep(1, ProcessingPanel.StepState.COMPLETED);
                processingPanel.updateStep(2, ProcessingPanel.StepState.ACTIVE);
                processingPanel.setDetails(String.valueOf(files.length), "—", "Intersecting chains...");

                List<Character> common = findCommonChains(allAvailableChains);
                List<Integer> residueCounts = new ArrayList<>();
                
                if (!fileList.isEmpty() && !common.isEmpty()) {
                    for (char ch : common) {
                        residueCounts.add(countResiduesInChain(fileList.get(0), ch));
                    }
                }

                processingPanel.updateStep(2, ProcessingPanel.StepState.COMPLETED);
                processingPanel.updateStep(3, ProcessingPanel.StepState.ACTIVE);
                processingPanel.setDetails(String.valueOf(files.length), "—", "Ready for selection");

                loadedFiles = fileList;
                loadedPdbNames = fileNames;
                commonChains = common;
                commonChainResidueCounts = residueCounts;

                SwingUtilities.invokeLater(() -> {
                    chainSelectionPanel.setChains(common, residueCounts);
                    mainLayout.show(mainContainer, "CHAIN_SELECTION");
                });

            } catch (Exception ex) {
                ex.printStackTrace();
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(frame, "Error scanning files: " + ex.getMessage(), "Loading Error", JOptionPane.ERROR_MESSAGE);
                    mainLayout.show(mainContainer, "HOME");
                });
            }
        }).start();
    }

    private void runAnalysisWorkflow(char selectedChain) {
        analysisPanel.reset();
        mainLayout.show(mainContainer, "ANALYSIS");

        new Thread(() -> {
            try {
                // Initialize logs pane HTML formatting
                initLogTextPane(analysisPanel.getLogTextPane());

                // Set step statuses for 4 major stages
                analysisPanel.setStepStatus(0, PipelinePanel.StepStatus.COMPLETED); // 01 Structure Complete
                analysisPanel.setStepStatus(1, PipelinePanel.StepStatus.ACTIVE);    // 02 Graph Analysis Active
                analysisPanel.setStepStatus(2, PipelinePanel.StepStatus.PENDING);
                analysisPanel.setStepStatus(3, PipelinePanel.StepStatus.PENDING);
                
                analysisPanel.setMetadataText("Running Contact Graph Analysis...");

                List<Structure> structures = new ArrayList<>();
                for (int i = 0; i < loadedFiles.size(); i++) {
                    String id = loadedPdbNames.get(i);
                    File file = loadedFiles.get(i);

                    append("Loading " + id + " chain " + selectedChain + "...\n");
                    Structure s = new Structure(id, selectedChain);
                    s.loadPDB(file.getAbsolutePath());
                    structures.add(s);
                }

                // Run actual scientific analysis
                Analyzer analyzer = new Analyzer(this::append, snapshot -> {
                    analysisPanel.addSnapshot(snapshot);
                });

                append("\n============================================\n");
                append("PROTEIN DOMAIN PREDICTOR\n");
                append("Graph-based rigid-domain estimation\n");
                append("============================================\n");

                append("\nParameters:\n");
                append("  Contact cutoff = 7.5 Å\n");
                append("  Rigidity threshold = 3.5 Å\n");
                append("  Merging threshold = 1.0\n");

                Analyzer.Result result = analyzer.analyze(structures, 7.5, 3.5, 1.0);

                lastResult = result;
                lastStructures = structures;

                // Complete remaining pipeline statuses
                analysisPanel.setStepStatus(1, PipelinePanel.StepStatus.COMPLETED);
                analysisPanel.setStepStatus(2, PipelinePanel.StepStatus.COMPLETED);
                analysisPanel.setStepStatus(3, PipelinePanel.StepStatus.COMPLETED);

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
                    append("Residues: " + (residues.get(0) + 1) + " - " + (residues.get(residues.size() - 1) + 1) + "\n");
                    append("Count: " + residues.size() + "\n");
                    append("Residue list: ");

                    for (int i = 0; i < residues.size(); i++) {
                        int index = residues.get(i);
                        Residue r = result.alignedResidues.get(0).get(index);
                        append(r.number + (r.insertionCode == ' ' ? "" : String.valueOf(r.insertionCode)) + r.name + (i == residues.size() - 1 ? "" : ", "));
                    }
                    append("\n");

                    Residue first = result.alignedResidues.get(0).get(residues.get(0));
                    Residue last = result.alignedResidues.get(0).get(residues.get(residues.size() - 1));
                    summaries.add(new DomainSummary(color, "Domain " + (d + 1) + ": residues " + first.number + "-" + last.number + " (" + residues.size() + " aa)"));
                }

                domainColors = colors;
                domainSummaries = summaries;

                append("\nAnalysis complete.\n");
                
                int hingeCount = result.lineNodes.size() - result.domains.stream().mapToInt(List::size).sum();
                if (hingeCount < 0) hingeCount = 0; // fallback calculation
                
                // Formatted metadata with correct singular/plural values
                String structStr = structures.size() == 1 ? "1 Structure" : structures.size() + " Structures";
                String resStr = result.alignedResidues.get(0).size() == 1 ? "1 Residue" : result.alignedResidues.get(0).size() + " Residues";
                String domStr = result.domains.size() == 1 ? "1 Domain" : result.domains.size() + " Domains";
                String hingeStr = hingeCount == 1 ? "1 Hinge Candidate" : hingeCount + " Hinge Candidates";

                String metadataText = String.format("%s  ·  Chain %c  ·  %s  ·  %s  ·  %s",
                        structStr, selectedChain, resStr, domStr, hingeStr);
                analysisPanel.setMetadataText(metadataText);

                // Populate 3D structures and enable toggle
                updateSuperimpositionData();
                analysisPanel.enable3DView(true);

            } catch (Exception ex) {
                ex.printStackTrace();
                append("\nERROR: " + ex.getMessage() + "\n");
                analysisPanel.setMetadataText("Error during analysis: " + ex.getMessage());
                analysisPanel.setStepStatus(1, PipelinePanel.StepStatus.ERROR);
                analysisPanel.setStepStatus(2, PipelinePanel.StepStatus.ERROR);
                analysisPanel.setStepStatus(3, PipelinePanel.StepStatus.ERROR);
                
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(frame, "Error running analysis: " + ex.getMessage(), "Analysis Error", JOptionPane.ERROR_MESSAGE);
                });
            }
        }).start();
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
            Superposition.Transform transform = (k == 0) ? null : Superposition.fit(mobile, reference);

            List<double[]> points = new ArrayList<>();
            for (Residue r : mobile) {
                points.add(transform == null
                        ? new double[]{r.x, r.y, r.z}
                        : transform.apply(r.x, r.y, r.z));
            }

            traces.add(new Structure3DPanel.Trace(structures.get(k).pdbId + "_" + structures.get(k).chain, points));
        }

        structure3DPanel.setData(traces, domainColors);

        JPanel legend = buildLegendPanel(structures);
        analysisPanel.getLegendContainer().removeAll();
        analysisPanel.getLegendContainer().add(legend, BorderLayout.CENTER);
        analysisPanel.getLegendContainer().revalidate();
        analysisPanel.getLegendContainer().repaint();
    }

    private JPanel buildLegendPanel(List<Structure> structures) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setPreferredSize(new Dimension(200, 0));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.setBackground(AppTheme.getBackground());

        JLabel titleLbl = AppTheme.createLabel("Structures Overlaid:", AppTheme.SMALL_BOLD, AppTheme.TEXT);
        panel.add(titleLbl);
        panel.add(Box.createVerticalStrut(4));

        for (Structure s : structures) {
            JLabel nameLbl = AppTheme.createLabel("  " + s.pdbId + " (Chain " + s.chain + ")", AppTheme.SMALL, AppTheme.TEXT_MUTED);
            panel.add(nameLbl);
        }

        panel.add(Box.createVerticalStrut(16));

        JLabel domainTitleLbl = AppTheme.createLabel("Domains Detected:", AppTheme.SMALL_BOLD, AppTheme.TEXT);
        panel.add(domainTitleLbl);
        panel.add(Box.createVerticalStrut(6));

        List<DomainSummary> summaries = domainSummaries;
        if (summaries != null) {
            for (DomainSummary ds : summaries) {
                JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
                row.setBackground(AppTheme.getBackground());

                JLabel swatch = new JLabel() {
                    @Override
                    protected void paintComponent(Graphics g) {
                        super.paintComponent(g);
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        g2.setColor(ds.color);
                        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 4, 4);
                        g2.dispose();
                    }
                };
                swatch.setPreferredSize(new Dimension(14, 14));
                row.add(swatch);

                JLabel swatchLabel = AppTheme.createLabel(ds.label, AppTheme.SMALL, AppTheme.TEXT_MUTED);
                row.add(swatchLabel);
                
                panel.add(row);
            }
        }

        panel.add(Box.createVerticalGlue());
        return panel;
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

    private List<Character> findCommonChains(List<List<Character>> allChains) {
        if (allChains == null || allChains.isEmpty()) return new ArrayList<>();
        List<Character> common = new ArrayList<>(allChains.get(0));
        for (int i = 1; i < allChains.size(); i++) {
            common.retainAll(allChains.get(i));
        }
        return common;
    }

    private int countResiduesInChain(File file, char chain) {
        int count = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if ((line.startsWith("ATOM") || line.startsWith("HETATM")) && line.length() >= 22) {
                    char lineChain = line.charAt(21);
                    String atomName = line.substring(12, 16).trim();
                    char altLoc = line.charAt(16);
                    if (atomName.equals("CA") && lineChain == chain && (altLoc == ' ' || altLoc == 'A')) {
                        count++;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return count;
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
                throw new IOException("RCSB download failed for " + id + " (HTTP " + response.statusCode() + ")");
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

    private String stripExtension(String name) {
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }

    private void initLogTextPane(JTextPane textPane) {
        boolean isDark = AppTheme.getThemeMode() == AppTheme.ThemeMode.DARK;
        String bg = isDark ? "#030712" : "#ffffff";
        String fg = isDark ? "#E5E7EB" : "#1f2937";
        String stepColor = isDark ? "#A5B4FC" : "#4f46e5";
        String paramColor = isDark ? "#9CA3AF" : "#6b7280";
        String hrColor = isDark ? "rgba(255,255,255,0.08)" : "#e2e8f0";

        textPane.setContentType("text/html");
        textPane.setEditable(false);
        HTMLEditorKit kit = new HTMLEditorKit();
        textPane.setEditorKit(kit);
        HTMLDocument doc = (HTMLDocument) textPane.getDocument();
        StyleSheet sheet = doc.getStyleSheet();
        sheet.addRule("body { font-family: monospace; font-size: 12px; background-color: " + bg + "; color: " + fg + "; padding: 10px; line-height: 1.4; }");
        sheet.addRule("h2 { color: " + stepColor + "; margin: 12px 0 6px 0; font-size: 14px; font-weight: bold; border-bottom: 1px solid " + hrColor + "; padding-bottom: 2px; }");
        sheet.addRule("hr { border: 0; border-top: 1px solid " + hrColor + "; margin: 10px 0; }");
        sheet.addRule(".success { color: #10B981; }");
        sheet.addRule(".error { color: #EF4444; font-weight: bold; }");
        sheet.addRule(".step { color: " + stepColor + "; font-weight: bold; }");
        sheet.addRule(".param { color: " + paramColor + "; }");
        textPane.setText("<html><body></body></html>");
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

                JTextPane logPane = analysisPanel.getLogTextPane();
                HTMLDocument doc = (HTMLDocument) logPane.getDocument();
                HTMLEditorKit kit = (HTMLEditorKit) logPane.getEditorKit();
                kit.insertHTML(doc, doc.getLength(), html, 0, 0, null);
                logPane.setCaretPosition(doc.getLength());
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
}
