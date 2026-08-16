import java.awt.Color;
import java.util.*;
import java.util.function.Consumer;

public class Predictor {

    static final class Edge {
        int u, v;
        double weight;
        double variance;

        Edge(int u, int v, double weight, double variance) {
            this.u = u;
            this.v = v;
            this.weight = weight;
            this.variance = variance;
        }
    }

    static final class CoarseEdge {
        int u, v;
        double variance;

        CoarseEdge(int u, int v, double variance) {
            this.u = u;
            this.v = v;
            this.variance = variance;
        }
    }

    static final class LineNode {
        int coarseEdgeIndex;
        int u, v;
        double variance;

        LineNode(int coarseEdgeIndex, int u, int v, double variance) {
            this.coarseEdgeIndex = coarseEdgeIndex;
            this.u = u;
            this.v = v;
            this.variance = variance;
        }
    }

    static final class LineEdge {
        int a, b;
        int mid;
        int end1, end2;
        boolean endNodesConnected;
        double variance;

        LineEdge(int a, int b, int mid, int end1, int end2,
                 boolean endNodesConnected, double variance) {
            this.a = a;
            this.b = b;
            this.mid = mid;
            this.end1 = end1;
            this.end2 = end2;
            this.endNodesConnected = endNodesConnected;
            this.variance = variance;
        }
    }

    static final class Result {
        List<List<Residue>> alignedResidues;
        double[][][] distances;
        List<Edge> proteinEdges;
        List<List<Integer>> communities;
        List<CoarseEdge> coarseEdges;
        List<LineNode> lineNodes;
        List<LineEdge> lineEdges;
        int[] lineLabels;
        List<List<Integer>> domains;
        double resolution;

        Result(List<List<Residue>> alignedResidues,
               double[][][] distances,
               List<Edge> proteinEdges,
               List<List<Integer>> communities,
               List<CoarseEdge> coarseEdges,
               List<LineNode> lineNodes,
               List<LineEdge> lineEdges,
               int[] lineLabels,
               List<List<Integer>> domains,
               double resolution) {
            this.alignedResidues = alignedResidues;
            this.distances = distances;
            this.proteinEdges = proteinEdges;
            this.communities = communities;
            this.coarseEdges = coarseEdges;
            this.lineNodes = lineNodes;
            this.lineEdges = lineEdges;
            this.lineLabels = lineLabels;
            this.domains = domains;
            this.resolution = resolution;
        }
    }

    private final Consumer<String> logger;
    private final Consumer<GraphPanel.Snapshot> onSnapshot;

    public Predictor(Consumer<String> logger) {
        this(logger, null);
    }

    public Predictor(Consumer<String> logger, Consumer<GraphPanel.Snapshot> onSnapshot) {
        this.logger = logger == null ? s -> {} : logger;
        this.onSnapshot = onSnapshot == null ? s -> {} : onSnapshot;
    }

    private void log(String s) {
        logger.accept(s);
    }

    private void emitSnapshot(String title, String subtitle, int nodeCount,
                               List<int[]> edges, String[] nodeLabels,
                               Color[] nodeColors, Color[] edgeColors) {
        onSnapshot.accept(new GraphPanel.Snapshot(
                title, subtitle, nodeCount, edges, nodeLabels, nodeColors, edgeColors));
    }

    private List<int[]> edgePairs(List<Edge> edges) {
        List<int[]> pairs = new ArrayList<>(edges.size());
        for (Edge e : edges) pairs.add(new int[]{e.u, e.v});
        return pairs;
    }

    private String[] residueLabels(List<Residue> residues) {
        String[] labels = new String[residues.size()];
        for (int i = 0; i < residues.size(); i++) {
            labels[i] = String.valueOf(residues.get(i).number);
        }
        return labels;
    }

    private String[] communityLabels(int count) {
        String[] labels = new String[count];
        for (int i = 0; i < count; i++) labels[i] = "C" + (i + 1);
        return labels;
    }

    private String[] lineNodeLabels(List<LineNode> nodes) {
        String[] labels = new String[nodes.size()];
        for (int i = 0; i < nodes.size(); i++) {
            LineNode n = nodes.get(i);
            labels[i] = "C" + (n.u + 1) + "-C" + (n.v + 1);
        }
        return labels;
    }

    private List<int[]> filteredCoarseEdgePairs(List<CoarseEdge> edges, int[] lineLabels) {
        List<int[]> pairs = new ArrayList<>();
        for (int i = 0; i < edges.size(); i++) {
            if (lineLabels[i] == -1) continue;
            CoarseEdge e = edges.get(i);
            pairs.add(new int[]{e.u, e.v});
        }
        return pairs;
    }

