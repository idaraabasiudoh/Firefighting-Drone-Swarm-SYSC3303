package drone;

public enum DroneState {
    IDLE,
    EN_ROUTE,
    DROPPING_AGENT,
    RETURNING_BASE,
    FAULT_STUCK,        // Soft fault: drone stuck mid-flight
    FAULT_NOZZLE,       // Hard fault: nozzle jammed during drop
    FAULT_SENSOR,       // Soft fault: arrival sensor failure
    OFFLINE,            // Drone taken offline after hard fault
    SHUTDOWN
}