import javax.swing.*;
import java.util.List;

public class GuiMain {
    public static void main(String[] args) throws Exception {
        List<Zone> zones = ZoneParser.loadZones("src/sample_zone_file.csv");

        // Example: static “fires” list for now (you can also read from event CSV)
        List<FireEvent> fires = List.of(
                new FireEvent("14:03:15", 1, FireEvent.EventType.FIRE_DETECTED, FireEvent.Severity.HIGH),
                new FireEvent("14:10:00", 3, FireEvent.EventType.FIRE_DETECTED, FireEvent.Severity.MODERATE)
        );

        // sanity check odd-by-odd requirement
        for (Zone z : zones) {
            if (!z.isOddByOdd()) {
                System.out.println("[WARN] Zone " + z.getId() + " is not odd-by-odd, center may not be perfect.");
            }
        }

        JFrame frame = new JFrame("Firefighting Drone Swarm - Iteration 1");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setContentPane(new MapPanel(zones, fires));
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
