package drone;

/**
 * Tracks per-drone state in the Scheduler's drone registry.
 * Used for multi-drone scheduling and load balancing.
 */
public class DroneInfo {
    private final int droneId;
    private DroneState state;
    private int currentX;
    private int currentY;
    private double remainingAgent;
    private double agentCapacity;
    private int assignedZoneId;       // -1 if not assigned
    private String assignedSeverity;  // null if not assigned
    private int tasksCompleted;
    private int listenPort;           // port this drone listens on for commands

    // Iteration 4: Fault tracking
    private long dispatchTimestamp;    // System.currentTimeMillis() when dispatched
    private FaultType currentFault = FaultType.NONE;
    private int faultCount;
    private boolean permanentlyOffline; // true after a hard fault (e.g. NOZZLE_STUCK)

    public DroneInfo(int droneId, double agentCapacity, int x, int y, int listenPort) {
        this.droneId = droneId;
        this.agentCapacity = agentCapacity;
        this.remainingAgent = agentCapacity;
        this.currentX = x;
        this.currentY = y;
        this.state = DroneState.IDLE;
        this.assignedZoneId = -1;
        this.assignedSeverity = null;
        this.tasksCompleted = 0;
        this.listenPort = listenPort;
        this.dispatchTimestamp = 0;
        this.currentFault = FaultType.NONE;
        this.faultCount = 0;
        this.permanentlyOffline = false;
    }

    // -------- Getters --------
    public int getDroneId() { return droneId; }
    public DroneState getState() { return state; }
    public int getCurrentX() { return currentX; }
    public int getCurrentY() { return currentY; }
    public double getRemainingAgent() { return remainingAgent; }
    public double getAgentCapacity() { return agentCapacity; }
    public int getAssignedZoneId() { return assignedZoneId; }
    public String getAssignedSeverity() { return assignedSeverity; }
    public int getTasksCompleted() { return tasksCompleted; }
    public int getListenPort() { return listenPort; }
    public long getDispatchTimestamp() { return dispatchTimestamp; }
    public FaultType getCurrentFault() { return currentFault; }
    public int getFaultCount() { return faultCount; }
    public boolean isPermanentlyOffline() { return permanentlyOffline; }

    // -------- Setters --------
    public void setState(DroneState state) { this.state = state; }
    public void setCurrentX(int x) { this.currentX = x; }
    public void setCurrentY(int y) { this.currentY = y; }
    public void setRemainingAgent(double agent) { this.remainingAgent = agent; }
    public void setAssignedZoneId(int zoneId) { this.assignedZoneId = zoneId; }
    public void setAssignedSeverity(String severity) { this.assignedSeverity = severity; }
    public void incrementTasksCompleted() { this.tasksCompleted++; }
    public void setListenPort(int port) { this.listenPort = port; }
    public void setDispatchTimestamp(long ts) { this.dispatchTimestamp = ts; }
    public void setCurrentFault(FaultType fault) { this.currentFault = fault; }
    public void incrementFaultCount() { this.faultCount++; }
    public void setPermanentlyOffline(boolean offline) { this.permanentlyOffline = offline; }

    public boolean isIdle() { return state == DroneState.IDLE; }
    public boolean hasEnoughAgent(int litersNeeded) { return remainingAgent >= litersNeeded; }
    public boolean isAvailable() { return isIdle() && !permanentlyOffline; }

    public void clearAssignment() {
        this.assignedZoneId = -1;
        this.assignedSeverity = null;
    }

    public void refill() {
        this.remainingAgent = agentCapacity;
    }

    @Override
    public String toString() {
        return String.format("DroneInfo[id=%d, state=%s, pos=(%d,%d), agent=%.1f, assigned=Zone%d, tasks=%d, fault=%s, offline=%s]",
                droneId, state, currentX, currentY, remainingAgent, assignedZoneId, tasksCompleted, currentFault, permanentlyOffline);
    }
}