    private Color[] groupColors(int vertexCount, List<List<Integer>> groups) {
        Color[] colors = new Color[vertexCount];
        for (int g = 0; g < groups.size(); g++) {
            Color color = GraphPanel.paletteColor(g);
            for (int v : groups.get(g)) {
                colors[v] = color;
            }
        }
        return colors;
    }

    public Result predict(List<Structure> structures,
                          double cutoff,
                          double rigidityThreshold,
                          double mergingThreshold) {

        if (structures.size() < 2) {
            throw new IllegalArgumentException("At least two conformations are required.");
        }

        log("STEP 1 - INPUT");
        for (Structure s : structures) {
            log("  " + s.pdbId + " chain " + s.chain
                    + " : " + s.residues.size() + " C-alpha residues");
        }

        log("\nSTEP 2 - RESIDUE CORRESPONDENCE");
        List<List<Residue>> aligned = alignAll(structures);
        log("  Common aligned residues: " + aligned.get(0).size());

        if (aligned.get(0).size() < 2) {
            throw new IllegalArgumentException("Too few common residues after alignment.");
        }

        log("\nSTEP 3 - DISTANCE MATRICES");
        double[][][] distances = buildDistanceMatrices(aligned);
        log("  Built " + structures.size() + " distance matrices.");

        log("\nSTEP 4 - MULTI-STRUCTURE PROTEIN GRAPH");
        List<Edge> proteinEdges = buildProteinGraph(distances, cutoff);
        log("  Vertices: " + aligned.get(0).size());
        log("  Cutoff: " + cutoff + " Å");
        log("  Edges: " + proteinEdges.size());
        log("  Edge weight = exp(-variance)");

        emitSnapshot("STEP 4 - Protein Graph",
                aligned.get(0).size() + " residues, " + proteinEdges.size()
                        + " edges (cutoff " + cutoff + " Å)",
                aligned.get(0).size(), edgePairs(proteinEdges),
                residueLabels(aligned.get(0)), null, null);

        log("\nSTEP 5 - LOUVAIN COARSE-GRAINING");
        int target = Math.min(20, Math.max(2, aligned.get(0).size() / 10));
        LouvainResult lr = findLouvainPartition(
                aligned.get(0).size(), proteinEdges, target);
        log("  Resolution: " + String.format(Locale.US, "%.6f", lr.resolution));
        log("  Communities: " + lr.communities.size());

        for (int i = 0; i < lr.communities.size(); i++) {
            log("    C" + (i + 1) + ": " + lr.communities.get(i).size() + " residues");
        }

        Color[] residueColors = new Color[aligned.get(0).size()];
        for (int c = 0; c < lr.communities.size(); c++) {
            for (int v : lr.communities.get(c)) {
                residueColors[v] = GraphPanel.paletteColor(c);
            }
        }

        emitSnapshot("STEP 5 - Louvain Communities",
                lr.communities.size() + " communities, resolution "
                        + String.format(Locale.US, "%.6f", lr.resolution),
                aligned.get(0).size(), edgePairs(proteinEdges),
                residueLabels(aligned.get(0)), residueColors, null);

        log("\nSTEP 6 - COARSE-GRAINED GRAPH");
        List<CoarseEdge> coarseEdges =
                buildCoarseGraph(lr.communities, proteinEdges, distances);
        log("  Coarse vertices: " + lr.communities.size());
        log("  Coarse edges: " + coarseEdges.size());

        Color[] communityColors = new Color[lr.communities.size()];
        for (int i = 0; i < communityColors.length; i++) {
            communityColors[i] = GraphPanel.paletteColor(i);
        }

        List<int[]> coarseEdgePairs = new ArrayList<>(coarseEdges.size());
        for (CoarseEdge e : coarseEdges) coarseEdgePairs.add(new int[]{e.u, e.v});

        emitSnapshot("STEP 6 - Coarse-Grained Graph",
                lr.communities.size() + " community vertices, " + coarseEdges.size() + " edges",
                lr.communities.size(), coarseEdgePairs,
                communityLabels(lr.communities.size()), communityColors, null);

        log("\nSTEP 7 - LINE GRAPH");
        List<LineNode> lineNodes = new ArrayList<>();
        for (int i = 0; i < coarseEdges.size(); i++) {
            CoarseEdge e = coarseEdges.get(i);
            lineNodes.add(new LineNode(i, e.u, e.v, e.variance));
        }

        List<LineEdge> lineEdges = buildLineGraph(coarseEdges, lineNodes, lr.communities, distances);
        log("  Line-graph vertices: " + lineNodes.size());
        log("  Line-graph edges: " + lineEdges.size());

        List<int[]> lineEdgePairs = new ArrayList<>(lineEdges.size());
        for (LineEdge e : lineEdges) lineEdgePairs.add(new int[]{e.a, e.b});

        emitSnapshot("STEP 7 - Line Graph",
                lineNodes.size() + " line vertices (one per coarse edge), " + lineEdges.size() + " line edges",
                lineNodes.size(), lineEdgePairs, lineNodeLabels(lineNodes), null, null);

        log("\nSTEP 8 - MAD OUTLIER DETECTION");
        double[] lineVertexVariance = lineNodes.stream().mapToDouble(x -> x.variance).toArray();
        double[] lineEdgeVariance = lineEdges.stream().mapToDouble(x -> x.variance).toArray();
        boolean[] vertexOutliers = expandOutliers(lineVertexVariance, detectOutliers(lineVertexVariance, 3.5), 0.05);
        boolean[] edgeOutliers = expandOutliers(lineEdgeVariance, detectOutliers(lineEdgeVariance, 3.5), 0.05);

        log("  Outlier line vertices: " + countTrue(vertexOutliers));
        log("  Outlier line edges: " + countTrue(edgeOutliers));

        Color[] outlierNodeColors = new Color[lineNodes.size()];
        for (int i = 0; i < outlierNodeColors.length; i++) {
            outlierNodeColors[i] = vertexOutliers[i]
                    ? new Color(200, 60, 60) : new Color(70, 130, 180);
        }

        Color[] outlierEdgeColors = new Color[lineEdges.size()];
        for (int i = 0; i < outlierEdgeColors.length; i++) {
            outlierEdgeColors[i] = edgeOutliers[i]
                    ? new Color(200, 60, 60, 180) : new Color(120, 120, 120, 140);
        }

        emitSnapshot("STEP 8 - MAD Outlier Detection",
                countTrue(vertexOutliers) + " outlier vertices, " + countTrue(edgeOutliers)
                        + " outlier edges (red)",
                lineNodes.size(), lineEdgePairs, lineNodeLabels(lineNodes),
                outlierNodeColors, outlierEdgeColors);

        log("\nSTEP 9 - GENERALIZED-VITERBI-STYLE LABEL INFERENCE");
        int[] labels = inferLabels(lineNodes, lineEdges,
                vertexOutliers, edgeOutliers, 10000);
        log("  Label +1 = same-domain side");
        log("  Label -1 = boundary/outlier side");

        Color[] labelColors = new Color[lineNodes.size()];
        for (int i = 0; i < labelColors.length; i++) {
            labelColors[i] = labels[i] == 1
                    ? new Color(60, 160, 90) : new Color(210, 140, 40);
        }

        emitSnapshot("STEP 9 - Label Inference",
                "Green = +1 same-domain, orange = -1 boundary/outlier",
                lineNodes.size(), lineEdgePairs, lineNodeLabels(lineNodes),
                labelColors, null);

        log("\nSTEP 10 - REMOVE NEGATIVE COARSE EDGES");
        List<List<Integer>> domains = splitCoarseGraph(
                lr.communities.size(), coarseEdges, labels);
        log("  Initial disconnected groups: " + domains.size());

        emitSnapshot("STEP 10 - Remove Negative Coarse Edges",
                domains.size() + " disconnected groups after removing boundary edges",
                lr.communities.size(), filteredCoarseEdgePairs(coarseEdges, labels),
                communityLabels(lr.communities.size()),
                groupColors(lr.communities.size(), domains), null);

        log("\nSTEP 11 - RIGIDITY CHECK");
        List<List<Integer>> refined = refineByRmsd(
                domains, lr.communities, aligned, rigidityThreshold);
        log("  Groups after RMSD refinement: " + refined.size());

        emitSnapshot("STEP 11 - Rigidity Check",
                refined.size() + " groups after RMSD refinement (threshold "
                        + rigidityThreshold + " Å)",
                lr.communities.size(), filteredCoarseEdgePairs(coarseEdges, labels),
                communityLabels(lr.communities.size()),
                groupColors(lr.communities.size(), refined), null);

        log("\nSTEP 12 - MERGING");
        List<List<Integer>> merged = mergeDomains(
                refined, lr.communities, aligned, mergingThreshold);
        log("  Final domains: " + merged.size());

        emitSnapshot("STEP 12 - Final Domains",
                merged.size() + " final domain(s) after merging (threshold " + mergingThreshold + ")",
                lr.communities.size(), coarseEdgePairs,
                communityLabels(lr.communities.size()),
                groupColors(lr.communities.size(), merged), null);

        for (int i = 0; i < merged.size(); i++) {
            List<Integer> residues = expandCommunityGroup(
                    merged.get(i), lr.communities);
            int first = residues.get(0) + 1;
            int last = residues.get(residues.size() - 1) + 1;
            double rmsd = rmsd(aligned, residues);

            log("  Domain " + (i + 1)
                    + ": residues " + first + "-" + last
                    + " (" + residues.size() + " aa)"
                    + ", RMSD=" + String.format(Locale.US, "%.3f Å", rmsd));
        }

        return new Result(
                aligned, distances, proteinEdges, lr.communities,
                coarseEdges, lineNodes, lineEdges, labels, merged,
                lr.resolution
        );
    }

