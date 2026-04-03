package gui;

import drone.DroneState;
import drone.FaultType;
import fireincident.FireEvent;

import java.awt.Point;
import java.util.*;

public class GuiModel {
    private static final GuiModel INSTANCE = new GuiModel();

    private final Map<Integer, DroneState> droneStates = new HashMap<>();
    private final Map<Integer, Integer> droneAssignments = new HashMap<>();
    private final Map<Integer, FaultType> droneFaults = new HashMap<>();
    private final Map<Integer, FireEvent.Severity> activeFireZones = new HashMap<>();
    private final Map<Integer, Point> dronePositions = new HashMap<>();
    private final Map<Integer, Double> fireIntensity = new HashMap<>();

    private GuiModel() {}

    public static GuiModel get() {
        return INSTANCE;
    }

    public synchronized void setDroneState(int droneId, DroneState state) {
        droneStates.put(droneId, state);
    }

    public synchronized DroneState getDroneState(int droneId) {
        return droneStates.getOrDefault(droneId, DroneState.IDLE);
    }

    public synchronized Map<Integer, DroneState> snapshotDroneStates() {
        return Collections.unmodifiableMap(new HashMap<>(droneStates));
    }

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

    public synchronized void setDroneFault(int droneId, FaultType fault) {
        if (fault == null || fault == FaultType.NONE) {
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

    public synchronized void addActiveFire(int zoneId, FireEvent.Severity severity) {
        activeFireZones.put(zoneId, severity);
        fireIntensity.put(zoneId, 1.0);
    }

    public synchronized void removeActiveFire(int zoneId) {
        activeFireZones.remove(zoneId);
        fireIntensity.remove(zoneId);
    }

    public synchronized int getActiveFireCount() {
        return activeFireZones.size();
    }

    public synchronized Map<Integer, FireEvent.Severity> snapshotActiveFireZones() {
        return Collections.unmodifiableMap(new HashMap<>(activeFireZones));
    }

    public synchronized FireEvent.Severity getFireSeverity(int zoneId) {
        return activeFireZones.get(zoneId);
    }

    public synchronized void setDronePosition(int droneId, int x, int y) {
        dronePositions.put(droneId, new Point(x, y));
    }

    public synchronized Point getDronePosition(int droneId) {
        Point p = dronePositions.get(droneId);
        if (p == null) return new Point(0, 0);
        return new Point(p);
    }

    public synchronized Map<Integer, Point> snapshotDronePositions() {
        Map<Integer, Point> copy = new HashMap<>();
        for (Map.Entry<Integer, Point> entry : dronePositions.entrySet()) {
            copy.put(entry.getKey(), new Point(entry.getValue()));
        }
        return Collections.unmodifiableMap(copy);
    }

    public synchronized void setFireIntensity(int zoneId, double intensity) {
        if (intensity <= 0.0) {
            fireIntensity.remove(zoneId);
        } else {
            fireIntensity.put(zoneId, Math.min(1.0, intensity));
        }
    }

    public synchronized double getFireIntensity(int zoneId) {
        return fireIntensity.getOrDefault(zoneId, 1.0);
    }

    public synchronized Map<Integer, Double> snapshotFireIntensity() {
        return Collections.unmodifiableMap(new HashMap<>(fireIntensity));
    }
}