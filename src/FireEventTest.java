public class FireEventTest {

    public void testConstructorAndGetters() {
        FireEvent e = new FireEvent("14:03:15.000", 3, FireEvent.EventType.FIRE_DETECTED, FireEvent.Severity.HIGH);

        TestRunner.assertEquals("14:03:15.000", e.getTime(), "Time getter failed");
        TestRunner.assertEquals(3, e.getZoneId(), "ZoneId getter failed");
        TestRunner.assertEquals(FireEvent.EventType.FIRE_DETECTED, e.getEventType(), "EventType getter failed");
        TestRunner.assertEquals(FireEvent.Severity.HIGH, e.getSeverity(), "Severity getter failed");
    }

    public void testLitersNeededMapping() {
        FireEvent low = new FireEvent("00:00:01.000", 1, FireEvent.EventType.FIRE_DETECTED, FireEvent.Severity.LOW);
        FireEvent mod = new FireEvent("00:00:02.000", 1, FireEvent.EventType.FIRE_DETECTED, FireEvent.Severity.MODERATE);
        FireEvent high = new FireEvent("00:00:03.000", 1, FireEvent.EventType.FIRE_DETECTED, FireEvent.Severity.HIGH);

        TestRunner.assertEquals(10, low.getLitersNeeded(), "LOW should require 10L");
        TestRunner.assertEquals(20, mod.getLitersNeeded(), "MODERATE should require 20L");
        TestRunner.assertEquals(30, high.getLitersNeeded(), "HIGH should require 30L");
    }

    public void testSetters() {
        FireEvent e = new FireEvent("10:00:00.000", 1, FireEvent.EventType.FIRE_DETECTED, FireEvent.Severity.LOW);

        e.setTime("10:00:05.000");
        e.setZoneId(9);
        e.setEventType(FireEvent.EventType.DRONE_REQUEST);
        e.setSeverity(FireEvent.Severity.HIGH);

        TestRunner.assertEquals("10:00:05.000", e.getTime(), "setTime failed");
        TestRunner.assertEquals(9, e.getZoneId(), "setZoneId failed");
        TestRunner.assertEquals(FireEvent.EventType.DRONE_REQUEST, e.getEventType(), "setEventType failed");
        TestRunner.assertEquals(FireEvent.Severity.HIGH, e.getSeverity(), "setSeverity failed");

        // Also confirm liters update after severity change
        TestRunner.assertEquals(30, e.getLitersNeeded(), "Liters should update when severity changes");
    }

    public void testEqualsAndHashCode() {
        FireEvent a = new FireEvent("10:00:00.000", 1, FireEvent.EventType.FIRE_DETECTED, FireEvent.Severity.HIGH);
        FireEvent b = new FireEvent("10:00:00.000", 1, FireEvent.EventType.FIRE_DETECTED, FireEvent.Severity.HIGH);
        FireEvent c = new FireEvent("10:00:01.000", 1, FireEvent.EventType.FIRE_DETECTED, FireEvent.Severity.HIGH);
        FireEvent d = new FireEvent("10:00:00.000", 2, FireEvent.EventType.FIRE_DETECTED, FireEvent.Severity.HIGH);
        FireEvent e = new FireEvent("10:00:00.000", 1, FireEvent.EventType.DRONE_REQUEST, FireEvent.Severity.HIGH);
        FireEvent f = new FireEvent("10:00:00.000", 1, FireEvent.EventType.FIRE_DETECTED, FireEvent.Severity.LOW);

        TestRunner.assertEquals(true, a.equals(b), "Equal events should be equal");
        TestRunner.assertEquals(a.hashCode(), b.hashCode(), "Equal events should have same hashCode");

        TestRunner.assertEquals(false, a.equals(c), "Different time should not be equal");
        TestRunner.assertEquals(false, a.equals(d), "Different zoneId should not be equal");
        TestRunner.assertEquals(false, a.equals(e), "Different eventType should not be equal");
        TestRunner.assertEquals(false, a.equals(f), "Different severity should not be equal");

        TestRunner.assertEquals(false, a.equals(null), "Should not be equal to null");
        TestRunner.assertEquals(false, a.equals("not a FireEvent"), "Should not be equal to a different type");
    }
}
