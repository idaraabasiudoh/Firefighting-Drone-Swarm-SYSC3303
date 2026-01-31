public class SubsystemsTest {

    public void testEndToEndSingleFireEventFlow_NoHang() throws InterruptedException {
        Scheduler scheduler = new Scheduler();

        DroneSubsystem drone = new DroneSubsystem(1, scheduler, 50.0);
        Thread droneThread = new Thread(drone, "Drone-1");
        droneThread.start();

        FireEvent fire = new FireEvent(
                "14:03:15.000",
                3,
                FireEvent.EventType.FIRE_DETECTED,
                FireEvent.Severity.HIGH
        );

        // Mutable holder (needed because lambdas can’t modify local variables directly)
        final DroneResult[] received = new DroneResult[1];

        Thread receiver = new Thread(() -> received[0] = scheduler.waitForConfirmation(), "Result-Receiver");
        receiver.start();

        scheduler.submitFireEvent(fire);

        // Give enough time for the drone’s simulated sleeps (travel + drop). Adjust if your drone sleeps longer.
        receiver.join(6000);

        // If still waiting, shut down and fail with a clear error
        if (receiver.isAlive()) {
            drone.shutdown();
            scheduler.shutdown();

            receiver.join(1000);
            droneThread.join(2000);

            throw new AssertionError("Timed out waiting for DroneResult. " +
                    "Either DroneSubsystem sleeps longer than 6s, or the drone never submitted a result.");
        }

        // Now safe to shutdown cleanly
        drone.shutdown();
        scheduler.shutdown();
        droneThread.join(2000);

        // Assertions
        TestRunner.assertTrue(received[0] != null, "Expected a DroneResult confirmation but got null");

        TestRunner.assertEquals(1, received[0].getDroneId(), "Drone ID mismatch");
        TestRunner.assertEquals(3, received[0].getZoneId(), "Zone ID mismatch");
        TestRunner.assertEquals(true, received[0].isTaskCompleted(), "Task should be marked completed");
        TestRunner.assertEquals(20.0, received[0].getRemainingAgent(),
                "Remaining agent should be 20.0 after dropping 30L");
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
