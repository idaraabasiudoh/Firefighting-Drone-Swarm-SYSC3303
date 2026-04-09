package drone;

import gui.GuiModel;
import network.UDPHelper;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;

public class DroneSubsystem implements Runnable {
    private final int droneId;
    private final InetAddress schedulerAddress;
    private final int schedulerPort;

    private final double agentCapacity;
    private double currentAgent;

    private volatile boolean running = true;

    private DroneState state = DroneState.IDLE;
    private int currentX = 0;
    private int currentY = 0;

    private volatile int redirectZoneId = -1;
    private volatile String redirectSeverity = null;
    private volatile int targetX = 0;
    private volatile int targetY = 0;

    private DatagramSocket commandSocket;
    private DatagramSocket sendSocket;

    private static final int FAULT_RESET_DELAY_MS = 3000;

    public DroneSubsystem(int droneId, InetAddress schedulerAddress, double agentCapacity) {
        this.droneId = droneId;
        this.schedulerAddress = schedulerAddress;
        this.schedulerPort = UDPHelper.DRONE_TO_SCHEDULER_PORT;
        this.agentCapacity = agentCapacity;
        this.currentAgent = agentCapacity;
    }

    public DroneSubsystem(int droneId, InetAddress schedulerAddress) {
        this(droneId, schedulerAddress, 30.0);
    }

