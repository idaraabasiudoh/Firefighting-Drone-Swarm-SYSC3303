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

## Iteration 4

**Objective:** Add **fault detection and handling** to the drone swarm system. Faults are injected via the input file (`fire_events.csv`), simulated by drones during task execution, and detected by the Scheduler through both direct fault reports and timer-based timeout monitoring. The GUI is updated to visually distinguish faulted drones. All log output now includes timestamps.

### 1.0 Key Changes from Iteration 3
- **Fault injection via input file**: A 5th column in `fire_events.csv` specifies the fault type (NONE, DRONE_STUCK, NOZZLE_STUCK, SENSOR_FAIL).
- **Three fault types**:
    - `DRONE_STUCK` (soft): Drone freezes mid-flight, self-recovers after 3s, returns to base.
    - `NOZZLE_STUCK` (hard): Nozzle jams during drop, drone goes permanently OFFLINE.
    - `SENSOR_FAIL` (soft): Arrival sensor fails, drone self-recovers after 3s, returns to base.
- **Drone fault simulation**: `DroneSubsystem.handleTask()` simulates faults based on the injected fault type and sends `DRONE_FAULT` messages to the Scheduler.
- **Scheduler fault handling**: New `handleDroneFault()` processes fault reports, re-queues affected fire events, and marks drones offline for hard faults.
- **Timer-based fault detection**: A `faultMonitorLoop` thread in the Scheduler detects unresponsive drones via configurable timeouts.
- **GUI fault visualization**: Faulted drones display with distinct colors (yellow = stuck, dark red = nozzle, orange = sensor, grey = offline) and X markers on the map.
- **Timestamped logging**: All Scheduler and Drone log output uses `[yyyy-MM-dd HH:mm:ss.SSS]` prefix via `UDPHelper.timestamp()`.

### 2.0 New/Modified Files
#### 2.1 New Files
- **FaultType.java:** Enum defining fault types (NONE, DRONE_STUCK, NOZZLE_STUCK, SENSOR_FAIL) with `isHardFault()`, `isSoftFault()`, and `fromString()` utility methods.

#### 2.2 Modified Files
- **DroneState.java:** Added fault states: FAULT_STUCK, FAULT_NOZZLE, FAULT_SENSOR, OFFLINE.
- **FireEvent.java:** Added `faultType` field (FaultType), 5-argument constructor, getter/setter.
- **DroneInfo.java:** Added `dispatchTimestamp`, `currentFault`, `faultCount`, `permanentlyOffline` fields and `isAvailable()` method.
- **UDPHelper.java:** Added `MSG_DRONE_FAULT` message type, fault fields in FIRE_EVENT/DRONE_COMMAND messages, `buildDroneFaultMessage()`, fault parsers, and `timestamp()`.
- **DroneSubsystem.java:** Fault simulation in `handleTask()`, `sendFaultToScheduler()`, timestamped `log()` method.
- **Scheduler.java:** `handleDroneFault()`, `requeueZone()`, `faultMonitorLoop()` thread, `handleTimeoutFault()`, timestamped `log()`.
- **GuiModel.java:** Added `droneFaults` map with `setDroneFault()`, `getDroneFault()`, `snapshotDroneFaults()`.
- **MapPanel.java:** Fault colors, X markers for faulted drones, fault labels in header.
- **FireIncidentSubsystem.java:** Parses optional 5th fault column from CSV.
- **fire_events.csv:** Added fault column (5th field) with sample fault injections.
- **Main.java:** Updated title to Iteration 4.
- **SchedulerMain.java:** Updated GUI title to Iteration 4.

#### 2.3 New Tests
- **DroneSubsystemTest.java:** Added tests for FaultType enum, hard/soft classification, `fromString()`, FireEvent with fault type, updated DroneState count.
- **SchedulerTest.java:** Added tests for offline drone skipping, fault tracking on DroneInfo, dispatch timestamp, UDP fault message build/parse, CSV fault parsing.

