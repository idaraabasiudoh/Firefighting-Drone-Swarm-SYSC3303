public class Main {
    public static void main(String[] args) {
        System.out.println("Firefighting Drone Swarm System - Iteration 1");

        String inputFile = "src/fire_events.csv";
        int numberOfDrones = 1;

        if (args.length > 0) {
            inputFile = args[0];
        }
        if (args.length > 1) {
            numberOfDrones = Integer.parseInt(args[1]);
        }

        System.out.println("  Input File: " + inputFile);
        System.out.println("  Number of Drones: " + numberOfDrones);
        System.out.println();

        Scheduler scheduler = new Scheduler();
        FireIncidentSubsystem fireSubsystem = new FireIncidentSubsystem(scheduler, inputFile);
        DroneSubsystem[] drones = new DroneSubsystem[numberOfDrones];

        Thread schedulerThread = new Thread(scheduler, "Scheduler-Thread");
        Thread fireThread = new Thread(fireSubsystem, "Fire-Subsystem-Thread");
        Thread[] droneThreads = new Thread[numberOfDrones];

        for (int i = 0; i < numberOfDrones; i++) {
            drones[i] = new DroneSubsystem(i + 1, scheduler);
            droneThreads[i] = new Thread(drones[i], "Drone-" + (i + 1) + "-Thread");
        }

        System.out.println("Starting all subsystems...\n");
        schedulerThread.start();

        // Small delay to ensure scheduler is ready
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            System.err.println("Error during sleep: " + e.getMessage());
        }

        fireThread.start();
        for (Thread droneThread : droneThreads) {
            droneThread.start();
        }

        try {
            fireThread.join();
            System.out.println("\nFire Incident Subsystem has finished processing events.");

            // Give drones time to complete their tasks
            Thread.sleep(2000);

            // Shutdown the system
            System.out.println("\nShutting down...");
            scheduler.shutdown();

            for (DroneSubsystem drone : drones) {
                drone.shutdown();
            }

            // Wait for all threads to complete
            schedulerThread.join(2000);
            for (Thread droneThread : droneThreads) {
                droneThread.join(2000);
            }

        } catch (InterruptedException e) {
            System.err.println("Error during shutdown: " + e.getMessage());
        }

        System.out.println("Shutdown complete.");
    }
}