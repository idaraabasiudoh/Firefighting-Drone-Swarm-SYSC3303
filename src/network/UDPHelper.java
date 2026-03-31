package network;

import drone.DroneResult;
import drone.FaultType;
import fireincident.FireEvent;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Utility class for UDP communication between subsystems.
 * Uses pipe-delimited string messages for easy testing and debugging.
 * Iteration 4: Added DRONE_FAULT message, fault fields in FIRE_EVENT/DRONE_COMMAND, timestamps.
 */
public class UDPHelper {

    // Port assignments
    public static final int FIRE_TO_SCHEDULER_PORT = 5000;
    public static final int SCHEDULER_TO_FIRE_PORT = 5001;
    public static final int DRONE_TO_SCHEDULER_PORT = 5002;
    public static final int SCHEDULER_TO_DRONE_BASE_PORT = 6000; // each drone listens on 6000 + droneId

    public static final int BUFFER_SIZE = 1024;

    // ==================== Message Types ====================
    public static final String MSG_FIRE_EVENT = "FIRE_EVENT";
    public static final String MSG_DRONE_REGISTER = "DRONE_REGISTER";
    public static final String MSG_DRONE_STATUS = "DRONE_STATUS";
    public static final String MSG_DRONE_COMMAND = "DRONE_COMMAND";
    public static final String MSG_DRONE_RESULT = "DRONE_RESULT";
    public static final String MSG_CONFIRMATION = "CONFIRMATION";
    public static final String MSG_DRONE_FAULT = "DRONE_FAULT";
    public static final String MSG_SHUTDOWN = "SHUTDOWN";

    // ==================== Timestamp Utility ====================
    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    public static String timestamp() {
        return LocalDateTime.now().format(TS_FMT);
    }

    // ==================== Send / Receive ====================

    public static void sendMessage(DatagramSocket socket, InetAddress address, int port, String message) throws IOException {
        byte[] data = message.getBytes();
        DatagramPacket packet = new DatagramPacket(data, data.length, address, port);
        socket.send(packet);
    }

