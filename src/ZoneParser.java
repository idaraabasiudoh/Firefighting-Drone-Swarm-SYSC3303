import java.io.*;
import java.util.*;

public class ZoneParser {
    public static List<Zone> loadZones(String path) throws IOException {
        List<Zone> zones = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.toLowerCase().contains("zone") || line.trim().isEmpty()) continue;

                // Zone ID,Zone Start,Zone End
                // 1,(0;0),(700;600)
                String[] p = line.split(",", 3);
                int id = Integer.parseInt(p[0].trim());

                int[] start = parsePoint(p[1].trim());
                int[] end   = parsePoint(p[2].trim());

                zones.add(new Zone(id, start[0], start[1], end[0], end[1]));
            }
        }
        return zones;
    }

    private static int[] parsePoint(String s) {
        // "(x;y)"
        s = s.replace("(", "").replace(")", "");
        String[] xy = s.split(";");
        return new int[] { Integer.parseInt(xy[0].trim()), Integer.parseInt(xy[1].trim()) };
    }
}
