public class SchedulerTest {

    public void testSubmitAndRequestFlow() throws InterruptedException {
        Scheduler scheduler = new Scheduler();
        FireEvent event = new FireEvent("12:00", 1, FireEvent.EventType.FIRE_DETECTED, FireEvent.Severity.LOW);
        
        // Start a consumer thread that requests a task
        final FireEvent[] receivedTask = new FireEvent[1];
        Thread droneThread = new Thread(() -> {
            receivedTask[0] = scheduler.requestNextTask();
        });
        droneThread.start();
        
        // Give the thread a moment to block
        Thread.sleep(100);
        
        // Producer submits task
        scheduler.submitFireEvent(event);
        
        // Wait for thread to finish
        droneThread.join(1000);
        
        TestRunner.assertNotNull(receivedTask[0], "Task should have been received by drone thread");
        TestRunner.assertEquals(event, receivedTask[0], "Received task does not match submitted task");
        
        scheduler.shutdown();
    }
    
    public void testResultFlow() throws InterruptedException {
        Scheduler scheduler = new Scheduler();
        DroneResult result = new DroneResult(1, 1, true, 10.0);
        
        // Start a consumer thread that waits for result (Fire Incident Subsystem)
        final DroneResult[] receivedResult = new DroneResult[1];
        Thread fireThread = new Thread(() -> {
            receivedResult[0] = scheduler.waitForConfirmation();
        });
        fireThread.start();
        
        Thread.sleep(100);
        
        // Producer submits result (Drone)
        scheduler.submitDroneResult(result);
        
        fireThread.join(1000);
        
        TestRunner.assertNotNull(receivedResult[0], "Result should have been received by fire thread");
        TestRunner.assertEquals(result, receivedResult[0], "Received result does not match submitted result");
        
        scheduler.shutdown();
    }
}
