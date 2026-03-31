package drone;

import network.UDPHelper;

// DroneSubsystem.java  (Iteration 4: UDP communication, fault handling, independent state machine)
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

    // Redirect support: if a REDIRECT arrives during travel, this gets updated
    private volatile int redirectZoneId = -1;
    private volatile String redirectSeverity = null;

    private DatagramSocket commandSocket; // listens for commands from scheduler
    private DatagramSocket sendSocket;    // sends messages to scheduler

    // Iteration 4: Fault simulation
    private static final int FAULT_RESET_DELAY_MS = 3000; // Time for soft fault recovery

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

    // ==================== MAIN RUN LOOP ====================
    @Override
    public void run() {
        try {
            int listenPort = UDPHelper.getDroneListenPort(droneId);
            commandSocket = new DatagramSocket(listenPort);
            commandSocket.setSoTimeout(2000); // 2s timeout for polling
            sendSocket = new DatagramSocket();

            log("Started, listening on port " + listenPort);

            // Register with scheduler
            registerWithScheduler();

            // Main command loop
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
                    // Normal timeout, just loop again
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

    // ==================== REGISTRATION & MESSAGING ====================

    private void registerWithScheduler() throws IOException {
        String msg = UDPHelper.buildDroneRegisterMessage(droneId, agentCapacity, currentX, currentY);
        UDPHelper.sendMessage(sendSocket, schedulerAddress, schedulerPort, msg);
        log("Registered with scheduler");
    }

    private void sendStatusToScheduler() {
        try {
            String msg = UDPHelper.buildDroneStatusMessage(droneId, state.name(),
                    currentX, currentY, currentAgent);
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

    // ==================== TASK HANDLING (with Fault Injection) ====================

    private void handleTask(String msg) {
        int zoneId = UDPHelper.parseDroneCommandZoneId(msg);
        String severity = UDPHelper.parseDroneCommandSeverity(msg);
        FaultType fault = FaultType.fromString(UDPHelper.parseDroneCommandFault(msg));
        int litersNeeded = litersForSeverity(severity);

        // If not enough agent, return to base first
        if (currentAgent < litersNeeded) {
            doReturnToBase();
        }

        try {
            // Reset redirect
            redirectZoneId = -1;
            redirectSeverity = null;

            setState(DroneState.EN_ROUTE);
            sendStatusToScheduler();
            log("EN_ROUTE to Zone " + zoneId + (fault != FaultType.NONE ? " [FAULT INJECTED: " + fault + "]" : ""));

            // ===== FAULT: DRONE_STUCK — drone freezes mid-flight =====
            if (fault == FaultType.DRONE_STUCK) {
                travelWithRedirectPolling(travelTimeMs() / 2); // Travel partway then freeze
                log("FAULT: Stuck mid-flight to Zone " + zoneId);
                setState(DroneState.FAULT_STUCK);
                sendStatusToScheduler();
                sendFaultToScheduler(FaultType.DRONE_STUCK, zoneId);

                Thread.sleep(FAULT_RESET_DELAY_MS); // Simulate recovery delay
                log("Recovered from DRONE_STUCK fault, resetting...");
                doReturnToBase();
                return;
            }

            // ===== FAULT: SENSOR_FAIL — drone doesn't detect zone arrival =====
            if (fault == FaultType.SENSOR_FAIL) {
                travelWithRedirectPolling(travelTimeMs());
                log("FAULT: Arrival sensor failed at Zone " + zoneId);
                setState(DroneState.FAULT_SENSOR);
                sendStatusToScheduler();
                sendFaultToScheduler(FaultType.SENSOR_FAIL, zoneId);

                Thread.sleep(FAULT_RESET_DELAY_MS); // Simulate recovery delay
                log("Recovered from SENSOR_FAIL, resetting...");
                doReturnToBase();
                return;
            }

            // Normal travel with redirect polling
            travelWithRedirectPolling(travelTimeMs());

            // Check if redirected during travel
            if (redirectZoneId != -1) {
                log("Redirected to Zone " + redirectZoneId + " during travel");
                zoneId = redirectZoneId;
                severity = redirectSeverity;
                litersNeeded = litersForSeverity(severity);
                redirectZoneId = -1;
                redirectSeverity = null;
            }

            setState(DroneState.DROPPING_AGENT);
            sendStatusToScheduler();
            log("DROPPING_AGENT at Zone " + zoneId);

            // ===== FAULT: NOZZLE_STUCK — nozzle jams during drop (HARD FAULT) =====
            if (fault == FaultType.NOZZLE_STUCK) {
                Thread.sleep(nozzleOpenMs()); // nozzle tries to open but jams
                log("FAULT: Nozzle jammed at Zone " + zoneId + " - HARD FAULT, shutting down");
                setState(DroneState.FAULT_NOZZLE);
                sendStatusToScheduler();
                sendFaultToScheduler(FaultType.NOZZLE_STUCK, zoneId);

                setState(DroneState.OFFLINE); // Hard fault: drone goes permanently offline
                sendStatusToScheduler();
                running = false;
                return;
            }

            // Normal drop
            Thread.sleep(nozzleOpenMs() + dropAgentMs(litersNeeded));
            currentAgent -= litersNeeded;

            // Send result to scheduler
            sendResultToScheduler(zoneId, true);

            setState(DroneState.IDLE);
            sendStatusToScheduler();
            log("IDLE (task complete, remaining=" + currentAgent + "L)");

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // ==================== TRAVEL WITH REDIRECT POLLING ====================

    private void travelWithRedirectPolling(int totalMs) throws InterruptedException {
        int elapsed = 0;
        int pollIntervalMs = 100;
        int originalTimeout = -1;

        try {
            originalTimeout = commandSocket.getSoTimeout();
            commandSocket.setSoTimeout(pollIntervalMs);

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
                        } else if (cmdType.equals("RETURN_BASE")) {
                            log("RETURN_BASE received during travel (will execute after task)");
                        } else if (cmdType.equals("SHUTDOWN")) {
                            log("SHUTDOWN received during travel");
                            setState(DroneState.SHUTDOWN);
                            running = false;
                            return;
                        }
                    }
                } catch (SocketTimeoutException e) {
                    // Normal - no message during this poll interval
                } catch (IOException e) {
                    if (running) System.err.println("[Drone " + droneId + "] Poll error: " + e.getMessage());
                }
                elapsed += pollIntervalMs;
            }
        } catch (java.net.SocketException e) {
            System.err.println("[Drone " + droneId + "] Socket error during travel: " + e.getMessage());
        } finally {
            try {
                if (originalTimeout >= 0) commandSocket.setSoTimeout(originalTimeout);
            } catch (java.net.SocketException ignored) {}
        }
    }

    // ==================== REDIRECT HANDLING ====================

    private void handleRedirect(String msg) {
        if (state == DroneState.EN_ROUTE) {
            redirectZoneId = UDPHelper.parseDroneCommandZoneId(msg);
            redirectSeverity = UDPHelper.parseDroneCommandSeverity(msg);
            log("Redirect received: Zone " + redirectZoneId);
        } else {
            handleTask(msg);
        }
    }

    // ==================== RETURN TO BASE ====================

    private void doReturnToBase() {
        try {
            setState(DroneState.RETURNING_BASE);
            sendStatusToScheduler();
            log("RETURNING_BASE");
            Thread.sleep(returnTimeMs());

            // Refill at base
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

    // ==================== HELPERS ====================

    private void setState(DroneState newState) {
        this.state = newState;
    }

    private void log(String message) {
        System.out.println("[" + UDPHelper.timestamp() + "] [Drone " + droneId + "] " + message);
    }

    // --- Timing helpers (fixed 800ms as per Iteration 2) ---
    private int travelTimeMs() { return 800; }
    private int returnTimeMs() { return 800; }
    private int nozzleOpenMs() { return 150; }
    private int dropAgentMs(int liters) { return liters * 40; }

    public static int litersForSeverity(String severity) {
        switch (severity.toUpperCase()) {
            case "LOW": return 10;
            case "MODERATE": return 20;
            case "HIGH": return 30;
            default: return 0;
        }
    }

    public void shutdown() {
        this.running = false;
        if (commandSocket != null && !commandSocket.isClosed()) commandSocket.close();
    }

    public int getDroneId() { return droneId; }
    public DroneState getState() { return state; }
    public double getCurrentAgent() { return currentAgent; }
}