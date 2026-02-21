
// Thread-safe shared model that the Scheduler updates and the GUI reads
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class GuiModel {
    private static final GuiModel INSTANCE = new GuiModel();

    private final Set<Integer> activeFireZones = new HashSet<>();
    private DroneState droneState = DroneState.IDLE;

    private GuiModel() {}

    public static GuiModel get() { return INSTANCE; }

    public synchronized void setDroneState(DroneState s) { this.droneState = s; }
    public synchronized DroneState getDroneState() { return droneState; }

    public synchronized void addActiveFire(int zoneId) { activeFireZones.add(zoneId); }
    public synchronized void removeActiveFire(int zoneId) { activeFireZones.remove(zoneId); }
    public synchronized int getActiveFireCount() { return activeFireZones.size(); }

    public synchronized Set<Integer> snapshotActiveFireZones() {
        return Collections.unmodifiableSet(new HashSet<>(activeFireZones));
    }
}