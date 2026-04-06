============================================================
Firefighting Drone Swarm - SYSC 3303A Winter 2026
Iteration 5 Submission
============================================================

============================================================
1.0 OVERVIEW
============================================================

This project simulates a firefighting drone swarm system composed of:
  - Scheduler (central coordinator)
  - Fire Incident Subsystem (reads fire events from CSV)
  - Drone Subsystem (one or more drones executing tasks)
  - GUI Visualization (real-time map of zones, fires, and drones)

All inter-subsystem communication uses UDP datagrams. The system
runs as 3+ separate processes that can execute on different machines.

Iteration 5 builds on Iteration 4 by adding:
  - Agent capacity enforcement (drones track remaining agent)
  - Refill station logic (drones return to base to refill when low)
  - Pre-task capacity check (drone auto-returns if agent < litersNeeded)
  - Post-task low-agent detection (Scheduler sends RETURN_BASE if agent < 10L)
  - Enhanced GUI (drone position, fire status, faults, agent level)
  - Fire intensity tracking and visualization
  - Comprehensive test suite (8 test files, ~130 test methods)

============================================================
2.0 FILE NAMES AND CONVENTIONS
============================================================

Source files are organized into packages under src/:

------------------------------------------------------------
2.1 Drone Package (src/drone/)
------------------------------------------------------------
  DroneCommand.java     Record for Scheduler-to-drone instructions (TASK, RETURN_BASE, SHUTDOWN).
  DroneInfo.java        Tracks per-drone state in the Scheduler registry: ID, position,
                        agent level, capacity, assignment, faults, and availability.
  DroneMain.java        Standalone entry point for a single drone process.
  DroneResult.java      Data class for drone task results (droneId, zoneId, completed, remainingAgent).
  DroneState.java       Enum: IDLE, EN_ROUTE, DROPPING_AGENT, RETURNING_BASE,
                        FAULT_STUCK, FAULT_NOZZLE, FAULT_SENSOR, OFFLINE, SHUTDOWN.
  DroneSubsystem.java   Independent drone worker communicating via UDP. Tracks agentCapacity
                        and currentAgent. Auto-returns to base for refill when insufficient.
  FaultType.java        Enum: NONE, DRONE_STUCK (soft), NOZZLE_STUCK (hard), SENSOR_FAIL (soft).
                        Includes isHardFault(), isSoftFault(), fromString().

------------------------------------------------------------
2.2 Scheduler Package (src/scheduler/)
------------------------------------------------------------
  Scheduler.java        Multi-drone UDP scheduler with 4 internal threads:
                        fire listener, drone listener, dispatch loop, fault monitor.
                        Implements load balancing, capacity-aware drone selection,
                        RETURN_BASE on low agent, fault handling, and re-queuing.
  SchedulerMain.java    Standalone entry point for the Scheduler process. Loads zones
                        and launches the GUI.

------------------------------------------------------------
2.3 Fire Incident Package (src/fireincident/)
------------------------------------------------------------
  FireEvent.java        Represents a fire incident: time, zoneId, eventType, severity,
                        faultType. Includes getLitersNeeded() (LOW=10, MODERATE=20, HIGH=30).
  FireIncidentMain.java Standalone entry point for the Fire Incident Subsystem.
  FireIncidentSubsystem.java  Reads fire events from CSV and sends them to the Scheduler
                        via UDP. Parses optional 5th fault column.

------------------------------------------------------------
2.4 Network Package (src/network/)
------------------------------------------------------------
  UDPHelper.java        Utility class for all UDP communication: message building/parsing
                        for 8 message types, send/receive helpers, port constants,
                        and timestamp formatting.

------------------------------------------------------------
2.5 GUI Package (src/gui/)
------------------------------------------------------------
  GuiModel.java         Thread-safe singleton tracking per-drone states, positions,
                        assignments, faults, active fire zones, and fire intensity.
  MapPanel.java         Renders the map with zone boundaries, severity-colored fires,
                        per-drone markers, assignment lines, fault colors, and legend.
  Zone.java             Represents a geographical zone with ID and boundary coordinates.
  ZoneParser.java       Reads zone definitions from CSV into Zone objects.

------------------------------------------------------------
2.6 Entry Points
------------------------------------------------------------
  Main.java             Convenience all-in-one launcher (Scheduler + N drones + Fire
                        Subsystem in one JVM). Default: 2 drones.
  SchedulerMain.java    Standalone Scheduler process (starts GUI).
  DroneMain.java        Standalone Drone process (one per drone).
  FireIncidentMain.java Standalone Fire Incident Subsystem process.