    private List<List<Residue>> alignAll(List<Structure> structures) {
        List<List<Residue>> result = new ArrayList<>();
        List<Residue> reference = structures.get(0).residues;

        List<int[]> mappings = new ArrayList<>();
        mappings.add(identityMapping(reference.size()));

        for (int s = 1; s < structures.size(); s++) {
            mappings.add(globalAlignmentMapping(reference, structures.get(s).residues));
        }

        for (int refIndex = 0; refIndex < reference.size(); refIndex++) {
            boolean present = true;
            for (int s = 1; s < structures.size(); s++) {
                if (mappings.get(s)[refIndex] < 0) {
                    present = false;
                    break;
                }
            }

            if (!present) continue;

            for (int s = 0; s < structures.size(); s++) {
                if (result.size() <= s) result.add(new ArrayList<>());
                int idx = mappings.get(s)[refIndex];
                result.get(s).add(structures.get(s).residues.get(idx));
            }
        }

        return result;
    }

    private int[] identityMapping(int n) {
        int[] map = new int[n];
        for (int i = 0; i < n; i++) map[i] = i;
        return map;
    }

    private int[] globalAlignmentMapping(List<Residue> reference,
                                         List<Residue> other) {

        String a = sequence(reference);
        String b = sequence(other);

        int n = a.length();
        int m = b.length();
        int gap = -2;
        int match = 2;
        int mismatch = -1;

        int[][] score = new int[n + 1][m + 1];

        for (int i = 1; i <= n; i++) score[i][0] = score[i - 1][0] + gap;
        for (int j = 1; j <= m; j++) score[0][j] = score[0][j - 1] + gap;

        for (int i = 1; i <= n; i++) {
            for (int j = 1; j <= m; j++) {
                int diag = score[i - 1][j - 1]
                        + (a.charAt(i - 1) == b.charAt(j - 1) ? match : mismatch);
                int up = score[i - 1][j] + gap;
                int left = score[i][j - 1] + gap;
                score[i][j] = Math.max(diag, Math.max(up, left));
            }
        }

        int[] map = new int[n];
        Arrays.fill(map, -1);

        int i = n;
        int j = m;

        while (i > 0 || j > 0) {
            if (i > 0 && j > 0) {
                int diagScore = score[i - 1][j - 1]
                        + (a.charAt(i - 1) == b.charAt(j - 1) ? match : mismatch);

                if (score[i][j] == diagScore) {
                    map[i - 1] = j - 1;
                    i--;
                    j--;
                    continue;
                }
            }

            if (i > 0 && score[i][j] == score[i - 1][j] + gap) {
                i--;
            } else {
                j--;
            }
        }

        return map;
    }

