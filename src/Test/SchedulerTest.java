package test;

import drone.*;
import fireincident.FireEvent;
import fireincident.FireIncidentSubsystem;
import gui.Zone;
import network.UDPHelper;
import scheduler.Scheduler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

public class SchedulerTest {

    private List<Zone> zones;

    @BeforeEach
    void setUp() {
        zones = new ArrayList<>();
        zones.add(new Zone(1, 0, 0, 301, 301));       // center ~(150, 150)
        zones.add(new Zone(2, 301, 0, 601, 301));      // center ~(451, 150)
        zones.add(new Zone(3, 0, 301, 301, 601));       // center ~(150, 451)
        zones.add(new Zone(4, 301, 301, 601, 601));     // center ~(451, 451)
        zones.add(new Zone(5, 601, 0, 901, 301));       // center ~(751, 150)
    }

    @Test
    void testFindBestDroneSelectsIdleDrone() {
        Scheduler scheduler = new Scheduler(zones);

        // Register two drones at (0,0)
        DroneInfo drone1 = new DroneInfo(1, 30.0, 0, 0, 6001);
        DroneInfo drone2 = new DroneInfo(2, 30.0, 0, 0, 6002);
        scheduler.getDroneRegistry().put(1, drone1);
        scheduler.getDroneRegistry().put(2, drone2);

        FireEvent event = new FireEvent("14:00:00", 1, FireEvent.EventType.FIRE_DETECTED, FireEvent.Severity.LOW);

        DroneInfo best = scheduler.findBestDrone(event);
        assertNotNull(best);
        assertTrue(best.isIdle());
    }

    @Test
    void testFindBestDroneSelectsClosest() {
        Scheduler scheduler = new Scheduler(zones);

        // Drone 1 at (0,0) — closer to Zone 1 center (150,150)
        DroneInfo drone1 = new DroneInfo(1, 30.0, 0, 0, 6001);
        // Drone 2 at (600,0) — farther from Zone 1
        DroneInfo drone2 = new DroneInfo(2, 30.0, 600, 0, 6002);
        scheduler.getDroneRegistry().put(1, drone1);
        scheduler.getDroneRegistry().put(2, drone2);

        FireEvent event = new FireEvent("14:00:00", 1, FireEvent.EventType.FIRE_DETECTED, FireEvent.Severity.LOW);

        DroneInfo best = scheduler.findBestDrone(event);
        assertNotNull(best);
        assertEquals(1, best.getDroneId());
    }

    @Test
    void testFindBestDroneSkipsNonIdle() {
        Scheduler scheduler = new Scheduler(zones);

        DroneInfo drone1 = new DroneInfo(1, 30.0, 0, 0, 6001);
        drone1.setState(DroneState.EN_ROUTE); // not idle
        DroneInfo drone2 = new DroneInfo(2, 30.0, 500, 500, 6002);
        scheduler.getDroneRegistry().put(1, drone1);
        scheduler.getDroneRegistry().put(2, drone2);

        FireEvent event = new FireEvent("14:00:00", 1, FireEvent.EventType.FIRE_DETECTED, FireEvent.Severity.LOW);

        DroneInfo best = scheduler.findBestDrone(event);
        assertNotNull(best);
        assertEquals(2, best.getDroneId());
    }

    @Test
    void testFindBestDroneSkipsInsufficientAgent() {
        Scheduler scheduler = new Scheduler(zones);

        DroneInfo drone1 = new DroneInfo(1, 30.0, 0, 0, 6001);
        drone1.setRemainingAgent(5.0); // not enough for HIGH (30L)
        DroneInfo drone2 = new DroneInfo(2, 30.0, 0, 0, 6002);
        scheduler.getDroneRegistry().put(1, drone1);
        scheduler.getDroneRegistry().put(2, drone2);

        FireEvent event = new FireEvent("14:00:00", 1, FireEvent.EventType.FIRE_DETECTED, FireEvent.Severity.HIGH);

        DroneInfo best = scheduler.findBestDrone(event);
        assertNotNull(best);
        assertEquals(2, best.getDroneId());
    }

