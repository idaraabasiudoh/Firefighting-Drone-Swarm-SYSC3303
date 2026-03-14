import java.net.InetAddress;

/**
 * Standalone main program for a Drone Subsystem.
 * Each drone runs as a separate process and communicates with the Scheduler via UDP.
 *
 * Usage: java DroneMain <droneId> [agentCapacity] [schedulerHost]
 */
public class DroneMain {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: java DroneMain <droneId> [agentCapacity] [schedulerHost]");
            System.err.println("  droneId: unique integer ID for this drone (e.g., 1, 2, 3)");
            System.err.println("  agentCapacity: agent capacity in liters (default: 30.0)");
            System.err.println("  schedulerHost: hostname or IP of scheduler (default: localhost)");
            System.exit(1);
        }

        int droneId = Integer.parseInt(args[0]);
        double agentCapacity = 30.0;
        String schedulerHost = "localhost";

        if (args.length > 1) agentCapacity = Double.parseDouble(args[1]);
        if (args.length > 2) schedulerHost = args[2];

        try {
            InetAddress schedulerAddress = InetAddress.getByName(schedulerHost);
            DroneSubsystem drone = new DroneSubsystem(droneId, schedulerAddress, agentCapacity);

            System.out.println("[DroneMain] Starting Drone " + droneId);
            System.out.println("[DroneMain] Agent capacity: " + agentCapacity + "L");
            System.out.println("[DroneMain] Scheduler address: " + schedulerHost);
            System.out.println("[DroneMain] Listen port: " + UDPHelper.getDroneListenPort(droneId));

            Thread droneThread = new Thread(drone, "Drone-" + droneId + "-Thread");
            droneThread.start();
            droneThread.join();
        } catch (Exception e) {
            System.err.println("[DroneMain] Error: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("[DroneMain] Drone " + droneId + " shutdown complete.");
    }
}