    public static String receiveMessage(DatagramSocket socket) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        socket.receive(packet);
        return new String(packet.getData(), 0, packet.getLength());
    }

    public static DatagramPacket receivePacket(DatagramSocket socket) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        socket.receive(packet);
        return packet;
    }

    public static String extractMessage(DatagramPacket packet) {
        return new String(packet.getData(), 0, packet.getLength());
    }

    // ==================== Message Builders ====================

    // FIRE_EVENT|time|zoneId|eventType|severity|faultType
    public static String buildFireEventMessage(String time, int zoneId, String eventType, String severity, String faultType) {
        return MSG_FIRE_EVENT + "|" + time + "|" + zoneId + "|" + eventType + "|" + severity + "|" + faultType;
    }

    public static String buildFireEventMessage(FireEvent event) {
        return buildFireEventMessage(event.getTime(), event.getZoneId(),
                event.getEventType().name(), event.getSeverity().name(),
                event.getFaultType().name());
    }

    // DRONE_REGISTER|droneId|capacity|x|y
    public static String buildDroneRegisterMessage(int droneId, double capacity, int x, int y) {
        return MSG_DRONE_REGISTER + "|" + droneId + "|" + capacity + "|" + x + "|" + y;
    }

    // DRONE_STATUS|droneId|state|x|y|remainingAgent
    public static String buildDroneStatusMessage(int droneId, String state, int x, int y, double remainingAgent) {
        return MSG_DRONE_STATUS + "|" + droneId + "|" + state + "|" + x + "|" + y + "|" + remainingAgent;
    }

    // DRONE_COMMAND|droneId|commandType|zoneId|severity|faultType
    public static String buildDroneCommandMessage(int droneId, String commandType, int zoneId, String severity) {
        return buildDroneCommandMessage(droneId, commandType, zoneId, severity, "NONE");
    }

    public static String buildDroneCommandMessage(int droneId, String commandType, int zoneId, String severity, String faultType) {
        return MSG_DRONE_COMMAND + "|" + droneId + "|" + commandType + "|" + zoneId + "|" + severity + "|" + faultType;
    }

    // DRONE_FAULT|droneId|faultType|zoneId
    public static String buildDroneFaultMessage(int droneId, String faultType, int zoneId) {
        return MSG_DRONE_FAULT + "|" + droneId + "|" + faultType + "|" + zoneId;
    }

    // DRONE_RESULT|droneId|zoneId|completed|remainingAgent
    public static String buildDroneResultMessage(int droneId, int zoneId, boolean completed, double remainingAgent) {
        return MSG_DRONE_RESULT + "|" + droneId + "|" + zoneId + "|" + completed + "|" + remainingAgent;
    }

    public static String buildDroneResultMessage(DroneResult result) {
        return buildDroneResultMessage(result.getDroneId(), result.getZoneId(),
                result.isTaskCompleted(), result.getRemainingAgent());
    }

    // CONFIRMATION|droneId|zoneId|completed
    public static String buildConfirmationMessage(int droneId, int zoneId, boolean completed) {
        return MSG_CONFIRMATION + "|" + droneId + "|" + zoneId + "|" + completed;
    }

    // SHUTDOWN
    public static String buildShutdownMessage() {
        return MSG_SHUTDOWN;
    }

    // ==================== Message Parsers ====================

    public static String getMessageType(String message) {
        int idx = message.indexOf('|');
        return idx == -1 ? message : message.substring(0, idx);
    }

    public static String[] parseFields(String message) {
        return message.split("\\|");
    }

    // Parse FIRE_EVENT|time|zoneId|eventType|severity|faultType -> FireEvent
    public static FireEvent parseFireEvent(String message) {
        String[] fields = parseFields(message);
        // fields[0] = "FIRE_EVENT"
        String time = fields[1];
        int zoneId = Integer.parseInt(fields[2]);
        FireEvent.EventType eventType = FireEvent.EventType.valueOf(fields[3]);
        FireEvent.Severity severity = FireEvent.Severity.valueOf(fields[4]);
        FaultType fault = (fields.length > 5) ? FaultType.fromString(fields[5]) : FaultType.NONE;
        return new FireEvent(time, zoneId, eventType, severity, fault);
    }

    // Parse DRONE_REGISTER|droneId|capacity|x|y -> int[] {droneId, x, y} + double capacity
    public static int parseDroneRegisterId(String message) {
        return Integer.parseInt(parseFields(message)[1]);
    }

    public static double parseDroneRegisterCapacity(String message) {
        return Double.parseDouble(parseFields(message)[2]);
    }

    public static int parseDroneRegisterX(String message) {
        return Integer.parseInt(parseFields(message)[3]);
    }

    public static int parseDroneRegisterY(String message) {
        return Integer.parseInt(parseFields(message)[4]);
    }

    // Parse DRONE_STATUS|droneId|state|x|y|remainingAgent
    public static int parseDroneStatusId(String message) {
        return Integer.parseInt(parseFields(message)[1]);
    }

    public static String parseDroneStatusState(String message) {
        return parseFields(message)[2];
    }

    public static int parseDroneStatusX(String message) {
        return Integer.parseInt(parseFields(message)[3]);
    }

    public static int parseDroneStatusY(String message) {
        return Integer.parseInt(parseFields(message)[4]);
    }

    public static double parseDroneStatusAgent(String message) {
        return Double.parseDouble(parseFields(message)[5]);
    }

    // Parse DRONE_COMMAND|droneId|commandType|zoneId|severity
    public static int parseDroneCommandId(String message) {
        return Integer.parseInt(parseFields(message)[1]);
    }

    public static String parseDroneCommandType(String message) {
        return parseFields(message)[2];
    }

    public static int parseDroneCommandZoneId(String message) {
        return Integer.parseInt(parseFields(message)[3]);
    }

    public static String parseDroneCommandSeverity(String message) {
        return parseFields(message)[4];
    }

    public static String parseDroneCommandFault(String message) {
        String[] fields = parseFields(message);
        return (fields.length > 5) ? fields[5] : "NONE";
    }

    // Parse DRONE_FAULT|droneId|faultType|zoneId
    public static int parseDroneFaultDroneId(String message) {
        return Integer.parseInt(parseFields(message)[1]);
    }

    public static String parseDroneFaultType(String message) {
        return parseFields(message)[2];
    }

    public static int parseDroneFaultZoneId(String message) {
        return Integer.parseInt(parseFields(message)[3]);
    }

    // Parse DRONE_RESULT|droneId|zoneId|completed|remainingAgent
    public static DroneResult parseDroneResult(String message) {
        String[] fields = parseFields(message);
        int droneId = Integer.parseInt(fields[1]);
        int zoneId = Integer.parseInt(fields[2]);
        boolean completed = Boolean.parseBoolean(fields[3]);
        double remainingAgent = Double.parseDouble(fields[4]);
        return new DroneResult(droneId, zoneId, completed, remainingAgent);
    }

    // Parse CONFIRMATION|droneId|zoneId|completed
    public static int parseConfirmationDroneId(String message) {
        return Integer.parseInt(parseFields(message)[1]);
    }

    public static int parseConfirmationZoneId(String message) {
        return Integer.parseInt(parseFields(message)[2]);
    }

    public static boolean parseConfirmationCompleted(String message) {
        return Boolean.parseBoolean(parseFields(message)[3]);
    }

    // ==================== Port Helper ====================

    public static int getDroneListenPort(int droneId) {
        return SCHEDULER_TO_DRONE_BASE_PORT + droneId;
    }
}
