// GuiMain.java  (UPDATED: start GUI + auto repaint)
// REPLACE your GuiMain with this version
import javax.swing.*;
import java.util.List;

public class GuiMain {
    public static void main(String[] args) throws Exception {
        List<Zone> zones = ZoneParser.loadZones("src/sample_zone_file.csv");

        JFrame frame = new JFrame("Firefighting Drone Swarm - Iteration 2");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        MapPanel panel = new MapPanel(zones);
        frame.setContentPane(panel);

        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        panel.startAutoRepaint(); // refresh every 200ms so you see state/count updates
    }
}