// DroneCommand.java  (NEW)
// Scheduler sends commands to the drone (either a task, return to base, or shutdown)
public record DroneCommand(Type type, FireEvent task) {
    public enum Type { TASK, RETURN_BASE, SHUTDOWN }

    public static DroneCommand task(FireEvent e) { return new DroneCommand(Type.TASK, e); }
    public static DroneCommand returnBase()     { return new DroneCommand(Type.RETURN_BASE, null); }
    public static DroneCommand shutdown()       { return new DroneCommand(Type.SHUTDOWN, null); }
}