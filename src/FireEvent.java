import java.util.Objects;

public class FireEvent {

    public enum EventType {
        FIRE_DETECTED,
        DRONE_REQUEST
    }

    public enum Severity {
        LOW,
        MODERATE,
        HIGH
    }

    private String time;          // hh:mm:ss.mmm (Iteration 1: keep as String)
    private int zoneId;
    private EventType eventType;
    private Severity severity;

    public FireEvent(String time, int zoneId, EventType eventType, Severity severity) {
        this.time = time;
        this.zoneId = zoneId;
        this.eventType = eventType;
        this.severity = severity;
    }

    // -------- Getters --------

    public String getTime() {
        return time;
    }

    public int getZoneId() {
        return zoneId;
    }

    public EventType getEventType() {
        return eventType;
    }

    public Severity getSeverity() {
        return severity;
    }

    // -------- Setters --------

    public void setTime(String time) {
        this.time = time;
    }

    public void setZoneId(int zoneId) {
        this.zoneId = zoneId;
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

    public void setSeverity(Severity severity) {
        this.severity = severity;
    }

    // -------- Domain Logic --------

    public int getLitersNeeded() {
        switch (severity) {
            case LOW:
                return 10;
            case MODERATE:
                return 20;
            case HIGH:
                return 30;
            default:
                return 0;
        }
    }

    // -------- Object Overrides --------

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FireEvent)) return false;
        FireEvent other = (FireEvent) o;
        return zoneId == other.zoneId
                && Objects.equals(time, other.time)
                && eventType == other.eventType
                && severity == other.severity;
    }

    @Override
    public int hashCode() {
        return Objects.hash(time, zoneId, eventType, severity);
    }

    @Override
    public String toString() {
        return "FireEvent{" +
                "time='" + time + '\'' +
                ", zoneId=" + zoneId +
                ", eventType=" + eventType +
                ", severity=" + severity +
                ", litersNeeded=" + getLitersNeeded() +
                '}';
    }
}
