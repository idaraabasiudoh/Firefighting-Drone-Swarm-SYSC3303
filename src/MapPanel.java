import javax.swing.*;
import java.awt.*;
import java.util.List;

public class MapPanel extends JPanel {
    private final List<Zone> zones;
    private final List<FireEvent> fires; // just for drawing fire markers

    // scale down large coordinates to fit window
    private final int scale = 2; // tweak as needed (1,2,3...)

    public MapPanel(List<Zone> zones, List<FireEvent> fires) {
        this.zones = zones;
        this.fires = fires;
        setPreferredSize(new Dimension(900, 700));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // background grid (optional)
        g.setColor(new Color(230, 230, 230));
        for (int x = 0; x < getWidth(); x += 25) g.drawLine(x, 0, x, getHeight());
        for (int y = 0; y < getHeight(); y += 25) g.drawLine(0, y, getWidth(), y);

        // draw zones
        for (Zone z : zones) {
            int x = Math.min(z.getX1(), z.getX2()) / scale;
            int y = Math.min(z.getY1(), z.getY2()) / scale;
            int w = Math.abs(z.getX2() - z.getX1()) / scale;
            int h = Math.abs(z.getY2() - z.getY1()) / scale;

            g.setColor(Color.BLACK);
            g.drawRect(x, y, w, h);

            g.drawString("Z(" + z.getId() + ")", x + 5, y + 15);

            // mark center (debug)
            int cx = z.centerX() / scale;
            int cy = z.centerY() / scale;
            g.fillOval(cx - 2, cy - 2, 4, 4);
        }

        // draw fires at zone centers (fire events happen "in the middle")
        if (fires != null) {
            for (FireEvent e : fires) {
                Zone z = zones.stream().filter(zz -> zz.getId() == e.getZoneId()).findFirst().orElse(null);
                if (z == null) continue;

                int cx = z.centerX() / scale;
                int cy = z.centerY() / scale;

                g.setColor(Color.ORANGE);
                g.fillOval(cx - 10, cy - 10, 20, 20);
                g.setColor(Color.RED);
                g.drawOval(cx - 10, cy - 10, 20, 20);
            }
        }
    }
}
