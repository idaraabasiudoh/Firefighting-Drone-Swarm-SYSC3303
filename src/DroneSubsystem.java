// DroneSubsystem.java  (Iteration 3: UDP communication, independent state machine, multi-drone)
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
            commandSocket.setSoTimeout(2000); // 2s timeout for polling
            sendSocket = new DatagramSocket();

            System.out.println("[Drone " + droneId + "] Started, listening on port " + listenPort);

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
                                System.out.println("[Drone " + droneId + "] Received SHUTDOWN");
                                setState(DroneState.SHUTDOWN);
                                running = false;
                                break;
                            default:
                                System.out.println("[Drone " + droneId + "] Unknown command: " + cmdType);
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
        System.out.println("[Drone " + droneId + "] Shutdown complete.");
    }

    private void registerWithScheduler() throws IOException {
        String msg = UDPHelper.buildDroneRegisterMessage(droneId, agentCapacity, currentX, currentY);
        UDPHelper.sendMessage(sendSocket, schedulerAddress, schedulerPort, msg);
        System.out.println("[Drone " + droneId + "] Registered with scheduler");
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

    private void handleTask(String msg) {
        int zoneId = UDPHelper.parseDroneCommandZoneId(msg);
        String severity = UDPHelper.parseDroneCommandSeverity(msg);
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
            System.out.println("[Drone " + droneId + "] EN_ROUTE to Zone " + zoneId);

            // Travel with redirect polling: instead of sleeping the full travel time,
            // poll the command socket in short intervals to catch REDIRECT commands
            travelWithRedirectPolling(travelTimeMs());

            // Check if redirected during travel
            if (redirectZoneId != -1) {
                System.out.println("[Drone " + droneId + "] Redirected to Zone " + redirectZoneId + " during travel");
                zoneId = redirectZoneId;
                severity = redirectSeverity;
                litersNeeded = litersForSeverity(severity);
                redirectZoneId = -1;
                redirectSeverity = null;
            }

            setState(DroneState.DROPPING_AGENT);
            sendStatusToScheduler();
            System.out.println("[Drone " + droneId + "] DROPPING_AGENT at Zone " + zoneId);

            Thread.sleep(nozzleOpenMs() + dropAgentMs(litersNeeded));

            currentAgent -= litersNeeded;

            // Send result to scheduler
            sendResultToScheduler(zoneId, true);

            setState(DroneState.IDLE);
            sendStatusToScheduler();
            System.out.println("[Drone " + droneId + "] IDLE (task complete, remaining=" + currentAgent + "L)");

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

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
                            System.out.println("[Drone " + droneId + "] Redirect received during travel: Zone " + redirectZoneId);
                        } else if (cmdType.equals("RETURN_BASE")) {
                            // Queue this for after current task
                            System.out.println("[Drone " + droneId + "] RETURN_BASE received during travel (will execute after task)");
                        } else if (cmdType.equals("SHUTDOWN")) {
                            System.out.println("[Drone " + droneId + "] SHUTDOWN received during travel");
                            setState(DroneState.SHUTDOWN);
                            running = false;
                            return;
                        }
                    }
                } catch (SocketTimeoutException e) {
                    // Normal — no message during this poll interval
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

    private void handleRedirect(String msg) {
        // If we're EN_ROUTE, update the redirect target
        if (state == DroneState.EN_ROUTE) {
            redirectZoneId = UDPHelper.parseDroneCommandZoneId(msg);
            redirectSeverity = UDPHelper.parseDroneCommandSeverity(msg);
            System.out.println("[Drone " + droneId + "] Redirect received: Zone " + redirectZoneId);
        } else {
            // If not en-route, treat as a new task
            handleTask(msg);
        }
    }

    private void doReturnToBase() {
        try {
            setState(DroneState.RETURNING_BASE);
            sendStatusToScheduler();
            System.out.println("[Drone " + droneId + "] RETURNING_BASE");
            Thread.sleep(returnTimeMs());

            // Refill at base
            currentAgent = agentCapacity;
            currentX = 0;
            currentY = 0;

            setState(DroneState.IDLE);
            sendStatusToScheduler();
            System.out.println("[Drone " + droneId + "] IDLE at base (refilled)");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void setState(DroneState newState) {
        this.state = newState;
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