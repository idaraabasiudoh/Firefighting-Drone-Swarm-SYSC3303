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
        // Iteration 4: 9 states (added FAULT_STUCK, FAULT_NOZZLE, FAULT_SENSOR, OFFLINE)
        assertEquals(9, DroneState.values().length);
        assertEquals(DroneState.IDLE, DroneState.valueOf("IDLE"));
        assertEquals(DroneState.EN_ROUTE, DroneState.valueOf("EN_ROUTE"));
        assertEquals(DroneState.DROPPING_AGENT, DroneState.valueOf("DROPPING_AGENT"));
        assertEquals(DroneState.RETURNING_BASE, DroneState.valueOf("RETURNING_BASE"));
        assertEquals(DroneState.FAULT_STUCK, DroneState.valueOf("FAULT_STUCK"));
        assertEquals(DroneState.FAULT_NOZZLE, DroneState.valueOf("FAULT_NOZZLE"));
        assertEquals(DroneState.FAULT_SENSOR, DroneState.valueOf("FAULT_SENSOR"));
        assertEquals(DroneState.OFFLINE, DroneState.valueOf("OFFLINE"));
        assertEquals(DroneState.SHUTDOWN, DroneState.valueOf("SHUTDOWN"));
    }

    // ==================== Iteration 4: Fault Type Tests ====================

    @Test
    void testFaultTypeEnum() {
        assertEquals(4, FaultType.values().length);
        assertEquals(FaultType.NONE, FaultType.valueOf("NONE"));
        assertEquals(FaultType.DRONE_STUCK, FaultType.valueOf("DRONE_STUCK"));
        assertEquals(FaultType.NOZZLE_STUCK, FaultType.valueOf("NOZZLE_STUCK"));
        assertEquals(FaultType.SENSOR_FAIL, FaultType.valueOf("SENSOR_FAIL"));
    }

    @Test
    void testFaultTypeHardSoft() {
        assertFalse(FaultType.NONE.isHardFault());
        assertFalse(FaultType.NONE.isSoftFault());

        assertFalse(FaultType.DRONE_STUCK.isHardFault());
        assertTrue(FaultType.DRONE_STUCK.isSoftFault());

        assertTrue(FaultType.NOZZLE_STUCK.isHardFault());
        assertFalse(FaultType.NOZZLE_STUCK.isSoftFault());

        assertFalse(FaultType.SENSOR_FAIL.isHardFault());
        assertTrue(FaultType.SENSOR_FAIL.isSoftFault());
    }

    @Test
    void testFaultTypeFromString() {
        assertEquals(FaultType.NONE, FaultType.fromString(null));
        assertEquals(FaultType.NONE, FaultType.fromString(""));
        assertEquals(FaultType.NONE, FaultType.fromString("  "));
        assertEquals(FaultType.NONE, FaultType.fromString("INVALID"));
        assertEquals(FaultType.DRONE_STUCK, FaultType.fromString("DRONE_STUCK"));
        assertEquals(FaultType.DRONE_STUCK, FaultType.fromString("drone_stuck"));
        assertEquals(FaultType.NOZZLE_STUCK, FaultType.fromString("NOZZLE_STUCK"));
        assertEquals(FaultType.SENSOR_FAIL, FaultType.fromString("sensor_fail"));
    }

    @Test
    void testFireEventWithFaultType() {
        FireEvent event = new FireEvent("14:00:00", 3,
                FireEvent.EventType.FIRE_DETECTED, FireEvent.Severity.HIGH, FaultType.DRONE_STUCK);
        assertEquals(FaultType.DRONE_STUCK, event.getFaultType());

        event.setFaultType(FaultType.NOZZLE_STUCK);
        assertEquals(FaultType.NOZZLE_STUCK, event.getFaultType());
    }

    @Test
    void testFireEventDefaultFaultType() {
        FireEvent event = new FireEvent("14:00:00", 3,
                FireEvent.EventType.FIRE_DETECTED, FireEvent.Severity.HIGH);
        assertEquals(FaultType.NONE, event.getFaultType());
    }
}
