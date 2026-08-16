import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Main {

    private final JTextField pdbIdsField = new JTextField("4AKE_A,1AKE_A");
    private final JTextField chainField = new JTextField("A");
    private final JTextArea output = new JTextArea();
    private final JLabel status = new JLabel("Ready");

    private final DefaultListModel<String> stepListModel = new DefaultListModel<>();
    private final JList<String> stepList = new JList<>(stepListModel);
    private final GraphPanel graphPanel = new GraphPanel();
    private final List<GraphPanel.Snapshot> snapshots = new ArrayList<>();
    private final JButton superimposeButton = new JButton("Show Superimposition");

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

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Main().createWindow());
    }

    private void createWindow() {

        JFrame frame = new JFrame("Protein Domain Analyzer");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1050, 720);
        frame.setLocationRelativeTo(null);

        JPanel top = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 6, 6, 6);
        c.fill = GridBagConstraints.HORIZONTAL;

        c.gridx = 0; c.gridy = 0;
        top.add(new JLabel("PDB IDs + chains:"), c);

        c.gridx = 1; c.weightx = 1;
        top.add(pdbIdsField, c);

        c.gridx = 2; c.weightx = 0;
        top.add(new JLabel("Default chain:"), c);

        c.gridx = 3;
        chainField.setColumns(3);
        top.add(chainField, c);

        JButton analyzeIds = new JButton("Analyze PDB IDs");
        c.gridx = 4;
        top.add(analyzeIds, c);

        JButton chooseFiles = new JButton("Analyze PDB Files");
        c.gridx = 5;
        top.add(chooseFiles, c);

        superimposeButton.setEnabled(false);
        c.gridx = 6;
        top.add(superimposeButton, c);

        output.setEditable(false);
        output.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        output.setLineWrap(false);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.add(status, BorderLayout.WEST);

        stepList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        stepList.addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            int index = stepList.getSelectedIndex();
            if (index >= 0 && index < snapshots.size()) {
                graphPanel.show(snapshots.get(index));
            }
        });

        JScrollPane stepScroll = new JScrollPane(stepList);
        stepScroll.setPreferredSize(new Dimension(220, 0));
        stepScroll.setBorder(BorderFactory.createTitledBorder("Pipeline steps"));

        graphPanel.setBorder(BorderFactory.createTitledBorder("Step graph"));

        JSplitPane rightSplit = new JSplitPane(
                JSplitPane.VERTICAL_SPLIT, graphPanel, new JScrollPane(output));
        rightSplit.setResizeWeight(0.6);

        JSplitPane mainSplit = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT, stepScroll, rightSplit);
        mainSplit.setResizeWeight(0.0);

        frame.add(top, BorderLayout.NORTH);
        frame.add(mainSplit, BorderLayout.CENTER);
        frame.add(bottom, BorderLayout.SOUTH);

        analyzeIds.addActionListener(e -> analyzeIds());
        chooseFiles.addActionListener(e -> analyzeFiles(frame));
        superimposeButton.addActionListener(e -> showSuperimposition());

        frame.setVisible(true);
    }

    private void analyzeIds() {

        output.setText("");
        clearSteps();
        superimposeButton.setEnabled(false);
        status.setText("Downloading / analyzing...");

        new Thread(() -> {
            try {
                String raw = pdbIdsField.getText().trim();
                if (raw.isEmpty()) {
                    throw new IllegalArgumentException(
                            "Enter at least two PDB IDs, e.g. 4AKE_A,1AKE_A");
                }

                String defaultChain = chainField.getText().trim();
                char defaultChainChar =
                        defaultChain.isEmpty() ? 'A' : defaultChain.charAt(0);

                List<Structure> structures = new ArrayList<>();
                Path data = Path.of("data");

                for (String token : raw.split(",")) {
                    token = token.trim();
                    if (token.isEmpty()) continue;

                    String[] parts = token.split("_");
                    String id = parts[0].trim();
                    char chain = parts.length > 1
                            ? parts[1].trim().charAt(0)
                            : defaultChainChar;

                    append("Loading " + id.toUpperCase()
                            + " chain " + chain + "...\n");

                    Structure s = Structure.fromPdbId(id, chain, data);
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
        chooser.setFileFilter(
                new FileNameExtensionFilter("PDB files", "pdb", "ent"));

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

        String chainText = chainField.getText().trim();
        char chain = chainText.isEmpty() ? 'A' : chainText.charAt(0);

        output.setText("");
        clearSteps();
        superimposeButton.setEnabled(false);
        status.setText("Analyzing files...");

        new Thread(() -> {
            try {
                List<Structure> structures = new ArrayList<>();

                for (File file : files) {
                    append("Reading " + file.getName() + " chain "
                            + chain + "...\n");

                    Structure s = new Structure(
                            stripExtension(file.getName()), chain);

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
        append("PROTEIN DOMAIN ANALYZER\n");
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

            append("\nDOMAIN " + (d + 1) + "\n");
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

    private void append(String text) {
        SwingUtilities.invokeLater(() -> output.append(text));
    }

    private void setStatus(String text) {
        SwingUtilities.invokeLater(() -> status.setText(text));
    }

    private void clearSteps() {
        stepListModel.clear();
        snapshots.clear();
        graphPanel.show(null);
    }

    private void onSnapshot(GraphPanel.Snapshot snapshot) {
        SwingUtilities.invokeLater(() -> {
            snapshots.add(snapshot);
            stepListModel.addElement(snapshot.title);
            stepList.setSelectedIndex(stepListModel.size() - 1);
            graphPanel.show(snapshot);
        });
    }

    private void showSuperimposition() {

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

        Structure3DPanel panel = new Structure3DPanel();
        panel.setData(traces, domainColors);

        JDialog dialog = new JDialog((Frame) null, "Superimposed Structures", false);
        dialog.setLayout(new BorderLayout());
        dialog.add(panel, BorderLayout.CENTER);
        dialog.add(buildLegendPanel(structures), BorderLayout.EAST);
        dialog.setSize(950, 700);
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
    }

    private JPanel buildLegendPanel(List<Structure> structures) {

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createTitledBorder("Legend"));
        panel.setPreferredSize(new Dimension(260, 0));

        panel.add(new JLabel("Structures overlaid:"));
        for (Structure s : structures) {
            panel.add(new JLabel("  " + s.pdbId + " chain " + s.chain));
        }

        panel.add(Box.createVerticalStrut(12));
        panel.add(new JLabel("Domains:"));

        List<DomainSummary> summaries = domainSummaries;
        if (summaries != null) {
            for (DomainSummary ds : summaries) {
                JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));

                JLabel swatch = new JLabel();
                swatch.setOpaque(true);
                swatch.setBackground(ds.color);
                swatch.setPreferredSize(new Dimension(16, 16));

                row.add(swatch);
                row.add(new JLabel(ds.label));
                panel.add(row);
            }
        }

        panel.add(Box.createVerticalGlue());
        return panel;
    }
}
