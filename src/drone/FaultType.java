package drone;

/**
 * Enumeration of fault types that can be injected into the system.
 * Faults are specified in the fire_events.csv input file.
 *
 * Iteration 4: Fault Handling
 */
public enum FaultType {
    NONE,              // No fault
    DRONE_STUCK,       // Soft fault: drone freezes mid-flight, recoverable after reset
    NOZZLE_STUCK,      // Hard fault: nozzle jams during drop, drone shuts down permanently
    SENSOR_FAIL;       // Soft fault: arrival sensor fails, drone doesn't detect zone arrival

    /**
     * Returns true if this is a hard fault (drone must be permanently shut down).
     */
    public boolean isHardFault() {
        return this == NOZZLE_STUCK;
    }

    /**
     * Returns true if this is a soft fault (drone can recover after reset).
     */
    public boolean isSoftFault() {
        return this == DRONE_STUCK || this == SENSOR_FAIL;
    }

    /**
     * Parse a fault type from a string (case-insensitive, defaults to NONE).
     */
    public static FaultType fromString(String s) {
        if (s == null || s.trim().isEmpty()) return NONE;
        try {
            return valueOf(s.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return NONE;
        }
    }
}