    @Override
    public void run() {
        try {
            int listenPort = UDPHelper.getDroneListenPort(droneId);
            commandSocket = new DatagramSocket(listenPort);
            commandSocket.setSoTimeout(2000);
            sendSocket = new DatagramSocket();

            log("Started, listening on port " + listenPort);
            registerWithScheduler();

            while (running) {
                try {
                    String msg = UDPHelper.receiveMessage(commandSocket);
                    String type = UDPHelper.getMessageType(msg);

                    if (type.equals(UDPHelper.MSG_DRONE_COMMAND)) {
                        String cmdType = UDPHelper.parseDroneCommandType(msg);

                        switch (cmdType) {
                            case "TASK":
                                handleTask(msg);
                                break;
                            case "RETURN_BASE":
                                doReturnToBase();
                                break;
                            case "REDIRECT":
                                handleRedirect(msg);
                                break;
                            case "SHUTDOWN":
                                log("Received SHUTDOWN");
                                setState(DroneState.SHUTDOWN);
                                running = false;
                                break;
                            default:
                                log("Unknown command: " + cmdType);
                        }
                    }
                } catch (SocketTimeoutException e) {
                    // normal polling timeout
                } catch (IOException e) {
                    if (running) {
                        System.err.println("[Drone " + droneId + "] Receive error: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("[Drone " + droneId + "] Fatal error: " + e.getMessage());
        } finally {
            if (commandSocket != null && !commandSocket.isClosed()) commandSocket.close();
            if (sendSocket != null && !sendSocket.isClosed()) sendSocket.close();
        }
        log("Shutdown complete.");
    }

    private void registerWithScheduler() throws IOException {
        String msg = UDPHelper.buildDroneRegisterMessage(droneId, agentCapacity, currentX, currentY);
        UDPHelper.sendMessage(sendSocket, schedulerAddress, schedulerPort, msg);
        log("Registered with scheduler");
        sendStatusToScheduler();
    }

    private void sendStatusToScheduler() {
        try {
            String msg = UDPHelper.buildDroneStatusMessage(
                    droneId, state.name(), currentX, currentY, currentAgent
            );
            UDPHelper.sendMessage(sendSocket, schedulerAddress, schedulerPort, msg);
        } catch (IOException e) {
            System.err.println("[Drone " + droneId + "] Failed to send status: " + e.getMessage());
        }
    }

    private void sendResultToScheduler(int zoneId, boolean completed) {
        try {
            String msg = UDPHelper.buildDroneResultMessage(droneId, zoneId, completed, currentAgent);
            UDPHelper.sendMessage(sendSocket, schedulerAddress, schedulerPort, msg);
        } catch (IOException e) {
            System.err.println("[Drone " + droneId + "] Failed to send result: " + e.getMessage());
        }
    }

    private void sendFaultToScheduler(FaultType fault, int zoneId) {
        try {
            String msg = UDPHelper.buildDroneFaultMessage(droneId, fault.name(), zoneId);
            UDPHelper.sendMessage(sendSocket, schedulerAddress, schedulerPort, msg);
        } catch (IOException e) {
            System.err.println("[Drone " + droneId + "] Failed to send fault report: " + e.getMessage());
        }
    }

    private void handleTask(String msg) {
        int zoneId = UDPHelper.parseDroneCommandZoneId(msg);
        String severity = UDPHelper.parseDroneCommandSeverity(msg);
        FaultType fault = FaultType.fromString(UDPHelper.parseDroneCommandFault(msg));

        // Read target coords from message (set by Scheduler from zone CSV)
        int msgTargetX = UDPHelper.parseDroneCommandTargetX(msg);
        int msgTargetY = UDPHelper.parseDroneCommandTargetY(msg);
        targetX = msgTargetX;
        targetY = msgTargetY;

        int litersNeeded = litersForSeverity(severity);

        if (currentAgent < litersNeeded) {
            doReturnToBase();
        }

        try {
            redirectZoneId = -1;
            redirectSeverity = null;

            setState(DroneState.EN_ROUTE);
            sendStatusToScheduler();
            log("EN_ROUTE to Zone " + zoneId + " at (" + targetX + "," + targetY + ")"
                    + (fault != FaultType.NONE ? " [FAULT INJECTED: " + fault + "]" : ""));

            if (fault == FaultType.DRONE_STUCK) {
                moveTowardsTarget(travelTimeMs() / 2);
                log("FAULT: Stuck mid-flight to Zone " + zoneId);
                setState(DroneState.FAULT_STUCK);
                sendStatusToScheduler();
                sendFaultToScheduler(FaultType.DRONE_STUCK, zoneId);

                Thread.sleep(FAULT_RESET_DELAY_MS);
                doReturnToBase();
                return;
            }

            if (fault == FaultType.SENSOR_FAIL) {
                moveTowardsTarget(travelTimeMs());
                log("FAULT: Arrival sensor failed at Zone " + zoneId);
                setState(DroneState.FAULT_SENSOR);
                sendStatusToScheduler();
                sendFaultToScheduler(FaultType.SENSOR_FAIL, zoneId);

                Thread.sleep(FAULT_RESET_DELAY_MS);
                doReturnToBase();
                return;
            }

            moveTowardsTarget(travelTimeMs());

            if (redirectZoneId != -1) {
                zoneId = redirectZoneId;
                severity = redirectSeverity;
                litersNeeded = litersForSeverity(severity);
                // redirectTargetX/Y were set by handleRedirect
                moveTowardsTarget(travelTimeMs());
                redirectZoneId = -1;
                redirectSeverity = null;
            }

            setState(DroneState.DROPPING_AGENT);
            sendStatusToScheduler();
            log("DROPPING_AGENT at Zone " + zoneId);

            if (fault == FaultType.NOZZLE_STUCK) {
                Thread.sleep(nozzleOpenMs());
                log("FAULT: Nozzle jammed at Zone " + zoneId);
                setState(DroneState.FAULT_NOZZLE);
                sendStatusToScheduler();
                sendFaultToScheduler(FaultType.NOZZLE_STUCK, zoneId);

                setState(DroneState.OFFLINE);
                sendStatusToScheduler();
                running = false;
                return;
            }

            animateFireReduction(zoneId);

            currentAgent -= litersNeeded;

            sendResultToScheduler(zoneId, true);

            setState(DroneState.IDLE);
            sendStatusToScheduler();
            log("IDLE (task complete, remaining=" + currentAgent + "L)");

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void animateFireReduction(int zoneId) throws InterruptedException {
        Thread.sleep(nozzleOpenMs());

        int steps = 10;
        for (int i = steps; i >= 1; i--) {
            double intensity = i / (double) steps;
            GuiModel.get().setFireIntensity(zoneId, intensity);
            Thread.sleep(120);
        }
    }

    private void moveTowardsTarget(int totalMs) throws InterruptedException {
        int startX = currentX;
        int startY = currentY;
        int elapsed = 0;
        int stepMs = 100;
        int originalTimeout = -1;

        try {
            originalTimeout = commandSocket.getSoTimeout();
            commandSocket.setSoTimeout(stepMs);

            while (elapsed < totalMs && running) {
                try {
                    String msg = UDPHelper.receiveMessage(commandSocket);
                    String type = UDPHelper.getMessageType(msg);

                    if (type.equals(UDPHelper.MSG_DRONE_COMMAND)) {
                        String cmdType = UDPHelper.parseDroneCommandType(msg);
                        if (cmdType.equals("REDIRECT")) {
                            redirectZoneId = UDPHelper.parseDroneCommandZoneId(msg);
                            redirectSeverity = UDPHelper.parseDroneCommandSeverity(msg);
                            log("Redirect received during travel: Zone " + redirectZoneId);
                        } else if (cmdType.equals("SHUTDOWN")) {
                            log("SHUTDOWN received during travel");
                            setState(DroneState.SHUTDOWN);
                            running = false;
                            return;
                        }
                    }
                } catch (SocketTimeoutException e) {
                    // no command in this tick
                } catch (IOException e) {
                    if (running) System.err.println("[Drone " + droneId + "] Poll error: " + e.getMessage());
                }

                elapsed += stepMs;
                double progress = Math.min(1.0, (double) elapsed / totalMs);
                currentX = startX + (int) Math.round((targetX - startX) * progress);
                currentY = startY + (int) Math.round((targetY - startY) * progress);
                sendStatusToScheduler();

                if (redirectZoneId != -1) {
                    return;
                }
            }

            currentX = targetX;
            currentY = targetY;
            sendStatusToScheduler();
        } catch (java.net.SocketException e) {
            System.err.println("[Drone " + droneId + "] Socket error during travel: " + e.getMessage());
        } finally {
            try {
                if (originalTimeout >= 0) commandSocket.setSoTimeout(originalTimeout);
            } catch (java.net.SocketException ignored) {
            }
        }
    }

    private void handleRedirect(String msg) {
        if (state == DroneState.EN_ROUTE) {
            redirectZoneId = UDPHelper.parseDroneCommandZoneId(msg);
            redirectSeverity = UDPHelper.parseDroneCommandSeverity(msg);
            // Update the target so moveTowardsTarget heads to the right place
            targetX = UDPHelper.parseDroneCommandTargetX(msg);
            targetY = UDPHelper.parseDroneCommandTargetY(msg);
            log("Redirect received: Zone " + redirectZoneId + " at (" + targetX + "," + targetY + ")");
        }
    }

    private void doReturnToBase() {
        try {
            setState(DroneState.RETURNING_BASE);
            sendStatusToScheduler();
            log("RETURNING_BASE");

            targetX = 0;
            targetY = 0;
            moveTowardsTarget(returnTimeMs());

            currentAgent = agentCapacity;
            currentX = 0;
            currentY = 0;

            setState(DroneState.IDLE);
            sendStatusToScheduler();
            log("IDLE at base (refilled)");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void setState(DroneState newState) {
        this.state = newState;
    }

    private void log(String message) {
        System.out.println("[" + UDPHelper.timestamp() + "] [Drone " + droneId + "] " + message);
    }

    private int travelTimeMs() {
        return 1200;
    }

    private int returnTimeMs() {
        return 1200;
    }

    private int nozzleOpenMs() {
        return 150;
    }

    public static int litersForSeverity(String severity) {
        switch (severity.toUpperCase()) {
            case "LOW":
                return 10;
            case "MODERATE":
                return 20;
            case "HIGH":
                return 30;
            default:
                return 0;
        }
    }

    public void shutdown() {
        this.running = false;
        if (commandSocket != null && !commandSocket.isClosed()) commandSocket.close();
    }

    public int getDroneId() {
        return droneId;
    }

    public DroneState getState() {
        return state;
    }

    public double getCurrentAgent() {
        return currentAgent;
    }
}