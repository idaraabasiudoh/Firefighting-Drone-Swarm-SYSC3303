# SYSC 3303: Firefighting Drone Swarm

## Iteration 1

**Objective:** Basic communication between subsystems

### 1.0 File Names
#### 1.1 Main System
  - **DroneResult.java:** Stores the outcome of a drone’s task execution (if the task is completed).
  - **DroneSubsystem.java:** Represents a drone worker thread. Requests tasks from the scheduler and processes assigned fire events.
  - **FireEvent.java:** Object representing a single fire incident, including attributes such as zone, severity, and time.
  - **FireIncidentSubsystem.java:** Reads fire incidents from the input file and submits them to the scheduler. 
  - **Main.java:** Entry of program. It initializes the scheduler, subsystems and starts simulation.
  - **Scheduler.java:** Manages shared task queues and synchronizes communication between fire events and drones.
  - **fire_events.csv:** Input file containing a list of fire incidents used to drive the simulation.
#### 1.2 GUI
  - **GUIMain.java:** Launches the graphical interface and initializes GUI components.
  - **MapPanel.java:** Displays the map and visual representation of zones, fires, and drone activity.
  - **Zone.java:** Represents a geographical zone on the map, including its boundaries and identifiers.
  - **ZoneParser.java:** Reads and parses zone data from a CSV file into Zone objects.
  - **sample_zone_file.csv:** Input file defining zone layouts and coordinates for the GUI map.
#### 1.3 Tests
  - **DroneResultTest.java:** Tests correctness of drone result data handling.
  - **FireEventTest.java:** Ensures fire event objects are created and interpreted correctly.
  - **SchedulerTest.java:** Tests task coordination, queue handling, and synchronization logic.
  - **SubsystemsTest.java:** Integration tests to verify interaction between scheduler, drones, and fire subsystem.
  - **TestRunner.java:** Executes all test cases.
#### 1.4 Diagrams
  - **FDS_UMLClass.png:** UML class diagram showing system structure, classes, and relationships.
  - **FDS_Sequence.png:** Sequence diagram illustrating runtime interactions between the main system which includes the scheduler, fire subsystem, and drone subsystem.
### 2.0 Set-Up Instructions
- **Java Version:** JAVA21 (at least), JAVA25 at most
- Download repository
#### 2.1 Main System
    RUN THE "Main.java" class
#### 2.2 GUI
    RUN THE "GUIMain.java" class (Just the view)
#### 2.3 Test
    RUN THE "TestRunner.java" class 
### 3.0 Responsibilities
  - Drone & FireIncident Subsystems: **Idara-Abasi Udoh**
  - Scheduler: **Ohioreuna Ajayi-Isuku**
  - Testing: **Divine Eyo**
  - Diagrams & Read-Me: **Suveatha Karunakaran**

## Iteration 2
**Objective:** This project simulates a firefighting drone system composed of: -Scheduler - Fire Incident Subsystem - Drone Subsystem - GUI Visualization. Iteration 2 introduces core scheduling logic, drone state transitions, and GUI updates to track drone states and active fire incidents.

### 1.0 File Names
#### 1.1 Main System
  - **DroneCommand.java:** Command object used by the scheduler to send instructions to drones (task assignment, return to base, or shutdown).
  - **DroneResult.java:** Stores the outcome of a drone's task execution (if the task is completed).
  - **DroneState.java:** Enum defining drone states (IDLE, EN_ROUTE, DROPPING_AGENT, RETURNING_BASE, SHUTDOWN).
  - **DroneSubsystem.java:** Represents a drone worker thread. Requests tasks from the scheduler and processes assigned fire events.
  - **FireEvent.java:** Object representing a single fire incident, including attributes such as zone, severity, and time.
  - **FireIncidentSubsystem.java:** Reads fire incidents from the input file and submits them to the scheduler.
  - **Main.java:** Entry of program. It initializes the scheduler, subsystems and starts simulation.
  - **Scheduler.java:** Manages shared task queues and synchronizes communication between fire events and drones.
  - **fire_events.csv:** Input file containing a list of fire incidents used to drive the simulation.
#### 1.2 GUI
  - **GuiMain.java:** Launches the graphical interface and initializes GUI components.
  - **GuiModel.java:** Thread-safe shared model that the scheduler updates and the GUI reads to track drone states and active fire zones.
  - **MapPanel.java:** Displays the map and visual representation of zones, fires, and drone activity.
  - **Zone.java:** Represents a geographical zone on the map, including its boundaries and identifiers.
  - **ZoneParser.java:** Reads and parses zone data from a CSV file into Zone objects.
  - **sample_zone_file.csv:** Input file defining zone layouts and coordinates for the GUI map.
