package scheduler;

import drone.*;
import fireincident.FireEvent;
import gui.GuiModel;
import gui.Zone;
import network.UDPHelper;

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

    private static final long TRAVEL_TIMEOUT_MS = 5000;
    private static final long DROP_TIMEOUT_MS = 5000;
    private static final long FAULT_MONITOR_INTERVAL_MS = 1000;

    public Scheduler(List<Zone> zones) {
        this.zones = zones;
    }

    @Override
    public void run() {
        try {
            fireListenSocket = new DatagramSocket(UDPHelper.FIRE_TO_SCHEDULER_PORT);
            droneListenSocket = new DatagramSocket(UDPHelper.DRONE_TO_SCHEDULER_PORT);
            sendSocket = new DatagramSocket();

            log("Running");
            log("Listening for fire events on port " + UDPHelper.FIRE_TO_SCHEDULER_PORT);
            log("Listening for drone messages on port " + UDPHelper.DRONE_TO_SCHEDULER_PORT);

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

    private void listenForFireEvents() {
        while (running) {
            try {
                DatagramPacket packet = UDPHelper.receivePacket(fireListenSocket);
                String msg = UDPHelper.extractMessage(packet);
                String type = UDPHelper.getMessageType(msg);

                fireSubsystemAddress = packet.getAddress();

                if (type.equals(UDPHelper.MSG_FIRE_EVENT)) {
                    FireEvent event = UDPHelper.parseFireEvent(msg);
                    synchronized (pendingEvents) {
                        pendingEvents.add(event);
                        GuiModel.get().addActiveFire(event.getZoneId(), event.getSeverity());
                        log("Received fire event: " + event);
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

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    private void handleDroneRegister(String msg, InetAddress address) {
        int droneId = UDPHelper.parseDroneRegisterId(msg);
        double capacity = UDPHelper.parseDroneRegisterCapacity(msg);
        int x = UDPHelper.parseDroneRegisterX(msg);
        int y = UDPHelper.parseDroneRegisterY(msg);
        int listenPort = UDPHelper.getDroneListenPort(droneId);

        DroneInfo info = new DroneInfo(droneId, capacity, x, y, listenPort);
        droneRegistry.put(droneId, info);
        droneAddresses.put(droneId, address);

        GuiModel.get().setDroneState(droneId, DroneState.IDLE);
        GuiModel.get().setDronePosition(droneId, x, y);

        log("Drone " + droneId + " registered: capacity=" + capacity
                + " pos=(" + x + "," + y + ") listenPort=" + listenPort);

        synchronized (pendingEvents) {
            pendingEvents.notifyAll();
        }
    }

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

            GuiModel.get().setDroneState(droneId, state);
            GuiModel.get().setDronePosition(droneId, x, y);

            if (state == DroneState.IDLE) {
                info.clearAssignment();
                synchronized (pendingEvents) {
                    pendingEvents.notifyAll();
                }
            }
        }
    }

    private void handleDroneResult(String msg) {
        DroneResult result = UDPHelper.parseDroneResult(msg);
        int droneId = result.getDroneId();
        DroneInfo info = droneRegistry.get(droneId);

        if (info != null) {
            info.setRemainingAgent(result.getRemainingAgent());
            info.setState(DroneState.IDLE);
            info.clearAssignment();
            info.incrementTasksCompleted();

            GuiModel.get().setDroneState(droneId, DroneState.IDLE);
            GuiModel.get().setDroneAssignment(droneId, -1);
            GuiModel.get().removeActiveFire(result.getZoneId());
        }

        log("Drone " + droneId + " completed Zone " + result.getZoneId()
                + " (completed=" + result.isTaskCompleted() + ")");

        if (info != null && result.isTaskCompleted() && result.getRemainingAgent() < 10) {
            sendReturnBase(info);
        }

        if (!result.isTaskCompleted()) {
            FireEvent requeue = new FireEvent(
                    "REQUEUE",
                    result.getZoneId(),
                    FireEvent.EventType.FIRE_DETECTED,
                    info != null && info.getAssignedSeverity() != null
                            ? FireEvent.Severity.valueOf(info.getAssignedSeverity())
                            : FireEvent.Severity.HIGH
            );

            synchronized (pendingEvents) {
                pendingEvents.add(requeue);
                GuiModel.get().addActiveFire(requeue.getZoneId(), requeue.getSeverity());
                pendingEvents.notifyAll();
            }
        } else {
            sendConfirmationToFire(droneId, result.getZoneId(), true);
        }

        synchronized (pendingEvents) {
            pendingEvents.notifyAll();
        }
    }

    public DroneInfo findBestDrone(FireEvent event) {
        int zoneId = event.getZoneId();
        int litersNeeded = event.getLitersNeeded();
        Zone targetZone = findZone(zoneId);

        DroneInfo best = null;
        double bestScore = Double.MAX_VALUE;

        for (DroneInfo drone : droneRegistry.values()) {
            if (!drone.isAvailable()) continue;
            if (!drone.hasEnoughAgent(litersNeeded)) continue;

            double distance = 0;
            if (targetZone != null) {
                double dx = drone.getCurrentX() - targetZone.centerX();
                double dy = drone.getCurrentY() - targetZone.centerY();
                distance = Math.sqrt(dx * dx + dy * dy);
            }

            double score = distance + drone.getTasksCompleted() * 50.0;

            if (score < bestScore) {
                bestScore = score;
                best = drone;
            }
        }

        return best;
    }

    private void dispatchToDrone(DroneInfo drone, FireEvent event) {
        int droneId = drone.getDroneId();
        drone.setState(DroneState.EN_ROUTE);
        drone.setAssignedZoneId(event.getZoneId());
        drone.setAssignedSeverity(event.getSeverity().name());

        GuiModel.get().setDroneState(droneId, DroneState.EN_ROUTE);
        GuiModel.get().setDroneAssignment(droneId, event.getZoneId());

        drone.setDispatchTimestamp(System.currentTimeMillis());
        String faultStr = event.getFaultType() != null ? event.getFaultType().name() : "NONE";

        String cmdMsg = UDPHelper.buildDroneCommandMessage(
                droneId,
                "TASK",
                event.getZoneId(),
                event.getSeverity().name(),
                faultStr
        );

        try {
            InetAddress droneAddr = droneAddresses.get(droneId);
            if (droneAddr != null) {
                UDPHelper.sendMessage(sendSocket, droneAddr, drone.getListenPort(), cmdMsg);
                log("Dispatched Drone " + droneId + " to Zone " + event.getZoneId());
            }
        } catch (IOException e) {
            System.err.println("[Scheduler] Failed to send command to Drone " + droneId + ": " + e.getMessage());
            synchronized (pendingEvents) {
                pendingEvents.add(event);
                drone.setState(DroneState.IDLE);
                drone.clearAssignment();
            }
        }
    }

    private void checkForRedirect(FireEvent newEvent) {
        Zone newZone = findZone(newEvent.getZoneId());
        if (newZone == null) return;

        for (DroneInfo drone : droneRegistry.values()) {
            if (drone.getState() != DroneState.EN_ROUTE) continue;
            if (drone.getAssignedZoneId() == newEvent.getZoneId()) continue;

            if (drone.getAssignedSeverity() != null
                    && drone.getAssignedSeverity().equals(newEvent.getSeverity().name())) {

                Zone assignedZone = findZone(drone.getAssignedZoneId());
                if (assignedZone == null) continue;

                double distToNew = distance(
                        drone.getCurrentX(), drone.getCurrentY(),
                        newZone.centerX(), newZone.centerY()
                );

                double distToAssigned = distance(
                        drone.getCurrentX(), drone.getCurrentY(),
                        assignedZone.centerX(), assignedZone.centerY()
                );

                if (distToNew < distToAssigned) {
                    FireEvent oldEvent = new FireEvent(
                            "REDIRECT",
                            drone.getAssignedZoneId(),
                            FireEvent.EventType.FIRE_DETECTED,
                            FireEvent.Severity.valueOf(drone.getAssignedSeverity())
                    );
                    pendingEvents.add(oldEvent);

                    pendingEvents.removeIf(e ->
                            e.getZoneId() == newEvent.getZoneId()
                                    && e.getSeverity() == newEvent.getSeverity());

                    drone.setAssignedZoneId(newEvent.getZoneId());
                    drone.setAssignedSeverity(newEvent.getSeverity().name());
                    GuiModel.get().setDroneAssignment(drone.getDroneId(), newEvent.getZoneId());

                    String cmdMsg = UDPHelper.buildDroneCommandMessage(
                            drone.getDroneId(),
                            "REDIRECT",
                            newEvent.getZoneId(),
                            newEvent.getSeverity().name()
                    );

                    try {
                        InetAddress droneAddr = droneAddresses.get(drone.getDroneId());
                        if (droneAddr != null) {
                            UDPHelper.sendMessage(sendSocket, droneAddr, drone.getListenPort(), cmdMsg);
                        }
                    } catch (IOException e) {
                        System.err.println("[Scheduler] Failed to send redirect to Drone " + drone.getDroneId());
                    }

                    break;
                }
            }
        }
    }

    private void sendReturnBase(DroneInfo drone) {
        int droneId = drone.getDroneId();
        drone.setState(DroneState.RETURNING_BASE);
        GuiModel.get().setDroneState(droneId, DroneState.RETURNING_BASE);

        String cmdMsg = UDPHelper.buildDroneCommandMessage(droneId, "RETURN_BASE", 0, "NONE");
        try {
            InetAddress droneAddr = droneAddresses.get(droneId);
            if (droneAddr != null) {
                UDPHelper.sendMessage(sendSocket, droneAddr, drone.getListenPort(), cmdMsg);
            }
        } catch (IOException e) {
            System.err.println("[Scheduler] Failed to send RETURN_BASE to Drone " + droneId + ": " + e.getMessage());
        }
    }

    private void sendConfirmationToFire(int droneId, int zoneId, boolean completed) {
        if (fireSubsystemAddress == null) return;

        String msg = UDPHelper.buildConfirmationMessage(droneId, zoneId, completed);
        try {
            UDPHelper.sendMessage(sendSocket, fireSubsystemAddress, UDPHelper.SCHEDULER_TO_FIRE_PORT, msg);
        } catch (IOException e) {
            System.err.println("[Scheduler] Failed to send confirmation: " + e.getMessage());
        }
    }

    public void shutdown() {
        running = false;

        for (Map.Entry<Integer, DroneInfo> entry : droneRegistry.entrySet()) {
            int droneId = entry.getKey();
            DroneInfo info = entry.getValue();
            InetAddress addr = droneAddresses.get(droneId);

            if (addr != null) {
                String shutdownMsg = UDPHelper.buildDroneCommandMessage(droneId, "SHUTDOWN", 0, "NONE");
                try {
                    UDPHelper.sendMessage(sendSocket, addr, info.getListenPort(), shutdownMsg);
                } catch (IOException ignored) {
                }
            }

            GuiModel.get().setDroneState(droneId, DroneState.SHUTDOWN);
        }

        if (fireSubsystemAddress != null) {
            try {
                UDPHelper.sendMessage(
                        sendSocket,
                        fireSubsystemAddress,
                        UDPHelper.SCHEDULER_TO_FIRE_PORT,
                        UDPHelper.buildShutdownMessage()
                );
            } catch (IOException ignored) {
            }
        }

        closeSockets();
    }

    private void closeSockets() {
        if (fireListenSocket != null && !fireListenSocket.isClosed()) fireListenSocket.close();
        if (droneListenSocket != null && !droneListenSocket.isClosed()) droneListenSocket.close();
        if (sendSocket != null && !sendSocket.isClosed()) sendSocket.close();
    }

    private void handleDroneFault(String msg) {
        int droneId = UDPHelper.parseDroneFaultDroneId(msg);
        String faultStr = UDPHelper.parseDroneFaultType(msg);
        int zoneId = UDPHelper.parseDroneFaultZoneId(msg);
        FaultType fault = FaultType.fromString(faultStr);

        DroneInfo info = droneRegistry.get(droneId);
        if (info == null) return;

        info.setCurrentFault(fault);
        info.incrementFaultCount();
        GuiModel.get().setDroneFault(droneId, fault);

        if (fault.isHardFault()) {
            info.setPermanentlyOffline(true);
            info.setState(DroneState.OFFLINE);
            GuiModel.get().setDroneState(droneId, DroneState.OFFLINE);
            requeueZone(zoneId, info.getAssignedSeverity());
            info.clearAssignment();
        } else if (fault.isSoftFault()) {
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

        FireEvent requeue = new FireEvent("FAULT_REQUEUE", zoneId, FireEvent.EventType.FIRE_DETECTED, sev);

        synchronized (pendingEvents) {
            pendingEvents.add(requeue);
            GuiModel.get().addActiveFire(zoneId, sev);
            pendingEvents.notifyAll();
        }
    }

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

                if (state == DroneState.EN_ROUTE && elapsed > TRAVEL_TIMEOUT_MS) {
                    handleTimeoutFault(drone);
                }

                if (state == DroneState.DROPPING_AGENT && elapsed > (TRAVEL_TIMEOUT_MS + DROP_TIMEOUT_MS)) {
                    handleTimeoutFault(drone);
                }
            }
        }
    }

    private void handleTimeoutFault(DroneInfo drone) {
        int droneId = drone.getDroneId();
        int zoneId = drone.getAssignedZoneId();

        if (drone.getCurrentFault() != FaultType.NONE) return;

        drone.setCurrentFault(FaultType.DRONE_STUCK);
        drone.incrementFaultCount();
        GuiModel.get().setDroneFault(droneId, FaultType.DRONE_STUCK);

        if (zoneId > 0) {
            requeueZone(zoneId, drone.getAssignedSeverity());
        }
    }

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

    public Map<Integer, DroneInfo> getDroneRegistry() {
        return droneRegistry;
    }

    public Queue<FireEvent> getPendingEvents() {
        return pendingEvents;
    }
}