    private String sequence(List<Residue> residues) {
        StringBuilder sb = new StringBuilder();
        for (Residue r : residues) sb.append(r.oneLetter());
        return sb.toString();
    }

    private double[][][] buildDistanceMatrices(List<List<Residue>> aligned) {
        int m = aligned.size();
        int n = aligned.get(0).size();
        double[][][] d = new double[m][n][n];

        for (int k = 0; k < m; k++) {
            for (int i = 0; i < n; i++) {
                for (int j = i + 1; j < n; j++) {
                    double value = distance(
                            aligned.get(k).get(i),
                            aligned.get(k).get(j));
                    d[k][i][j] = value;
                    d[k][j][i] = value;
                }
            }
        }
        return d;
    }

    private List<Edge> buildProteinGraph(double[][][] d, double cutoff) {
        int n = d[0].length;
        List<Edge> edges = new ArrayList<>();

        for (int i = 0; i < n - 1; i++) {
            for (int j = i + 1; j < n; j++) {

                double max = 0.0;
                double mean = 0.0;

                for (double[][] matrix : d) {
                    max = Math.max(max, matrix[i][j]);
                    mean += matrix[i][j];
                }

                mean /= d.length;

                double variance = 0.0;
                for (double[][] matrix : d) {
                    double diff = matrix[i][j] - mean;
                    variance += diff * diff;
                }
                variance /= d.length;

                if (max <= cutoff) {
                    edges.add(new Edge(i, j, Math.exp(-variance), variance));
                }
            }
        }

        return edges;
    }

