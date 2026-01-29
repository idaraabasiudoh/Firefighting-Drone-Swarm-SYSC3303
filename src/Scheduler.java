import java.util.LinkedList;
import java.util.Queue;

public class Scheduler implements Runnable {

    private final Queue<FireEvent> pendingEvents = new LinkedList<>();
    private final Queue<DroneResult> completedResults = new LinkedList<>();

    private boolean running = true;

    // ---------------- FIRE → SCHEDULER ----------------

    public synchronized void submitFireEvent(FireEvent event) {
        pendingEvents.add(event);
        System.out.println("[Scheduler] Received from Fire: " + event);
        notifyAll(); // wake drones waiting for work
    }

    // ---------------- DRONE → SCHEDULER ----------------

    public synchronized FireEvent requestNextTask() {
        while (pendingEvents.isEmpty() && running) {
            try {
                wait(); // wait until Fire submits an event
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        return pendingEvents.poll();
    }

    public synchronized void submitDroneResult(DroneResult result) {
        completedResults.add(result);
        System.out.println("[Scheduler] Received from Drone: " + result);
        notifyAll(); // wake Fire waiting for confirmation
    }

    // ---------------- SCHEDULER → FIRE ----------------

    public synchronized DroneResult waitForConfirmation() {
        while (completedResults.isEmpty() && running) {
            try {
                wait(); // wait until Drone finishes
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        return completedResults.poll();
    }

    public synchronized void shutdown() {
        running = false;
        notifyAll();
    }

    // ---------------- MAIN LOOP (not much needed in Iteration 1) ----------------

    @Override
    public void run() {
        System.out.println("[Scheduler] Running (monitor-based)");
        while (running) {
            try {
                Thread.sleep(500); // Scheduler mostly waits; logic is in methods
            } catch (InterruptedException e) {
                break;
            }
        }
    }

}
