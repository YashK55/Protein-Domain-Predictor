import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

public class Structure {

    String pdbId;
    char chain;
    ArrayList<Residue> residues = new ArrayList<>();

    public Structure(String pdbId, char chain) {
        this.pdbId = pdbId.toUpperCase();
        this.chain = chain;
    }

    public void loadPDB(String filePath) throws IOException {
        residues.clear();

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            boolean hasModelRecords = false;
            boolean firstModelSeen = false;
            boolean readingFirstModel = true;

            while ((line = reader.readLine()) != null) {
                if (line.startsWith("MODEL")) {
                    hasModelRecords = true;
                    if (firstModelSeen) {
                        readingFirstModel = false;
                    } else {
                        firstModelSeen = true;
                        readingFirstModel = true;
                    }
                    continue;
                }

                if (line.startsWith("ENDMDL") && hasModelRecords && firstModelSeen) {
                    readingFirstModel = false;
                    continue;
                }

                if (hasModelRecords && !readingFirstModel) {
                    continue;
                }

                parseAtomLine(line);
            }
        }

        if (residues.isEmpty()) {
            throw new IOException("No C-alpha residues found for chain " + chain + " in " + filePath);
        }
    }

    // Modified residues recorded as HETATM that still occupy a normal backbone
    // position and should be treated as part of the chain. MSE (selenomethionine)
    // is by far the most common case, arising from SeMet phasing.
    private static final java.util.Set<String> BACKBONE_HETERO_RESIDUES =
            java.util.Set.of("MSE");

    private void parseAtomLine(String line) {
        boolean isAtom = line.startsWith("ATOM");
        boolean isHetAtom = line.startsWith("HETATM");

        if ((!isAtom && !isHetAtom) || line.length() < 54) {
            return;
        }

        String atomName = safeSubstring(line, 12, 16).trim();
        char altLoc = line.charAt(16);
        char lineChain = line.charAt(21);

        if (!atomName.equals("CA") || lineChain != chain) {
            return;
        }

        // Keep only the primary conformer: blank or 'A'. Without this, a residue
        // with alternate CA locations (altLoc B, C, ...) would be added twice,
        // silently desynchronizing every aligned index downstream.
        if (altLoc != ' ' && altLoc != 'A') {
            return;
        }

        String residueName = safeSubstring(line, 17, 20).trim();

        // HETATM covers everything from waters to metal ions (which can even be
        // named "CA" for calcium) - only accept it for known backbone-equivalent
        // modified residues, never atom name alone.
        if (isHetAtom && !BACKBONE_HETERO_RESIDUES.contains(residueName)) {
            return;
        }

        try {
            int number = Integer.parseInt(safeSubstring(line, 22, 26).trim());
            char insertionCode = line.charAt(26);

            double x = Double.parseDouble(safeSubstring(line, 30, 38).trim());
            double y = Double.parseDouble(safeSubstring(line, 38, 46).trim());
            double z = Double.parseDouble(safeSubstring(line, 46, 54).trim());

            residues.add(new Residue(number, insertionCode, residueName, chain, x, y, z));
        } catch (NumberFormatException ignored) {
            // Ignore malformed ATOM/HETATM records.
        }
    }

    private String safeSubstring(String s, int start, int end) {
        return s.substring(start, Math.min(end, s.length()));
    }

    public static Structure fromPdbId(String pdbId, char chain, Path downloadDirectory)
            throws IOException, InterruptedException {

        Files.createDirectories(downloadDirectory);

        String id = pdbId.toUpperCase();
        Path target = downloadDirectory.resolve(id + ".pDB".toLowerCase());

        if (!Files.exists(target)) {
            String url = "https://files.rcsb.org/download/" + id + ".pdb";

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response =
                    client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new IOException("RCSB download failed for " + id
                        + " (HTTP " + response.statusCode() + ")");
            }

            Files.writeString(target, response.body());
        }

        Structure structure = new Structure(id, chain);
        structure.loadPDB(target.toString());
        return structure;
    }
}
