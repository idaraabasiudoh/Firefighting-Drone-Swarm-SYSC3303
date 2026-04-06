package test;

import drone.DroneState;
import drone.FaultType;
import fireincident.FireEvent;
import gui.GuiModel;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.awt.Point;
import java.util.Map;

public class GuiModelTest {

    private GuiModel model;

    @BeforeEach
    void setUp() {
        model = GuiModel.get();

        // Reset state between tests by clearing known entries
        for (int id = 1; id <= 10; id++) {
            model.setDroneState(id, DroneState.IDLE);
            model.setDroneAssignment(id, -1);
            model.setDroneFault(id, FaultType.NONE);
            model.setDronePosition(id, 0, 0);
        }
        for (int z = 1; z <= 7; z++) {
            model.removeActiveFire(z);
        }
    }

    // ==================== Singleton ====================

    @Test
    void testSingletonInstance() {
        assertNotNull(GuiModel.get());
        assertSame(GuiModel.get(), GuiModel.get());
    }

    // ==================== Drone State ====================

    @Test
    void testSetAndGetDroneState() {
        model.setDroneState(1, DroneState.EN_ROUTE);
        assertEquals(DroneState.EN_ROUTE, model.getDroneState(1));
    }

    @Test
    void testGetDroneStateDefault() {
        assertEquals(DroneState.IDLE, model.getDroneState(999));
    }

    @Test
    void testSnapshotDroneStates() {
        model.setDroneState(1, DroneState.EN_ROUTE);
        model.setDroneState(2, DroneState.DROPPING_AGENT);

        Map<Integer, DroneState> snapshot = model.snapshotDroneStates();
        assertEquals(DroneState.EN_ROUTE, snapshot.get(1));
        assertEquals(DroneState.DROPPING_AGENT, snapshot.get(2));
    }

    @Test
    void testSnapshotDroneStatesIsImmutable() {
        model.setDroneState(1, DroneState.IDLE);
        Map<Integer, DroneState> snapshot = model.snapshotDroneStates();

        assertThrows(UnsupportedOperationException.class, () ->
                snapshot.put(99, DroneState.OFFLINE));
    }

    // ==================== Drone Assignment ====================

    @Test
    void testSetDroneAssignment() {
        model.setDroneAssignment(1, 3);
        Map<Integer, Integer> assignments = model.snapshotDroneAssignments();
        assertEquals(3, assignments.get(1));
    }

    @Test
    void testClearDroneAssignment() {
        model.setDroneAssignment(1, 3);
        model.setDroneAssignment(1, -1);

        Map<Integer, Integer> assignments = model.snapshotDroneAssignments();
        assertNull(assignments.get(1));
    }

    // ==================== Drone Faults ====================

    @Test
    void testSetDroneFault() {
        model.setDroneFault(1, FaultType.DRONE_STUCK);
        assertEquals(FaultType.DRONE_STUCK, model.getDroneFault(1));
    }

    @Test
    void testClearDroneFaultWithNone() {
        model.setDroneFault(1, FaultType.DRONE_STUCK);
        model.setDroneFault(1, FaultType.NONE);
        assertEquals(FaultType.NONE, model.getDroneFault(1));
    }

    @Test
    void testClearDroneFaultWithNull() {
        model.setDroneFault(1, FaultType.NOZZLE_STUCK);
        model.setDroneFault(1, null);
        assertEquals(FaultType.NONE, model.getDroneFault(1));
    }

    @Test
    void testGetDroneFaultDefault() {
        assertEquals(FaultType.NONE, model.getDroneFault(999));
    }

    @Test
    void testSnapshotDroneFaults() {
        model.setDroneFault(1, FaultType.DRONE_STUCK);
        model.setDroneFault(2, FaultType.NOZZLE_STUCK);

        Map<Integer, FaultType> faults = model.snapshotDroneFaults();
        assertEquals(FaultType.DRONE_STUCK, faults.get(1));
        assertEquals(FaultType.NOZZLE_STUCK, faults.get(2));
    }

    // ==================== Active Fires ====================