------------------------------------------------------------
2.7 Input Files
------------------------------------------------------------
  fire_events.csv       Fire event input file. Format per line:
                        time,zoneId,eventType,severity[,faultType]
                        Example: 14:03:15,3,FIRE_DETECTED,High,NONE

                        Current contents:
                          14:03:15,3,FIRE_DETECTED,High,NONE
                          14:03:20,1,FIRE_DETECTED,Low,DRONE_STUCK
                          14:05:45,5,DRONE_REQUEST,Moderate,NONE
                          14:06:00,7,FIRE_DETECTED,High,NOZZLE_STUCK
                          14:08:30,2,FIRE_DETECTED,Low,NONE
                          14:10:00,4,FIRE_DETECTED,Moderate,SENSOR_FAIL
                          14:12:15,6,FIRE_DETECTED,High,NONE

  sample_zone_file.csv  Zone layout file. Format per line:
                        zoneId,(startX;startY),(endX;endY)
                        Example: 1,(0;0),(301;301)

                        Current contents (7 zones):
                          1,(0;0),(301;301)
                          2,(301;0),(601;301)
                          3,(0;301),(301;601)
                          4,(301;301),(601;601)
                          5,(601;0),(901;301)
                          6,(601;301),(901;601)
                          7,(0;601),(301;901)

------------------------------------------------------------
2.8 Test Files (src/Test/)
------------------------------------------------------------
  DroneSubsystemTest.java          Tests drone init, capacity, litersForSeverity, DroneResult,
                                   DroneCommand, DroneState enum, FaultType enum, fault types,
                                   agent capacity enforcement, shutdown.
  DroneInfoTest.java               Tests DroneInfo construction, state management, availability,
                                   hasEnoughAgent, refill, assignment, task tracking, fault
                                   tracking, dispatch timestamp, full lifecycle scenarios.
  FireEventTest.java               Tests litersNeeded (all severities), constructors, all
                                   setters, equals/hashCode edge cases, toString, enums.
  FireIncidentSubsystemTest.java   Tests CSV line parsing for all severity levels, all fault
                                   types, missing fault column, whitespace, case insensitivity.
  GuiModelTest.java                Tests singleton, drone states, assignments, faults, active
                                   fires, positions, fire intensity, immutable snapshots.
  SchedulerTest.java               Tests findBestDrone (no drones, single, closest, busy/offline
                                   skip, insufficient agent, load balancing), registry, queue.
  UDPHelperTest.java               Tests all message build/parse methods, round-trips, port
                                   constants, message type extraction, timestamp format.
  ZoneTest.java                    Tests Zone construction, center calculation, isOddByOdd,
                                   multiple zones.

------------------------------------------------------------
2.9 Diagrams (Diagrams/Iteration5/)
------------------------------------------------------------
  class_diagram.puml      PlantUML class diagram showing all classes, fields, methods,
                          and relationships for Iteration 5.
  state_diagram.puml      PlantUML state machine diagram for the Drone Subsystem,
                          including fault states and refill transitions.
  sequence_diagram.puml   PlantUML sequence diagram showing normal dispatch, agent refill,
                          soft fault (DRONE_STUCK), hard fault (NOZZLE_STUCK), re-dispatch,
                          and shutdown scenarios.

============================================================
3.0 TEAM RESPONSIBILITIES BREAKDOWN
============================================================

  Agent Capacity & Refill Logic:        Idara-Abasi Udoh
  Scheduler Capacity Enforcement:       Ohioreuna Ajayi-Isuku
  Testing (8 test files, ~130 tests):   Divine Eyo & Suveatha Karunakaran
  Diagrams & README:                    Idara-Abasi Udoh & Divine Eyo

  Detailed breakdown:

  Idara-Abasi Udoh
    - DroneSubsystem: agent capacity tracking, doReturnToBase() refill logic,
      pre-task capacity check (currentAgent < litersNeeded)
    - DroneInfo: agentCapacity, remainingAgent, hasEnoughAgent(), refill()
    - UDPHelper: capacity/agent fields in register/status/result messages
    - GUI enhancements: drone position, fire intensity, fault colors
    - Sequence diagram, class diagram, README

  Ohioreuna Ajayi-Isuku
    - Scheduler: findBestDrone() capacity-aware selection (hasEnoughAgent check),
      handleDroneResult() low-agent detection, sendReturnBase() command,
      fault monitor loop, timeout fault detection
    - State diagram

  Divine Eyo
    - UDPHelperTest, DroneInfoTest, GuiModelTest, SchedulerTest
    - FireEventTest expansion, DroneSubsystemTest expansion
    - README contributions

  Suveatha Karunakaran
    - FireIncidentSubsystemTest, ZoneTest
    - Test review and validation

