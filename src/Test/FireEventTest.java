import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class FireEventTest {

    @Test
    void testLitersNeededLow() {
        FireEvent event = new FireEvent(
                "14:00:00",
                1,
                FireEvent.EventType.FIRE_DETECTED,
                FireEvent.Severity.LOW
        );

        assertEquals(10, event.getLitersNeeded());
    }

    @Test
    void testLitersNeededHigh() {
        FireEvent event = new FireEvent(
                "14:00:00",
                1,
                FireEvent.EventType.FIRE_DETECTED,
                FireEvent.Severity.HIGH
        );

        assertEquals(30, event.getLitersNeeded());
    }

    @Test
    void testEquals() {
        FireEvent e1 = new FireEvent("14:00:00", 1,
                FireEvent.EventType.FIRE_DETECTED,
                FireEvent.Severity.MODERATE);

        FireEvent e2 = new FireEvent("14:00:00", 1,
                FireEvent.EventType.FIRE_DETECTED,
                FireEvent.Severity.MODERATE);

        assertEquals(e1, e2);
    }
}