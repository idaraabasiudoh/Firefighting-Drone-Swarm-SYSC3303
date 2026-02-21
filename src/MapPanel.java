// MapPanel.java  (UPDATED: show drone state + active fires count; draw fires from GuiModel)
// REPLACE your MapPanel with this version
import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Set;

public class MapPanel extends JPanel {
    private final List<Zone> zones;
    private final int scale = 2;

    public MapPanel(List<Zone> zones) {
        this.zones = zones;
        setPreferredSize(new Dimension(900, 700));
        setDoubleBuffered(true);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // UI header
        g.setColor(Color.BLACK);
        g.drawString("Drone State: " + GuiModel.get().getDroneState(), 20, 20);
        g.drawString("Active Fires: " + GuiModel.get().getActiveFireCount(), 20, 40);

        // background grid (optional)
        g.setColor(new Color(230, 230, 230));
        for (int x = 0; x < getWidth(); x += 25) g.drawLine(x, 0, x, getHeight());
        for (int y = 0; y < getHeight(); y += 25) g.drawLine(0, y, getWidth(), y);

        // draw zones
        g.setColor(Color.BLACK);
        for (Zone z : zones) {
            int x = Math.min(z.getX1(), z.getX2()) / scale;
            int y = Math.min(z.getY1(), z.getY2()) / scale;
            int w = Math.abs(z.getX2() - z.getX1()) / scale;
            int h = Math.abs(z.getY2() - z.getY1()) / scale;

            g.drawRect(x, y, w, h);
            g.drawString("Z(" + z.getId() + ")", x + 5, y + 15);

            // center dot (debug)
            int cx = z.centerX() / scale;
            int cy = z.centerY() / scale;
            g.fillOval(cx - 2, cy - 2, 4, 4);
        }

        // draw active fires at zone centers
        Set<Integer> activeZones = GuiModel.get().snapshotActiveFireZones();
        for (Integer zoneId : activeZones) {
            Zone z = zones.stream().filter(zz -> zz.getId() == zoneId).findFirst().orElse(null);
            if (z == null) continue;

            int cx = z.centerX() / scale;
            int cy = z.centerY() / scale;

            g.setColor(Color.ORANGE);
            g.fillOval(cx - 10, cy - 10, 20, 20);
            g.setColor(Color.RED);
            g.drawOval(cx - 10, cy - 10, 20, 20);
        }
    }

    // Call this from a Swing timer to keep GUI refreshed
    public void startAutoRepaint() {
        new Timer(200, e -> repaint()).start();
    }
}