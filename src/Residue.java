public class Residue {

    int number;
    char insertionCode;
    String name;
    char chain;

    double x;
    double y;
    double z;

    public Residue(int number, char insertionCode, String name, char chain,
                   double x, double y, double z) {
        this.number = number;
        this.insertionCode = insertionCode;
        this.name = name;
        this.chain = chain;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public char oneLetter() {
        switch (name) {
            case "ALA": return 'A';
            case "ARG": return 'R';
            case "ASN": return 'N';
            case "ASP": return 'D';
            case "CYS": return 'C';
            case "GLN": return 'Q';
            case "GLU": return 'E';
            case "GLY": return 'G';
            case "HIS": return 'H';
            case "ILE": return 'I';
            case "LEU": return 'L';
            case "LYS": return 'K';
            case "MET": return 'M';
            case "MSE": return 'M'; // selenomethionine, chemically equivalent for alignment
            case "PHE": return 'F';
            case "PRO": return 'P';
            case "SER": return 'S';
            case "THR": return 'T';
            case "TRP": return 'W';
            case "TYR": return 'Y';
            case "VAL": return 'V';
            default: return 'X';
        }
    }

    @Override
    public String toString() {
        return number + (insertionCode == ' ' ? "" : String.valueOf(insertionCode))
                + " " + name + " " + chain
                + " (" + x + ", " + y + ", " + z + ")";
    }
}
