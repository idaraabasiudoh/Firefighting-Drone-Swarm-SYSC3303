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
}
