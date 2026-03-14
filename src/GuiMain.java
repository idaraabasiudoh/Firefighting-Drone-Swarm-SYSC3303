// GuiMain.java  (UPDATED: start GUI + auto repaint)
// REPLACE your GuiMain with this version
import javax.swing.*;
import java.io.File;
import java.util.List;

public class GuiMain {
    public static void main(String[] args) throws Exception {
        List<Zone> zones = ZoneParser.loadZones(resolvePath("sample_zone_file.csv"));

        JFrame frame = new JFrame("Firefighting Drone Swarm - Iteration 3");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        MapPanel panel = new MapPanel(zones);
        frame.setContentPane(panel);

        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        panel.startAutoRepaint(); // refresh every 200ms so you see state/count updates
    }

    private static String resolvePath(String filename) {
        if (new File(filename).exists()) return filename;
        if (new File("src/" + filename).exists()) return "src/" + filename;
        return filename;
    }
}