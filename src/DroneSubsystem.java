public class DroneSubsystem implements Runnable {
    private final int droneId;
    private final Scheduler scheduler;
    private final double agentCapacity;
    private double currentAgent;
    private volatile boolean running = true;

    public DroneSubsystem(int droneId, Scheduler scheduler, double agentCapacity) {
        this.droneId = droneId;
        this.scheduler = scheduler;
        this.agentCapacity = agentCapacity;
        this.currentAgent = agentCapacity;
    }


    /**
     * Constructor with default capacity (15L)
     */
    public DroneSubsystem(int droneId, Scheduler scheduler) {
        this(droneId, scheduler, 15.0); 
    }

    @Override
    public void run() {
        while (running) {
            FireEvent task = scheduler.requestNextTask();

            if (task == null) break;

            try {
                handleTask(task);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void handleTask(FireEvent task) throws InterruptedException {
        int needed = task.getLitersNeeded();

        if (currentAgent < needed) {
            System.out.println("[Drone " + droneId + "] Low agent. Refilling...");
            Thread.sleep(500);
            currentAgent = agentCapacity;
        }

        System.out.println("[Drone " + droneId + "] Servicing Zone " + task.getZoneId());
        Thread.sleep(500);
        Thread.sleep(needed * 50);
        
        currentAgent -= needed;

        DroneResult result = new DroneResult(droneId, task.getZoneId(), true, currentAgent);
        scheduler.submitDroneResult(result); 
    }

    public void shutdown() {
        this.running = false;
    }
}