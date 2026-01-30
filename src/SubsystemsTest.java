public class SubsystemsTest {

    public void testEndToEndSingleFireEventFlow_NoHang() throws InterruptedException {
        Scheduler scheduler = new Scheduler();

        // Start one drone with enough agent so we avoid "capacity < needed" edge cases here.
        DroneSubsystem drone = new DroneSubsystem(1, scheduler, 50.0);
        Thread droneThread = new Thread(drone, "Drone-1");
        droneThread.start();

        FireEvent fire = new FireEvent(
                "14:03:15.000",
                3,
                FireEvent.EventType.FIRE_DETECTED,
                FireEvent.Severity.HIGH
        );

        // Receive confirmation in a separate thread so the test never blocks indefinitely.
        final DroneResult[] received = new DroneResult[1];
        Thread receiver = new Thread(() -> received[0] = scheduler.waitForConfirmation(), "Result-Receiver");
        receiver.start();

        // Submit the event (should trigger the drone to pick it up and produce a result)
        scheduler.submitFireEvent(fire);

        // Wait (with timeout) for the result to arrive
        receiver.join(2000);

        // Shutdown cleanly (also ensures the drone unblocks if it's waiting)
        drone.shutdown();
        scheduler.shutdown();

        // Make sure threads exit
        droneThread.join(2000);

        // Assertions
        TestRunner.assertTrue(!receiver.isAlive(), "Result receiver thread should have finished (no indefinite wait)");
        TestRunner.assertTrue(received[0] != null, "Expected a DroneResult confirmation but got null");

        TestRunner.assertEquals(1, received[0].getDroneId(), "Drone ID mismatch");
        TestRunner.assertEquals(3, received[0].getZoneId(), "Zone ID mismatch");
        TestRunner.assertEquals(true, received[0].isTaskCompleted(), "Task should be marked completed");

        // HIGH severity = 30L, capacity started at 50L -> remaining 20.0
        TestRunner.assertEquals(20.0, received[0].getRemainingAgent(), "Remaining agent should be 20.0 after dropping 30L");
    }

    public void testNoResultIfShutdownBeforeWork_NoHang() throws InterruptedException {
        Scheduler scheduler = new Scheduler();

        DroneSubsystem drone = new DroneSubsystem(1, scheduler, 50.0);
        Thread droneThread = new Thread(drone, "Drone-1");
        droneThread.start();

        final DroneResult[] received = new DroneResult[1];
        Thread receiver = new Thread(() -> received[0] = scheduler.waitForConfirmation(), "Result-Receiver");
        receiver.start();

        // Shutdown immediately (no work submitted)
        drone.shutdown();
        scheduler.shutdown();

        receiver.join(1000);
        droneThread.join(2000);

        TestRunner.assertTrue(!receiver.isAlive(), "Receiver should unblock on shutdown");
        TestRunner.assertEquals(null, received[0], "Expected null result after shutdown with no work");
    }
}
