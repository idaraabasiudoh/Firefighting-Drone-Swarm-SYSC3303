package test;

import drone.FaultType;
import fireincident.FireEvent;
import fireincident.FireIncidentSubsystem;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class FireIncidentSubsystemTest {

    // ==================== parseLineToFireEvent ====================

    @Test
    void testParseLineBasic() {
        String line = "14:03:15,3,FIRE_DETECTED,High,NONE";
        FireEvent event = FireIncidentSubsystem.parseLineToFireEvent(line);

        assertEquals("14:03:15", event.getTime());
        assertEquals(3, event.getZoneId());
        assertEquals(FireEvent.EventType.FIRE_DETECTED, event.getEventType());
        assertEquals(FireEvent.Severity.HIGH, event.getSeverity());
        assertEquals(FaultType.NONE, event.getFaultType());
    }

    @Test
    void testParseLineLowSeverity() {
        String line = "14:03:20,1,FIRE_DETECTED,Low,DRONE_STUCK";
        FireEvent event = FireIncidentSubsystem.parseLineToFireEvent(line);

        assertEquals("14:03:20", event.getTime());
        assertEquals(1, event.getZoneId());
        assertEquals(FireEvent.Severity.LOW, event.getSeverity());
        assertEquals(FaultType.DRONE_STUCK, event.getFaultType());
    }

    @Test
    void testParseLineModerateSeverity() {
        String line = "14:05:45,5,DRONE_REQUEST,Moderate,NONE";
        FireEvent event = FireIncidentSubsystem.parseLineToFireEvent(line);

        assertEquals(5, event.getZoneId());
        assertEquals(FireEvent.EventType.DRONE_REQUEST, event.getEventType());
        assertEquals(FireEvent.Severity.MODERATE, event.getSeverity());
        assertEquals(FaultType.NONE, event.getFaultType());
    }

    @Test
    void testParseLineNozzleStuck() {
        String line = "14:06:00,7,FIRE_DETECTED,High,NOZZLE_STUCK";
        FireEvent event = FireIncidentSubsystem.parseLineToFireEvent(line);

        assertEquals(7, event.getZoneId());
        assertEquals(FireEvent.Severity.HIGH, event.getSeverity());
        assertEquals(FaultType.NOZZLE_STUCK, event.getFaultType());
    }

    @Test
    void testParseLineSensorFail() {
        String line = "14:10:00,4,FIRE_DETECTED,Moderate,SENSOR_FAIL";
        FireEvent event = FireIncidentSubsystem.parseLineToFireEvent(line);

        assertEquals(4, event.getZoneId());
        assertEquals(FireEvent.Severity.MODERATE, event.getSeverity());
        assertEquals(FaultType.SENSOR_FAIL, event.getFaultType());
    }

    @Test
    void testParseLineNoFaultColumn() {
        String line = "14:08:30,2,FIRE_DETECTED,Low";
        FireEvent event = FireIncidentSubsystem.parseLineToFireEvent(line);

        assertEquals(2, event.getZoneId());
        assertEquals(FireEvent.Severity.LOW, event.getSeverity());
        assertEquals(FaultType.NONE, event.getFaultType());
    }

    @Test
    void testParseLineWithSpaces() {
        String line = "  14:03:15 , 3 , FIRE_DETECTED , High , NONE  ";
        FireEvent event = FireIncidentSubsystem.parseLineToFireEvent(line);

        assertEquals("14:03:15", event.getTime());
        assertEquals(3, event.getZoneId());
        assertEquals(FireEvent.Severity.HIGH, event.getSeverity());
    }

    @Test
    void testParseLineCaseInsensitiveSeverity() {
        String line = "14:00:00,1,FIRE_DETECTED,high,NONE";
        FireEvent event = FireIncidentSubsystem.parseLineToFireEvent(line);
        assertEquals(FireEvent.Severity.HIGH, event.getSeverity());
    }

    @Test
    void testParseLineCaseInsensitiveEventType() {
        String line = "14:00:00,1,fire_detected,High,NONE";
        FireEvent event = FireIncidentSubsystem.parseLineToFireEvent(line);
        assertEquals(FireEvent.EventType.FIRE_DETECTED, event.getEventType());
    }

    // ==================== All CSV rows from fire_events.csv ====================

    @Test
    void testParseAllCsvRows() {
        String[] lines = {
                "14:03:15,3,FIRE_DETECTED,High,NONE",
                "14:03:20,1,FIRE_DETECTED,Low,DRONE_STUCK",
                "14:05:45,5,DRONE_REQUEST,Moderate,NONE",
                "14:06:00,7,FIRE_DETECTED,High,NOZZLE_STUCK",
                "14:08:30,2,FIRE_DETECTED,Low,NONE",
                "14:10:00,4,FIRE_DETECTED,Moderate,SENSOR_FAIL",
                "14:12:15,6,FIRE_DETECTED,High,NONE"
        };

        for (String line : lines) {
            FireEvent event = FireIncidentSubsystem.parseLineToFireEvent(line);
            assertNotNull(event);
            assertNotNull(event.getTime());
            assertTrue(event.getZoneId() >= 1 && event.getZoneId() <= 7);
            assertNotNull(event.getEventType());
            assertNotNull(event.getSeverity());
            assertNotNull(event.getFaultType());
        }
    }

    @Test
    void testParseLineLitersNeededMatchesSeverity() {
        FireEvent low = FireIncidentSubsystem.parseLineToFireEvent("14:00:00,1,FIRE_DETECTED,Low,NONE");
        FireEvent mod = FireIncidentSubsystem.parseLineToFireEvent("14:00:00,1,FIRE_DETECTED,Moderate,NONE");
        FireEvent high = FireIncidentSubsystem.parseLineToFireEvent("14:00:00,1,FIRE_DETECTED,High,NONE");

        assertEquals(10, low.getLitersNeeded());
        assertEquals(20, mod.getLitersNeeded());
        assertEquals(30, high.getLitersNeeded());
    }
}
