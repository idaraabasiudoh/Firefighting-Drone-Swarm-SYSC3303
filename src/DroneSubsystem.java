// DroneSubsystem.java  (UPDATED for Iteration 2 state transitions + command model)
// REPLACE your DroneSubsystem with this version
public class DroneSubsystem implements Runnable {
    private final int droneId;
    private final Scheduler scheduler;

    private final double agentCapacity;
    private double currentAgent;

    private volatile boolean running = true;

    private DroneState state = DroneState.IDLE;

    public DroneSubsystem(int droneId, Scheduler scheduler, double agentCapacity) {
        this.droneId = droneId;
        this.scheduler = scheduler;
        this.agentCapacity = agentCapacity;
        this.currentAgent = agentCapacity;
    }

    public DroneSubsystem(int droneId, Scheduler scheduler) {
        this(droneId, scheduler, 30.0);  // was 15.0
    }

    @Override
    public void run() {
        while (running) {
            DroneCommand cmd = scheduler.requestNextCommand();

            if (cmd.type() == DroneCommand.Type.SHUTDOWN) {
                setState(DroneState.SHUTDOWN);
                break;
            }

            if (cmd.type() == DroneCommand.Type.RETURN_BASE) {
                doReturnToBase();
                continue;
            }

            // TASK
            FireEvent task = cmd.task();
            if (task == null) continue;

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

        // If somehow not enough, return to base (scheduler tries to prevent this already)
        if (currentAgent < needed) {
            doReturnToBase();
        }

        setState(DroneState.EN_ROUTE);
        System.out.println("[Drone " + droneId + "] EN_ROUTE to Zone " + task.getZoneId());
        Thread.sleep(travelTimeMs(task));

        setState(DroneState.DROPPING_AGENT);
        System.out.println("[Drone " + droneId + "] DROPPING_AGENT at Zone " + task.getZoneId());

        // “Nozzle door opening time” folded into drop time
        Thread.sleep(nozzleOpenMs() + dropAgentMs(needed));

        currentAgent -= needed;

        DroneResult result = new DroneResult(droneId, task.getZoneId(), true, currentAgent);
        scheduler.submitDroneResult(result);

        // After reporting, drone goes back to scheduler for next command (loop continues)
        setState(DroneState.IDLE);
    }

    private void doReturnToBase() {
        try {
            setState(DroneState.RETURNING_BASE);
            System.out.println("[Drone " + droneId + "] RETURNING_BASE");
            Thread.sleep(returnTimeMs());

            // Assumption: instantly refilled/recharged at base
            currentAgent = agentCapacity;
            scheduler.onDroneArrivedAtBaseRefillComplete();

            setState(DroneState.IDLE);
            System.out.println("[Drone " + droneId + "] IDLE at base (refilled)");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void setState(DroneState newState) {
        this.state = newState;
        GuiModel.get().setDroneState(newState); // GUI tracks current drone state
    }

    // --- Timing helpers (simple in Iteration 2; tune later with zone coords if you want) ---
    private int travelTimeMs(FireEvent task) { return 800; }
    private int returnTimeMs() { return 800; }
    private int nozzleOpenMs() { return 150; }
    private int dropAgentMs(int liters) { return liters * 40; }

    public void shutdown() {
        this.running = false;
    }
}