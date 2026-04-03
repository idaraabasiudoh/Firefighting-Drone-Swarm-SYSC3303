package gui;

import drone.DroneState;
import drone.FaultType;
import fireincident.FireEvent;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class MapPanel extends JPanel {
    private final List<Zone> zones;
    private final int scale = 2;
    private BufferedImage droneImage;
    private BufferedImage fireImage;

    private static final Color COLOR_LOW = new Color(76, 175, 80);
    private static final Color COLOR_MODERATE = new Color(255, 152, 0);
    private static final Color COLOR_HIGH = new Color(244, 67, 54);

    private static final Color[] DRONE_COLORS = {
            new Color(33, 150, 243),
            new Color(156, 39, 176),
            new Color(0, 150, 136),
            new Color(255, 87, 34)
    };

    private static final Color COLOR_FAULT_STUCK = new Color(255, 235, 59);
    private static final Color COLOR_FAULT_NOZZLE = new Color(183, 28, 28);
    private static final Color COLOR_FAULT_SENSOR = new Color(255, 152, 0);
    private static final Color COLOR_OFFLINE = new Color(117, 117, 117);

    public MapPanel(List<Zone> zones) {
        this.zones = zones;
        setPreferredSize(new Dimension(900, 750));
        setDoubleBuffered(true);

        try {
            droneImage = ImageIO.read(new File("src/gui/drone.png"));
        } catch (IOException e) {
            System.out.println("Could not load drone image, using dots instead.");
            droneImage = null;
        }

        try {
            fireImage = ImageIO.read(new File("src/gui/fire.png"));
        } catch (IOException e) {
            System.out.println("Could not load fire image, using circles instead.");
            fireImage = null;
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Map<Integer, DroneState> droneStates = GuiModel.get().snapshotDroneStates();
        Map<Integer, Integer> droneAssignments = GuiModel.get().snapshotDroneAssignments();
        Map<Integer, FireEvent.Severity> activeFires = GuiModel.get().snapshotActiveFireZones();
        Map<Integer, FaultType> droneFaults = GuiModel.get().snapshotDroneFaults();
        Map<Integer, Point> dronePositions = GuiModel.get().snapshotDronePositions();

        int headerY = 20;
        g.setColor(Color.BLACK);
        g.setFont(new Font("SansSerif", Font.BOLD, 13));
        g.drawString("Firefighting Drone Swarm", 20, headerY);
        headerY += 20;

        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g.drawString("Active Fires: " + activeFires.size(), 20, headerY);
        headerY += 18;

        if (droneStates.isEmpty()) {
            g.drawString("No drones registered.", 20, headerY);
        } else {
            for (Map.Entry<Integer, DroneState> entry : droneStates.entrySet()) {
                int droneId = entry.getKey();
                DroneState state = entry.getValue();
                Integer assignedZone = droneAssignments.get(droneId);
                FaultType fault = droneFaults.getOrDefault(droneId, FaultType.NONE);

                Color labelColor = (fault != FaultType.NONE)
                        ? getFaultColor(fault, state)
                        : getDroneColor(droneId);

                g.setColor(labelColor);
                String label = "Drone " + droneId + ": " + state;
                if (fault != FaultType.NONE) {
                    label += " [FAULT: " + fault + "]";
                }
                if (assignedZone != null) {
                    label += " -> Zone " + assignedZone;
                }
                g.drawString(label, 20, headerY);
                headerY += 16;
            }
        }

        g.setColor(new Color(230, 230, 230));
        int offsetY = 120;
        for (int x = 0; x < getWidth(); x += 25) {
            g.drawLine(x, offsetY, x, getHeight());
        }
        for (int y = offsetY; y < getHeight(); y += 25) {
            g.drawLine(0, y, getWidth(), y);
        }

        g.setFont(new Font("SansSerif", Font.PLAIN, 11));
        for (Zone z : zones) {
            int x = Math.min(z.getX1(), z.getX2()) / scale;
            int y = Math.min(z.getY1(), z.getY2()) / scale + offsetY;
            int w = Math.abs(z.getX2() - z.getX1()) / scale;
            int h = Math.abs(z.getY2() - z.getY1()) / scale;

            g.setColor(Color.BLACK);
            g.drawRect(x, y, w, h);
            g.drawString("Z(" + z.getId() + ")", x + 5, y + 15);

            int cx = z.centerX() / scale;
            int cy = z.centerY() / scale + offsetY;
            g.fillOval(cx - 2, cy - 2, 4, 4);
        }

        for (Map.Entry<Integer, FireEvent.Severity> entry : activeFires.entrySet()) {
            int zoneId = entry.getKey();
            FireEvent.Severity severity = entry.getValue();
            Zone z = findZone(zoneId);
            if (z == null) continue;

            int cx = z.centerX() / scale;
            int cy = z.centerY() / scale + offsetY;

            double intensity = GuiModel.get().getFireIntensity(zoneId);
            int size = Math.max(14, (int) (42 * intensity));

            if (fireImage != null) {
                g.drawImage(fireImage, cx - size / 2, cy - size / 2, size, size, null);
            } else {
                Color fireColor = getSeverityColor(severity);
                g2.setColor(new Color(fireColor.getRed(), fireColor.getGreen(), fireColor.getBlue(), 120));
                g2.fillOval(cx - size / 2, cy - size / 2, size, size);
                g2.setColor(fireColor);
                g2.setStroke(new BasicStroke(2));
                g2.drawOval(cx - size / 2, cy - size / 2, size, size);
                g2.setStroke(new BasicStroke(1));

                g.setColor(Color.WHITE);
                g.setFont(new Font("SansSerif", Font.BOLD, 10));
                g.drawString(severity.name().substring(0, 1), cx - 3, cy + 4);
                g.setFont(new Font("SansSerif", Font.PLAIN, 11));
            }
        }

        for (Map.Entry<Integer, Integer> entry : droneAssignments.entrySet()) {
            int droneId = entry.getKey();
            int zoneId = entry.getValue();
            Zone z = findZone(zoneId);
            if (z == null) continue;

            Point p = dronePositions.getOrDefault(droneId, new Point(0, 0));
            int px = p.x / scale;
            int py = p.y / scale + offsetY;
            int cx = z.centerX() / scale;
            int cy = z.centerY() / scale + offsetY;

            g2.setColor(getDroneColor(droneId));
            g2.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                    1.0f, new float[]{6, 4}, 0));
            g2.drawLine(px, py, cx, cy);
            g2.setStroke(new BasicStroke(1));
        }

        for (Map.Entry<Integer, DroneState> entry : droneStates.entrySet()) {
            int droneId = entry.getKey();
            DroneState state = entry.getValue();
            Point p = dronePositions.getOrDefault(droneId, new Point(0, 0));

            int px = p.x / scale;
            int py = p.y / scale + offsetY;

            FaultType fault = droneFaults.getOrDefault(droneId, FaultType.NONE);
            Color markerColor = (fault != FaultType.NONE)
                    ? getFaultColor(fault, state)
                    : getDroneColor(droneId);

            if (droneImage != null) {
                g.drawImage(droneImage, px - 16, py - 16, 32, 32, null);
                g.setColor(markerColor);
                g.drawOval(px - 18, py - 18, 36, 36);
                g.setColor(Color.BLACK);
                g.drawString("D" + droneId, px + 18, py - 10);
            } else {
                g.setColor(markerColor);
                g.fillOval(px - 8, py - 8, 16, 16);
                g.setColor(Color.BLACK);
                g.drawOval(px - 8, py - 8, 16, 16);
                g.drawString("D" + droneId, px + 10, py - 10);
            }
        }
    }

    private Zone findZone(int zoneId) {
        return zones.stream().filter(z -> z.getId() == zoneId).findFirst().orElse(null);
    }

    private static Color getSeverityColor(FireEvent.Severity severity) {
        if (severity == null) return COLOR_MODERATE;
        switch (severity) {
            case LOW:
                return COLOR_LOW;
            case MODERATE:
                return COLOR_MODERATE;
            case HIGH:
                return COLOR_HIGH;
            default:
                return COLOR_MODERATE;
        }
    }

    private static Color getDroneColor(int droneId) {
        return DRONE_COLORS[(droneId - 1) % DRONE_COLORS.length];
    }

    private static Color getFaultColor(FaultType fault, DroneState state) {
        if (state == DroneState.OFFLINE || state == DroneState.SHUTDOWN) return COLOR_OFFLINE;
        switch (fault) {
            case DRONE_STUCK:
                return COLOR_FAULT_STUCK;
            case NOZZLE_STUCK:
                return COLOR_FAULT_NOZZLE;
            case SENSOR_FAIL:
                return COLOR_FAULT_SENSOR;
            default:
                return COLOR_OFFLINE;
        }
    }

    public void startAutoRepaint() {
        new Timer(100, e -> repaint()).start();
    }
}