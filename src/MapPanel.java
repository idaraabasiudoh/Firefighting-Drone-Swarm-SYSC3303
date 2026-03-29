// MapPanel.java  (Iteration 4: multi-drone display, severity color-coding, drone assignments, fault visualization)
import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Map;

public class MapPanel extends JPanel {
    private final List<Zone> zones;
    private final int scale = 2;

    // Colors for severity
    private static final Color COLOR_LOW = new Color(76, 175, 80);        // green
    private static final Color COLOR_MODERATE = new Color(255, 152, 0);    // orange
    private static final Color COLOR_HIGH = new Color(244, 67, 54);        // red

    // Colors for drones
    private static final Color[] DRONE_COLORS = {
            new Color(33, 150, 243),   // blue
            new Color(156, 39, 176),   // purple
            new Color(0, 150, 136),    // teal
            new Color(255, 87, 34),    // deep orange
    };

    // Iteration 4: Fault colors
    private static final Color COLOR_FAULT_STUCK = new Color(255, 235, 59);   // yellow
    private static final Color COLOR_FAULT_NOZZLE = new Color(183, 28, 28);   // dark red
    private static final Color COLOR_FAULT_SENSOR = new Color(255, 152, 0);   // orange
    private static final Color COLOR_OFFLINE = new Color(117, 117, 117);      // grey

