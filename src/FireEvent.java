public class FireEvent {
    private String time;
    private int zoneId;
    private EventType eventType;
    private Severity severity;

    public enum EventType {
        FIRE_DETECTED,
        DRONE_REQUEST
    }

    public enum Severity {
        LOW(10),
        MODERATE(20),
        HIGH(30);

        private final int litersNeeded;

        Severity(int litersNeeded) {
            this.litersNeeded = litersNeeded;
        }

        public int getLitersNeeded() {
            return litersNeeded;
        }
    }

    public FireEvent(String time, int zoneId, EventType eventType, Severity severity) {
        this.time = time;
        this.zoneId = zoneId;
        this.eventType = eventType;
        this.severity = severity;
    }

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

    public int getLitersNeeded() {
        return severity.getLitersNeeded();
    }

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

    @Override
    public String toString() {
        return String.format("FireEvent[Time=%s, Zone=%d, Type=%s, Severity=%s, Liters=%d]",
                time, zoneId, eventType, severity, severity.getLitersNeeded());
    }
}