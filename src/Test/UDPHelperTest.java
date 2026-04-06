package test;

import drone.DroneResult;
import drone.FaultType;
import fireincident.FireEvent;
import network.UDPHelper;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class UDPHelperTest {

    // ==================== Port Constants ====================

    @Test
    void testPortConstants() {
        assertEquals(5000, UDPHelper.FIRE_TO_SCHEDULER_PORT);
        assertEquals(5001, UDPHelper.DRONE_TO_SCHEDULER_PORT);
        assertEquals(5002, UDPHelper.SCHEDULER_TO_FIRE_PORT);
    }

    @Test
    void testGetDroneListenPort() {
        assertEquals(6001, UDPHelper.getDroneListenPort(1));
        assertEquals(6002, UDPHelper.getDroneListenPort(2));
        assertEquals(6010, UDPHelper.getDroneListenPort(10));
    }

    // ==================== Message Type Constants ====================

    @Test
    void testMessageTypeConstants() {
        assertEquals("FIRE_EVENT", UDPHelper.MSG_FIRE_EVENT);
        assertEquals("DRONE_REGISTER", UDPHelper.MSG_DRONE_REGISTER);
        assertEquals("DRONE_COMMAND", UDPHelper.MSG_DRONE_COMMAND);
        assertEquals("DRONE_STATUS", UDPHelper.MSG_DRONE_STATUS);
        assertEquals("DRONE_RESULT", UDPHelper.MSG_DRONE_RESULT);
        assertEquals("DRONE_FAULT", UDPHelper.MSG_DRONE_FAULT);
        assertEquals("CONFIRMATION", UDPHelper.MSG_CONFIRMATION);
        assertEquals("SHUTDOWN", UDPHelper.MSG_SHUTDOWN);
    }

    // ==================== getMessageType ====================

    @Test
    void testGetMessageType() {
        assertEquals("FIRE_EVENT", UDPHelper.getMessageType("FIRE_EVENT|14:03:15|3|FIRE_DETECTED|HIGH|NONE"));
        assertEquals("DRONE_REGISTER", UDPHelper.getMessageType("DRONE_REGISTER|1|30.0|0|0"));
        assertEquals("SHUTDOWN", UDPHelper.getMessageType("SHUTDOWN"));
    }

    @Test
    void testGetMessageTypeNoPipe() {
        assertEquals("SHUTDOWN", UDPHelper.getMessageType("SHUTDOWN"));
        assertEquals("UNKNOWN", UDPHelper.getMessageType("UNKNOWN"));
    }

    // ==================== Fire Event Messages ====================

    @Test
    void testBuildFireEventMessageFromObject() {
        FireEvent event = new FireEvent("14:03:15", 3, FireEvent.EventType.FIRE_DETECTED, FireEvent.Severity.HIGH);
        String msg = UDPHelper.buildFireEventMessage(event);
        assertEquals("FIRE_EVENT|14:03:15|3|FIRE_DETECTED|HIGH|NONE", msg);
    }

    @Test
    void testBuildFireEventMessageWithFault() {
        FireEvent event = new FireEvent("14:03:15", 3, FireEvent.EventType.FIRE_DETECTED,
                FireEvent.Severity.HIGH, FaultType.DRONE_STUCK);
        String msg = UDPHelper.buildFireEventMessage(event);
        assertEquals("FIRE_EVENT|14:03:15|3|FIRE_DETECTED|HIGH|DRONE_STUCK", msg);
    }

    @Test
    void testParseFireEvent() {
        String msg = "FIRE_EVENT|14:03:15|3|FIRE_DETECTED|HIGH|NONE";
        FireEvent event = UDPHelper.parseFireEvent(msg);

        assertEquals("14:03:15", event.getTime());
        assertEquals(3, event.getZoneId());
        assertEquals(FireEvent.EventType.FIRE_DETECTED, event.getEventType());
        assertEquals(FireEvent.Severity.HIGH, event.getSeverity());
        assertEquals(FaultType.NONE, event.getFaultType());
    }

    @Test
    void testParseFireEventWithFault() {
        String msg = "FIRE_EVENT|14:03:15|3|FIRE_DETECTED|HIGH|NOZZLE_STUCK";
        FireEvent event = UDPHelper.parseFireEvent(msg);

        assertEquals(FaultType.NOZZLE_STUCK, event.getFaultType());
    }

    @Test
    void testParseFireEventNoFaultField() {
        String msg = "FIRE_EVENT|14:03:15|3|FIRE_DETECTED|HIGH";
        FireEvent event = UDPHelper.parseFireEvent(msg);

        assertEquals(FaultType.NONE, event.getFaultType());
    }

    @Test
    void testFireEventRoundTrip() {
        FireEvent original = new FireEvent("14:05:00", 5, FireEvent.EventType.DRONE_REQUEST,
                FireEvent.Severity.MODERATE, FaultType.SENSOR_FAIL);
        String msg = UDPHelper.buildFireEventMessage(original);
        FireEvent parsed = UDPHelper.parseFireEvent(msg);

        assertEquals(original.getTime(), parsed.getTime());
        assertEquals(original.getZoneId(), parsed.getZoneId());
        assertEquals(original.getEventType(), parsed.getEventType());
        assertEquals(original.getSeverity(), parsed.getSeverity());
        assertEquals(original.getFaultType(), parsed.getFaultType());
    }

    // ==================== Drone Register Messages ====================

    @Test
    void testBuildDroneRegisterMessage() {
        String msg = UDPHelper.buildDroneRegisterMessage(1, 30.0, 0, 0);
        assertEquals("DRONE_REGISTER|1|30.0|0|0", msg);
    }

    @Test
    void testParseDroneRegisterFields() {
        String msg = "DRONE_REGISTER|2|25.0|100|200";

        assertEquals(2, UDPHelper.parseDroneRegisterId(msg));
        assertEquals(25.0, UDPHelper.parseDroneRegisterCapacity(msg));
        assertEquals(100, UDPHelper.parseDroneRegisterX(msg));
        assertEquals(200, UDPHelper.parseDroneRegisterY(msg));
    }

    @Test
    void testDroneRegisterRoundTrip() {
        String msg = UDPHelper.buildDroneRegisterMessage(3, 15.5, 50, 75);

        assertEquals(3, UDPHelper.parseDroneRegisterId(msg));
        assertEquals(15.5, UDPHelper.parseDroneRegisterCapacity(msg));
        assertEquals(50, UDPHelper.parseDroneRegisterX(msg));
        assertEquals(75, UDPHelper.parseDroneRegisterY(msg));
    }

    // ==================== Drone Command Messages ====================

    @Test
    void testBuildDroneCommandMessage4Args() {
        String msg = UDPHelper.buildDroneCommandMessage(1, "TASK", 3, "HIGH");
        assertEquals("DRONE_COMMAND|1|TASK|3|HIGH|NONE", msg);
    }

    @Test
    void testBuildDroneCommandMessage5Args() {
        String msg = UDPHelper.buildDroneCommandMessage(1, "TASK", 3, "HIGH", "DRONE_STUCK");
        assertEquals("DRONE_COMMAND|1|TASK|3|HIGH|DRONE_STUCK", msg);
    }

    @Test
    void testParseDroneCommandFields() {
        String msg = "DRONE_COMMAND|2|REDIRECT|5|MODERATE|NONE";

        assertEquals("REDIRECT", UDPHelper.parseDroneCommandType(msg));
        assertEquals(5, UDPHelper.parseDroneCommandZoneId(msg));
        assertEquals("MODERATE", UDPHelper.parseDroneCommandSeverity(msg));
        assertEquals("NONE", UDPHelper.parseDroneCommandFault(msg));
    }

    @Test
    void testParseDroneCommandFaultPresent() {
        String msg = "DRONE_COMMAND|1|TASK|3|HIGH|NOZZLE_STUCK";
        assertEquals("NOZZLE_STUCK", UDPHelper.parseDroneCommandFault(msg));
    }

    @Test
    void testParseDroneCommandFaultMissing() {
        String msg = "DRONE_COMMAND|1|TASK|3|HIGH";
        assertEquals("NONE", UDPHelper.parseDroneCommandFault(msg));
    }

    @Test
    void testBuildReturnBaseCommand() {
        String msg = UDPHelper.buildDroneCommandMessage(1, "RETURN_BASE", 0, "NONE");
        assertEquals("DRONE_COMMAND|1|RETURN_BASE|0|NONE|NONE", msg);
    }

    @Test
    void testBuildShutdownCommand() {
        String msg = UDPHelper.buildDroneCommandMessage(1, "SHUTDOWN", 0, "NONE");
        assertEquals("DRONE_COMMAND|1|SHUTDOWN|0|NONE|NONE", msg);
    }

    // ==================== Drone Status Messages ====================

    @Test
    void testBuildDroneStatusMessage() {
        String msg = UDPHelper.buildDroneStatusMessage(1, "EN_ROUTE", 100, 200, 25.0);
        assertEquals("DRONE_STATUS|1|EN_ROUTE|100|200|25.0", msg);
    }

    @Test
    void testParseDroneStatusFields() {
        String msg = "DRONE_STATUS|2|DROPPING_AGENT|350|300|15.0";

        assertEquals(2, UDPHelper.parseDroneStatusId(msg));
        assertEquals("DROPPING_AGENT", UDPHelper.parseDroneStatusState(msg));
        assertEquals(350, UDPHelper.parseDroneStatusX(msg));
        assertEquals(300, UDPHelper.parseDroneStatusY(msg));
        assertEquals(15.0, UDPHelper.parseDroneStatusAgent(msg));
    }

    @Test
    void testDroneStatusRoundTrip() {
        String msg = UDPHelper.buildDroneStatusMessage(3, "IDLE", 0, 0, 30.0);

        assertEquals(3, UDPHelper.parseDroneStatusId(msg));
        assertEquals("IDLE", UDPHelper.parseDroneStatusState(msg));
        assertEquals(0, UDPHelper.parseDroneStatusX(msg));
        assertEquals(0, UDPHelper.parseDroneStatusY(msg));
        assertEquals(30.0, UDPHelper.parseDroneStatusAgent(msg));
    }

    // ==================== Drone Result Messages ====================

    @Test
    void testBuildDroneResultMessage() {
        String msg = UDPHelper.buildDroneResultMessage(1, 3, true, 20.0);
        assertEquals("DRONE_RESULT|1|3|true|20.0", msg);
    }

    @Test
    void testBuildDroneResultMessageFailed() {
        String msg = UDPHelper.buildDroneResultMessage(2, 5, false, 10.0);
        assertEquals("DRONE_RESULT|2|5|false|10.0", msg);
    }

    @Test
    void testParseDroneResult() {
        String msg = "DRONE_RESULT|1|3|true|20.0";
        DroneResult result = UDPHelper.parseDroneResult(msg);

        assertEquals(1, result.getDroneId());
        assertEquals(3, result.getZoneId());
        assertTrue(result.isTaskCompleted());
        assertEquals(20.0, result.getRemainingAgent());
    }

    @Test
    void testDroneResultRoundTrip() {
        String msg = UDPHelper.buildDroneResultMessage(4, 7, false, 5.5);
        DroneResult result = UDPHelper.parseDroneResult(msg);

        assertEquals(4, result.getDroneId());
        assertEquals(7, result.getZoneId());
        assertFalse(result.isTaskCompleted());
        assertEquals(5.5, result.getRemainingAgent());
    }

    // ==================== Drone Fault Messages ====================

    @Test
    void testBuildDroneFaultMessage() {
        String msg = UDPHelper.buildDroneFaultMessage(1, "DRONE_STUCK", 3);
        assertEquals("DRONE_FAULT|1|DRONE_STUCK|3", msg);
    }

    @Test
    void testParseDroneFaultFields() {
        String msg = "DRONE_FAULT|2|NOZZLE_STUCK|5";

        assertEquals(2, UDPHelper.parseDroneFaultDroneId(msg));
        assertEquals("NOZZLE_STUCK", UDPHelper.parseDroneFaultType(msg));
        assertEquals(5, UDPHelper.parseDroneFaultZoneId(msg));
    }

    @Test
    void testDroneFaultRoundTrip() {
        String msg = UDPHelper.buildDroneFaultMessage(3, "SENSOR_FAIL", 7);

        assertEquals(3, UDPHelper.parseDroneFaultDroneId(msg));
        assertEquals("SENSOR_FAIL", UDPHelper.parseDroneFaultType(msg));
        assertEquals(7, UDPHelper.parseDroneFaultZoneId(msg));
    }

    // ==================== Confirmation Messages ====================

    @Test
    void testBuildConfirmationMessage() {
        String msg = UDPHelper.buildConfirmationMessage(1, 3, true);
        assertEquals("CONFIRMATION|1|3|true", msg);
    }

    @Test
    void testParseConfirmationFields() {
        String msg = "CONFIRMATION|2|5|false";

        assertEquals(5, UDPHelper.parseConfirmationZoneId(msg));
        assertFalse(UDPHelper.parseConfirmationCompleted(msg));
    }

    @Test
    void testConfirmationRoundTrip() {
        String msg = UDPHelper.buildConfirmationMessage(1, 4, true);

        assertEquals(4, UDPHelper.parseConfirmationZoneId(msg));
        assertTrue(UDPHelper.parseConfirmationCompleted(msg));
    }

    // ==================== Shutdown Message ====================

    @Test
    void testBuildShutdownMessage() {
        assertEquals("SHUTDOWN", UDPHelper.buildShutdownMessage());
    }

    @Test
    void testShutdownMessageType() {
        String msg = UDPHelper.buildShutdownMessage();
        assertEquals("SHUTDOWN", UDPHelper.getMessageType(msg));
    }

    // ==================== Timestamp ====================

    @Test
    void testTimestampFormat() {
        String ts = UDPHelper.timestamp();
        assertNotNull(ts);
        assertTrue(ts.matches("\\d{2}:\\d{2}:\\d{2}"), "Timestamp should match HH:mm:ss format");
    }
}
