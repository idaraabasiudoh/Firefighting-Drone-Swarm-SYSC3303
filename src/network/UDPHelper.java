package network;

import drone.DroneResult;
import fireincident.FireEvent;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class UDPHelper {

    public static final int FIRE_TO_SCHEDULER_PORT = 5000;
    public static final int DRONE_TO_SCHEDULER_PORT = 5001;
    public static final int SCHEDULER_TO_FIRE_PORT = 5002;

    public static final String MSG_FIRE_EVENT = "FIRE_EVENT";
    public static final String MSG_DRONE_REGISTER = "DRONE_REGISTER";
    public static final String MSG_DRONE_COMMAND = "DRONE_COMMAND";
    public static final String MSG_DRONE_STATUS = "DRONE_STATUS";
    public static final String MSG_DRONE_RESULT = "DRONE_RESULT";
    public static final String MSG_DRONE_FAULT = "DRONE_FAULT";
    public static final String MSG_CONFIRMATION = "CONFIRMATION";
    public static final String MSG_SHUTDOWN = "SHUTDOWN";

    public static void sendMessage(DatagramSocket socket, InetAddress address, int port, String message) throws IOException {
        byte[] data = message.getBytes(StandardCharsets.UTF_8);
        DatagramPacket packet = new DatagramPacket(data, data.length, address, port);
        socket.send(packet);
    }

    public static DatagramPacket receivePacket(DatagramSocket socket) throws IOException {
        byte[] buffer = new byte[2048];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        socket.receive(packet);
        return packet;
    }

    public static String receiveMessage(DatagramSocket socket) throws IOException {
        DatagramPacket packet = receivePacket(socket);
        return extractMessage(packet);
    }

    public static String extractMessage(DatagramPacket packet) {
        return new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
    }

    public static String getMessageType(String msg) {
        int idx = msg.indexOf('|');
        return idx == -1 ? msg : msg.substring(0, idx);
    }

    public static int getDroneListenPort(int droneId) {
        return 6000 + droneId;
    }

    public static String buildFireEventMessage(FireEvent e) {
        String fault = (e.getFaultType() == null) ? "NONE" : e.getFaultType().name();
        return MSG_FIRE_EVENT + "|" +
                e.getTime() + "|" +
                e.getZoneId() + "|" +
                e.getEventType().name() + "|" +
                e.getSeverity().name() + "|" +
                fault;
    }

    public static FireEvent parseFireEvent(String msg) {
        String[] p = msg.split("\\|");
        FireEvent event = new FireEvent(
                p[1],
                Integer.parseInt(p[2]),
                FireEvent.EventType.valueOf(p[3]),
                FireEvent.Severity.valueOf(p[4])
        );
        if (p.length > 5) {
            event.setFaultType(drone.FaultType.fromString(p[5]));
        }
        return event;
    }

    public static String buildDroneRegisterMessage(int droneId, double capacity, int x, int y) {
        return MSG_DRONE_REGISTER + "|" + droneId + "|" + capacity + "|" + x + "|" + y;
    }

    public static int parseDroneRegisterId(String msg) {
        return Integer.parseInt(msg.split("\\|")[1]);
    }

    public static double parseDroneRegisterCapacity(String msg) {
        return Double.parseDouble(msg.split("\\|")[2]);
    }

    public static int parseDroneRegisterX(String msg) {
        return Integer.parseInt(msg.split("\\|")[3]);
    }

    public static int parseDroneRegisterY(String msg) {
        return Integer.parseInt(msg.split("\\|")[4]);
    }

    public static String buildDroneCommandMessage(int droneId, String commandType, int zoneId, String severity) {
        return buildDroneCommandMessage(droneId, commandType, zoneId, severity, "NONE", 0, 0);
    }

    public static String buildDroneCommandMessage(int droneId, String commandType, int zoneId, String severity, String faultType) {
        return buildDroneCommandMessage(droneId, commandType, zoneId, severity, faultType, 0, 0);
    }

    /**
     * Full command message including target coordinates so the drone knows exactly
     * where to fly to (the zone center), eliminating hardcoded lookup tables.
     * Format: DRONE_COMMAND|droneId|cmdType|zoneId|severity|faultType|targetX|targetY
     */
    public static String buildDroneCommandMessage(int droneId, String commandType, int zoneId,
                                                  String severity, String faultType,
                                                  int targetX, int targetY) {
        return MSG_DRONE_COMMAND + "|" + droneId + "|" + commandType + "|" + zoneId
                + "|" + severity + "|" + faultType + "|" + targetX + "|" + targetY;
    }

    public static String parseDroneCommandType(String msg) {
        return msg.split("\\|")[2];
    }

    public static int parseDroneCommandZoneId(String msg) {
        return Integer.parseInt(msg.split("\\|")[3]);
    }

    public static String parseDroneCommandSeverity(String msg) {
        return msg.split("\\|")[4];
    }

    public static String parseDroneCommandFault(String msg) {
        String[] p = msg.split("\\|");
        return p.length > 5 ? p[5] : "NONE";
    }

    public static int parseDroneCommandTargetX(String msg) {
        String[] p = msg.split("\\|");
        return p.length > 6 ? Integer.parseInt(p[6]) : 0;
    }

    public static int parseDroneCommandTargetY(String msg) {
        String[] p = msg.split("\\|");
        return p.length > 7 ? Integer.parseInt(p[7]) : 0;
    }

    public static String buildDroneStatusMessage(int droneId, String state, int x, int y, double remainingAgent) {
        return MSG_DRONE_STATUS + "|" + droneId + "|" + state + "|" + x + "|" + y + "|" + remainingAgent;
    }

    public static int parseDroneStatusId(String msg) {
        return Integer.parseInt(msg.split("\\|")[1]);
    }

    public static String parseDroneStatusState(String msg) {
        return msg.split("\\|")[2];
    }

    public static int parseDroneStatusX(String msg) {
        return Integer.parseInt(msg.split("\\|")[3]);
    }

    public static int parseDroneStatusY(String msg) {
        return Integer.parseInt(msg.split("\\|")[4]);
    }

    public static double parseDroneStatusAgent(String msg) {
        return Double.parseDouble(msg.split("\\|")[5]);
    }

    public static String buildDroneResultMessage(int droneId, int zoneId, boolean completed, double remainingAgent) {
        return MSG_DRONE_RESULT + "|" + droneId + "|" + zoneId + "|" + completed + "|" + remainingAgent;
    }

    public static DroneResult parseDroneResult(String msg) {
        String[] p = msg.split("\\|");
        return new DroneResult(
                Integer.parseInt(p[1]),
                Integer.parseInt(p[2]),
                Boolean.parseBoolean(p[3]),
                Double.parseDouble(p[4])
        );
    }

    public static String buildDroneFaultMessage(int droneId, String faultType, int zoneId) {
        return MSG_DRONE_FAULT + "|" + droneId + "|" + faultType + "|" + zoneId;
    }

    public static int parseDroneFaultDroneId(String msg) {
        return Integer.parseInt(msg.split("\\|")[1]);
    }

    public static String parseDroneFaultType(String msg) {
        return msg.split("\\|")[2];
    }

    public static int parseDroneFaultZoneId(String msg) {
        return Integer.parseInt(msg.split("\\|")[3]);
    }

    public static String buildConfirmationMessage(int droneId, int zoneId, boolean completed) {
        return MSG_CONFIRMATION + "|" + droneId + "|" + zoneId + "|" + completed;
    }

    public static int parseConfirmationZoneId(String msg) {
        return Integer.parseInt(msg.split("\\|")[2]);
    }

    public static boolean parseConfirmationCompleted(String msg) {
        return Boolean.parseBoolean(msg.split("\\|")[3]);
    }

    public static String buildShutdownMessage() {
        return MSG_SHUTDOWN;
    }

    public static String timestamp() {
        return LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }
}