#### 2.4 Diagrams
- **Diagrams/Iteration4/state_drone.mmd:** Mermaid state diagram showing all drone states including fault transitions.
- **Diagrams/Iteration4/sequence_fault.mmd:** Mermaid sequence diagram illustrating normal, soft fault, and hard fault scenarios.
- **Diagrams/Iteration4/class_diagram.puml:** PlantUML class diagram with all Iteration 4 classes, fields, and relationships.

#### 2.5 Documentation
- **docs.md:** Comprehensive Iteration 4 documentation covering fault types, injection, simulation, handling, UDP protocol, GUI, logging, testing, and configuration.

### 3.0 Updated UDP Protocol
| Message Format | Direction |
|---|---|
| `FIRE_EVENT\|time\|zoneId\|eventType\|severity\|faultType` | Fire Subsystem → Scheduler |
| `DRONE_COMMAND\|droneId\|commandType\|zoneId\|severity\|faultType` | Scheduler → Drone |
| `DRONE_FAULT\|droneId\|faultType\|zoneId` | Drone → Scheduler *(NEW)* |
| `DRONE_REGISTER\|droneId\|capacity\|x\|y` | Drone → Scheduler |
| `DRONE_STATUS\|droneId\|state\|x\|y\|remainingAgent` | Drone → Scheduler |
| `DRONE_RESULT\|droneId\|zoneId\|completed\|remainingAgent` | Drone → Scheduler |
| `CONFIRMATION\|droneId\|zoneId\|completed` | Scheduler → Fire Subsystem |
| `SHUTDOWN` | Scheduler → All |

### 4.0 Configuration Constants
| Constant | Value | Location |
|---|---|---|
| `FAULT_RESET_DELAY_MS` | 3000ms | DroneSubsystem |
| `TRAVEL_TIMEOUT_MS` | 5000ms | Scheduler |
| `DROP_TIMEOUT_MS` | 5000ms | Scheduler |
| `FAULT_MONITOR_INTERVAL_MS` | 1000ms | Scheduler |

### 5.0 Set-Up Instructions
Same as Iteration 3. The `fire_events.csv` now includes a 5th column for fault injection. Events without a fault column default to `NONE`.

### 6.0 Responsibilities
- Fault Handling & Drone Simulation: **Idara-Abasi Udoh**
- Scheduler Fault Detection & Timer: **Ohioreuna Ajayi-Isuku**
- Testing: **Divine Eyo & Suveatha Karunakaran**
- Diagrams, Documentation & Read-Me: **Idara-Abasi Udoh & Divine Eyo**

## Iteration 5

**Objective:** Add **agent capacity enforcement** with refill station logic, **enhanced GUI** (drone position, fire intensity, fault visualization), and **comprehensive test coverage** (~130 tests across 8 test files). Drones now track remaining agent and automatically return to base for refill when capacity is insufficient. The Scheduler enforces capacity-aware drone selection and sends RETURN_BASE commands when agent drops below threshold.

### 1.0 Key Changes from Iteration 4
- **Agent capacity enforcement**: `DroneSubsystem` tracks `currentAgent` and `agentCapacity`. Before starting a task, if `currentAgent < litersNeeded`, the drone auto-returns to base for a refill.
- **Refill station**: `doReturnToBase()` moves the drone to (0,0), resets `currentAgent = agentCapacity`, and transitions to IDLE.
- **Scheduler capacity-aware dispatch**: `findBestDrone()` now calls `drone.hasEnoughAgent(litersNeeded)` to skip drones with insufficient agent.
- **Post-task low-agent detection**: `handleDroneResult()` checks if `remainingAgent < 10` and sends `RETURN_BASE` if needed.
- **DroneInfo refill tracking**: Added `agentCapacity`, `remainingAgent`, `refill()`, `hasEnoughAgent()`, and `isAvailable()` (checks IDLE && !offline).
- **GUI enhancements**: `GuiModel` now tracks drone positions (`dronePositions`), fire intensity (`fireIntensity`), and fault types. `MapPanel` renders drone markers at actual positions, severity-colored fires with intensity scaling, and fault-colored labels.
- **Comprehensive test suite**: 8 test files with ~130 test methods covering all subsystems, data classes, enums, GUI model, scheduling logic, UDP protocol, and CSV parsing.

