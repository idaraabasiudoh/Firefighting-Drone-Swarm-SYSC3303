// Main.java  (Iteration 4: convenience launcher — starts all 3 subsystems in one JVM via UDP, fault handling)
// For production, run SchedulerMain, DroneMain, and FireIncidentMain as separate processes.

import drone.DroneSubsystem;
import fireincident.FireIncidentSubsystem;
import gui.MapPanel;
import gui.Zone;
import gui.ZoneParser;
import network.UDPHelper;
import scheduler.Scheduler;

import javax.swing.*;
import java.io.File;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        System.out.println("Firefighting Drone Swarm System - Iteration 4 (Multi-Drone, UDP, Fault Handling)");

        String inputFile = resolvePath("fire_events.csv");
        String zoneFile = resolvePath("sample_zone_file.csv");
        int numberOfDrones = 2; // default 2 drones

        if (args.length > 0) inputFile = args[0];
        if (args.length > 1) numberOfDrones = Integer.parseInt(args[1]);

        try {
            List<Zone> zones = ZoneParser.loadZones(zoneFile);
            InetAddress localhost = InetAddress.getByName("localhost");

            // Start GUI on Swing thread
            final List<Zone> guiZones = zones;
            SwingUtilities.invokeLater(() -> {
                try {
                    JFrame frame = new JFrame("Firefighting Drone Swarm - Iteration 4");
                    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                    MapPanel panel = new MapPanel(guiZones);
                    frame.setContentPane(panel);
                    frame.pack();
                    frame.setLocationRelativeTo(null);
                    frame.setVisible(true);
                    panel.startAutoRepaint();
                } catch (Exception e) { e.printStackTrace(); }
            });

            // Start Scheduler
            Scheduler scheduler = new Scheduler(zones);
            Thread schedulerThread = new Thread(scheduler, "Scheduler-Thread");
            schedulerThread.start();

            // Give scheduler time to bind ports
            Thread.sleep(500);

            // Start Drones
            List<DroneSubsystem> drones = new ArrayList<>();
            List<Thread> droneThreads = new ArrayList<>();
            for (int i = 1; i <= numberOfDrones; i++) {
                DroneSubsystem drone = new DroneSubsystem(i, localhost);
                drones.add(drone);
                Thread t = new Thread(drone, "Drone-" + i + "-Thread");
                droneThreads.add(t);
                t.start();
                Thread.sleep(200); // stagger registration
            }

            // Give drones time to register
            Thread.sleep(500);

            // Start Fire Incident Subsystem
            FireIncidentSubsystem fireSubsystem = new FireIncidentSubsystem(inputFile, localhost);
            Thread fireThread = new Thread(fireSubsystem, "Fire-Subsystem-Thread");
            fireThread.start();

            // Wait for fire subsystem to finish sending + receiving confirmations
            fireThread.join();
            Thread.sleep(15000); // allow remaining tasks to complete (drones may need to refill)

            System.out.println("\nShutting down...");
            scheduler.shutdown();

            for (DroneSubsystem drone : drones) {
                drone.shutdown();
            }

            schedulerThread.join(3000);
            for (Thread t : droneThreads) {
                t.join(2000);
            }

        } catch (Exception e) {
            System.err.println("[Main] Error: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("Shutdown complete.");
    }

    private static String resolvePath(String filename) {
        if (new File(filename).exists()) return filename;
        if (new File("src/" + filename).exists()) return "src/" + filename;
        return filename;
    }
}