    @Test
    void testAddActiveFire() {
        model.addActiveFire(3, FireEvent.Severity.HIGH);
        assertEquals(FireEvent.Severity.HIGH, model.getFireSeverity(3));
    }

    @Test
    void testRemoveActiveFire() {
        model.addActiveFire(3, FireEvent.Severity.HIGH);
        model.removeActiveFire(3);
        assertNull(model.getFireSeverity(3));
    }

    @Test
    void testGetActiveFireCount() {
        assertEquals(0, model.getActiveFireCount());

        model.addActiveFire(1, FireEvent.Severity.LOW);
        model.addActiveFire(3, FireEvent.Severity.HIGH);
        assertEquals(2, model.getActiveFireCount());

        model.removeActiveFire(1);
        assertEquals(1, model.getActiveFireCount());
    }

    @Test
    void testSnapshotActiveFireZones() {
        model.addActiveFire(1, FireEvent.Severity.LOW);
        model.addActiveFire(5, FireEvent.Severity.MODERATE);

        Map<Integer, FireEvent.Severity> fires = model.snapshotActiveFireZones();
        assertEquals(FireEvent.Severity.LOW, fires.get(1));
        assertEquals(FireEvent.Severity.MODERATE, fires.get(5));
    }

    // ==================== Drone Position ====================

    @Test
    void testSetAndGetDronePosition() {
        model.setDronePosition(1, 350, 300);
        Point p = model.getDronePosition(1);
        assertEquals(350, p.x);
        assertEquals(300, p.y);
    }

    @Test
    void testGetDronePositionDefault() {
        Point p = model.getDronePosition(999);
        assertEquals(0, p.x);
        assertEquals(0, p.y);
    }

    @Test
    void testDronePositionReturnsCopy() {
        model.setDronePosition(1, 100, 200);
        Point p1 = model.getDronePosition(1);
        Point p2 = model.getDronePosition(1);

        assertEquals(p1, p2);
        assertNotSame(p1, p2);
    }

    @Test
    void testSnapshotDronePositions() {
        model.setDronePosition(1, 100, 200);
        model.setDronePosition(2, 300, 400);

        Map<Integer, Point> positions = model.snapshotDronePositions();
        assertEquals(new Point(100, 200), positions.get(1));
        assertEquals(new Point(300, 400), positions.get(2));
    }

    // ==================== Fire Intensity ====================

    @Test
    void testSetFireIntensity() {
        model.addActiveFire(1, FireEvent.Severity.HIGH);
        assertEquals(1.0, model.getFireIntensity(1));

        model.setFireIntensity(1, 0.5);
        assertEquals(0.5, model.getFireIntensity(1));
    }

    @Test
    void testSetFireIntensityClampedToOne() {
        model.setFireIntensity(1, 1.5);
        assertEquals(1.0, model.getFireIntensity(1));
    }

    @Test
    void testSetFireIntensityZeroRemoves() {
        model.addActiveFire(1, FireEvent.Severity.HIGH);
        model.setFireIntensity(1, 0.0);

        Map<Integer, Double> intensities = model.snapshotFireIntensity();
        assertNull(intensities.get(1));
    }

    @Test
    void testSetFireIntensityNegativeRemoves() {
        model.addActiveFire(1, FireEvent.Severity.HIGH);
        model.setFireIntensity(1, -0.5);

        Map<Integer, Double> intensities = model.snapshotFireIntensity();
        assertNull(intensities.get(1));
    }

    @Test
    void testGetFireIntensityDefault() {
        assertEquals(1.0, model.getFireIntensity(999));
    }

    @Test
    void testAddActiveFireSetsIntensityToOne() {
        model.addActiveFire(3, FireEvent.Severity.MODERATE);
        assertEquals(1.0, model.getFireIntensity(3));
    }

    @Test
    void testRemoveActiveFireClearsIntensity() {
        model.addActiveFire(3, FireEvent.Severity.MODERATE);
        model.removeActiveFire(3);

        Map<Integer, Double> intensities = model.snapshotFireIntensity();
        assertNull(intensities.get(3));
    }
}
