
// If you want ONE run button for everything, use this Main.
// Otherwise you can keep separate GuiMain and Main.
//

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        System.out.println("Firefighting Drone Swarm System - Iteration 2 (Single Drone)");

        String inputFile = "src/fire_events.csv";
        int numberOfDrones = 1;

        if (args.length > 0) inputFile = args[0];
        if (args.length > 1) numberOfDrones = Integer.parseInt(args[1]);

        // Start GUI on Swing thread
        SwingUtilities.invokeLater(() -> {
            try { GuiMain.main(new String[0]); }
            catch (Exception e) { e.printStackTrace(); }
        });

        Scheduler scheduler = new Scheduler();
        FireIncidentSubsystem fireSubsystem = new FireIncidentSubsystem(scheduler, inputFile);

        Thread schedulerThread = new Thread(scheduler, "Scheduler-Thread");
        Thread fireThread = new Thread(fireSubsystem, "Fire-Subsystem-Thread");

        DroneSubsystem drone = new DroneSubsystem(1, scheduler);
        Thread droneThread = new Thread(drone, "Drone-1-Thread");

        schedulerThread.start();

        try { Thread.sleep(100); } catch (InterruptedException ignored) {}

        fireThread.start();
        droneThread.start();

        try {
            fireThread.join();
            Thread.sleep(1500);

            System.out.println("\nShutting down...");
            scheduler.shutdown();
            drone.shutdown();

            schedulerThread.join(2000);
            droneThread.join(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.println("Shutdown complete.");
    }
}