    private static final class LouvainResult {
        List<List<Integer>> communities;
        double resolution;

        LouvainResult(List<List<Integer>> communities, double resolution) {
            this.communities = communities;
            this.resolution = resolution;
        }
    }

    private LouvainResult findLouvainPartition(
            int n, List<Edge> edges, int target) {

        double low = 0.001;
        double high = 0.75;
        double bestResolution = 0.75;
        List<List<Integer>> best = null;
        int bestDiff = Integer.MAX_VALUE;

        for (int iter = 0; iter < 30; iter++) {
            double resolution = (low + high) / 2.0;
            List<List<Integer>> communities =
                    louvainOnce(n, edges, resolution);

            int diff = Math.abs(communities.size() - target);

            if (diff < bestDiff) {
                bestDiff = diff;
                best = communities;
                bestResolution = resolution;
            }

            if (communities.size() == target) {
                return new LouvainResult(communities, resolution);
            }

            if (communities.size() > target) {
                high = resolution;
            } else {
                low = resolution;
            }
        }

        return new LouvainResult(best, bestResolution);
    }

    private List<List<Integer>> louvainOnce(
            int n, List<Edge> edges, double resolution) {

        List<Map<Integer, Double>> adjacency = new ArrayList<>();
        for (int i = 0; i < n; i++) adjacency.add(new HashMap<>());

        for (Edge e : edges) {
            adjacency.get(e.u).merge(e.v, e.weight, Double::sum);
            adjacency.get(e.v).merge(e.u, e.weight, Double::sum);
        }

        int[] community = new int[n];
        for (int i = 0; i < n; i++) community[i] = i;

        int[] communitySizes = new int[n];
        Arrays.fill(communitySizes, 1);

        boolean moved = true;
        int rounds = 0;

        while (moved && rounds++ < 100) {
            moved = false;

            for (int node = 0; node < n; node++) {
                int current = community[node];

                Set<Integer> candidates = new HashSet<>();
                candidates.add(current);
                for (int neighbor : adjacency.get(node).keySet()) {
                    candidates.add(community[neighbor]);
                }

                int bestCommunity = current;
                double bestGain = 0.0;

                double currentGain = communityScore(
                        node, current, community, communitySizes, adjacency, resolution);

                for (int candidate : candidates) {
                    if (candidate == current) continue;

                    double gain = communityScore(
                            node, candidate, community, communitySizes, adjacency, resolution)
                            - currentGain;

                    if (gain > bestGain + 1e-12) {
                        bestGain = gain;
                        bestCommunity = candidate;
                    }
                }

                if (bestCommunity != current) {
                    community[node] = bestCommunity;
                    communitySizes[current]--;
                    communitySizes[bestCommunity]++;
                    moved = true;
                }
            }
        }

        Map<Integer, List<Integer>> groups = new LinkedHashMap<>();
        for (int i = 0; i < n; i++) {
            groups.computeIfAbsent(community[i], k -> new ArrayList<>()).add(i);
        }

        return new ArrayList<>(groups.values());
    }

    private double communityScore(
            int node,
            int communityId,
            int[] community,
            int[] communitySizes,
            List<Map<Integer, Double>> adjacency,
            double resolution) {

        double connection = 0.0;
        int size = communitySizes[communityId];

        for (Map.Entry<Integer, Double> e : adjacency.get(node).entrySet()) {
            if (community[e.getKey()] == communityId) {
                connection += e.getValue();
            }
        }

        return connection - resolution * size;
    }

