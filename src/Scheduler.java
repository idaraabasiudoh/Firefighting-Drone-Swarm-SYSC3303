// Scheduler.java  (UPDATED for Iteration 2: single-drone scheduling + GUI updates)
// REPLACE your Scheduler with this version
import java.util.LinkedList;
import java.util.Queue;

public class Scheduler implements Runnable {

    private final Queue<FireEvent> pendingEvents = new LinkedList<>();
    private final Queue<DroneResult> completedResults = new LinkedList<>();

    // Iteration 2 (single drone) scheduling state
    private DroneState droneState = DroneState.IDLE;
    private final double agentCapacityLiters = 30.0;  // was 15.0
    private double droneRemainingAgent = agentCapacityLiters;
    private boolean running = true;

    // ---------------- FIRE → SCHEDULER ----------------
    public synchronized void submitFireEvent(FireEvent event) {
        pendingEvents.add(event);

        // GUI: new incident becomes active
        GuiModel.get().addActiveFire(event.getZoneId());

        System.out.println("[Scheduler] Received from Fire: " + event);
        notifyAll();
    }

    // ---------------- DRONE → SCHEDULER ----------------
    // Drone asks: "what should I do next?"
    public synchronized DroneCommand requestNextCommand() {
        while (running) {
            // If drone is idle and we have a pending event, try dispatch
            if (droneState == DroneState.IDLE && !pendingEvents.isEmpty()) {
                FireEvent next = pendingEvents.peek();
                int needed = next.getLitersNeeded();

                // If not enough agent, tell drone to return to base (instant refill at base after travel)
                if (droneRemainingAgent < needed) {
                    droneState = DroneState.RETURNING_BASE;
                    GuiModel.get().setDroneState(droneState);
                    return DroneCommand.returnBase();
                }

                // Dispatch task
                pendingEvents.poll();
                droneState = DroneState.EN_ROUTE;
                GuiModel.get().setDroneState(droneState);
                return DroneCommand.task(next);
            }

            // If drone is idle and no pending work: return to base (optional),
            // but only if it's not already at base (we treat IDLE as "at base").
            // So just wait.
            try {
                wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return DroneCommand.shutdown();
            }
        }

        return DroneCommand.shutdown();
    }

    // Drone reports completion
    public synchronized void submitDroneResult(DroneResult result) {
        completedResults.add(result);

        // Update remaining agent from result
        droneRemainingAgent = result.getRemainingAgent();

        // GUI: incident cleared
        GuiModel.get().removeActiveFire(result.getZoneId());

        // After completion, drone is effectively "checking queue"
        droneState = DroneState.IDLE;
        GuiModel.get().setDroneState(droneState);

        System.out.println("[Scheduler] Received from Drone: " + result);
        notifyAll();
    }

    // ---------------- SCHEDULER → FIRE ----------------
    public synchronized DroneResult waitForConfirmation() {
        while (completedResults.isEmpty() && running) {
            try {
                wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }
        return completedResults.poll();
    }

    public synchronized void onDroneArrivedAtBaseRefillComplete() {
        // Assumption: at base, drone is instantly refilled/recharged and ready
        droneRemainingAgent = agentCapacityLiters;
        droneState = DroneState.IDLE;
        GuiModel.get().setDroneState(droneState);
        notifyAll();
    }

    public synchronized void shutdown() {
        running = false;
        droneState = DroneState.SHUTDOWN;
        GuiModel.get().setDroneState(droneState);
        notifyAll();
    }

    @Override
    public void run() {
        System.out.println("[Scheduler] Running (Iteration 2, single-drone scheduling)");
        while (running) {
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                break;
            }
        }
        System.out.println("[Scheduler] Stopped.");
    }
}