    @Test
    void testFindBestDroneReturnsNullWhenNoneAvailable() {
        Scheduler scheduler = new Scheduler(zones);

        DroneInfo drone1 = new DroneInfo(1, 30.0, 0, 0, 6001);
        drone1.setState(DroneState.EN_ROUTE);
        scheduler.getDroneRegistry().put(1, drone1);

        FireEvent event = new FireEvent("14:00:00", 1, FireEvent.EventType.FIRE_DETECTED, FireEvent.Severity.LOW);

        DroneInfo best = scheduler.findBestDrone(event);
        assertNull(best);
    }

    @Test
    void testLoadBalancingFavorsLessBusyDrone() {
        Scheduler scheduler = new Scheduler(zones);

        // Both drones at same location, but drone1 completed more tasks
        DroneInfo drone1 = new DroneInfo(1, 30.0, 0, 0, 6001);
        for (int i = 0; i < 5; i++) drone1.incrementTasksCompleted();
        DroneInfo drone2 = new DroneInfo(2, 30.0, 0, 0, 6002);
        scheduler.getDroneRegistry().put(1, drone1);
        scheduler.getDroneRegistry().put(2, drone2);

        FireEvent event = new FireEvent("14:00:00", 1, FireEvent.EventType.FIRE_DETECTED, FireEvent.Severity.LOW);

        DroneInfo best = scheduler.findBestDrone(event);
        assertNotNull(best);
        // Drone 2 should be preferred (fewer tasks completed = lower score)
        assertEquals(2, best.getDroneId());
    }

    @Test
    void testPendingEventsQueue() {
        Scheduler scheduler = new Scheduler(zones);

        FireEvent e1 = new FireEvent("14:00:00", 1, FireEvent.EventType.FIRE_DETECTED, FireEvent.Severity.LOW);
        FireEvent e2 = new FireEvent("14:01:00", 2, FireEvent.EventType.FIRE_DETECTED, FireEvent.Severity.HIGH);

        scheduler.getPendingEvents().add(e1);
        scheduler.getPendingEvents().add(e2);

        assertEquals(2, scheduler.getPendingEvents().size());
        assertEquals(e1, scheduler.getPendingEvents().peek());
    }

    @Test
    void testDroneInfoHelpers() {
        DroneInfo info = new DroneInfo(1, 30.0, 0, 0, 6001);

        assertTrue(info.isIdle());
        assertTrue(info.hasEnoughAgent(10));
        assertTrue(info.hasEnoughAgent(30));
        assertFalse(info.hasEnoughAgent(31));

        info.setState(DroneState.EN_ROUTE);
        assertFalse(info.isIdle());

        info.setAssignedZoneId(3);
        info.setAssignedSeverity("HIGH");
        assertEquals(3, info.getAssignedZoneId());
        assertEquals("HIGH", info.getAssignedSeverity());

        info.clearAssignment();
        assertEquals(-1, info.getAssignedZoneId());
        assertNull(info.getAssignedSeverity());

        info.refill();
        assertEquals(30.0, info.getRemainingAgent());
    }

    // ==================== Iteration 4: Fault Handling Tests ====================

    @Test
    void testFindBestDroneSkipsOfflineDrone() {
        Scheduler scheduler = new Scheduler(zones);

        DroneInfo drone1 = new DroneInfo(1, 30.0, 0, 0, 6001);
        drone1.setPermanentlyOffline(true);
        DroneInfo drone2 = new DroneInfo(2, 30.0, 500, 500, 6002);
        scheduler.getDroneRegistry().put(1, drone1);
        scheduler.getDroneRegistry().put(2, drone2);

        FireEvent event = new FireEvent("14:00:00", 1, FireEvent.EventType.FIRE_DETECTED, FireEvent.Severity.LOW);

        DroneInfo best = scheduler.findBestDrone(event);
        assertNotNull(best);
        assertEquals(2, best.getDroneId());
    }

    @Test
    void testFindBestDroneReturnsNullWhenAllOffline() {
        Scheduler scheduler = new Scheduler(zones);

        DroneInfo drone1 = new DroneInfo(1, 30.0, 0, 0, 6001);
        drone1.setPermanentlyOffline(true);
        scheduler.getDroneRegistry().put(1, drone1);

        FireEvent event = new FireEvent("14:00:00", 1, FireEvent.EventType.FIRE_DETECTED, FireEvent.Severity.LOW);

        DroneInfo best = scheduler.findBestDrone(event);
        assertNull(best);
    }

