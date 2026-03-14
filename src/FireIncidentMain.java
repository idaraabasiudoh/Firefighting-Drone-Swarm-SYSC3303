import java.io.File;
import java.net.InetAddress;

/**
 * Standalone main program for the Fire Incident Subsystem.
 * Communicates with the Scheduler via UDP only.
 *
 * Usage: java FireIncidentMain [inputFile] [schedulerHost]
 */
public class FireIncidentMain {
    public static void main(String[] args) {
        String inputFile = resolvePath("fire_events.csv");
        String schedulerHost = "localhost";

        if (args.length > 0) inputFile = args[0];
        if (args.length > 1) schedulerHost = args[1];

        try {
            InetAddress schedulerAddress = InetAddress.getByName(schedulerHost);
            FireIncidentSubsystem fireSubsystem = new FireIncidentSubsystem(inputFile, schedulerAddress);

            System.out.println("[FireIncidentMain] Starting Fire Incident Subsystem...");
            System.out.println("[FireIncidentMain] Input file: " + inputFile);
            System.out.println("[FireIncidentMain] Scheduler address: " + schedulerHost);

            Thread fireThread = new Thread(fireSubsystem, "Fire-Subsystem-Thread");
            fireThread.start();
            fireThread.join();
        } catch (Exception e) {
            System.err.println("[FireIncidentMain] Error: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("[FireIncidentMain] Shutdown complete.");
    }

    private static String resolvePath(String filename) {
        if (new File(filename).exists()) return filename;
        if (new File("src/" + filename).exists()) return "src/" + filename;
        return filename;
    }
}