#### 1.3 Tests
  - **DroneSubsystemTest.java:** Tests drone subsystem behavior and state transitions.
  - **FireEventTest.java:** Ensures fire event objects are created and interpreted correctly.
  - **SchedulerTest.java:** Tests task coordination, queue handling, and synchronization logic.
### 2.0 Set-Up Instructions
- **Java Version:** JAVA21 (at least), JAVA25 at most
- Download repository
#### 2.1 Main System
    RUN THE "Main.java" class
#### 2.2 GUI
    RUN THE "GuiMain.java" class (Just the view)
#### 2.3 Test
    RUN THE test files in the "Test/" directory
### 3.0 Responsibilities
  - Drone & FireIncident Subsystems: **Ohioreuna Ajayi-Isuku**
  - Scheduler: **Ohioreuna Ajayi-Isuku**
  - Testing: **Divine Eyo & Suveatha Karunakaran**
  - Diagrams & Read-Me: **Idara-Abasi Udoh & Divine Eyo**

## Iteration 3

**Objective:** Refactor the system into three separate, independently runnable programs (Fire Incident Subsystem, Scheduler, Drone Subsystem) that communicate **exclusively via UDP**. Iteration 3 introduces multi-drone support with load balancing, closest-drone selection, passthrough redirection, automatic agent refill, and drop failure re-queuing. The GUI is updated to display multiple drone states, drone-to-zone assignment lines, and severity color-coded fires.

### 1.0 Key Changes from Iteration 2
- All inter-subsystem communication now uses **UDP datagrams** (no shared memory or direct method calls).
- System is split into **3 standalone programs** that can run in separate JVM processes (or on separate machines).
- **Multiple drones** operate concurrently, each with its own independent state machine.
- Scheduler performs **load balancing** (closest idle drone with sufficient agent, task-count tiebreaker).
- Drones **poll for REDIRECT commands** during travel and can be rerouted mid-flight.
- Scheduler automatically sends **RETURN_BASE** when a drone's agent drops below the minimum threshold (10L), triggering an automatic refill at base.
- **Drop failure handling**: incomplete tasks are re-queued for another drone.
- GUI shows **per-drone markers**, **assignment lines** to target zones, and **severity-colored fires** (green = LOW, orange = MODERATE, red = HIGH).

### 2.0 File Names
#### 2.1 Main System
  - **DroneCommand.java:** Command record used by the Scheduler to instruct drones (TASK, RETURN_BASE, REDIRECT, SHUTDOWN).
  - **DroneInfo.java:** *(NEW)* Tracks per-drone state in the Scheduler's registry — ID, position, agent level, assignment, and tasks completed.
  - **DroneResult.java:** Stores the outcome of a drone's task execution (drone ID, zone ID, completion status, remaining agent).
  - **DroneState.java:** Enum defining drone states (IDLE, EN_ROUTE, DROPPING_AGENT, RETURNING_BASE, SHUTDOWN).
  - **DroneSubsystem.java:** *(REWRITTEN)* Independent drone worker communicating with the Scheduler via UDP. Polls for REDIRECT commands during travel using short-interval socket reads.
  - **FireEvent.java:** Represents a single fire incident with time, zone, event type, and severity.
  - **FireIncidentSubsystem.java:** *(REWRITTEN)* Reads fire incidents from CSV and sends them to the Scheduler via UDP. Waits for completion confirmations via UDP.
  - **Scheduler.java:** *(REWRITTEN)* Multi-drone UDP scheduler with three internal threads (fire listener, drone listener, dispatch loop). Implements load balancing, passthrough redirect, automatic RETURN_BASE on low agent, and drop failure re-queuing.
  - **UDPHelper.java:** *(NEW)* Utility class for UDP communication — message building/parsing for all 7 message types, send/receive helpers, and port constants.
  - **fire_events.csv:** *(UPDATED)* Input file containing 7 fire events across 7 zones for multi-drone demonstration.
#### 2.2 Standalone Entry Points (3 separate processes)
  - **SchedulerMain.java:** *(NEW)* Entry point for the Scheduler process. Loads zones, launches the GUI, and starts the Scheduler.
  - **DroneMain.java:** *(NEW)* Entry point for a single Drone process. Takes droneId as a required argument. Each drone runs as its own process.
  - **FireIncidentMain.java:** *(NEW)* Entry point for the Fire Incident Subsystem process.
  - **Main.java:** *(UPDATED)* Convenience launcher that starts all 3 subsystems (Scheduler, N drones, Fire Subsystem) in one JVM for quick testing. Default: 2 drones.
