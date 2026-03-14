// Thread-safe shared model that the Scheduler updates and the GUI reads (Iteration 3: multi-drone)
import java.util.*;

public class GuiModel {
    private static final GuiModel INSTANCE = new GuiModel();

    // Multi-drone state tracking
    private final Map<Integer, DroneState> droneStates = new HashMap<>();
    private final Map<Integer, Integer> droneAssignments = new HashMap<>(); // droneId -> zoneId

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