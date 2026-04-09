package gui;

import drone.DroneState;
import drone.FaultType;
import fireincident.FireEvent;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class MapPanel extends JPanel {
    private final List<Zone> zones;
    private final int scale = 2;
    private BufferedImage droneImage;

    // Pulse animation state
    private int pulseCounter = 0;

    // ── Dark palette ──────────────────────────────────────────────────────────
    private static final Color BG_TOP        = new Color(10, 14, 26);
    private static final Color BG_BOTTOM     = new Color(6, 10, 20);
    private static final Color GRID_COLOR    = new Color(255, 255, 255, 18);
    private static final Color ZONE_BORDER   = new Color(255, 255, 255, 45);
    private static final Color ZONE_FILL     = new Color(255, 255, 255, 7);
    private static final Color ZONE_LABEL    = new Color(120, 140, 180);
    private static final Color HUD_BG        = new Color(12, 18, 36, 210);
    private static final Color HUD_BORDER    = new Color(60, 100, 180, 120);
    private static final Color ACCENT        = new Color(80, 140, 255);
    private static final Color TEXT_PRIMARY  = new Color(220, 230, 255);
    private static final Color TEXT_DIM      = new Color(120, 140, 180);

    // Severity colors
    private static final Color COLOR_LOW      = new Color(72, 199, 116);
    private static final Color COLOR_MODERATE = new Color(255, 180, 40);
    private static final Color COLOR_HIGH     = new Color(255, 70, 70);

    // Drone palette
    private static final Color[] DRONE_COLORS = {
        new Color(80,  160, 255),  // electric blue
        new Color(180, 80,  255),  // violet
        new Color(40,  210, 180),  // teal
        new Color(255, 120, 50),   // orange
        new Color(255, 80,  160),  // pink
        new Color(100, 255, 120),  // lime
    };

    private static final Color COLOR_FAULT_STUCK  = new Color(255, 230, 60);
    private static final Color COLOR_FAULT_NOZZLE = new Color(220, 40,  60);
    private static final Color COLOR_FAULT_SENSOR = new Color(255, 150, 40);
    private static final Color COLOR_OFFLINE      = new Color(90, 100, 120);

    // Layout
    private static final int HUD_W = 230;
    private static final int MAP_OFFSET_Y = 0;

    public MapPanel(List<Zone> zones) {
        this.zones = zones;
        setPreferredSize(new Dimension(1100, 780));
        setDoubleBuffered(true);
        setBackground(BG_TOP);

        try {
            droneImage = ImageIO.read(new File("src/gui/drone.png"));
        } catch (IOException e) {
            System.out.println("Could not load drone image, using shapes instead.");
            droneImage = null;
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,        RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,   RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING,           RenderingHints.VALUE_RENDER_QUALITY);

        pulseCounter = (pulseCounter + 1) % 60;

        // ── Background ──────────────────────────────────────────────────────
        GradientPaint bgGrad = new GradientPaint(0, 0, BG_TOP, 0, getHeight(), BG_BOTTOM);
        g2.setPaint(bgGrad);
        g2.fillRect(0, 0, getWidth(), getHeight());

        // Map area (right of HUD)
        int mapX = HUD_W + 10;
        int mapW = getWidth() - mapX - 10;

        pullSnapshots(g2, mapX, mapW);
    }

    private void pullSnapshots(Graphics2D g2, int mapX, int mapW) {
        Map<Integer, DroneState>      droneStates     = GuiModel.get().snapshotDroneStates();
        Map<Integer, Integer>         droneAssignments= GuiModel.get().snapshotDroneAssignments();
        Map<Integer, FireEvent.Severity> activeFires  = GuiModel.get().snapshotActiveFireZones();
        Map<Integer, FaultType>       droneFaults     = GuiModel.get().snapshotDroneFaults();
        Map<Integer, Point>           dronePositions  = GuiModel.get().snapshotDronePositions();

        drawGrid(g2, mapX, mapW);
        drawZones(g2, mapX, activeFires);
        drawFireMarkers(g2, mapX, activeFires);
        drawRouteLines(g2, mapX, droneAssignments, dronePositions);
        drawDrones(g2, mapX, droneStates, dronePositions, droneFaults);
        drawHUD(g2, droneStates, droneAssignments, activeFires, droneFaults);
    }

    // ── Grid ─────────────────────────────────────────────────────────────────
    private void drawGrid(Graphics2D g2, int mapX, int mapW) {
        g2.setColor(GRID_COLOR);
        g2.setStroke(new BasicStroke(0.5f));
        for (int x = mapX; x < mapX + mapW; x += 25) {
            g2.drawLine(x, MAP_OFFSET_Y, x, getHeight());
        }
        for (int y = MAP_OFFSET_Y; y < getHeight(); y += 25) {
            g2.drawLine(mapX, y, mapX + mapW, y);
        }
    }

    // ── Zones ────────────────────────────────────────────────────────────────
    private void drawZones(Graphics2D g2, int mapX, Map<Integer, FireEvent.Severity> activeFires) {
        for (Zone z : zones) {
            int x = mapX + Math.min(z.getX1(), z.getX2()) / scale;
            int y = MAP_OFFSET_Y + Math.min(z.getY1(), z.getY2()) / scale;
            int w = Math.abs(z.getX2() - z.getX1()) / scale;
            int h = Math.abs(z.getY2() - z.getY1()) / scale;

            // Subtle fill tint on active fire zones
            if (activeFires.containsKey(z.getId())) {
                Color sev = getSeverityColor(activeFires.get(z.getId()));
                g2.setColor(new Color(sev.getRed(), sev.getGreen(), sev.getBlue(), 20));
                g2.fillRect(x, y, w, h);
            } else {
                g2.setColor(ZONE_FILL);
                g2.fillRect(x, y, w, h);
            }

            g2.setColor(ZONE_BORDER);
            g2.setStroke(new BasicStroke(1f));
            g2.drawRect(x, y, w, h);

            // Zone label
            g2.setColor(ZONE_LABEL);
            g2.setFont(new Font("SansSerif", Font.PLAIN, 10));
            g2.drawString("Z" + z.getId(), x + 5, y + 14);
        }
    }

    // ── Fire markers ─────────────────────────────────────────────────────────
    private void drawFireMarkers(Graphics2D g2, int mapX, Map<Integer, FireEvent.Severity> activeFires) {
        float pulse = (float)(0.85 + 0.15 * Math.sin(pulseCounter * Math.PI / 30.0));

        for (Map.Entry<Integer, FireEvent.Severity> entry : activeFires.entrySet()) {
            int zoneId = entry.getKey();
            FireEvent.Severity severity = entry.getValue();
            Zone z = findZone(zoneId);
            if (z == null) continue;

            int cx = mapX + z.centerX() / scale;
            int cy = MAP_OFFSET_Y + z.centerY() / scale;

            double intensity = GuiModel.get().getFireIntensity(zoneId);
            int baseSize = getSeverityBaseSize(severity);
            int size = Math.max(16, (int)(baseSize * intensity));

            Color fireColor = getSeverityColor(severity);

            // Outer glow ring (pulsing)
            int glowSize = (int)(size * 1.8 * pulse);
            g2.setColor(new Color(fireColor.getRed(), fireColor.getGreen(), fireColor.getBlue(), 30));
            g2.fillOval(cx - glowSize / 2, cy - glowSize / 2, glowSize, glowSize);

            // Mid ring
            g2.setColor(new Color(fireColor.getRed(), fireColor.getGreen(), fireColor.getBlue(), 70));
            g2.fillOval(cx - size / 2 - 4, cy - size / 2 - 4, size + 8, size + 8);

            // Core fire circle with gradient
            RadialGradientPaint fireGrad = new RadialGradientPaint(
                    new Point2D.Float(cx, cy),
                    size / 2f,
                    new float[]{0f, 0.6f, 1f},
                    new Color[]{
                        Color.WHITE,
                        new Color(fireColor.getRed(), fireColor.getGreen(), fireColor.getBlue(), 230),
                        new Color(fireColor.getRed(), fireColor.getGreen(), fireColor.getBlue(), 80)
                    }
            );
            g2.setPaint(fireGrad);
            g2.fillOval(cx - size / 2, cy - size / 2, size, size);

            // Severity badge
            g2.setColor(new Color(0, 0, 0, 160));
            g2.fillRoundRect(cx - 14, cy + size / 2 + 2, 28, 14, 6, 6);
            g2.setColor(fireColor);
            g2.setFont(new Font("SansSerif", Font.BOLD, 9));
            String label = severity.name().substring(0, 3);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(label, cx - fm.stringWidth(label) / 2, cy + size / 2 + 12);
        }
    }

    // ── Route lines ───────────────────────────────────────────────────────────
    private void drawRouteLines(Graphics2D g2, int mapX,
                                Map<Integer, Integer> droneAssignments,
                                Map<Integer, Point> dronePositions) {
        for (Map.Entry<Integer, Integer> entry : droneAssignments.entrySet()) {
            int droneId = entry.getKey();
            int zoneId  = entry.getValue();
            Zone z = findZone(zoneId);
            if (z == null) continue;

            Point p = dronePositions.getOrDefault(droneId, new Point(0, 0));
            int px = mapX + p.x / scale;
            int py = MAP_OFFSET_Y + p.y / scale;
            int cx = mapX + z.centerX() / scale;
            int cy = MAP_OFFSET_Y + z.centerY() / scale;

            Color dc = getDroneColor(droneId);
            g2.setColor(new Color(dc.getRed(), dc.getGreen(), dc.getBlue(), 80));
            g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                    1f, new float[]{8, 5}, pulseCounter % 13));
            g2.drawLine(px, py, cx, cy);
            g2.setStroke(new BasicStroke(1f));
        }
    }

    // ── Drones ────────────────────────────────────────────────────────────────
    private void drawDrones(Graphics2D g2, int mapX,
                            Map<Integer, DroneState> droneStates,
                            Map<Integer, Point> dronePositions,
                            Map<Integer, FaultType> droneFaults) {

        for (Map.Entry<Integer, DroneState> entry : droneStates.entrySet()) {
            int droneId      = entry.getKey();
            DroneState state = entry.getValue();
            Point p = dronePositions.getOrDefault(droneId, new Point(0, 0));

            int px = mapX + p.x / scale;
            int py = MAP_OFFSET_Y + p.y / scale;

            FaultType fault = droneFaults.getOrDefault(droneId, FaultType.NONE);
            Color dc = (fault != FaultType.NONE) ? getFaultColor(fault, state) : getDroneColor(droneId);

            // Outer glow
            int glowR = 22;
            g2.setColor(new Color(dc.getRed(), dc.getGreen(), dc.getBlue(), 40));
            g2.fillOval(px - glowR, py - glowR, glowR * 2, glowR * 2);

            if (droneImage != null) {
                // Draw tinted drone image
                g2.drawImage(droneImage, px - 14, py - 14, 28, 28, null);
                // Color ring
                g2.setColor(dc);
                g2.setStroke(new BasicStroke(2.5f));
                g2.drawOval(px - 16, py - 16, 32, 32);
                g2.setStroke(new BasicStroke(1f));
            } else {
                // Hexagon-style drone body
                Polygon hex = makeHexagon(px, py, 12);
                g2.setColor(new Color(dc.getRed(), dc.getGreen(), dc.getBlue(), 200));
                g2.fillPolygon(hex);
                g2.setColor(dc);
                g2.setStroke(new BasicStroke(2f));
                g2.drawPolygon(hex);
                g2.setStroke(new BasicStroke(1f));
                // Inner dot
                g2.setColor(Color.WHITE);
                g2.fillOval(px - 3, py - 3, 6, 6);
            }

            // Drone ID badge
            String label = "D" + droneId;
            g2.setFont(new Font("SansSerif", Font.BOLD, 10));
            FontMetrics fm = g2.getFontMetrics();
            int badgeW = fm.stringWidth(label) + 8;
            g2.setColor(new Color(0, 0, 0, 180));
            g2.fillRoundRect(px + 14, py - 18, badgeW, 16, 6, 6);
            g2.setColor(dc);
            g2.drawString(label, px + 18, py - 6);

            // State tag
            if (state != DroneState.IDLE) {
                String stateShort = shortState(state, fault);
                g2.setFont(new Font("SansSerif", Font.PLAIN, 9));
                fm = g2.getFontMetrics();
                int sw = fm.stringWidth(stateShort) + 6;
                g2.setColor(new Color(dc.getRed(), dc.getGreen(), dc.getBlue(), 160));
                g2.fillRoundRect(px + 14, py - 2, sw, 13, 5, 5);
                g2.setColor(Color.WHITE);
                g2.drawString(stateShort, px + 17, py + 9);
            }
        }
    }

    // ── HUD Panel ─────────────────────────────────────────────────────────────
    private void drawHUD(Graphics2D g2,
                         Map<Integer, DroneState>      droneStates,
                         Map<Integer, Integer>         droneAssignments,
                         Map<Integer, FireEvent.Severity> activeFires,
                         Map<Integer, FaultType>       droneFaults) {

        int pad = 12;
        int x = pad;
        int y = pad;
        int w = HUD_W - pad * 2;

        // Panel background
        g2.setColor(HUD_BG);
        g2.fillRoundRect(x, y, w, getHeight() - pad * 2, 14, 14);
        g2.setColor(HUD_BORDER);
        g2.setStroke(new BasicStroke(1.2f));
        g2.drawRoundRect(x, y, w, getHeight() - pad * 2, 14, 14);
        g2.setStroke(new BasicStroke(1f));

        int tx = x + 14;
        int ty = y + 22;

        // Title
        g2.setFont(new Font("SansSerif", Font.BOLD, 14));
        g2.setColor(ACCENT);
        g2.drawString("🔥 Drone Swarm", tx, ty);
        ty += 6;

        // Divider
        g2.setColor(HUD_BORDER);
        g2.drawLine(x + 10, ty, x + w - 10, ty);
        ty += 16;

        // Stats
        g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
        g2.setColor(TEXT_DIM);
        g2.drawString("Active Fires", tx, ty);
        g2.setColor(activeFires.isEmpty() ? COLOR_LOW : COLOR_HIGH);
        g2.setFont(new Font("SansSerif", Font.BOLD, 11));
        g2.drawString(String.valueOf(activeFires.size()), tx + 90, ty);
        ty += 16;

        g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
        g2.setColor(TEXT_DIM);
        g2.drawString("Total Drones", tx, ty);
        g2.setColor(TEXT_PRIMARY);
        g2.setFont(new Font("SansSerif", Font.BOLD, 11));
        g2.drawString(String.valueOf(droneStates.size()), tx + 90, ty);
        ty += 16;

        long active = droneStates.values().stream()
                .filter(s -> s != DroneState.IDLE && s != DroneState.SHUTDOWN && s != DroneState.OFFLINE)
                .count();
        g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
        g2.setColor(TEXT_DIM);
        g2.drawString("Active Drones", tx, ty);
        g2.setColor(active > 0 ? ACCENT : TEXT_DIM);
        g2.setFont(new Font("SansSerif", Font.BOLD, 11));
        g2.drawString(String.valueOf(active), tx + 90, ty);
        ty += 20;

        // Divider
        g2.setColor(HUD_BORDER);
        g2.drawLine(x + 10, ty, x + w - 10, ty);
        ty += 14;

        // Drone list
        g2.setFont(new Font("SansSerif", Font.BOLD, 11));
        g2.setColor(TEXT_DIM);
        g2.drawString("DRONES", tx, ty);
        ty += 14;

        if (droneStates.isEmpty()) {
            g2.setFont(new Font("SansSerif", Font.ITALIC, 10));
            g2.setColor(TEXT_DIM);
            g2.drawString("No drones registered", tx, ty);
            ty += 14;
        } else {
            for (Map.Entry<Integer, DroneState> entry : droneStates.entrySet()) {
                int droneId = entry.getKey();
                DroneState state = entry.getValue();
                Integer assignedZone = droneAssignments.get(droneId);
                FaultType fault = droneFaults.getOrDefault(droneId, FaultType.NONE);

                Color dc = (fault != FaultType.NONE) ? getFaultColor(fault, state) : getDroneColor(droneId);

                // Color swatch
                g2.setColor(dc);
                g2.fillRoundRect(tx, ty - 10, 8, 10, 3, 3);

                g2.setFont(new Font("SansSerif", Font.BOLD, 11));
                g2.setColor(dc);
                g2.drawString("D" + droneId, tx + 12, ty);

                g2.setFont(new Font("SansSerif", Font.PLAIN, 10));
                g2.setColor(TEXT_DIM);
                String stateStr = state.name().replace("_", " ");
                if (fault != FaultType.NONE) stateStr = "⚠ " + fault.name().replace("_", " ");
                g2.drawString(stateStr, tx + 38, ty);
                ty += 4;

                if (assignedZone != null) {
                    g2.setColor(new Color(dc.getRed(), dc.getGreen(), dc.getBlue(), 150));
                    g2.setFont(new Font("SansSerif", Font.PLAIN, 9));
                    g2.drawString("  → Zone " + assignedZone, tx + 12, ty + 8);
                    ty += 10;
                }

                ty += 10;

                // Guard against HUD overflow
                if (ty > getHeight() - 120) break;
            }
        }

        // Divider
        g2.setColor(HUD_BORDER);
        g2.drawLine(x + 10, ty, x + w - 10, ty);
        ty += 14;

        // Legend
        g2.setFont(new Font("SansSerif", Font.BOLD, 11));
        g2.setColor(TEXT_DIM);
        g2.drawString("SEVERITY", tx, ty);
        ty += 14;

        drawLegendItem(g2, tx, ty, COLOR_LOW,      "Low");      ty += 16;
        drawLegendItem(g2, tx, ty, COLOR_MODERATE,  "Moderate"); ty += 16;
        drawLegendItem(g2, tx, ty, COLOR_HIGH,       "High");    ty += 20;

        // Fault legend
        g2.setColor(HUD_BORDER);
        g2.drawLine(x + 10, ty, x + w - 10, ty);
        ty += 14;
        g2.setFont(new Font("SansSerif", Font.BOLD, 11));
        g2.setColor(TEXT_DIM);
        g2.drawString("FAULTS", tx, ty);
        ty += 14;
        drawLegendItem(g2, tx, ty, COLOR_FAULT_STUCK,  "Drone Stuck");  ty += 16;
        drawLegendItem(g2, tx, ty, COLOR_FAULT_NOZZLE, "Nozzle Stuck"); ty += 16;
        drawLegendItem(g2, tx, ty, COLOR_FAULT_SENSOR, "Sensor Fail");  ty += 16;
        drawLegendItem(g2, tx, ty, COLOR_OFFLINE,       "Offline");
    }

    private void drawLegendItem(Graphics2D g2, int x, int y, Color c, String label) {
        g2.setColor(c);
        g2.fillOval(x, y - 9, 10, 10);
        g2.setFont(new Font("SansSerif", Font.PLAIN, 10));
        g2.setColor(TEXT_PRIMARY);
        g2.drawString(label, x + 16, y);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private Zone findZone(int zoneId) {
        return zones.stream().filter(z -> z.getId() == zoneId).findFirst().orElse(null);
    }

    private static Color getSeverityColor(FireEvent.Severity severity) {
        if (severity == null) return COLOR_MODERATE;
        switch (severity) {
            case LOW:      return COLOR_LOW;
            case HIGH:     return COLOR_HIGH;
            default:       return COLOR_MODERATE;
        }
    }

    private static int getSeverityBaseSize(FireEvent.Severity severity) {
        if (severity == null) return 28;
        switch (severity) {
            case LOW:      return 22;
            case HIGH:     return 40;
            default:       return 30;
        }
    }

    private static Color getDroneColor(int droneId) {
        return DRONE_COLORS[(droneId - 1) % DRONE_COLORS.length];
    }

    private static Color getFaultColor(FaultType fault, DroneState state) {
        if (state == DroneState.OFFLINE || state == DroneState.SHUTDOWN) return COLOR_OFFLINE;
        switch (fault) {
            case DRONE_STUCK:  return COLOR_FAULT_STUCK;
            case NOZZLE_STUCK: return COLOR_FAULT_NOZZLE;
            case SENSOR_FAIL:  return COLOR_FAULT_SENSOR;
            default:           return COLOR_OFFLINE;
        }
    }

    private static String shortState(DroneState state, FaultType fault) {
        if (fault != FaultType.NONE) return "FAULT";
        switch (state) {
            case EN_ROUTE:       return "EN ROUTE";
            case DROPPING_AGENT: return "DROPPING";
            case RETURNING_BASE: return "RTB";
            case IDLE:           return "IDLE";
            default:             return state.name().replace("_", " ");
        }
    }

    private static Polygon makeHexagon(int cx, int cy, int r) {
        Polygon p = new Polygon();
        for (int i = 0; i < 6; i++) {
            double angle = Math.PI / 6 + i * Math.PI / 3;
            p.addPoint((int)(cx + r * Math.cos(angle)), (int)(cy + r * Math.sin(angle)));
        }
        return p;
    }

    public void startAutoRepaint() {
        new Timer(50, e -> repaint()).start();   // 20 fps smooth animation
    }
}