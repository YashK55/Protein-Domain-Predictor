# Protein Domain Predictor

A high-performance, dual-themed (Light/Dark) Java Swing desktop application implementing the multi-conformation graph-based rigid domain decomposition workflow described by **Dang et al. (2021)**.

The tool identifies rigid structural domains and hinge boundaries by modeling proteins as contact graphs and using Louvain community partitioning, Line Graph variance mapping, Median Absolute Deviation (MAD) outlier detection, and Viterbi optimization.

---

## 💾 Download

Download the latest Windows installer or portable ZIP from the **Releases** page:

[![Download Latest Release](https://img.shields.io/github/v/release/YashK55/Protein-Domain-Predictor?style=for-the-badge&logo=github&label=Download%20Latest)](https://github.com/YashK55/Protein-Domain-Predictor/releases/latest)

---

## 🚀 Key Features

*   **Dual Theme UI (Light/Dark Mode)**: Custom anti-aliased rounded widgets (`RoundedButton` and `RoundedTextField`) and responsive panels supporting seamless light and dark mode switching with high-contrast visualization canvases.
*   **Step-wise Chain Auto-Detection**: Enter PDB IDs or select local files, and the app will scan the structures, detect all common chain identifiers, and prompt you to select specific chains before running analysis.
*   **Interactive 2D Network Canvas**: Interactive circular layouts for pipeline stages. Hovering over nodes highlights edge paths and displays residue indexes, group memberships, and degree connectivity in real-time.
*   **Vector Icons Integration**: Native vector-drawn components (`ThemeIcon`, `ArrowIcon`, `StatusIcon`) to provide seamless resolution scalability without emoji dependencies.
*   **Algorithmic Optimization**: Louvain partitioning optimized from $O(N)$ community size scans to $O(1)$ size caching, delivering massive performance boosts.
*   **Zero External Dependencies**: Written in pure Java Swing/AWT (no external JARs needed).

---

## 📁 Repository Structure

```
ProteinDomainPredictor/
├── .github/workflows/          # CI/CD pipeline definitions
│   ├── ci.yml                  # Build & compile on every push/PR
│   └── release.yml             # Automated releases on version tags
├── data/                       # Local PDB cache directory
├── src/                        # Java source directory
│   ├── AboutPanel.java         # About & Changelog panel layout (GridBagLayout)
│   ├── AnalysisPanel.java      # Guided Analysis Panel
│   ├── Analyzer.java           # Math core: Contact graph, Louvain, MAD, Viterbi
│   ├── AppTheme.java           # Color modes, vector icons, & custom widgets
│   ├── ChainSelectionPanel.java# Inter-structure chain selection view
│   ├── GraphPanel.java         # Interactive 2D circular network graph renderer
│   ├── HomePanel.java          # Central home screen panel
│   ├── Main.java               # App entry point, frame coordinator, & theme switcher
│   ├── PipelinePanel.java      # Left pipeline step navigation panel
│   ├── ProcessingPanel.java    # Progress loader panel with custom vector status icons
│   ├── Residue.java            # C-alpha residue coordinates data model
│   └── Structure.java          # Download manager and standard PDB coordinate parser
├── Logo.ico                    # Windows installer icon
├── Logo.png                    # High-quality app logo
├── package.bat                 # Windows packaging script
├── README.md                   # Core project documentation
└── walkthrough.md              # Detailed final changes walkthrough
```

---

## 🛠️ Compilation & Execution

Since the project is built in pure Java without external dependencies, it compiles and runs out-of-the-box on any computer with Java Development Kit (JDK) installed.

### Standard Command Line

1. Open your terminal or PowerShell inside the `ProteinDomainPredictor` directory.
2. Compile the files:
   ```bash
   javac -d bin src/*.java
   ```
3. Run the application:
   ```bash
   java -cp bin Main
   ```

### Running in Eclipse
1. Import the directory into Eclipse as a **Java Project**.
2. Right-click [`src/Main.java`](file:///c:/Users/Yash%20katekhaye/eclipse-workspace/ProteinDomainPredictor/src/Main.java) and select **Run As** > **Java Application**.

---

## 🧬 Algorithm Pipeline

The core analysis follows the multi-structure rigid domain pipeline:

```mermaid
graph TD
    A[1. Extract CA coordinates] --> B[2. Needleman-Wunsch Alignment]
    B --> C[3. intra-structure Distance Matrices]
    C --> D[4. Multi-Structure Contact Graph - 7.5 Å Cutoff]
    D --> E[5. Louvain Community Partitioning]
    E --> F[6. Modified Line Graph Transformation]
    F --> G[7. MAD Hinge Outlier Detection]
    G --> H[8. Boundary Optimization via Viterbi Updates]
    H --> I[9. Rigidity Check & equivalent Domain Merging]
```

1. **Alignment**: Sequences are aligned to map structural residue correspondences across conformations.
2. **Contact Graph**: Builds contacts with a 7.5 Å cutoff threshold, weighted by $exp(-variance)$ of distances.
3. **Partitioning**: Coarse-grains residues into communities using Louvain optimizations.
4. **Hinges**: Maps edges to nodes in a Line Graph and uses MAD outlier detection to flag high-variance hinge coordinates.
5. **Prediction**: Predicts boundary coordinates using local Viterbi update calculations, reporting final domains.

---

## 📝 Important Scientific Note

The published Dang et al. implementation is Python-based and depends on igraph, Louvain-igraph, and an external Java Viterbi executable JAR. 

This Java codebase is a compact, **dependency-free** reimplementation. Its graph calculations, distance variance modeling, line graphs, and outlier rules sets follow the reference closely. The pure-Java local label boundary optimizer is tailored to run locally without external executables, and provides equivalent structural domain boundary segmentation.

---

## 📚 References

*   **Dang TKL, Nguyen T, Habeck M, Gültas M, Waack S.**  
    *A graph-based algorithm for detecting rigid domains in protein structures.*  
    BMC Bioinformatics. 2021;22:66.  
    DOI: [10.1186/s12859-021-03966-3](https://doi.org/10.1186/s12859-021-03966-3)
