import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.net.InetAddress;

public class DroneSubsystemTest {

    @Test
    void testDroneInitialState() throws Exception {
        InetAddress localhost = InetAddress.getByName("localhost");
        DroneSubsystem drone = new DroneSubsystem(1, localhost, 30.0);

        assertNotNull(drone);
        assertEquals(1, drone.getDroneId());
        assertEquals(DroneState.IDLE, drone.getState());
        assertEquals(30.0, drone.getCurrentAgent());
    }

    @Test
    void testDroneDefaultCapacity() throws Exception {
        InetAddress localhost = InetAddress.getByName("localhost");
        DroneSubsystem drone = new DroneSubsystem(2, localhost);

        assertEquals(2, drone.getDroneId());
        assertEquals(30.0, drone.getCurrentAgent());
    }

    @Test
    void testDroneResultCreation() {
        DroneResult result = new DroneResult(1, 2, true, 20.0);

        assertEquals(1, result.getDroneId());
        assertEquals(2, result.getZoneId());
        assertTrue(result.isTaskCompleted());
        assertEquals(20.0, result.getRemainingAgent());
    }

    @Test
    void testDroneResultSetters() {
        DroneResult result = new DroneResult(1, 2, true, 20.0);

        result.setDroneId(3);
        result.setZoneId(5);
        result.setTaskCompleted(false);
        result.setRemainingAgent(10.0);

        assertEquals(3, result.getDroneId());
        assertEquals(5, result.getZoneId());
        assertFalse(result.isTaskCompleted());
        assertEquals(10.0, result.getRemainingAgent());
    }

    @Test
    void testDroneResultToString() {
        DroneResult result = new DroneResult(1, 3, true, 15.0);
        String str = result.toString();
        assertTrue(str.contains("DroneID=1"));
        assertTrue(str.contains("Zone=3"));
        assertTrue(str.contains("Completed=true"));
        assertTrue(str.contains("15.0"));
    }

    @Test
    void testLitersForSeverity() {
        assertEquals(10, DroneSubsystem.litersForSeverity("LOW"));
        assertEquals(20, DroneSubsystem.litersForSeverity("MODERATE"));
        assertEquals(30, DroneSubsystem.litersForSeverity("HIGH"));
        assertEquals(0, DroneSubsystem.litersForSeverity("UNKNOWN"));
    }

    @Test
    void testLitersForSeverityCaseInsensitive() {
        assertEquals(10, DroneSubsystem.litersForSeverity("low"));
        assertEquals(20, DroneSubsystem.litersForSeverity("Moderate"));
        assertEquals(30, DroneSubsystem.litersForSeverity("high"));
    }

    @Test
    void testDroneCommandRecord() {
        FireEvent event = new FireEvent("14:00:00", 3,
                FireEvent.EventType.FIRE_DETECTED, FireEvent.Severity.HIGH);

        DroneCommand taskCmd = DroneCommand.task(event);
        assertEquals(DroneCommand.Type.TASK, taskCmd.type());
        assertEquals(event, taskCmd.task());

        DroneCommand returnCmd = DroneCommand.returnBase();
        assertEquals(DroneCommand.Type.RETURN_BASE, returnCmd.type());
        assertNull(returnCmd.task());

        DroneCommand shutdownCmd = DroneCommand.shutdown();
        assertEquals(DroneCommand.Type.SHUTDOWN, shutdownCmd.type());
        assertNull(shutdownCmd.task());
    }

    @Test
    void testDroneStateEnum() {
        assertEquals(5, DroneState.values().length);
        assertEquals(DroneState.IDLE, DroneState.valueOf("IDLE"));
        assertEquals(DroneState.EN_ROUTE, DroneState.valueOf("EN_ROUTE"));
        assertEquals(DroneState.DROPPING_AGENT, DroneState.valueOf("DROPPING_AGENT"));
        assertEquals(DroneState.RETURNING_BASE, DroneState.valueOf("RETURNING_BASE"));
        assertEquals(DroneState.SHUTDOWN, DroneState.valueOf("SHUTDOWN"));
    }
}
