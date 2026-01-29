import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class FireIncidentSubsystem implements Runnable {
    private final Scheduler scheduler;
    private final String inputFilePath;
    private volatile boolean running = true;

    public FireIncidentSubsystem(Scheduler scheduler, String inputFilePath) {
        this.scheduler = scheduler;
        this.inputFilePath = inputFilePath;
    }

    @Override
    public void run() {
        try {
            readAndProcessInputFile();
            waitForConfirmations();
        } catch (IOException | InterruptedException e) {
            System.err.println("[Fire Subsystem] Terminated: " + e.getMessage());
        }
        System.out.println("[Fire Subsystem] Shutdown complete.");
    }

    private void readAndProcessInputFile() throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(inputFilePath))) {
            String line;
            while ((line = reader.readLine()) != null && running) {
                if (line.trim().isEmpty() || line.startsWith("#") || line.toLowerCase().contains("time")) continue;

                FireEvent event = parseLineToFireEvent(line);
                if (event != null) {
                    scheduler.submitFireEvent(event);
                }
            }
        }
    }

    private void waitForConfirmations() throws InterruptedException {
        int received = 0;
        int expected = 5; 

        while (running && received < expected) {
            DroneResult result = scheduler.waitForConfirmation();
            if (result == null) break; 

            System.out.println("[Fire Subsystem] Result Verified: Zone " + result.getZoneId());
            received++;
        }
    }

    private FireEvent parseLineToFireEvent(String line) {
        String[] p = line.split(",");
        return new FireEvent(p[0].trim(), Integer.parseInt(p[1].trim()), 
                            FireEvent.EventType.valueOf(p[2].trim().toUpperCase()), 
                            FireEvent.Severity.valueOf(p[3].trim().toUpperCase()));
    }

    public void shutdown() {
        this.running = false;
    }
}