============================================================
4.0 SETUP AND BUILD INSTRUCTIONS
============================================================

------------------------------------------------------------
4.1 Prerequisites
------------------------------------------------------------
  - Java Version: Java 21 (minimum), Java 25 (maximum)
  - IDE: IntelliJ IDEA (recommended) or any Java IDE with JUnit 5 support
  - Download/clone the repository

------------------------------------------------------------
4.2 Project Structure in IntelliJ
------------------------------------------------------------
  1. Open IntelliJ IDEA -> File -> Open -> select the project root folder
     (Firefighting-Drone-Swarm-SYSC3303)
  2. Mark "src" as Sources Root:
     Right-click src/ -> Mark Directory as -> Sources Root
  3. Mark "src/Test" as Test Sources Root:
     Right-click src/Test/ -> Mark Directory as -> Test Sources Root
  4. Ensure JUnit 5 is on the classpath:
     File -> Project Structure -> Libraries -> Add (+) -> From Maven
     Search for: org.junit.jupiter:junit-jupiter:5.10.0
     (Or use the built-in IntelliJ JUnit support)

------------------------------------------------------------
4.3 Running the Simulation
------------------------------------------------------------

  OPTION A: All-in-One Launcher (Recommended for Quick Demo)
  -----------------------------------------------------------
  Run Main.java in IntelliJ:
    - Right-click Main.java -> Run 'Main.main()'
    - This starts the Scheduler, GUI, 2 drones, and Fire Subsystem
      in a single JVM.
    - Optional program arguments:
        fire_events.csv 3     (custom input file, 3 drones)

  OPTION B: Three Separate Processes (Recommended for Full Demo)
  ---------------------------------------------------------------
  Start in this order (open 4+ separate terminals or IntelliJ run configs):

  Terminal 1 - Scheduler (start FIRST):
    Run SchedulerMain.java
    - This starts the Scheduler and opens the GUI window.
    - Optional argument: path to zone file

  Terminal 2 - Drone 1:
    Run DroneMain.java with program argument: 1
    - Optional arguments: 1 30.0 localhost
      (droneId, agentCapacity, schedulerHost)

  Terminal 3 - Drone 2:
    Run DroneMain.java with program argument: 2

  Terminal 4 - Fire Incident Subsystem (start LAST):
    Run FireIncidentMain.java
    - Optional arguments: fire_events.csv localhost
      (inputFile, schedulerHost)

  COMMAND LINE (if not using IDE):
  --------------------------------
  Open a terminal in the src/ directory:

    javac -d ../out drone/*.java scheduler/*.java fireincident/*.java
          network/*.java gui/*.java *.java

    # Terminal 1:
    java -cp ../out scheduler.SchedulerMain

    # Terminal 2:
    java -cp ../out drone.DroneMain 1

    # Terminal 3:
    java -cp ../out drone.DroneMain 2

    # Terminal 4:
    java -cp ../out fireincident.FireIncidentMain

------------------------------------------------------------
4.4 Expected Simulation Behavior
------------------------------------------------------------
  1. Scheduler starts and listens on ports 5000 (fire events) and
     5001 (drone messages).
  2. Each drone registers with the Scheduler (sends capacity and position).
  3. Fire Subsystem reads fire_events.csv and sends events to the Scheduler.
  4. Scheduler dispatches drones based on:
     - Drone availability (IDLE and not offline)
     - Agent capacity (hasEnoughAgent for the severity)
     - Distance to target zone (closest preferred)
     - Load balancing (fewer tasks completed preferred)
  5. Drones travel to zones, drop agent, and report results.
  6. If a drone's remaining agent drops below 10L after a task,
     the Scheduler sends RETURN_BASE -> drone returns and refills.
  7. If a drone doesn't have enough agent before starting a task,
     it auto-returns to base for a refill first.
  8. Faults are injected per the CSV:
     - DRONE_STUCK: drone freezes 3s, self-recovers, returns to base
     - NOZZLE_STUCK: drone goes permanently OFFLINE
     - SENSOR_FAIL: drone freezes 3s, self-recovers, returns to base
  9. Affected zones are re-queued and dispatched to available drones.
  10. GUI displays all activity in real time.

============================================================
5.0 TEST INSTRUCTIONS
============================================================

------------------------------------------------------------
5.1 Running Tests in IntelliJ (Recommended)
------------------------------------------------------------
  1. Ensure JUnit 5 is configured (see Section 4.2 step 4).
  2. Right-click the src/Test/ directory -> Run 'All Tests'
     This runs all 8 test files (~130 test methods).
  3. Alternatively, right-click any individual test file to run it:
     - DroneSubsystemTest.java    (22 tests)
     - DroneInfoTest.java         (24 tests)
     - FireEventTest.java         (18 tests)
     - FireIncidentSubsystemTest.java (11 tests)
     - GuiModelTest.java          (23 tests)
     - SchedulerTest.java         (10 tests)
     - UDPHelperTest.java         (30 tests)
     - ZoneTest.java              (8 tests)

------------------------------------------------------------
5.2 Running Tests via Command Line
------------------------------------------------------------
  Requires JUnit Platform Console Standalone JAR:

    java -jar junit-platform-console-standalone-1.10.0.jar \
      --class-path ../out \
      --scan-class-path

------------------------------------------------------------
5.3 Test Coverage Summary
------------------------------------------------------------

  DroneSubsystemTest:
    - Drone initial state, default/custom capacity
    - litersForSeverity (all levels + case insensitivity + unknown)
    - DroneResult creation, setters, toString
    - DroneCommand record (task, returnBase, shutdown)
    - DroneState enum (9 states)
    - FaultType enum (4 types, hard/soft classification, fromString)
    - FireEvent with/without fault type
    - Agent capacity enforcement, shutdown behavior

  DroneInfoTest:
    - Construction with defaults, custom position
    - State management (setState, isIdle)
    - Availability (idle, busy, offline, offline+idle)
    - Agent capacity (hasEnoughAgent at boundaries, after depletion)
    - Refill (restores to capacity)
    - Assignment set/clear
    - Task tracking (incrementTasksCompleted)
    - Fault tracking (currentFault, faultCount, permanentlyOffline)
    - Dispatch timestamp
    - Position updates
    - toString validation
    - Full lifecycle: normal dispatch -> complete -> refill
    - Full lifecycle: hard fault -> offline

  FireEventTest:
    - getLitersNeeded for LOW, MODERATE, HIGH
    - 4-arg and 5-arg constructors
    - All setters (time, zoneId, eventType, severity, faultType)
    - equals: same object, equal objects, different zone/severity/fault
    - equals: null, different class
    - hashCode consistency
    - toString content validation
    - EventType and Severity enum coverage

  FireIncidentSubsystemTest:
    - CSV parsing for all severity levels
    - CSV parsing for all fault types (NONE, DRONE_STUCK, NOZZLE_STUCK, SENSOR_FAIL)
    - Missing fault column defaults to NONE
    - Whitespace handling in CSV
    - Case-insensitive severity and event type
    - All 7 rows from fire_events.csv
    - litersNeeded matches parsed severity

  GuiModelTest:
    - Singleton identity
    - Drone state set/get/default/snapshot/immutability
    - Drone assignment set/clear
    - Drone fault set/clear (NONE and null)/default/snapshot
    - Active fire add/remove/count/snapshot
    - Drone position set/get/default/copy semantics/snapshot
    - Fire intensity set/clamp/zero-remove/negative-remove/default
    - addActiveFire sets intensity to 1.0
    - removeActiveFire clears intensity

  SchedulerTest:
    - findBestDrone: no drones returns null
    - findBestDrone: single available drone
    - findBestDrone: closer drone preferred
    - findBestDrone: skips busy (EN_ROUTE) drones
    - findBestDrone: skips offline drones
    - findBestDrone: skips drones with insufficient agent
    - findBestDrone: all unavailable returns null
    - findBestDrone: all insufficient agent returns null
    - findBestDrone: low severity needs less agent
    - findBestDrone: load balancing (fewer tasks preferred)
    - Pending events queue initially empty
    - Drone registry initially empty, add drone

  UDPHelperTest:
    - Port constants (5000, 5001, 5002)
    - getDroneListenPort (6000 + droneId)
    - Message type constants (8 types)
    - getMessageType (with/without pipe)
    - Fire event build/parse/round-trip (with and without fault)
    - Drone register build/parse/round-trip
    - Drone command build (4-arg, 5-arg), parse fields, fault present/missing
    - RETURN_BASE and SHUTDOWN command building
    - Drone status build/parse/round-trip
    - Drone result build/parse/round-trip (completed and failed)
    - Drone fault build/parse/round-trip
    - Confirmation build/parse/round-trip
    - Shutdown message build and type
    - Timestamp format validation (HH:mm:ss)

  ZoneTest:
    - Zone construction and getters
    - Center calculation (origin, non-zero origin, small zone)
    - isOddByOdd (true, false for even width/height/both)
    - Multiple zones with distinct centers

============================================================
6.0 UDP COMMUNICATION PROTOCOL
============================================================

All messages use pipe-delimited (|) UDP datagrams:

  Message Format                                          Direction
  -------------------------------------------------------+--------------------------
  FIRE_EVENT|time|zoneId|eventType|severity|faultType     Fire -> Scheduler
  DRONE_REGISTER|droneId|capacity|x|y                     Drone -> Scheduler
  DRONE_STATUS|droneId|state|x|y|remainingAgent           Drone -> Scheduler
  DRONE_COMMAND|droneId|cmdType|zoneId|severity|faultType  Scheduler -> Drone
  DRONE_RESULT|droneId|zoneId|completed|remainingAgent     Drone -> Scheduler
  DRONE_FAULT|droneId|faultType|zoneId                     Drone -> Scheduler
  CONFIRMATION|droneId|zoneId|completed                    Scheduler -> Fire
  SHUTDOWN                                                 Scheduler -> All

============================================================
7.0 PORT ASSIGNMENTS
============================================================

  Port            Usage
  ---------------+-----------------------------------------------
  5000            Fire Subsystem -> Scheduler (fire events)
  5001            Drone -> Scheduler (register, status, results, faults)
  5002            Scheduler -> Fire Subsystem (confirmations)
  6000 + droneId  Scheduler -> Drone (commands, e.g., 6001 for Drone 1)

============================================================
8.0 CONFIGURATION CONSTANTS
============================================================

  Constant                   Value     Location
  -------------------------+----------+--------------------
  FAULT_RESET_DELAY_MS       3000 ms   DroneSubsystem
  TRAVEL_TIMEOUT_MS          5000 ms   Scheduler
  DROP_TIMEOUT_MS            5000 ms   Scheduler
  FAULT_MONITOR_INTERVAL_MS  1000 ms   Scheduler
  Default agent capacity     30.0 L    DroneSubsystem
  Low agent threshold        10.0 L    Scheduler
  Agent per LOW severity     10 L      FireEvent / DroneSubsystem
  Agent per MODERATE          20 L      FireEvent / DroneSubsystem
  Agent per HIGH              30 L      FireEvent / DroneSubsystem

============================================================
9.0 ASSUMPTIONS
============================================================

  - All drones start at base position (0, 0) with default capacity 30.0L.
  - Travel time is simulated (not real GPS-based).
  - Agent refill at base is instantaneous upon arrival.
  - One fire per zone at a time.
  - The 5th column in fire_events.csv (fault type) is optional;
    events without it default to NONE.
  - All subsystems run on localhost by default but support
    configurable host addresses via command-line arguments.
  - Drones with insufficient agent are skipped during dispatch.
  - Drones automatically return to base when agent < litersNeeded
    (pre-task check) or when Scheduler detects agent < 10L (post-task).

============================================================
10.0 UML DIAGRAMS
============================================================

All diagrams are in Diagrams/Iteration5/ in PlantUML format (.puml).
Render with any PlantUML tool (IntelliJ PlantUML plugin, plantuml.com, etc.).

  class_diagram.puml
    - Complete class diagram showing all 18+ classes, their fields, methods,
      and relationships (composition, aggregation, dependency).
    - Includes DroneInfo capacity/refill methods, FaultType enum,
      GuiModel fault/position/intensity tracking, and Scheduler
      capacity-aware dispatch logic.

  state_diagram.puml
    - Drone state machine with 9 states.
    - Shows transitions for normal operation, pre-task refill,
      post-task RETURN_BASE, soft fault recovery (DRONE_STUCK,
      SENSOR_FAIL -> 3s reset -> RETURNING_BASE), and hard fault
      (NOZZLE_STUCK -> OFFLINE).

  sequence_diagram.puml
    - End-to-end sequence showing:
      1. Drone registration
      2. Normal fire dispatch and agent drop
      3. Low-agent detection and RETURN_BASE
      4. Agent refill at base
      5. Soft fault (DRONE_STUCK): freeze, re-queue, recovery, re-dispatch
      6. Hard fault (NOZZLE_STUCK): permanent offline, re-queue, re-dispatch
      7. Shutdown sequence

============================================================
