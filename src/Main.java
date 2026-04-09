import drone.DroneSubsystem;
import fireincident.FireIncidentSubsystem;
import gui.MapPanel;
import gui.Zone;
import gui.ZoneParser;
import scheduler.Scheduler;

import javax.swing.*;
import java.io.File;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        System.out.println("Firefighting Drone Swarm System");

        String inputFile = resolvePath("fire_events.csv");
        String zoneFile = resolvePath("sample_zone_file.csv");
        int numberOfDrones = 2;

        if (args.length > 0) inputFile = args[0];
        if (args.length > 1) numberOfDrones = Integer.parseInt(args[1]);

        try {
            List<Zone> zones = ZoneParser.loadZones(zoneFile);
            InetAddress localhost = InetAddress.getByName("localhost");

            final List<Zone> guiZones = zones;
            SwingUtilities.invokeLater(() -> {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception ignored) {}

                JFrame frame = new JFrame("Firefighting Drone Swarm");
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setBackground(new java.awt.Color(10, 14, 26));
                MapPanel panel = new MapPanel(guiZones);
                frame.setContentPane(panel);
                frame.pack();
                frame.setMinimumSize(new java.awt.Dimension(900, 600));
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
                panel.startAutoRepaint();
            });

            Scheduler scheduler = new Scheduler(zones);
            Thread schedulerThread = new Thread(scheduler, "Scheduler-Thread");
            schedulerThread.start();

            Thread.sleep(500);

            List<DroneSubsystem> drones = new ArrayList<>();
            List<Thread> droneThreads = new ArrayList<>();
            for (int i = 1; i <= numberOfDrones; i++) {
                DroneSubsystem drone = new DroneSubsystem(i, localhost);
                drones.add(drone);
                Thread t = new Thread(drone, "Drone-" + i + "-Thread");
                droneThreads.add(t);
                t.start();
                Thread.sleep(200);
            }

            Thread.sleep(500);

            FireIncidentSubsystem fireSubsystem = new FireIncidentSubsystem(inputFile, localhost);
            Thread fireThread = new Thread(fireSubsystem, "Fire-Subsystem-Thread");
            fireThread.start();

            fireThread.join();
            Thread.sleep(15000);

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