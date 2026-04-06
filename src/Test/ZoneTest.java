package test;

import gui.Zone;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ZoneTest {

    // ==================== Construction & Getters ====================

    @Test
    void testZoneConstruction() {
        Zone z = new Zone(1, 0, 0, 700, 600);

        assertEquals(1, z.getId());
        assertEquals(0, z.getX1());
        assertEquals(0, z.getY1());
        assertEquals(700, z.getX2());
        assertEquals(600, z.getY2());
    }

    // ==================== Center Calculation ====================

    @Test
    void testCenterXY() {
        Zone z = new Zone(1, 0, 0, 700, 600);

        assertEquals(350, z.centerX());
        assertEquals(300, z.centerY());
    }

    @Test
    void testCenterXYNonZeroOrigin() {
        Zone z = new Zone(2, 100, 200, 500, 800);

        assertEquals(300, z.centerX());
        assertEquals(500, z.centerY());
    }

    @Test
    void testCenterXYSmallZone() {
        Zone z = new Zone(3, 0, 0, 10, 10);

        assertEquals(5, z.centerX());
        assertEquals(5, z.centerY());
    }

    // ==================== isOddByOdd ====================

    @Test
    void testIsOddByOddTrue() {
        Zone z = new Zone(1, 0, 0, 701, 601);
        assertTrue(z.isOddByOdd());
    }

    @Test
    void testIsOddByOddFalseEvenWidth() {
        Zone z = new Zone(1, 0, 0, 700, 601);
        assertFalse(z.isOddByOdd());
    }

    @Test
    void testIsOddByOddFalseEvenHeight() {
        Zone z = new Zone(1, 0, 0, 701, 600);
        assertFalse(z.isOddByOdd());
    }

    @Test
    void testIsOddByOddFalseBothEven() {
        Zone z = new Zone(1, 0, 0, 700, 600);
        assertFalse(z.isOddByOdd());
    }

    // ==================== Multiple Zones ====================

    @Test
    void testMultipleZones() {
        Zone z1 = new Zone(1, 0, 0, 700, 600);
        Zone z2 = new Zone(2, 0, 600, 700, 1200);
        Zone z3 = new Zone(3, 700, 0, 1300, 600);

        assertEquals(350, z1.centerX());
        assertEquals(300, z1.centerY());

        assertEquals(350, z2.centerX());
        assertEquals(900, z2.centerY());

        assertEquals(1000, z3.centerX());
        assertEquals(300, z3.centerY());
    }
}