    private List<CoarseEdge> buildCoarseGraph(
            List<List<Integer>> communities,
            List<Edge> proteinEdges,
            double[][][] distances) {

        int[] membership = new int[distances[0].length];

        for (int c = 0; c < communities.size(); c++) {
            for (int v : communities.get(c)) {
                membership[v] = c;
            }
        }

        Map<String, Double> varianceByPair = new LinkedHashMap<>();

        for (Edge e : proteinEdges) {
            int a = membership[e.u];
            int b = membership[e.v];

            if (a == b) continue;

            int u = Math.min(a, b);
            int v = Math.max(a, b);
            String key = u + ":" + v;

            varianceByPair.putIfAbsent(key,
                    communityPairVariance(communities.get(u),
                            communities.get(v), distances));
        }

        List<CoarseEdge> result = new ArrayList<>();

        for (Map.Entry<String, Double> entry : varianceByPair.entrySet()) {
            String[] p = entry.getKey().split(":");
            result.add(new CoarseEdge(
                    Integer.parseInt(p[0]),
                    Integer.parseInt(p[1]),
                    entry.getValue()));
        }

        return result;
    }

    private double communityPairVariance(
            List<Integer> c1,
            List<Integer> c2,
            double[][][] distances) {

        int m = distances.length;
        double sum = 0.0;
        int count = 0;

        for (int a : c1) {
            for (int b : c2) {

                double mean = 0.0;

                for (int k = 0; k < m; k++) {
                    mean += distances[k][a][b];
                }

                mean /= m;

                for (int k = 0; k < m; k++) {
                    double diff = distances[k][a][b] - mean;
                    sum += diff * diff;
                }

                count++;
            }
        }

        if (count == 0 || m <= 1) return 0.0;

        return sum / (count * (m - 1));
    }

    private List<LineEdge> buildLineGraph(
            List<CoarseEdge> coarseEdges,
            List<LineNode> lineNodes,
            List<List<Integer>> communities,
            double[][][] distances) {

        List<LineEdge> result = new ArrayList<>();

        for (int i = 0; i < coarseEdges.size(); i++) {
            CoarseEdge e1 = coarseEdges.get(i);

            for (int j = i + 1; j < coarseEdges.size(); j++) {
                CoarseEdge e2 = coarseEdges.get(j);

                int mid = commonVertex(e1, e2);
                if (mid < 0) continue;

                int[] ends = otherEnds(e1, e2, mid);
                boolean connected = hasCoarseEdge(coarseEdges, ends[0], ends[1]);

                if (!connected) {
                    double variance = communityPairVariance(
                            communities.get(ends[0]),
                            communities.get(ends[1]),
                            distances);

                    result.add(new LineEdge(
                            i, j, mid, ends[0], ends[1],
                            false, variance));
                }
            }
        }

        return result;
    }

    private int commonVertex(CoarseEdge a, CoarseEdge b) {
        if (a.u == b.u || a.u == b.v) return a.u;
        if (a.v == b.u || a.v == b.v) return a.v;
        return -1;
    }

    private int[] otherEnds(CoarseEdge a, CoarseEdge b, int common) {
        int x = a.u == common ? a.v : a.u;
        int y = b.u == common ? b.v : b.u;
        return new int[]{Math.min(x, y), Math.max(x, y)};
    }

    private boolean hasCoarseEdge(
            List<CoarseEdge> edges, int u, int v) {

        int a = Math.min(u, v);
        int b = Math.max(u, v);

        for (CoarseEdge e : edges) {
            if (e.u == a && e.v == b) return true;
        }
        return false;
    }

    private boolean[] expandOutliers(double[] values, boolean[] initial, double fraction) {
        boolean[] result = Arrays.copyOf(initial, initial.length);
        List<Double> normal = new ArrayList<>();
        for (int i = 0; i < values.length; i++) {
            if (!initial[i]) normal.add(values[i]);
        }
        normal.sort(Collections.reverseOrder());
        int extra = (int) Math.floor(normal.size() * fraction);
        if (extra > 0 && !normal.isEmpty()) {
            double threshold = normal.get(Math.min(extra - 1, normal.size() - 1));
            for (int i = 0; i < values.length; i++) {
                if (!initial[i] && values[i] >= threshold) result[i] = true;
            }
        }
        return result;
    }

