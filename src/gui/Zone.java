package gui;

public class Zone {
    private final int id;
    private final int x1, y1, x2, y2;

    public Zone(int id, int x1, int y1, int x2, int y2) {
        this.id = id;
        this.x1 = x1; this.y1 = y1; this.x2 = x2; this.y2 = y2;
    }

    public int getId() { return id; }
    public int getX1() { return x1; }
    public int getY1() { return y1; }
    public int getX2() { return x2; }
    public int getY2() { return y2; }

    public int centerX() { return (x1 + x2) / 2; }
    public int centerY() { return (y1 + y2) / 2; }

    public boolean isOddByOdd() {
        int w = Math.abs(x2 - x1);
        int h = Math.abs(y2 - y1);
        return (w % 2 == 1) && (h % 2 == 1);
    }
}
