import javax.swing.*;
import java.io.File;
import java.util.List;

/**
 * Standalone main program for the Scheduler.
 * Communicates with Fire Incident Subsystem and Drones via UDP only.
 * Also launches the GUI.
 *
 * Usage: java SchedulerMain [zoneFile]
 */
public class SchedulerMain {
    public static void main(String[] args) {
        String zoneFile = resolvePath("sample_zone_file.csv");

        if (args.length > 0) zoneFile = args[0];

        try {
            List<Zone> zones = ZoneParser.loadZones(zoneFile);

            System.out.println("[SchedulerMain] Starting Scheduler...");
            System.out.println("[SchedulerMain] Loaded " + zones.size() + " zones.");
            System.out.println("[SchedulerMain] Fire listen port: " + UDPHelper.FIRE_TO_SCHEDULER_PORT);
            System.out.println("[SchedulerMain] Drone listen port: " + UDPHelper.DRONE_TO_SCHEDULER_PORT);

            // Start GUI on Swing thread
            final List<Zone> guiZones = zones;
            SwingUtilities.invokeLater(() -> {
                try {
                    JFrame frame = new JFrame("Firefighting Drone Swarm - Iteration 3");
                    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

                    MapPanel panel = new MapPanel(guiZones);
                    frame.setContentPane(panel);

                    frame.pack();
                    frame.setLocationRelativeTo(null);
                    frame.setVisible(true);

                    panel.startAutoRepaint();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            Scheduler scheduler = new Scheduler(zones);
            Thread schedulerThread = new Thread(scheduler, "Scheduler-Thread");
            schedulerThread.start();
            schedulerThread.join();

        } catch (Exception e) {
            System.err.println("[SchedulerMain] Error: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("[SchedulerMain] Shutdown complete.");
    }

    private static String resolvePath(String filename) {
        if (new File(filename).exists()) return filename;
        if (new File("src/" + filename).exists()) return "src/" + filename;
        return filename;
    }
}
