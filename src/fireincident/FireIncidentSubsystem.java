package fireincident;

import drone.FaultType;
import network.UDPHelper;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

public class FireIncidentSubsystem implements Runnable {
    private final String inputFilePath;
    private final InetAddress schedulerAddress;
    private DatagramSocket sendSocket;
    private DatagramSocket receiveSocket;
    private volatile boolean running = true;
    private int eventCount = 0;

    public FireIncidentSubsystem(String inputFilePath, InetAddress schedulerAddress) {
        this.inputFilePath = inputFilePath;
        this.schedulerAddress = schedulerAddress;
    }

    @Override
    public void run() {
        try {
            sendSocket = new DatagramSocket();
            receiveSocket = new DatagramSocket(UDPHelper.SCHEDULER_TO_FIRE_PORT);
            receiveSocket.setSoTimeout(30000);

            readAndProcessInputFile();
            waitForConfirmations();
        } catch (IOException | InterruptedException e) {
            System.err.println("[Fire Subsystem] Terminated: " + e.getMessage());
        } finally {
            if (sendSocket != null && !sendSocket.isClosed()) sendSocket.close();
            if (receiveSocket != null && !receiveSocket.isClosed()) receiveSocket.close();
        }
        System.out.println("[Fire Subsystem] Shutdown complete.");
    }

    private void readAndProcessInputFile() throws IOException {
        List<FireEvent> events = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(inputFilePath))) {
            String line;
            while ((line = reader.readLine()) != null && running) {
                if (line.trim().isEmpty() || line.startsWith("#") || line.toLowerCase().contains("time")) {
                    continue;
                }

                FireEvent event = parseLineToFireEvent(line);
                events.add(event);
            }
        }

        eventCount = events.size();

        for (FireEvent event : events) {
            if (!running) break;

            String msg = UDPHelper.buildFireEventMessage(event);
            UDPHelper.sendMessage(sendSocket, schedulerAddress, UDPHelper.FIRE_TO_SCHEDULER_PORT, msg);
            System.out.println("[Fire Subsystem] Sent to Scheduler: " + event);

            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    private void waitForConfirmations() throws InterruptedException {
        int received = 0;

        while (running && received < eventCount) {
            try {
                String msg = UDPHelper.receiveMessage(receiveSocket);
                String type = UDPHelper.getMessageType(msg);

                if (type.equals(UDPHelper.MSG_SHUTDOWN)) {
                    System.out.println("[Fire Subsystem] Received shutdown.");
                    break;
                }

                if (type.equals(UDPHelper.MSG_CONFIRMATION)) {
                    int zoneId = UDPHelper.parseConfirmationZoneId(msg);
                    boolean completed = UDPHelper.parseConfirmationCompleted(msg);
                    System.out.println("[Fire Subsystem] Result Verified: Zone " + zoneId + " completed=" + completed);
                    received++;
                }
            } catch (IOException e) {
                System.out.println("[Fire Subsystem] Timeout waiting for confirmations (" + received + "/" + eventCount + ")");
                break;
            }
        }
    }

    public static FireEvent parseLineToFireEvent(String line) {
        String[] p = line.split(",");

        FireEvent event = new FireEvent(
                p[0].trim(),
                Integer.parseInt(p[1].trim()),
                FireEvent.EventType.valueOf(p[2].trim().toUpperCase()),
                FireEvent.Severity.valueOf(p[3].trim().toUpperCase())
        );

        if (p.length > 4) {
            event.setFaultType(FaultType.fromString(p[4].trim()));
        }

        return event;
    }

    public void shutdown() {
        running = false;
        if (receiveSocket != null && !receiveSocket.isClosed()) receiveSocket.close();
    }
}