    private boolean[] detectOutliers(double[] values, double threshold) {
        boolean[] result = new boolean[values.length];

        if (values.length == 0) return result;

        double median = median(values.clone());

        double[] deviations = new double[values.length];
        for (int i = 0; i < values.length; i++) {
            deviations[i] = Math.abs(values[i] - median);
        }

        double mad = median(deviations);

        if (mad < 1e-12) {
            return result;
        }

        for (int i = 0; i < values.length; i++) {
            double modifiedZ = 0.6745 * Math.abs(values[i] - median) / mad;
            result[i] = modifiedZ > threshold;
        }

        return result;
    }

    private double median(double[] values) {
        if (values.length == 0) return 0.0;
        Arrays.sort(values);
        int mid = values.length / 2;
        if (values.length % 2 == 0) {
            return (values[mid - 1] + values[mid]) / 2.0;
        }
        return values[mid];
    }

    private int countTrue(boolean[] a) {
        int count = 0;
        for (boolean v : a) if (v) count++;
        return count;
    }

    private int[] inferLabels(
            List<LineNode> nodes,
            List<LineEdge> edges,
            boolean[] vertexOutliers,
            boolean[] edgeOutliers,
            int iterations) {

        int n = nodes.size();
        int[] labels = new int[n];

        for (int i = 0; i < n; i++) {
            labels[i] = vertexOutliers[i] ? -1 : 1;
        }

        for (int iteration = 0; iteration < iterations; iteration++) {
            boolean changed = false;

            for (int v = 0; v < n; v++) {
                int old = labels[v];

                double scorePlus = localScore(v, 1, labels,
                        nodes, edges, vertexOutliers, edgeOutliers);
                double scoreMinus = localScore(v, -1, labels,
                        nodes, edges, vertexOutliers, edgeOutliers);

                labels[v] = scorePlus >= scoreMinus ? 1 : -1;

                if (labels[v] != old) changed = true;
            }

            if (!changed) break;
        }

        return labels;
    }

    private double localScore(
            int node,
            int candidate,
            int[] labels,
            List<LineNode> nodes,
            List<LineEdge> edges,
            boolean[] vertexOutliers,
            boolean[] edgeOutliers) {

        double score = vertexOutliers[node]
                ? (candidate == -1 ? 1 : -1)
                : (candidate == 1 ? 1 : -1);

        for (int i = 0; i < edges.size(); i++) {
            LineEdge e = edges.get(i);
            int other = -1;

            if (e.a == node) other = e.b;
            else if (e.b == node) other = e.a;

            if (other < 0) continue;

            score += pairScore(
                    candidate, labels[other],
                    e, vertexOutliers[e.a],
                    vertexOutliers[e.b],
                    edgeOutliers[i],
                    nodes.get(e.a).variance,
                    nodes.get(e.b).variance
            );
        }

        return score;
    }

    private double pairScore(
            int y1,
            int y2,
            LineEdge e,
            boolean gamma1Outlier,
            boolean gamma2Outlier,
            boolean gammaEdgeOutlier,
            double xi1,
            double xi2) {

        int g1 = gamma1Outlier ? -1 : 1;
        int g2 = gamma2Outlier ? -1 : 1;
        int ge = gammaEdgeOutlier ? -1 : 1;

        // Case 1: two or more gamma values are -1.
        int negatives = (g1 == -1 ? 1 : 0)
                + (g2 == -1 ? 1 : 0)
                + (ge == -1 ? 1 : 0);

        if (negatives >= 2) {
            return (y1 * g1 + y2 * g2 == 2) ? 1 : -1;
        }

        // Case 2 from the paper.
        if (g1 == 1 && g2 == 1) {
            if (ge == -1) {
                if (y1 == -1 && y2 == 1 && xi1 > xi2) return 1;
                if (y1 == 1 && y2 == -1 && xi1 < xi2) return 1;
                if (y1 != y2 && Math.abs(xi1 - xi2) < 1e-12) return 0;
                return -1;
            }

            if (ge == 1) {
                return (y1 == 1 && y2 == 1) ? 1 : -1;
            }
        }

        return 0;
    }

    private List<List<Integer>> splitCoarseGraph(
            int n,
            List<CoarseEdge> edges,
            int[] lineLabels) {

        List<List<Integer>> adjacency = new ArrayList<>();
        for (int i = 0; i < n; i++) adjacency.add(new ArrayList<>());

        for (int i = 0; i < edges.size(); i++) {
            // A line-graph vertex corresponds to one coarse edge.
            // label -1 removes that coarse edge.
            if (lineLabels[i] == -1) continue;

            CoarseEdge e = edges.get(i);
            adjacency.get(e.u).add(e.v);
            adjacency.get(e.v).add(e.u);
        }

        boolean[] visited = new boolean[n];
        List<List<Integer>> groups = new ArrayList<>();

        for (int start = 0; start < n; start++) {
            if (visited[start]) continue;

            List<Integer> group = new ArrayList<>();
            ArrayDeque<Integer> queue = new ArrayDeque<>();
            queue.add(start);
            visited[start] = true;

            while (!queue.isEmpty()) {
                int u = queue.removeFirst();
                group.add(u);

                for (int v : adjacency.get(u)) {
                    if (!visited[v]) {
                        visited[v] = true;
                        queue.addLast(v);
                    }
                }
            }

            groups.add(group);
        }

        return groups;
    }

