package gui;

import drone.DroneState;
import drone.FaultType;
import fireincident.FireEvent;

// Thread-safe shared model that the Scheduler updates and the GUI reads (Iteration 4: multi-drone + faults)
import java.util.*;

public class GuiModel {
    private static final GuiModel INSTANCE = new GuiModel();

    // Multi-drone state tracking
    private final Map<Integer, DroneState> droneStates = new HashMap<>();
    private final Map<Integer, Integer> droneAssignments = new HashMap<>(); // droneId -> zoneId

    // Iteration 4: Drone fault tracking
    private final Map<Integer, FaultType> droneFaults = new HashMap<>(); // droneId -> current fault

    // Active fires with severity
    private final Map<Integer, FireEvent.Severity> activeFireZones = new HashMap<>(); // zoneId -> severity

    private GuiModel() {}

    public static GuiModel get() { return INSTANCE; }

    // -------- Drone State (per drone) --------
    public synchronized void setDroneState(int droneId, DroneState s) {
        droneStates.put(droneId, s);
    }

    public synchronized DroneState getDroneState(int droneId) {
        return droneStates.getOrDefault(droneId, DroneState.IDLE);
    }

    public synchronized Map<Integer, DroneState> snapshotDroneStates() {
        return Collections.unmodifiableMap(new HashMap<>(droneStates));
    }

    // -------- Drone Faults (Iteration 4) --------
    public synchronized void setDroneFault(int droneId, FaultType fault) {
        if (fault == FaultType.NONE) {
            droneFaults.remove(droneId);
        } else {
            droneFaults.put(droneId, fault);
        }
    }

    public synchronized FaultType getDroneFault(int droneId) {
        return droneFaults.getOrDefault(droneId, FaultType.NONE);
    }

    public synchronized Map<Integer, FaultType> snapshotDroneFaults() {
        return Collections.unmodifiableMap(new HashMap<>(droneFaults));
    }

    // -------- Drone Assignments --------
    public synchronized void setDroneAssignment(int droneId, int zoneId) {
        if (zoneId == -1) {
            droneAssignments.remove(droneId);
        } else {
            droneAssignments.put(droneId, zoneId);
        }
    }

    public synchronized Map<Integer, Integer> snapshotDroneAssignments() {
        return Collections.unmodifiableMap(new HashMap<>(droneAssignments));
    }

    // -------- Active Fires with Severity --------
    public synchronized void addActiveFire(int zoneId, FireEvent.Severity severity) {
        activeFireZones.put(zoneId, severity);
    }

    public synchronized void removeActiveFire(int zoneId) {
        activeFireZones.remove(zoneId);
    }

    public synchronized int getActiveFireCount() { return activeFireZones.size(); }

    public synchronized Set<Integer> snapshotActiveFireZoneIds() {
        return Collections.unmodifiableSet(new HashSet<>(activeFireZones.keySet()));
    }

    public synchronized Map<Integer, FireEvent.Severity> snapshotActiveFireZones() {
        return Collections.unmodifiableMap(new HashMap<>(activeFireZones));
    }

    public synchronized FireEvent.Severity getFireSeverity(int zoneId) {
        return activeFireZones.get(zoneId);
    }
}