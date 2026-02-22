import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class DroneSubsystemTest {

    @Test
    void testDroneInitialState() {
        Scheduler scheduler = new Scheduler();
        DroneSubsystem drone = new DroneSubsystem(1, scheduler, 30.0);

        assertNotNull(drone);
    }

    @Test
    void testDroneResultCreation() {
        DroneResult result = new DroneResult(1, 2, true, 20.0);

        assertEquals(1, result.getDroneId());
        assertEquals(2, result.getZoneId());
        assertTrue(result.isTaskCompleted());
        assertEquals(20.0, result.getRemainingAgent());
    }
}