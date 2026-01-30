public class DroneResultTest {

    public void testDroneResultConstruction() {
        DroneResult result = new DroneResult(1, 101, true, 5.5);
        
        TestRunner.assertEquals(1, result.getDroneId(), "Drone ID mismatch");
        TestRunner.assertEquals(101, result.getZoneId(), "Zone ID mismatch");
        TestRunner.assertEquals(true, result.isTaskCompleted(), "Task completion mismatch");
        TestRunner.assertEquals(5.5, result.getRemainingAgent(), "Remaining agent mismatch");
    }

    public void testSetters() {
        DroneResult result = new DroneResult(1, 101, true, 5.5);
        
        result.setDroneId(2);
        TestRunner.assertEquals(2, result.getDroneId(), "Set Drone ID failed");
        
        result.setZoneId(202);
        TestRunner.assertEquals(202, result.getZoneId(), "Set Zone ID failed");
        
        result.setTaskCompleted(false);
        TestRunner.assertEquals(false, result.isTaskCompleted(), "Set Task Completed failed");
        
        result.setRemainingAgent(10.0);
        TestRunner.assertEquals(10.0, result.getRemainingAgent(), "Set Remaining Agent failed");
    }
}
