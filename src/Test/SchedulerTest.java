package test;

import drone.DroneInfo;
import drone.DroneState;
import drone.FaultType;
import fireincident.FireEvent;
import gui.Zone;
import scheduler.Scheduler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

public class SchedulerTest {

    private Scheduler scheduler;
    private List<Zone> zones;

    @BeforeEach
    void setUp() {
        zones = new ArrayList<>();
        zones.add(new Zone(1, 0, 0, 700, 600));
        zones.add(new Zone(2, 0, 600, 700, 1500));
        zones.add(new Zone(3, 700, 0, 1300, 600));
        zones.add(new Zone(4, 700, 600, 1300, 1500));
        zones.add(new Zone(5, 1300, 0, 2000, 600));
        zones.add(new Zone(6, 1300, 600, 2000, 1500));
        zones.add(new Zone(7, 0, 1500, 700, 2100));

        scheduler = new Scheduler(zones);
    }

    // ==================== findBestDrone ====================

    @Test
    void testFindBestDroneNoRegisteredDrones() {
        FireEvent event = new FireEvent("14:00:00", 3,
                FireEvent.EventType.FIRE_DETECTED, FireEvent.Severity.HIGH);

        DroneInfo best = scheduler.findBestDrone(event);
        assertNull(best);
    }

    @Test
    void testFindBestDroneSingleAvailable() {
        DroneInfo drone1 = new DroneInfo(1, 30.0, 0, 0, 6001);
        scheduler.getDroneRegistry().put(1, drone1);

        FireEvent event = new FireEvent("14:00:00", 3,
                FireEvent.EventType.FIRE_DETECTED, FireEvent.Severity.HIGH);

        DroneInfo best = scheduler.findBestDrone(event);
        assertNotNull(best);
        assertEquals(1, best.getDroneId());
    }

    @Test
    void testFindBestDroneCloserDronePreferred() {
        // Drone 1 at origin (0,0)
        DroneInfo drone1 = new DroneInfo(1, 30.0, 0, 0, 6001);
        // Drone 2 closer to Zone 3 center (1000, 300)
        DroneInfo drone2 = new DroneInfo(2, 30.0, 900, 250, 6002);

        scheduler.getDroneRegistry().put(1, drone1);
        scheduler.getDroneRegistry().put(2, drone2);

        FireEvent event = new FireEvent("14:00:00", 3,
                FireEvent.EventType.FIRE_DETECTED, FireEvent.Severity.HIGH);

        DroneInfo best = scheduler.findBestDrone(event);
        assertNotNull(best);
        assertEquals(2, best.getDroneId());
    }

    @Test
    void testFindBestDroneSkipsBusyDrones() {
        DroneInfo drone1 = new DroneInfo(1, 30.0, 0, 0, 6001);
        drone1.setState(DroneState.EN_ROUTE);
        DroneInfo drone2 = new DroneInfo(2, 30.0, 500, 500, 6002);

        scheduler.getDroneRegistry().put(1, drone1);
        scheduler.getDroneRegistry().put(2, drone2);

        FireEvent event = new FireEvent("14:00:00", 1,
                FireEvent.EventType.FIRE_DETECTED, FireEvent.Severity.LOW);

        DroneInfo best = scheduler.findBestDrone(event);
        assertNotNull(best);
        assertEquals(2, best.getDroneId());
    }

    @Test
    void testFindBestDroneSkipsOfflineDrones() {
        DroneInfo drone1 = new DroneInfo(1, 30.0, 0, 0, 6001);
        drone1.setPermanentlyOffline(true);
        DroneInfo drone2 = new DroneInfo(2, 30.0, 500, 500, 6002);

        scheduler.getDroneRegistry().put(1, drone1);
        scheduler.getDroneRegistry().put(2, drone2);

        FireEvent event = new FireEvent("14:00:00", 1,
                FireEvent.EventType.FIRE_DETECTED, FireEvent.Severity.LOW);

        DroneInfo best = scheduler.findBestDrone(event);
        assertNotNull(best);
        assertEquals(2, best.getDroneId());
    }

