import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.net.DatagramSocket;
import java.net.InetAddress;

public class UDPHelperTest {

    // ==================== Message Building Tests ====================

    @Test
    void testBuildFireEventMessage() {
        String msg = UDPHelper.buildFireEventMessage("14:03:15", 3, "FIRE_DETECTED", "HIGH", "NONE");
        assertEquals("FIRE_EVENT|14:03:15|3|FIRE_DETECTED|HIGH|NONE", msg);
    }

    @Test
    void testBuildFireEventMessageFromObject() {
        FireEvent event = new FireEvent("14:03:15", 3, FireEvent.EventType.FIRE_DETECTED, FireEvent.Severity.HIGH);
        String msg = UDPHelper.buildFireEventMessage(event);
        assertEquals("FIRE_EVENT|14:03:15|3|FIRE_DETECTED|HIGH|NONE", msg);
    }

    @Test
    void testBuildDroneRegisterMessage() {
        String msg = UDPHelper.buildDroneRegisterMessage(1, 30.0, 0, 0);
        assertEquals("DRONE_REGISTER|1|30.0|0|0", msg);
    }

    @Test
    void testBuildDroneStatusMessage() {
        String msg = UDPHelper.buildDroneStatusMessage(2, "EN_ROUTE", 150, 150, 20.0);
        assertEquals("DRONE_STATUS|2|EN_ROUTE|150|150|20.0", msg);
    }

    @Test
    void testBuildDroneCommandMessage() {
        String msg = UDPHelper.buildDroneCommandMessage(1, "TASK", 3, "HIGH");
        assertEquals("DRONE_COMMAND|1|TASK|3|HIGH|NONE", msg);
    }

    @Test
    void testBuildDroneResultMessage() {
        String msg = UDPHelper.buildDroneResultMessage(1, 3, true, 20.0);
        assertEquals("DRONE_RESULT|1|3|true|20.0", msg);
    }

    @Test
    void testBuildDroneResultMessageFromObject() {
        DroneResult result = new DroneResult(2, 5, true, 10.0);
        String msg = UDPHelper.buildDroneResultMessage(result);
        assertEquals("DRONE_RESULT|2|5|true|10.0", msg);
    }

    @Test
    void testBuildConfirmationMessage() {
        String msg = UDPHelper.buildConfirmationMessage(1, 3, true);
        assertEquals("CONFIRMATION|1|3|true", msg);
    }

    @Test
    void testBuildShutdownMessage() {
        assertEquals("SHUTDOWN", UDPHelper.buildShutdownMessage());
    }

    // ==================== Message Parsing Tests ====================

    @Test
    void testGetMessageType() {
        assertEquals("FIRE_EVENT", UDPHelper.getMessageType("FIRE_EVENT|14:03:15|3|FIRE_DETECTED|HIGH"));
        assertEquals("DRONE_COMMAND", UDPHelper.getMessageType("DRONE_COMMAND|1|TASK|3|HIGH"));
        assertEquals("SHUTDOWN", UDPHelper.getMessageType("SHUTDOWN"));
    }

    @Test
    void testParseFields() {
        String[] fields = UDPHelper.parseFields("FIRE_EVENT|14:03:15|3|FIRE_DETECTED|HIGH");
        assertEquals(5, fields.length);
        assertEquals("FIRE_EVENT", fields[0]);
        assertEquals("14:03:15", fields[1]);
        assertEquals("3", fields[2]);
        assertEquals("FIRE_DETECTED", fields[3]);
        assertEquals("HIGH", fields[4]);
    }

    @Test
    void testParseFireEvent() {
        String msg = "FIRE_EVENT|14:03:15|3|FIRE_DETECTED|HIGH";
        FireEvent event = UDPHelper.parseFireEvent(msg);

        assertEquals("14:03:15", event.getTime());
        assertEquals(3, event.getZoneId());
        assertEquals(FireEvent.EventType.FIRE_DETECTED, event.getEventType());
        assertEquals(FireEvent.Severity.HIGH, event.getSeverity());
        assertEquals(30, event.getLitersNeeded());
    }