### 2.0 New/Modified Files
#### 2.1 New Files
- **DroneInfoTest.java:** 24 tests for DroneInfo construction, availability, capacity, refill, fault tracking, and lifecycle.
- **GuiModelTest.java:** 23 tests for singleton, drone states/positions/faults, active fires, fire intensity, snapshot immutability.
- **SchedulerTest.java:** 10 tests for findBestDrone (capacity-aware selection, load balancing, skip busy/offline/insufficient).
- **UDPHelperTest.java:** 30 tests for all message build/parse methods, round-trips, port constants, timestamp.
- **ZoneTest.java:** 8 tests for Zone geometry and center calculation.
- **FireIncidentSubsystemTest.java:** 11 tests for CSV line parsing with all fault types and severity levels.

#### 2.2 Modified Files
- **DroneSubsystem.java:** Agent capacity tracking (`agentCapacity`, `currentAgent`), pre-task capacity check, `doReturnToBase()` with refill.
- **DroneInfo.java:** Added `agentCapacity`, `remainingAgent`, `refill()`, `hasEnoughAgent()`, `isAvailable()`.
- **Scheduler.java:** `findBestDrone()` capacity check, `handleDroneResult()` low-agent detection, `sendReturnBase()`.
- **GuiModel.java:** Added `dronePositions`, `fireIntensity` maps with getters/setters/snapshots.
- **MapPanel.java:** Drone position rendering, fire intensity scaling, fault color markers.
- **DroneSubsystemTest.java:** Added 9 tests for capacity enforcement, shutdown, boundary cases.
- **FireEventTest.java:** Added 15 tests for constructors, setters, equals edge cases, hashCode, toString, enums.

#### 2.3 Diagrams (Diagrams/Iteration5/)
- **class_diagram.puml:** Complete UML class diagram with all Iteration 5 classes, fields, methods, and relationships.
- **state_diagram.puml:** Drone state machine showing all 9 states including fault transitions and refill paths.
- **sequence_diagram.puml:** End-to-end sequence showing normal dispatch, refill, soft/hard faults, re-dispatch, and shutdown.

### 3.0 Updated Input File Format
`fire_events.csv` format (unchanged from Iteration 4):
```
time,zoneId,eventType,severity[,faultType]
```
- **faultType** (optional): NONE, DRONE_STUCK, NOZZLE_STUCK, SENSOR_FAIL

### 4.0 Set-Up Instructions
Same as Iteration 3/4. See `README_Iteration5.txt` for detailed step-by-step instructions including:
- IntelliJ project setup (Sources Root, Test Sources Root, JUnit 5)
- All-in-one launcher (`Main.java`)
- Three separate processes (`SchedulerMain`, `DroneMain`, `FireIncidentMain`)
- Command-line compilation and execution

### 5.0 Test Instructions
Run all 8 test files in `src/Test/`:
- **IntelliJ:** Right-click `Test/` directory → Run 'All Tests'
- **Total:** ~130 test methods across 8 files
- See `README_Iteration5.txt` Section 5.0 for detailed test coverage summary.

### 6.0 Responsibilities
- Agent Capacity & Refill Logic: **Idara-Abasi Udoh**
- Scheduler Capacity Enforcement: **Ohioreuna Ajayi-Isuku**
- Testing (8 test files, ~130 tests): **Divine Eyo & Suveatha Karunakaran**
- Diagrams, Documentation & Read-Me: **Idara-Abasi Udoh & Divine Eyo**
