package ar.edu.unse.siga.common;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class CsvExporter {
    private CsvExporter() {}

    public static void write(Path path, List<String[]> rows) throws IOException {
        try (BufferedWriter w = Files.newBufferedWriter(path)) {
            for (String[] r : rows) {
                String line = String.join(",", escape(r));
                w.write(line);
                w.newLine();
            }
        }
    }
    private static String[] escape(String[] r) {
        String[] out = new String[r.length];
        for (int i = 0; i < r.length; i++) {
            String s = r[i] == null ? "" : r[i];
            if (s.contains(",") || s.contains("\"")) {
                s = "\"" + s.replace("\"","\"\"") + "\"";
            }
            out[i] = s;
        }
        return out;
    }
}

