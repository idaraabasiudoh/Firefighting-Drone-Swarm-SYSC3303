package drone;

public class DroneResult {
    private int droneId;
    private int zoneId;
    private boolean taskCompleted;
    private double remainingAgent;

    public DroneResult(int droneId, int zoneId, boolean taskCompleted, double remainingAgent) {
        this.droneId = droneId;
        this.zoneId = zoneId;
        this.taskCompleted = taskCompleted;
        this.remainingAgent = remainingAgent;
    }

    public int getDroneId() {
        return droneId;
    }

    public int getZoneId() {
        return zoneId;
    }

    public boolean isTaskCompleted() {
        return taskCompleted;
    }

    public double getRemainingAgent() {
        return remainingAgent;
    }

    public void setDroneId(int droneId) {
        this.droneId = droneId;
    }

    public void setZoneId(int zoneId) {
        this.zoneId = zoneId;
    }

    public void setTaskCompleted(boolean taskCompleted) {
        this.taskCompleted = taskCompleted;
    }

    public void setRemainingAgent(double remainingAgent) {
        this.remainingAgent = remainingAgent;
    }

    @Override
    public String toString() {
        return String.format("DroneResult[DroneID=%d, Zone=%d, Completed=%s, RemainingAgent=%.1fL]",
                droneId, zoneId, taskCompleted, remainingAgent);
    }
}