    @Test
    void testFindBestDroneSkipsInsufficientAgent() {
        DroneInfo drone1 = new DroneInfo(1, 30.0, 0, 0, 6001);
        drone1.setRemainingAgent(5.0);
        DroneInfo drone2 = new DroneInfo(2, 30.0, 500, 500, 6002);

        scheduler.getDroneRegistry().put(1, drone1);
        scheduler.getDroneRegistry().put(2, drone2);

        FireEvent event = new FireEvent("14:00:00", 1,
                FireEvent.EventType.FIRE_DETECTED, FireEvent.Severity.HIGH);

        DroneInfo best = scheduler.findBestDrone(event);
        assertNotNull(best);
        assertEquals(2, best.getDroneId());
    }

    @Test
    void testFindBestDroneAllUnavailable() {
        DroneInfo drone1 = new DroneInfo(1, 30.0, 0, 0, 6001);
        drone1.setState(DroneState.EN_ROUTE);
        DroneInfo drone2 = new DroneInfo(2, 30.0, 0, 0, 6002);
        drone2.setPermanentlyOffline(true);

        scheduler.getDroneRegistry().put(1, drone1);
        scheduler.getDroneRegistry().put(2, drone2);

        FireEvent event = new FireEvent("14:00:00", 1,
                FireEvent.EventType.FIRE_DETECTED, FireEvent.Severity.LOW);

        DroneInfo best = scheduler.findBestDrone(event);
        assertNull(best);
    }

    @Test
    void testFindBestDroneAllInsufficientAgent() {
        DroneInfo drone1 = new DroneInfo(1, 30.0, 0, 0, 6001);
        drone1.setRemainingAgent(5.0);
        DroneInfo drone2 = new DroneInfo(2, 30.0, 0, 0, 6002);
        drone2.setRemainingAgent(5.0);

        scheduler.getDroneRegistry().put(1, drone1);
        scheduler.getDroneRegistry().put(2, drone2);

        FireEvent event = new FireEvent("14:00:00", 1,
                FireEvent.EventType.FIRE_DETECTED, FireEvent.Severity.HIGH);

        DroneInfo best = scheduler.findBestDrone(event);
        assertNull(best);
    }

    @Test
    void testFindBestDroneLowSeverityNeedsLessAgent() {
        DroneInfo drone1 = new DroneInfo(1, 30.0, 0, 0, 6001);
        drone1.setRemainingAgent(10.0);

        scheduler.getDroneRegistry().put(1, drone1);

        FireEvent lowEvent = new FireEvent("14:00:00", 1,
                FireEvent.EventType.FIRE_DETECTED, FireEvent.Severity.LOW);

        DroneInfo best = scheduler.findBestDrone(lowEvent);
        assertNotNull(best);
        assertEquals(1, best.getDroneId());
    }

    @Test
    void testFindBestDroneLoadBalancing() {
        // Both drones at same position, drone1 has more tasks completed
        DroneInfo drone1 = new DroneInfo(1, 30.0, 0, 0, 6001);
        drone1.incrementTasksCompleted();
        drone1.incrementTasksCompleted();
        drone1.incrementTasksCompleted();

        DroneInfo drone2 = new DroneInfo(2, 30.0, 0, 0, 6002);

        scheduler.getDroneRegistry().put(1, drone1);
        scheduler.getDroneRegistry().put(2, drone2);

        FireEvent event = new FireEvent("14:00:00", 1,
                FireEvent.EventType.FIRE_DETECTED, FireEvent.Severity.LOW);

        DroneInfo best = scheduler.findBestDrone(event);
        assertNotNull(best);
        assertEquals(2, best.getDroneId());
    }

    // ==================== Pending Events Queue ====================

    @Test
    void testPendingEventsInitiallyEmpty() {
        assertTrue(scheduler.getPendingEvents().isEmpty());
    }

    // ==================== Drone Registry ====================

    @Test
    void testDroneRegistryInitiallyEmpty() {
        assertTrue(scheduler.getDroneRegistry().isEmpty());
    }

    @Test
    void testDroneRegistryAddDrone() {
        DroneInfo drone = new DroneInfo(1, 30.0, 0, 0, 6001);
        scheduler.getDroneRegistry().put(1, drone);

        assertEquals(1, scheduler.getDroneRegistry().size());
        assertNotNull(scheduler.getDroneRegistry().get(1));
    }
}
