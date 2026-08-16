# ProteinDomainAnalyzer

A compact Java/Swing implementation of the graph-based rigid-domain workflow described by Dang et al. (2021).

## Current files

src/
- Main.java
- Residue.java
- Structure.java
- Analyzer.java

data/
- put local PDB files here

## Run

Open the project in Eclipse as a Java project and run `Main.java`.

### PDB IDs

Enter:

4AKE_A,1AKE_A

and click **Analyze PDB IDs**.

The program downloads PDB files from RCSB PDB into `data/`.

### Local PDB files

Click **Analyze PDB Files** and select two or more structures.

## Pipeline

1. Read C-alpha atoms
2. Sequence-align structures to a common reference
3. Build distance matrices
4. Build the multi-structure protein graph
5. Apply the 7.5 Å maximum-distance cutoff
6. Weight edges by exp(-variance)
7. Louvain-style coarse graining toward 20 communities
8. Construct the modified line graph
9. Detect high-variance outliers using MAD / modified z-score
10. Infer binary line-graph labels with a generalized-Viterbi-style local optimizer
11. Remove boundary edges
12. Calculate domain RMSD
13. Apply the merging heuristic
14. Report final domains

## Important scientific note

The published implementation is Python-based and uses igraph/louvain plus an external Java Viterbi JAR. The Java version here is a compact, dependency-free reimplementation of the published workflow. Its graph construction, variance feature, modified line graph, outlier rule, and scoring logic follow the paper/source closely, but the pure-Java local label optimizer is not guaranteed to produce bit-for-bit identical segmentation to the authors' external Viterbi JAR.

For exact reproduction of the authors' numerical output, the external Viterbi component and the original implementation details should be ported as a separate validation step.

## Reference

Dang TKL, Nguyen T, Habeck M, Gültas M, Waack S.
A graph-based algorithm for detecting rigid domains in protein structures.
BMC Bioinformatics. 2021;22:66.
DOI: 10.1186/s12859-021-03966-3.
