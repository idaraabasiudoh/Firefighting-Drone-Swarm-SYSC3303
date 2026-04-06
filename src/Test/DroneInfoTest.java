package test;

import drone.DroneInfo;
import drone.DroneState;
import drone.FaultType;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class DroneInfoTest {

    // ==================== Construction & Defaults ====================

    @Test
    void testConstructorDefaults() {
        DroneInfo info = new DroneInfo(1, 30.0, 0, 0, 6001);

        assertEquals(1, info.getDroneId());
        assertEquals(30.0, info.getAgentCapacity());
        assertEquals(30.0, info.getRemainingAgent());
        assertEquals(0, info.getCurrentX());
        assertEquals(0, info.getCurrentY());
        assertEquals(DroneState.IDLE, info.getState());
        assertEquals(-1, info.getAssignedZoneId());
        assertNull(info.getAssignedSeverity());
        assertEquals(0, info.getTasksCompleted());
        assertEquals(6001, info.getListenPort());
        assertEquals(0, info.getDispatchTimestamp());
        assertEquals(FaultType.NONE, info.getCurrentFault());
        assertEquals(0, info.getFaultCount());
        assertFalse(info.isPermanentlyOffline());
    }

    @Test
    void testConstructorCustomPosition() {
        DroneInfo info = new DroneInfo(2, 25.0, 100, 200, 6002);

        assertEquals(2, info.getDroneId());
        assertEquals(25.0, info.getAgentCapacity());
        assertEquals(100, info.getCurrentX());
        assertEquals(200, info.getCurrentY());
    }

    // ==================== State Management ====================

    @Test
    void testSetState() {
        DroneInfo info = new DroneInfo(1, 30.0, 0, 0, 6001);

        info.setState(DroneState.EN_ROUTE);
        assertEquals(DroneState.EN_ROUTE, info.getState());

        info.setState(DroneState.DROPPING_AGENT);
        assertEquals(DroneState.DROPPING_AGENT, info.getState());
    }

    @Test
    void testIsIdle() {
        DroneInfo info = new DroneInfo(1, 30.0, 0, 0, 6001);
        assertTrue(info.isIdle());

        info.setState(DroneState.EN_ROUTE);
        assertFalse(info.isIdle());

        info.setState(DroneState.IDLE);
        assertTrue(info.isIdle());
    }

    // ==================== Availability ====================

    @Test
    void testIsAvailableWhenIdle() {
        DroneInfo info = new DroneInfo(1, 30.0, 0, 0, 6001);
        assertTrue(info.isAvailable());
    }

    @Test
    void testIsAvailableWhenBusy() {
        DroneInfo info = new DroneInfo(1, 30.0, 0, 0, 6001);
        info.setState(DroneState.EN_ROUTE);
        assertFalse(info.isAvailable());
    }

    @Test
    void testIsAvailableWhenOffline() {
        DroneInfo info = new DroneInfo(1, 30.0, 0, 0, 6001);
        info.setPermanentlyOffline(true);
        assertFalse(info.isAvailable());
    }

    @Test
    void testIsAvailableWhenOfflineAndIdle() {
        DroneInfo info = new DroneInfo(1, 30.0, 0, 0, 6001);
        info.setPermanentlyOffline(true);
        info.setState(DroneState.IDLE);
        assertFalse(info.isAvailable());
    }

    // ==================== Agent Capacity ====================

    @Test
    void testHasEnoughAgent() {
        DroneInfo info = new DroneInfo(1, 30.0, 0, 0, 6001);
        assertTrue(info.hasEnoughAgent(10));
        assertTrue(info.hasEnoughAgent(20));
        assertTrue(info.hasEnoughAgent(30));
        assertFalse(info.hasEnoughAgent(31));
    }

    @Test
    void testHasEnoughAgentAfterDepletion() {
        DroneInfo info = new DroneInfo(1, 30.0, 0, 0, 6001);
        info.setRemainingAgent(5.0);
        assertTrue(info.hasEnoughAgent(5));
        assertFalse(info.hasEnoughAgent(10));
    }

    @Test
    void testRefill() {
        DroneInfo info = new DroneInfo(1, 30.0, 0, 0, 6001);
        info.setRemainingAgent(5.0);
        assertEquals(5.0, info.getRemainingAgent());

        info.refill();
        assertEquals(30.0, info.getRemainingAgent());
    }

    @Test
    void testRefillRestoresToCapacity() {
        DroneInfo info = new DroneInfo(1, 25.0, 0, 0, 6001);
        info.setRemainingAgent(0.0);
        info.refill();
        assertEquals(25.0, info.getRemainingAgent());
    }

    // ==================== Assignment ====================

    @Test
    void testSetAssignment() {
        DroneInfo info = new DroneInfo(1, 30.0, 0, 0, 6001);
        info.setAssignedZoneId(3);
        info.setAssignedSeverity("HIGH");

        assertEquals(3, info.getAssignedZoneId());
        assertEquals("HIGH", info.getAssignedSeverity());
    }

    @Test
    void testClearAssignment() {
        DroneInfo info = new DroneInfo(1, 30.0, 0, 0, 6001);
        info.setAssignedZoneId(3);
        info.setAssignedSeverity("HIGH");

        info.clearAssignment();
        assertEquals(-1, info.getAssignedZoneId());
        assertNull(info.getAssignedSeverity());
    }

    // ==================== Task Tracking ====================

    @Test
    void testIncrementTasksCompleted() {
        DroneInfo info = new DroneInfo(1, 30.0, 0, 0, 6001);
        assertEquals(0, info.getTasksCompleted());

        info.incrementTasksCompleted();
        assertEquals(1, info.getTasksCompleted());

        info.incrementTasksCompleted();
        assertEquals(2, info.getTasksCompleted());
    }

    // ==================== Fault Tracking ====================

    @Test
    void testSetCurrentFault() {
        DroneInfo info = new DroneInfo(1, 30.0, 0, 0, 6001);
        assertEquals(FaultType.NONE, info.getCurrentFault());

        info.setCurrentFault(FaultType.DRONE_STUCK);
        assertEquals(FaultType.DRONE_STUCK, info.getCurrentFault());
    }

    @Test
    void testIncrementFaultCount() {
        DroneInfo info = new DroneInfo(1, 30.0, 0, 0, 6001);
        assertEquals(0, info.getFaultCount());

        info.incrementFaultCount();
        assertEquals(1, info.getFaultCount());

        info.incrementFaultCount();
        info.incrementFaultCount();
        assertEquals(3, info.getFaultCount());
    }

    @Test
    void testPermanentlyOffline() {
        DroneInfo info = new DroneInfo(1, 30.0, 0, 0, 6001);
        assertFalse(info.isPermanentlyOffline());

        info.setPermanentlyOffline(true);
        assertTrue(info.isPermanentlyOffline());
    }

    // ==================== Dispatch Timestamp ====================

    @Test
    void testDispatchTimestamp() {
        DroneInfo info = new DroneInfo(1, 30.0, 0, 0, 6001);
        assertEquals(0, info.getDispatchTimestamp());

        long now = System.currentTimeMillis();
        info.setDispatchTimestamp(now);
        assertEquals(now, info.getDispatchTimestamp());
    }

    // ==================== Position ====================

    @Test
    void testSetPosition() {
        DroneInfo info = new DroneInfo(1, 30.0, 0, 0, 6001);
        info.setCurrentX(350);
        info.setCurrentY(300);

        assertEquals(350, info.getCurrentX());
        assertEquals(300, info.getCurrentY());
    }

    // ==================== toString ====================

    @Test
    void testToString() {
        DroneInfo info = new DroneInfo(1, 30.0, 0, 0, 6001);
        String str = info.toString();

        assertTrue(str.contains("id=1"));
        assertTrue(str.contains("state=IDLE"));
        assertTrue(str.contains("pos=(0,0)"));
        assertTrue(str.contains("agent=30.0"));
        assertTrue(str.contains("fault=NONE"));
        assertTrue(str.contains("offline=false"));
    }

    // ==================== Full Lifecycle ====================

    @Test
    void testDroneLifecycleNormal() {
        DroneInfo info = new DroneInfo(1, 30.0, 0, 0, 6001);

        // Dispatch
        info.setState(DroneState.EN_ROUTE);
        info.setAssignedZoneId(3);
        info.setAssignedSeverity("HIGH");
        info.setDispatchTimestamp(System.currentTimeMillis());

        assertFalse(info.isAvailable());
        assertEquals(3, info.getAssignedZoneId());

        // Arrive and drop
        info.setState(DroneState.DROPPING_AGENT);
        info.setRemainingAgent(0.0);

        // Complete
        info.setState(DroneState.IDLE);
        info.clearAssignment();
        info.incrementTasksCompleted();

        assertTrue(info.isIdle());
        assertEquals(-1, info.getAssignedZoneId());
        assertEquals(1, info.getTasksCompleted());
        assertFalse(info.hasEnoughAgent(10));

        // Refill
        info.refill();
        assertTrue(info.hasEnoughAgent(30));
        assertTrue(info.isAvailable());
    }

    @Test
    void testDroneLifecycleHardFault() {
        DroneInfo info = new DroneInfo(1, 30.0, 0, 0, 6001);

        info.setState(DroneState.EN_ROUTE);
        info.setAssignedZoneId(5);

        // Hard fault
        info.setCurrentFault(FaultType.NOZZLE_STUCK);
        info.incrementFaultCount();
        info.setPermanentlyOffline(true);
        info.setState(DroneState.OFFLINE);
        info.clearAssignment();

        assertFalse(info.isAvailable());
        assertTrue(info.isPermanentlyOffline());
        assertEquals(DroneState.OFFLINE, info.getState());
        assertEquals(1, info.getFaultCount());
    }
}
