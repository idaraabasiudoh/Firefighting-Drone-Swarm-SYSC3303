// Scheduler.java  (Iteration 4: UDP, multi-drone scheduling, load balancing, redirect, fault handling)
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Scheduler implements Runnable {

    private final Queue<FireEvent> pendingEvents = new LinkedList<>();
    private final Map<Integer, DroneInfo> droneRegistry = new ConcurrentHashMap<>();
    private final Map<Integer, InetAddress> droneAddresses = new ConcurrentHashMap<>();
    private final List<Zone> zones;

    private DatagramSocket fireListenSocket;
    private DatagramSocket droneListenSocket;
    private DatagramSocket sendSocket;

    private InetAddress fireSubsystemAddress;
    private volatile boolean running = true;

    // Iteration 4: Fault detection timeout thresholds
    private static final long TRAVEL_TIMEOUT_MS = 5000;   // 2x travel + buffer
    private static final long DROP_TIMEOUT_MS   = 5000;   // 2x max drop + buffer
    private static final long FAULT_MONITOR_INTERVAL_MS = 1000; // check every 1s

    public Scheduler(List<Zone> zones) {
        this.zones = zones;
    }

    // ==================== MAIN RUN LOOP ====================
    @Override
    public void run() {
        try {
            fireListenSocket = new DatagramSocket(UDPHelper.FIRE_TO_SCHEDULER_PORT);
            droneListenSocket = new DatagramSocket(UDPHelper.DRONE_TO_SCHEDULER_PORT);
            sendSocket = new DatagramSocket();

            log("Running (Iteration 4, multi-drone UDP scheduling with fault handling)");
            log("Listening for fire events on port " + UDPHelper.FIRE_TO_SCHEDULER_PORT);
            log("Listening for drone messages on port " + UDPHelper.DRONE_TO_SCHEDULER_PORT);

            // Start listener threads
            Thread fireListener = new Thread(this::listenForFireEvents, "Scheduler-FireListener");
            Thread droneListener = new Thread(this::listenForDroneMessages, "Scheduler-DroneListener");
            Thread dispatcher = new Thread(this::dispatchLoop, "Scheduler-Dispatcher");
            Thread faultMonitor = new Thread(this::faultMonitorLoop, "Scheduler-FaultMonitor");

            fireListener.start();
            droneListener.start();
            dispatcher.start();
            faultMonitor.start();

            fireListener.join();
            droneListener.join();
            dispatcher.join();
            faultMonitor.join();

        } catch (IOException | InterruptedException e) {
            System.err.println("[Scheduler] Error: " + e.getMessage());
        } finally {
            closeSockets();
        }
        log("Stopped.");
    }

    // ==================== FIRE EVENT LISTENER ====================
    private void listenForFireEvents() {
        while (running) {
            try {
                DatagramPacket packet = UDPHelper.receivePacket(fireListenSocket);
                String msg = UDPHelper.extractMessage(packet);
                String type = UDPHelper.getMessageType(msg);

                // Remember fire subsystem address for sending confirmations back
                fireSubsystemAddress = packet.getAddress();

                if (type.equals(UDPHelper.MSG_FIRE_EVENT)) {
                    FireEvent event = UDPHelper.parseFireEvent(msg);
                    synchronized (pendingEvents) {
                        pendingEvents.add(event);

                        // GUI update
                        GuiModel.get().addActiveFire(event.getZoneId(), event.getSeverity());

                        log("Received fire event: " + event);

                        // Check if any en-route drone should be redirected
                        checkForRedirect(event);

                        pendingEvents.notifyAll();
                    }
                } else if (type.equals(UDPHelper.MSG_SHUTDOWN)) {
                    log("Received shutdown from fire subsystem.");
                    shutdown();
                }
            } catch (IOException e) {
                if (running) System.err.println("[Scheduler] Fire listener error: " + e.getMessage());
            }
        }
    }

    // ==================== DRONE MESSAGE LISTENER ====================
    private void listenForDroneMessages() {
        while (running) {
            try {
                DatagramPacket packet = UDPHelper.receivePacket(droneListenSocket);
                String msg = UDPHelper.extractMessage(packet);
                String type = UDPHelper.getMessageType(msg);

                switch (type) {
                    case UDPHelper.MSG_DRONE_REGISTER:
                        handleDroneRegister(msg, packet.getAddress());
                        break;
                    case UDPHelper.MSG_DRONE_STATUS:
                        handleDroneStatus(msg);
                        break;
                    case UDPHelper.MSG_DRONE_RESULT:
                        handleDroneResult(msg);
                        break;
                    case UDPHelper.MSG_DRONE_FAULT:
                        handleDroneFault(msg);
                        break;
                    default:
                        log("Unknown drone message: " + type);
                }
            } catch (IOException e) {
                if (running) System.err.println("[Scheduler] Drone listener error: " + e.getMessage());
            }
        }
    }

    // ==================== DISPATCH LOOP ====================
    private void dispatchLoop() {
        while (running) {
            synchronized (pendingEvents) {
                while (pendingEvents.isEmpty() && running) {
                    try {
                        pendingEvents.wait(500);
                    } catch (InterruptedException e) {
                        return;
                    }
                }

                if (!running) break;

                // Try to dispatch pending events to idle drones
                Iterator<FireEvent> it = pendingEvents.iterator();
                while (it.hasNext()) {
                    FireEvent event = it.next();
                    DroneInfo bestDrone = findBestDrone(event);

                    if (bestDrone != null) {
                        it.remove();
                        dispatchToDrone(bestDrone, event);
                    }
                }
            }

            try { Thread.sleep(100); } catch (InterruptedException e) { break; }
        }
    }

    // ==================== DRONE REGISTRATION ====================
    private void handleDroneRegister(String msg, InetAddress address) {
        int droneId = UDPHelper.parseDroneRegisterId(msg);
        double capacity = UDPHelper.parseDroneRegisterCapacity(msg);
        int x = UDPHelper.parseDroneRegisterX(msg);
        int y = UDPHelper.parseDroneRegisterY(msg);
        int listenPort = UDPHelper.getDroneListenPort(droneId);

        DroneInfo info = new DroneInfo(droneId, capacity, x, y, listenPort);
        droneRegistry.put(droneId, info);
        droneAddresses.put(droneId, address);

        // GUI update
        GuiModel.get().setDroneState(droneId, DroneState.IDLE);

        log("Drone " + droneId + " registered: capacity=" + capacity
                + " pos=(" + x + "," + y + ") listenPort=" + listenPort);

        // Try to dispatch pending events now that a new drone is available
        synchronized (pendingEvents) {
            pendingEvents.notifyAll();
        }
    }

    // ==================== DRONE STATUS UPDATE ====================
    private void handleDroneStatus(String msg) {
        int droneId = UDPHelper.parseDroneStatusId(msg);
        String stateStr = UDPHelper.parseDroneStatusState(msg);
        int x = UDPHelper.parseDroneStatusX(msg);
        int y = UDPHelper.parseDroneStatusY(msg);
        double agent = UDPHelper.parseDroneStatusAgent(msg);

        DroneInfo info = droneRegistry.get(droneId);
        if (info != null) {
            DroneState state = DroneState.valueOf(stateStr);
            info.setState(state);
            info.setCurrentX(x);
            info.setCurrentY(y);
            info.setRemainingAgent(agent);

            // GUI update
            GuiModel.get().setDroneState(droneId, state);

            // If drone returned to base and is now idle, check for pending work
            if (state == DroneState.IDLE) {
                info.clearAssignment();
                synchronized (pendingEvents) {
                    pendingEvents.notifyAll();
                }
            }
        }
    }

    // ==================== DRONE RESULT HANDLING ====================
    private void handleDroneResult(String msg) {
        DroneResult result = UDPHelper.parseDroneResult(msg);
        int droneId = result.getDroneId();
        DroneInfo info = droneRegistry.get(droneId);

        if (info != null) {
            info.setRemainingAgent(result.getRemainingAgent());
            info.setState(DroneState.IDLE);
            info.clearAssignment();
            info.incrementTasksCompleted();

            // GUI updates
            GuiModel.get().setDroneState(droneId, DroneState.IDLE);
            GuiModel.get().setDroneAssignment(droneId, -1);
            GuiModel.get().removeActiveFire(result.getZoneId());
        }

        log("Drone " + droneId + " completed Zone " + result.getZoneId()
                + " (completed=" + result.isTaskCompleted() + ")");

        // If drone has insufficient agent for even a LOW fire, send it back to base to refill
        if (info != null && result.isTaskCompleted() && result.getRemainingAgent() < 10) {
            sendReturnBase(info);
        }

        // Handle drop failure: re-queue the event
        if (!result.isTaskCompleted()) {
            log("Drop failure at Zone " + result.getZoneId() + " - re-queuing.");
            // Re-create the fire event from the zone info (use HIGH as default for re-queue)
            FireEvent requeue = new FireEvent("REQUEUE", result.getZoneId(),
                    FireEvent.EventType.FIRE_DETECTED,
                    info != null && info.getAssignedSeverity() != null
                            ? FireEvent.Severity.valueOf(info.getAssignedSeverity())
                            : FireEvent.Severity.HIGH);
            synchronized (pendingEvents) {
                pendingEvents.add(requeue);
                GuiModel.get().addActiveFire(requeue.getZoneId(), requeue.getSeverity());
                pendingEvents.notifyAll();
            }
        } else {
            // Send confirmation to Fire Subsystem
            sendConfirmationToFire(droneId, result.getZoneId(), true);
        }

        // Trigger dispatch for pending events
        synchronized (pendingEvents) {
            pendingEvents.notifyAll();
        }
    }

    // ==================== LOAD BALANCING: FIND BEST DRONE ====================
    public DroneInfo findBestDrone(FireEvent event) {
        int zoneId = event.getZoneId();
        int litersNeeded = event.getLitersNeeded();
        Zone targetZone = findZone(zoneId);

        DroneInfo best = null;
        double bestScore = Double.MAX_VALUE;

        for (DroneInfo drone : droneRegistry.values()) {
            if (!drone.isAvailable()) continue;  // Iteration 4: skip offline drones
            if (!drone.hasEnoughAgent(litersNeeded)) continue;

            // Score = distance to zone center (lower is better), with task-count tiebreaker
            double distance = 0;
            if (targetZone != null) {
                double dx = drone.getCurrentX() - targetZone.centerX();
                double dy = drone.getCurrentY() - targetZone.centerY();
                distance = Math.sqrt(dx * dx + dy * dy);
            }

            // Add penalty for drones with more completed tasks (load balancing)
            double score = distance + drone.getTasksCompleted() * 50.0;

            if (score < bestScore) {
                bestScore = score;
                best = drone;
            }
        }

        return best;
    }

    // ==================== DISPATCH TO DRONE ====================
    private void dispatchToDrone(DroneInfo drone, FireEvent event) {
        int droneId = drone.getDroneId();
        drone.setState(DroneState.EN_ROUTE);
        drone.setAssignedZoneId(event.getZoneId());
        drone.setAssignedSeverity(event.getSeverity().name());

        // GUI updates
        GuiModel.get().setDroneState(droneId, DroneState.EN_ROUTE);
        GuiModel.get().setDroneAssignment(droneId, event.getZoneId());

        // Iteration 4: include fault type in command, record dispatch timestamp
        drone.setDispatchTimestamp(System.currentTimeMillis());
        String faultStr = event.getFaultType() != null ? event.getFaultType().name() : "NONE";
        String cmdMsg = UDPHelper.buildDroneCommandMessage(droneId, "TASK",
                event.getZoneId(), event.getSeverity().name(), faultStr);

        try {
            InetAddress droneAddr = droneAddresses.get(droneId);
            if (droneAddr != null) {
                UDPHelper.sendMessage(sendSocket, droneAddr, drone.getListenPort(), cmdMsg);
                log("Dispatched Drone " + droneId + " to Zone " + event.getZoneId()
                        + " (severity=" + event.getSeverity() + ", fault=" + faultStr + ")");
            }
        } catch (IOException e) {
            System.err.println("[Scheduler] Failed to send command to Drone " + droneId + ": " + e.getMessage());
            // Re-queue the event
            synchronized (pendingEvents) {
                pendingEvents.add(event);
                drone.setState(DroneState.IDLE);
                drone.clearAssignment();
            }
        }
    }

    // ==================== PASSTHROUGH REDIRECT ====================
    private void checkForRedirect(FireEvent newEvent) {
        Zone newZone = findZone(newEvent.getZoneId());
        if (newZone == null) return;

        for (DroneInfo drone : droneRegistry.values()) {
            if (drone.getState() != DroneState.EN_ROUTE) continue;
            if (drone.getAssignedZoneId() == newEvent.getZoneId()) continue;

            // Check if same severity
            if (drone.getAssignedSeverity() != null
                    && drone.getAssignedSeverity().equals(newEvent.getSeverity().name())) {

                Zone assignedZone = findZone(drone.getAssignedZoneId());
                if (assignedZone == null) continue;

                // Check if drone passes through the new zone on the way to its assigned zone
                // Simple check: is the new zone closer to the drone than its current target?
                double distToNew = distance(drone.getCurrentX(), drone.getCurrentY(),
                        newZone.centerX(), newZone.centerY());
                double distToAssigned = distance(drone.getCurrentX(), drone.getCurrentY(),
                        assignedZone.centerX(), assignedZone.centerY());

                if (distToNew < distToAssigned) {
                    // Redirect: swap assignments
                    log("Redirecting Drone " + drone.getDroneId()
                            + " from Zone " + drone.getAssignedZoneId() + " to Zone " + newEvent.getZoneId());

                    // Re-queue the old assignment
                    FireEvent oldEvent = new FireEvent("REDIRECT", drone.getAssignedZoneId(),
                            FireEvent.EventType.FIRE_DETECTED,
                            FireEvent.Severity.valueOf(drone.getAssignedSeverity()));
                    pendingEvents.add(oldEvent);

                    // Remove the new event from pending (it was just added)
                    pendingEvents.removeIf(e -> e.getZoneId() == newEvent.getZoneId()
                            && e.getSeverity() == newEvent.getSeverity());

                    // Update drone assignment
                    drone.setAssignedZoneId(newEvent.getZoneId());
                    drone.setAssignedSeverity(newEvent.getSeverity().name());
                    GuiModel.get().setDroneAssignment(drone.getDroneId(), newEvent.getZoneId());

                    // Send redirect command to drone
                    String cmdMsg = UDPHelper.buildDroneCommandMessage(drone.getDroneId(), "REDIRECT",
                            newEvent.getZoneId(), newEvent.getSeverity().name());
                    try {
                        InetAddress droneAddr = droneAddresses.get(drone.getDroneId());
                        if (droneAddr != null) {
                            UDPHelper.sendMessage(sendSocket, droneAddr, drone.getListenPort(), cmdMsg);
                        }
                    } catch (IOException e) {
                        System.err.println("[Scheduler] Failed to send redirect to Drone " + drone.getDroneId());
                    }

                    break; // Only redirect one drone per new event
                }
            }
        }
    }

    // ==================== SEND RETURN_BASE TO DRONE ====================
    private void sendReturnBase(DroneInfo drone) {
        int droneId = drone.getDroneId();
        drone.setState(DroneState.RETURNING_BASE);
        GuiModel.get().setDroneState(droneId, DroneState.RETURNING_BASE);

        String cmdMsg = UDPHelper.buildDroneCommandMessage(droneId, "RETURN_BASE", 0, "NONE");
        try {
            InetAddress droneAddr = droneAddresses.get(droneId);
            if (droneAddr != null) {
                UDPHelper.sendMessage(sendSocket, droneAddr, drone.getListenPort(), cmdMsg);
                log("Sent RETURN_BASE to Drone " + droneId + " (low agent: " + drone.getRemainingAgent() + "L)");
            }
        } catch (IOException e) {
            System.err.println("[Scheduler] Failed to send RETURN_BASE to Drone " + droneId + ": " + e.getMessage());
        }
    }

    // ==================== SEND CONFIRMATION TO FIRE SUBSYSTEM ====================
    private void sendConfirmationToFire(int droneId, int zoneId, boolean completed) {
        if (fireSubsystemAddress == null) return;
        String msg = UDPHelper.buildConfirmationMessage(droneId, zoneId, completed);
        try {
            UDPHelper.sendMessage(sendSocket, fireSubsystemAddress,
                    UDPHelper.SCHEDULER_TO_FIRE_PORT, msg);
        } catch (IOException e) {
            System.err.println("[Scheduler] Failed to send confirmation: " + e.getMessage());
        }
    }

    // ==================== SHUTDOWN ====================
    public void shutdown() {
        running = false;

        // Send shutdown to all drones
        for (Map.Entry<Integer, DroneInfo> entry : droneRegistry.entrySet()) {
            int droneId = entry.getKey();
            DroneInfo info = entry.getValue();
            InetAddress addr = droneAddresses.get(droneId);
            if (addr != null) {
                String shutdownMsg = UDPHelper.buildDroneCommandMessage(droneId, "SHUTDOWN", 0, "NONE");
                try {
                    UDPHelper.sendMessage(sendSocket, addr, info.getListenPort(), shutdownMsg);
                } catch (IOException e) {
                    // ignore on shutdown
                }
            }
            GuiModel.get().setDroneState(droneId, DroneState.SHUTDOWN);
        }

        // Send shutdown to fire subsystem
        if (fireSubsystemAddress != null) {
            try {
                UDPHelper.sendMessage(sendSocket, fireSubsystemAddress,
                        UDPHelper.SCHEDULER_TO_FIRE_PORT, UDPHelper.buildShutdownMessage());
            } catch (IOException e) {
                // ignore on shutdown
            }
        }

        closeSockets();
    }

    private void closeSockets() {
        if (fireListenSocket != null && !fireListenSocket.isClosed()) fireListenSocket.close();
        if (droneListenSocket != null && !droneListenSocket.isClosed()) droneListenSocket.close();
        if (sendSocket != null && !sendSocket.isClosed()) sendSocket.close();
    }

    // ==================== FAULT HANDLING (Iteration 4) ====================

    private void handleDroneFault(String msg) {
        int droneId = UDPHelper.parseDroneFaultDroneId(msg);
        String faultStr = UDPHelper.parseDroneFaultType(msg);
        int zoneId = UDPHelper.parseDroneFaultZoneId(msg);
        FaultType fault = FaultType.fromString(faultStr);

        DroneInfo info = droneRegistry.get(droneId);
        if (info == null) return;

        info.setCurrentFault(fault);
        info.incrementFaultCount();
        log("FAULT DETECTED: Drone " + droneId + " reported " + fault + " at Zone " + zoneId);

        // GUI update for fault
        GuiModel.get().setDroneFault(droneId, fault);

        if (fault.isHardFault()) {
            // Hard fault: mark drone permanently offline, re-queue event
            info.setPermanentlyOffline(true);
            info.setState(DroneState.OFFLINE);
            GuiModel.get().setDroneState(droneId, DroneState.OFFLINE);
            log("Drone " + droneId + " PERMANENTLY OFFLINE due to " + fault);

            // Re-queue the fire event for another drone
            requeueZone(zoneId, info.getAssignedSeverity());
            info.clearAssignment();

        } else if (fault.isSoftFault()) {
            // Soft fault: drone will self-recover, re-queue the event
            log("Drone " + droneId + " soft fault (" + fault + ") — will self-recover, re-queuing Zone " + zoneId);

            requeueZone(zoneId, info.getAssignedSeverity());
            info.clearAssignment();
        }
    }

    private void requeueZone(int zoneId, String severity) {
        FireEvent.Severity sev;
        try {
            sev = (severity != null) ? FireEvent.Severity.valueOf(severity) : FireEvent.Severity.HIGH;
        } catch (IllegalArgumentException e) {
            sev = FireEvent.Severity.HIGH;
        }
        FireEvent requeue = new FireEvent("FAULT_REQUEUE", zoneId,
                FireEvent.EventType.FIRE_DETECTED, sev);
        synchronized (pendingEvents) {
            pendingEvents.add(requeue);
            GuiModel.get().addActiveFire(zoneId, sev);
            pendingEvents.notifyAll();
        }
        log("Re-queued Zone " + zoneId + " (severity=" + sev + ") due to fault");
    }

    // ==================== FAULT MONITOR THREAD ====================

    private void faultMonitorLoop() {
        while (running) {
            try {
                Thread.sleep(FAULT_MONITOR_INTERVAL_MS);
            } catch (InterruptedException e) {
                break;
            }

            long now = System.currentTimeMillis();
            for (DroneInfo drone : droneRegistry.values()) {
                if (drone.isPermanentlyOffline()) continue;
                if (drone.getDispatchTimestamp() == 0) continue;

                long elapsed = now - drone.getDispatchTimestamp();
                DroneState state = drone.getState();

                // Timeout detection for EN_ROUTE drones
                if (state == DroneState.EN_ROUTE && elapsed > TRAVEL_TIMEOUT_MS) {
                    log("TIMEOUT: Drone " + drone.getDroneId() + " stuck EN_ROUTE for " + elapsed + "ms");
                    handleTimeoutFault(drone);
                }

                // Timeout detection for DROPPING_AGENT drones
                if (state == DroneState.DROPPING_AGENT && elapsed > (TRAVEL_TIMEOUT_MS + DROP_TIMEOUT_MS)) {
                    log("TIMEOUT: Drone " + drone.getDroneId() + " stuck DROPPING_AGENT for " + elapsed + "ms");
                    handleTimeoutFault(drone);
                }
            }
        }
    }

    private void handleTimeoutFault(DroneInfo drone) {
        int droneId = drone.getDroneId();
        int zoneId = drone.getAssignedZoneId();

        // If drone already reported a fault, don't double-handle
        if (drone.getCurrentFault() != FaultType.NONE) return;

        log("Timeout fault for Drone " + droneId + " — marking as potential fault, re-queuing Zone " + zoneId);
        drone.setCurrentFault(FaultType.DRONE_STUCK);
        drone.incrementFaultCount();
        GuiModel.get().setDroneFault(droneId, FaultType.DRONE_STUCK);

        // Re-queue the zone
        if (zoneId > 0) {
            requeueZone(zoneId, drone.getAssignedSeverity());
        }
    }

    // ==================== HELPERS ====================

    private void log(String message) {
        System.out.println("[" + UDPHelper.timestamp() + "] [Scheduler] " + message);
    }

    private Zone findZone(int zoneId) {
        for (Zone z : zones) {
            if (z.getId() == zoneId) return z;
        }
        return null;
    }

    private static double distance(double x1, double y1, double x2, double y2) {
        return Math.sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1));
    }

    // Expose for testing
    public Map<Integer, DroneInfo> getDroneRegistry() { return droneRegistry; }
    public Queue<FireEvent> getPendingEvents() { return pendingEvents; }
}