    private List<List<Integer>> refineByRmsd(
            List<List<Integer>> groups,
            List<List<Integer>> communities,
            List<List<Residue>> aligned,
            double threshold) {

        List<List<Integer>> result = new ArrayList<>();

        for (List<Integer> group : groups) {
            List<Integer> residueIds = expandCommunityGroup(group, communities);
            double value = rmsd(aligned, residueIds);

            if (value <= threshold || group.size() <= 1) {
                result.add(new ArrayList<>(group));
            } else {
                // Keep the coarse partition. The original implementation
                // recursively invokes the line-graph inference; this
                // no-dependency version reports the high-RMSD group rather
                // than inventing a new domain boundary.
                result.add(new ArrayList<>(group));
            }
        }

        return result;
    }

    private List<List<Integer>> mergeDomains(
            List<List<Integer>> groups,
            List<List<Integer>> communities,
            List<List<Residue>> aligned,
            double threshold) {

        List<List<Integer>> current = new ArrayList<>();
        for (List<Integer> g : groups) {
            current.add(new ArrayList<>(g));
        }

        boolean merged = true;

        while (merged && current.size() > 1) {
            merged = false;

            double bestValue = -Double.MAX_VALUE;
            int bestA = -1;
            int bestB = -1;

            for (int i = 0; i < current.size(); i++) {
                for (int j = i + 1; j < current.size(); j++) {
                    List<Integer> a = expandCommunityGroup(
                            current.get(i), communities);
                    List<Integer> b = expandCommunityGroup(
                            current.get(j), communities);

                    double ra = rmsd(aligned, a);
                    double rb = rmsd(aligned, b);

                    List<Integer> both = new ArrayList<>(a);
                    both.addAll(b);

                    double rab = rmsd(aligned, both);

                    if (rab < 1e-12) continue;

                    double value = (ra + rb) / rab;

                    if (value > bestValue) {
                        bestValue = value;
                        bestA = i;
                        bestB = j;
                    }
                }
            }

            if (bestA >= 0 && bestValue > threshold) {
                List<Integer> mergedGroup = new ArrayList<>(current.get(bestA));
                mergedGroup.addAll(current.get(bestB));

                current.remove(bestB);
                current.remove(bestA);
                current.add(mergedGroup);
                merged = true;
            }
        }

        return current;
    }

    private List<Integer> expandCommunityGroup(
            List<Integer> communityIds,
            List<List<Integer>> communities) {

        List<Integer> residues = new ArrayList<>();

        for (int community : communityIds) {
            residues.addAll(communities.get(community));
        }

        Collections.sort(residues);
        return residues;
    }

    private double rmsd(
            List<List<Residue>> aligned,
            List<Integer> residueIds) {

        if (residueIds.size() < 2 || aligned.size() < 2) {
            return 0.0;
        }

        double total = 0.0;
        int comparisons = 0;

        // Pairwise RMSD across conformations without superposition.
        // The original implementation uses a structure/RMSD routine;
        // this keeps the calculation self-contained.
        for (int a = 0; a < aligned.size() - 1; a++) {
            for (int b = a + 1; b < aligned.size(); b++) {

                double sum = 0.0;

                for (int id : residueIds) {
                    Residue r1 = aligned.get(a).get(id);
                    Residue r2 = aligned.get(b).get(id);

                    double dx = r1.x - r2.x;
                    double dy = r1.y - r2.y;
                    double dz = r1.z - r2.z;

                    sum += dx * dx + dy * dy + dz * dz;
                }

                double value = Math.sqrt(sum / residueIds.size());
                total += value;
                comparisons++;
            }
        }

        return comparisons == 0 ? 0.0 : total / comparisons;
    }

    private double distance(Residue a, Residue b) {
        double dx = a.x - b.x;
        double dy = a.y - b.y;
        double dz = a.z - b.z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
}
