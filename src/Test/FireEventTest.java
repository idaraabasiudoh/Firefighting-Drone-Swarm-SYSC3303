package test;

import drone.FaultType;
import fireincident.FireEvent;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class FireEventTest {

    // ==================== Liters Needed ====================

    @Test
    void testLitersNeededLow() {
        FireEvent event = new FireEvent(
                "14:00:00",
                1,
                FireEvent.EventType.FIRE_DETECTED,
                FireEvent.Severity.LOW
        );

        assertEquals(10, event.getLitersNeeded());
    }

    @Test
    void testLitersNeededModerate() {
        FireEvent event = new FireEvent(
                "14:00:00",
                1,
                FireEvent.EventType.FIRE_DETECTED,
                FireEvent.Severity.MODERATE
        );

        assertEquals(20, event.getLitersNeeded());
    }

    @Test
    void testLitersNeededHigh() {
        FireEvent event = new FireEvent(
                "14:00:00",
                1,
                FireEvent.EventType.FIRE_DETECTED,
                FireEvent.Severity.HIGH
        );

        assertEquals(30, event.getLitersNeeded());
    }

    // ==================== Constructors ====================

    @Test
    void testConstructor4Args() {
        FireEvent event = new FireEvent("14:00:00", 3,
                FireEvent.EventType.FIRE_DETECTED, FireEvent.Severity.HIGH);

        assertEquals("14:00:00", event.getTime());
        assertEquals(3, event.getZoneId());
        assertEquals(FireEvent.EventType.FIRE_DETECTED, event.getEventType());
        assertEquals(FireEvent.Severity.HIGH, event.getSeverity());
        assertEquals(FaultType.NONE, event.getFaultType());
    }

    @Test
    void testConstructor5Args() {
        FireEvent event = new FireEvent("14:00:00", 3,
                FireEvent.EventType.FIRE_DETECTED, FireEvent.Severity.HIGH, FaultType.DRONE_STUCK);

        assertEquals(FaultType.DRONE_STUCK, event.getFaultType());
    }

    // ==================== Getters & Setters ====================

    @Test
    void testSetTime() {
        FireEvent event = new FireEvent("14:00:00", 1,
                FireEvent.EventType.FIRE_DETECTED, FireEvent.Severity.LOW);
        event.setTime("15:30:00");
        assertEquals("15:30:00", event.getTime());
    }

    @Test
    void testSetZoneId() {
        FireEvent event = new FireEvent("14:00:00", 1,
                FireEvent.EventType.FIRE_DETECTED, FireEvent.Severity.LOW);
        event.setZoneId(5);
        assertEquals(5, event.getZoneId());
    }

    @Test
    void testSetEventType() {
        FireEvent event = new FireEvent("14:00:00", 1,
                FireEvent.EventType.FIRE_DETECTED, FireEvent.Severity.LOW);
        event.setEventType(FireEvent.EventType.DRONE_REQUEST);
        assertEquals(FireEvent.EventType.DRONE_REQUEST, event.getEventType());
    }

    @Test
    void testSetSeverity() {
        FireEvent event = new FireEvent("14:00:00", 1,
                FireEvent.EventType.FIRE_DETECTED, FireEvent.Severity.LOW);
        event.setSeverity(FireEvent.Severity.HIGH);
        assertEquals(FireEvent.Severity.HIGH, event.getSeverity());
    }

    @Test
    void testSetFaultType() {
        FireEvent event = new FireEvent("14:00:00", 1,
                FireEvent.EventType.FIRE_DETECTED, FireEvent.Severity.LOW);
        event.setFaultType(FaultType.NOZZLE_STUCK);
        assertEquals(FaultType.NOZZLE_STUCK, event.getFaultType());
    }

    // ==================== Equals & HashCode ====================

    @Test
    void testEquals() {
        FireEvent e1 = new FireEvent("14:00:00", 1,
                FireEvent.EventType.FIRE_DETECTED,
                FireEvent.Severity.MODERATE);

        FireEvent e2 = new FireEvent("14:00:00", 1,
                FireEvent.EventType.FIRE_DETECTED,
                FireEvent.Severity.MODERATE);

        assertEquals(e1, e2);
    }

    @Test
    void testEqualsSameObject() {
        FireEvent e = new FireEvent("14:00:00", 1,
                FireEvent.EventType.FIRE_DETECTED, FireEvent.Severity.HIGH);
        assertEquals(e, e);
    }

    @Test
    void testNotEqualsDifferentZone() {
        FireEvent e1 = new FireEvent("14:00:00", 1,
                FireEvent.EventType.FIRE_DETECTED, FireEvent.Severity.HIGH);
        FireEvent e2 = new FireEvent("14:00:00", 2,
                FireEvent.EventType.FIRE_DETECTED, FireEvent.Severity.HIGH);
        assertNotEquals(e1, e2);
    }

    @Test
    void testNotEqualsDifferentSeverity() {
        FireEvent e1 = new FireEvent("14:00:00", 1,
                FireEvent.EventType.FIRE_DETECTED, FireEvent.Severity.LOW);
        FireEvent e2 = new FireEvent("14:00:00", 1,
                FireEvent.EventType.FIRE_DETECTED, FireEvent.Severity.HIGH);
        assertNotEquals(e1, e2);
    }

    @Test
    void testNotEqualsDifferentFault() {
        FireEvent e1 = new FireEvent("14:00:00", 1,
                FireEvent.EventType.FIRE_DETECTED, FireEvent.Severity.HIGH, FaultType.NONE);
        FireEvent e2 = new FireEvent("14:00:00", 1,
                FireEvent.EventType.FIRE_DETECTED, FireEvent.Severity.HIGH, FaultType.DRONE_STUCK);
        assertNotEquals(e1, e2);
    }

    @Test
    void testNotEqualsNull() {
        FireEvent e = new FireEvent("14:00:00", 1,
                FireEvent.EventType.FIRE_DETECTED, FireEvent.Severity.HIGH);
        assertNotEquals(null, e);
    }

    @Test
    void testNotEqualsDifferentClass() {
        FireEvent e = new FireEvent("14:00:00", 1,
                FireEvent.EventType.FIRE_DETECTED, FireEvent.Severity.HIGH);
        assertNotEquals("not a fire event", e);
    }

    @Test
    void testHashCodeConsistent() {
        FireEvent e1 = new FireEvent("14:00:00", 1,
                FireEvent.EventType.FIRE_DETECTED, FireEvent.Severity.HIGH);
        FireEvent e2 = new FireEvent("14:00:00", 1,
                FireEvent.EventType.FIRE_DETECTED, FireEvent.Severity.HIGH);
        assertEquals(e1.hashCode(), e2.hashCode());
    }

    // ==================== toString ====================

    @Test
    void testToString() {
        FireEvent event = new FireEvent("14:03:15", 3,
                FireEvent.EventType.FIRE_DETECTED, FireEvent.Severity.HIGH, FaultType.DRONE_STUCK);
        String str = event.toString();

        assertTrue(str.contains("14:03:15"));
        assertTrue(str.contains("zoneId=3"));
        assertTrue(str.contains("FIRE_DETECTED"));
        assertTrue(str.contains("HIGH"));
        assertTrue(str.contains("DRONE_STUCK"));
        assertTrue(str.contains("litersNeeded=30"));
    }

    // ==================== Enum Coverage ====================

    @Test
    void testEventTypeValues() {
        assertEquals(2, FireEvent.EventType.values().length);
        assertNotNull(FireEvent.EventType.valueOf("FIRE_DETECTED"));
        assertNotNull(FireEvent.EventType.valueOf("DRONE_REQUEST"));
    }

    @Test
    void testSeverityValues() {
        assertEquals(3, FireEvent.Severity.values().length);
        assertNotNull(FireEvent.Severity.valueOf("LOW"));
        assertNotNull(FireEvent.Severity.valueOf("MODERATE"));
        assertNotNull(FireEvent.Severity.valueOf("HIGH"));
    }
}