    public MapPanel(List<Zone> zones) {
        this.zones = zones;
        setPreferredSize(new Dimension(900, 750));
        setDoubleBuffered(true);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Snapshots from GuiModel
        Map<Integer, DroneState> droneStates = GuiModel.get().snapshotDroneStates();
        Map<Integer, Integer> droneAssignments = GuiModel.get().snapshotDroneAssignments();
        Map<Integer, FireEvent.Severity> activeFires = GuiModel.get().snapshotActiveFireZones();
        Map<Integer, FaultType> droneFaults = GuiModel.get().snapshotDroneFaults();

        // ---- UI header: drone statuses ----
        int headerY = 20;
        g.setColor(Color.BLACK);
        g.setFont(new Font("SansSerif", Font.BOLD, 13));
        g.drawString("Firefighting Drone Swarm - Iteration 4 (Fault Handling)", 20, headerY);
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
                Color droneColor = getDroneColor(droneId);

                FaultType fault = droneFaults.getOrDefault(droneId, FaultType.NONE);
                Color labelColor = (fault != FaultType.NONE) ? getFaultColor(fault, state) : droneColor;
                g.setColor(labelColor);
                String label = "Drone " + droneId + ": " + state;
                if (fault != FaultType.NONE) {
                    label += " [FAULT: " + fault + "]";
                }
                if (assignedZone != null) {
                    label += " -> Zone " + assignedZone;
                    FireEvent.Severity sev = activeFires.get(assignedZone);
                    if (sev != null) label += " (" + sev + ")";
                }
                g.drawString(label, 20, headerY);
                headerY += 16;
            }
        }

        // ---- background grid ----
        g.setColor(new Color(230, 230, 230));
        int offsetY = 120; // shift map down below header
        for (int x = 0; x < getWidth(); x += 25) g.drawLine(x, offsetY, x, getHeight());
        for (int y = offsetY; y < getHeight(); y += 25) g.drawLine(0, y, getWidth(), y);

        // ---- draw zones ----
        g.setFont(new Font("SansSerif", Font.PLAIN, 11));
        for (Zone z : zones) {
            int x = Math.min(z.getX1(), z.getX2()) / scale;
            int y = Math.min(z.getY1(), z.getY2()) / scale + offsetY;
            int w = Math.abs(z.getX2() - z.getX1()) / scale;
            int h = Math.abs(z.getY2() - z.getY1()) / scale;

            g.setColor(Color.BLACK);
            g.drawRect(x, y, w, h);
            g.drawString("Z(" + z.getId() + ")", x + 5, y + 15);

            // center dot
            int cx = z.centerX() / scale;
            int cy = z.centerY() / scale + offsetY;
            g.fillOval(cx - 2, cy - 2, 4, 4);
        }

        // ---- draw active fires with severity color ----
        for (Map.Entry<Integer, FireEvent.Severity> entry : activeFires.entrySet()) {
            int zoneId = entry.getKey();
            FireEvent.Severity severity = entry.getValue();
            Zone z = findZone(zoneId);
            if (z == null) continue;

            int cx = z.centerX() / scale;
            int cy = z.centerY() / scale + offsetY;

            Color fireColor = getSeverityColor(severity);
            g2.setColor(new Color(fireColor.getRed(), fireColor.getGreen(), fireColor.getBlue(), 120));
            g2.fillOval(cx - 14, cy - 14, 28, 28);
            g2.setColor(fireColor);
            g2.setStroke(new BasicStroke(2));
            g2.drawOval(cx - 14, cy - 14, 28, 28);
            g2.setStroke(new BasicStroke(1));

            // Severity label
            g.setColor(Color.WHITE);
            g.setFont(new Font("SansSerif", Font.BOLD, 10));
            String sevLabel = severity.name().substring(0, 1);
            g.drawString(sevLabel, cx - 3, cy + 4);
            g.setFont(new Font("SansSerif", Font.PLAIN, 11));
        }

        // ---- draw drone assignment lines ----
        for (Map.Entry<Integer, Integer> entry : droneAssignments.entrySet()) {
            int droneId = entry.getKey();
            int zoneId = entry.getValue();
            Zone z = findZone(zoneId);
            if (z == null) continue;

            int cx = z.centerX() / scale;
            int cy = z.centerY() / scale + offsetY;

            Color droneColor = getDroneColor(droneId);
            g2.setColor(droneColor);
            g2.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                    1.0f, new float[]{6, 4}, 0));

            // Draw dashed line from base (0,0) to zone center
            g2.drawLine(0, offsetY, cx, cy);
            g2.setStroke(new BasicStroke(1));

            // Drone marker at zone center
            FaultType fault = droneFaults.getOrDefault(droneId, FaultType.NONE);
            DroneState dState = droneStates.getOrDefault(droneId, DroneState.IDLE);
            Color markerColor = (fault != FaultType.NONE) ? getFaultColor(fault, dState) : droneColor;

            g2.setColor(markerColor);
            if (fault != FaultType.NONE) {
                // Draw X marker for faulted drones
                g2.setStroke(new BasicStroke(3));
                g2.drawLine(cx - 8, cy - 8, cx + 8, cy + 8);
                g2.drawLine(cx - 8, cy + 8, cx + 8, cy - 8);
                g2.setStroke(new BasicStroke(1));
                g.setColor(markerColor);
                g.setFont(new Font("SansSerif", Font.BOLD, 9));
                g.drawString("D" + droneId, cx - 5, cy - 10);
            } else {
                g2.fillRect(cx - 6, cy - 6, 12, 12);
                g.setColor(Color.WHITE);
                g.setFont(new Font("SansSerif", Font.BOLD, 9));
                g.drawString("D" + droneId, cx - 5, cy + 4);
            }
            g.setFont(new Font("SansSerif", Font.PLAIN, 11));
        }
    }

    private Zone findZone(int zoneId) {
        return zones.stream().filter(z -> z.getId() == zoneId).findFirst().orElse(null);
    }

    private static Color getSeverityColor(FireEvent.Severity severity) {
        if (severity == null) return COLOR_MODERATE;
        switch (severity) {
            case LOW: return COLOR_LOW;
            case MODERATE: return COLOR_MODERATE;
            case HIGH: return COLOR_HIGH;
            default: return COLOR_MODERATE;
        }
    }

    private static Color getDroneColor(int droneId) {
        return DRONE_COLORS[(droneId - 1) % DRONE_COLORS.length];
    }

    private static Color getFaultColor(FaultType fault, DroneState state) {
        if (state == DroneState.OFFLINE) return COLOR_OFFLINE;
        switch (fault) {
            case DRONE_STUCK: return COLOR_FAULT_STUCK;
            case NOZZLE_STUCK: return COLOR_FAULT_NOZZLE;
            case SENSOR_FAIL: return COLOR_FAULT_SENSOR;
            default: return COLOR_OFFLINE;
        }
    }

    // Call this from a Swing timer to keep GUI refreshed
    public void startAutoRepaint() {
        new Timer(200, e -> repaint()).start();
    }
}