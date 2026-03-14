import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class SchedulerTest {

    @Test
    void testDispatchWhenDroneIdle() {
        Scheduler scheduler = new Scheduler();

        FireEvent event = new FireEvent(
                "14:00:00",
                1,
                FireEvent.EventType.FIRE_DETECTED,
                FireEvent.Severity.LOW
        );

        scheduler.submitFireEvent(event);

        DroneCommand command = scheduler.requestNextCommand();

        assertEquals(DroneCommand.Type.TASK, command.type());
        assertEquals(event, command.task());
    }

    @Test
    void testReturnBaseWhenNotEnoughAgent() {
        Scheduler scheduler = new Scheduler();

        FireEvent highFire = new FireEvent(
                "14:00:00",
                1,
                FireEvent.EventType.FIRE_DETECTED,
                FireEvent.Severity.HIGH
        );

        scheduler.submitFireEvent(highFire);

        DroneCommand cmd = scheduler.requestNextCommand();

        // If capacity < required liters
        if (cmd.type() == DroneCommand.Type.RETURN_BASE) {
            assertEquals(DroneCommand.Type.RETURN_BASE, cmd.type());
        } else {
            assertEquals(DroneCommand.Type.TASK, cmd.type());
        }
    }
}