    @Test
    void testParseDroneRegister() {
        String msg = "DRONE_REGISTER|2|30.0|10|20";
        assertEquals(2, UDPHelper.parseDroneRegisterId(msg));
        assertEquals(30.0, UDPHelper.parseDroneRegisterCapacity(msg));
        assertEquals(10, UDPHelper.parseDroneRegisterX(msg));
        assertEquals(20, UDPHelper.parseDroneRegisterY(msg));
    }

    @Test
    void testParseDroneStatus() {
        String msg = "DRONE_STATUS|1|EN_ROUTE|150|200|25.0";
        assertEquals(1, UDPHelper.parseDroneStatusId(msg));
        assertEquals("EN_ROUTE", UDPHelper.parseDroneStatusState(msg));
        assertEquals(150, UDPHelper.parseDroneStatusX(msg));
        assertEquals(200, UDPHelper.parseDroneStatusY(msg));
        assertEquals(25.0, UDPHelper.parseDroneStatusAgent(msg));
    }

    @Test
    void testParseDroneCommand() {
        String msg = "DRONE_COMMAND|1|TASK|3|HIGH";
        assertEquals(1, UDPHelper.parseDroneCommandId(msg));
        assertEquals("TASK", UDPHelper.parseDroneCommandType(msg));
        assertEquals(3, UDPHelper.parseDroneCommandZoneId(msg));
        assertEquals("HIGH", UDPHelper.parseDroneCommandSeverity(msg));
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
    void testParseConfirmation() {
        String msg = "CONFIRMATION|2|5|true";
        assertEquals(2, UDPHelper.parseConfirmationDroneId(msg));
        assertEquals(5, UDPHelper.parseConfirmationZoneId(msg));
        assertTrue(UDPHelper.parseConfirmationCompleted(msg));
    }

    @Test
    void testParseConfirmationFailed() {
        String msg = "CONFIRMATION|1|3|false";
        assertFalse(UDPHelper.parseConfirmationCompleted(msg));
    }

    // ==================== Port Helper Tests ====================

    @Test
    void testGetDroneListenPort() {
        assertEquals(6001, UDPHelper.getDroneListenPort(1));
        assertEquals(6002, UDPHelper.getDroneListenPort(2));
        assertEquals(6003, UDPHelper.getDroneListenPort(3));
    }

    // ==================== UDP Round-Trip Test ====================

    @Test
    void testUDPSendReceiveRoundTrip() throws Exception {
        DatagramSocket receiver = new DatagramSocket(0); // random available port
        int port = receiver.getLocalPort();
        receiver.setSoTimeout(3000);

        DatagramSocket sender = new DatagramSocket();

        String original = UDPHelper.buildFireEventMessage("14:03:15", 3, "FIRE_DETECTED", "HIGH", "NONE");

        InetAddress localhost = InetAddress.getByName("localhost");
        UDPHelper.sendMessage(sender, localhost, port, original);

        String received = UDPHelper.receiveMessage(receiver);
        assertEquals(original, received);

        // Parse the received message
        FireEvent event = UDPHelper.parseFireEvent(received);
        assertEquals(3, event.getZoneId());
        assertEquals(FireEvent.Severity.HIGH, event.getSeverity());

        sender.close();
        receiver.close();
    }

    @Test
    void testUDPDroneResultRoundTrip() throws Exception {
        DatagramSocket receiver = new DatagramSocket(0);
        int port = receiver.getLocalPort();
        receiver.setSoTimeout(3000);

        DatagramSocket sender = new DatagramSocket();

        DroneResult original = new DroneResult(1, 5, true, 15.0);
        String msg = UDPHelper.buildDroneResultMessage(original);

        UDPHelper.sendMessage(sender, InetAddress.getByName("localhost"), port, msg);

        String received = UDPHelper.receiveMessage(receiver);
        DroneResult parsed = UDPHelper.parseDroneResult(received);

        assertEquals(original.getDroneId(), parsed.getDroneId());
        assertEquals(original.getZoneId(), parsed.getZoneId());
        assertEquals(original.isTaskCompleted(), parsed.isTaskCompleted());
        assertEquals(original.getRemainingAgent(), parsed.getRemainingAgent());

        sender.close();
        receiver.close();
    }
}