#### 2.3 GUI
  - **GuiMain.java:** Launches the graphical interface and initializes GUI components.
  - **GuiModel.java:** *(REWRITTEN)* Thread-safe singleton tracking per-drone states, drone-to-zone assignments, and zone fire severities.
  - **MapPanel.java:** *(REWRITTEN)* Renders the map with multi-drone status display, severity color-coded fire zones, drone-to-zone assignment lines, and a drone legend panel.
  - **Zone.java:** Represents a geographical zone with ID and boundary coordinates.
  - **ZoneParser.java:** Reads and parses zone definitions from a CSV file into Zone objects.
  - **sample_zone_file.csv:** *(UPDATED)* Defines 7 zones (zones 1–7) for the GUI map.
#### 2.4 Tests
  - **UDPHelperTest.java:** *(NEW)* Tests message building, parsing, and UDP send/receive round-trips on localhost.
  - **DroneSubsystemTest.java:** *(UPDATED)* Tests drone constructor, initial state, litersForSeverity, DroneResult creation, DroneCommand types, and DroneState enum.
  - **FireEventTest.java:** Tests fire event construction, getLitersNeeded, and equality.
  - **SchedulerTest.java:** *(REWRITTEN)* Tests multi-drone load balancing, closest-drone selection, insufficient-agent filtering, no-drone-available scenario, pending event queuing, and DroneInfo state helpers.

### 3.0 Set-Up Instructions
- **Java Version:** JAVA 21 (at least), JAVA 25 at most
- Download/clone the repository

#### 3.1 Running as 3 Separate Processes (Recommended)
Open **four separate terminals** and start in this order:

**Terminal 1 — Scheduler (start first):**
```
cd src
javac *.java
java SchedulerMain
```

**Terminal 2 — Drone 1:**
```
cd src
java DroneMain 1
```

**Terminal 3 — Drone 2:**
```
cd src
java DroneMain 2
```

**Terminal 4 — Fire Incident Subsystem (start last):**
```
cd src
java FireIncidentMain
```

Optional arguments:
- `java SchedulerMain [zoneFile]`
- `java DroneMain <droneId> [agentCapacity] [schedulerHost]`
- `java FireIncidentMain [inputFile] [schedulerHost]`

#### 3.2 Convenience Launcher (All-in-One)
```
cd src
javac *.java
java Main                    # default: 2 drones
java Main fire_events.csv 3  # custom fire file, 3 drones
```

#### 3.3 Tests
Run the test files in the `Test/` directory using JUnit 5:
```
# From IDE: right-click Test/ directory → Run Tests
# Or via command line with JUnit Platform Console Launcher
```

### 4.0 UDP Communication Protocol
All inter-subsystem communication uses **pipe-delimited (`|`) UDP datagrams**:

| Message Format | Direction |
|---|---|
| `FIRE_EVENT\|time\|zoneId\|eventType\|severity` | Fire Subsystem → Scheduler |
| `DRONE_REGISTER\|droneId\|capacity\|x\|y` | Drone → Scheduler |
| `DRONE_STATUS\|droneId\|state\|x\|y\|remainingAgent` | Drone → Scheduler |
| `DRONE_COMMAND\|droneId\|commandType\|zoneId\|severity` | Scheduler → Drone |
| `DRONE_RESULT\|droneId\|zoneId\|completed\|remainingAgent` | Drone → Scheduler |
| `CONFIRMATION\|droneId\|zoneId\|completed` | Scheduler → Fire Subsystem |
| `SHUTDOWN` | Scheduler → All |

### 5.0 Port Assignments
| Port | Usage |
|------|-------|
| 5000 | Fire Subsystem → Scheduler (fire events) |
| 5001 | Scheduler → Fire Subsystem (confirmations) |
| 5002 | Drone → Scheduler (register, status, results) |
| 6000 + droneId | Scheduler → Drone (commands, e.g., 6001 for Drone 1) |

### 6.0 Assumptions
- All drones start at base position (0, 0) with a default agent capacity of 30.0 liters.
- Travel time is fixed at 800ms (both to zone and returning to base).
- Nozzle open time is 150ms; agent drop rate is 40ms per liter.
- Agent required per severity: LOW = 10L, MODERATE = 20L, HIGH = 30L.
- Drones are automatically sent back to base to refill when remaining agent drops below 10L.
- The Scheduler, Drones, and Fire Subsystem all run on localhost by default but support configurable host addresses.

### 7.0 Responsibilities
  - UDP Communication & Multi-Drone Refactor: **Idara-Abasi Udoh**
  - Scheduler Load Balancing & Redirect: **Ohioreuna Ajayi-Isuku**
  - Testing: **Divine Eyo & Suveatha Karunakaran**
  - Diagrams & Read-Me: **Idara-Abasi Udoh & Divine Eyo**