    @Test
    void testDroneInfoFaultTracking() {
        DroneInfo info = new DroneInfo(1, 30.0, 0, 0, 6001);

        assertEquals(FaultType.NONE, info.getCurrentFault());
        assertFalse(info.isPermanentlyOffline());
        assertEquals(0, info.getFaultCount());
        assertTrue(info.isAvailable());

        // Set soft fault
        info.setCurrentFault(FaultType.DRONE_STUCK);
        info.incrementFaultCount();
        assertEquals(FaultType.DRONE_STUCK, info.getCurrentFault());
        assertEquals(1, info.getFaultCount());
        assertTrue(info.isAvailable()); // soft fault drone is still available

        // Set hard fault -> permanently offline
        info.setCurrentFault(FaultType.NOZZLE_STUCK);
        info.setPermanentlyOffline(true);
        info.incrementFaultCount();
        assertEquals(FaultType.NOZZLE_STUCK, info.getCurrentFault());
        assertTrue(info.isPermanentlyOffline());
        assertFalse(info.isAvailable());
        assertEquals(2, info.getFaultCount());
    }

    @Test
    void testDroneInfoDispatchTimestamp() {
        DroneInfo info = new DroneInfo(1, 30.0, 0, 0, 6001);

        assertEquals(0, info.getDispatchTimestamp());

        long now = System.currentTimeMillis();
        info.setDispatchTimestamp(now);
        assertEquals(now, info.getDispatchTimestamp());
    }

    @Test
    void testUDPHelperFaultMessages() {
        // Test DRONE_FAULT message build/parse
        String faultMsg = UDPHelper.buildDroneFaultMessage(1, "DRONE_STUCK", 3);
        assertEquals("DRONE_FAULT", UDPHelper.getMessageType(faultMsg));
        assertEquals(1, UDPHelper.parseDroneFaultDroneId(faultMsg));
        assertEquals("DRONE_STUCK", UDPHelper.parseDroneFaultType(faultMsg));
        assertEquals(3, UDPHelper.parseDroneFaultZoneId(faultMsg));
    }

    @Test
    void testUDPHelperDroneCommandWithFault() {
        String cmd = UDPHelper.buildDroneCommandMessage(1, "TASK", 3, "HIGH", "NOZZLE_STUCK");
        assertEquals("DRONE_COMMAND", UDPHelper.getMessageType(cmd));
        assertEquals(1, UDPHelper.parseDroneCommandId(cmd));
        assertEquals("TASK", UDPHelper.parseDroneCommandType(cmd));
        assertEquals(3, UDPHelper.parseDroneCommandZoneId(cmd));
        assertEquals("HIGH", UDPHelper.parseDroneCommandSeverity(cmd));
        assertEquals("NOZZLE_STUCK", UDPHelper.parseDroneCommandFault(cmd));
    }

    @Test
    void testUDPHelperDroneCommandDefaultFault() {
        String cmd = UDPHelper.buildDroneCommandMessage(1, "TASK", 3, "LOW");
        assertEquals("NONE", UDPHelper.parseDroneCommandFault(cmd));
    }

    @Test
    void testUDPHelperFireEventWithFault() {
        String msg = UDPHelper.buildFireEventMessage("14:00:00", 3, "FIRE_DETECTED", "HIGH", "SENSOR_FAIL");
        FireEvent event = UDPHelper.parseFireEvent(msg);
        assertEquals(3, event.getZoneId());
        assertEquals(FireEvent.Severity.HIGH, event.getSeverity());
        assertEquals(FaultType.SENSOR_FAIL, event.getFaultType());
    }

    @Test
    void testFireIncidentParseLineWithFault() {
        String line = "14:03:20,1,FIRE_DETECTED,Low,DRONE_STUCK";
        FireEvent event = FireIncidentSubsystem.parseLineToFireEvent(line);
        assertNotNull(event);
        assertEquals(1, event.getZoneId());
        assertEquals(FireEvent.Severity.LOW, event.getSeverity());
        assertEquals(FaultType.DRONE_STUCK, event.getFaultType());
    }

    @Test
    void testFireIncidentParseLineWithoutFault() {
        String line = "14:03:15,3,FIRE_DETECTED,High";
        FireEvent event = FireIncidentSubsystem.parseLineToFireEvent(line);
        assertNotNull(event);
        assertEquals(3, event.getZoneId());
        assertEquals(FaultType.NONE, event.getFaultType());
    }
}
