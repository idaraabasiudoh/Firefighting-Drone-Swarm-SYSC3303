Firefighting Drone Swarm SYSC 3303A – Winter 2026 Iteration 2 Submission

============================================================ Overview
============================================================

This project simulates a firefighting drone system composed of: -
Scheduler - Fire Incident Subsystem - Drone Subsystem - GUI
Visualization

Iteration 2 introduces core scheduling logic, drone state transitions,
and GUI updates to track drone states and active fire incidents.

============================================================ What Was
Implemented in Iteration 2
============================================================

1.  Core Scheduling Logic

-   Scheduler maintains a queue of pending fire events.
-   Scheduler tracks drone state and remaining agent.
-   Drone is dispatched only if:
    -   Drone is IDLE
    -   Drone has sufficient agent
-   Otherwise the drone returns to base.

2.  Drone State Transitions Drone transitions through:

-   IDLE
-   EN_ROUTE
-   DROPPING_AGENT
-   RETURNING_BASE
-   SHUTDOWN

After completing a fire: - If another task can be serviced, drone
proceeds directly. - If not, drone returns to base. - Refill and
recharge at base are assumed instantaneous.

3.  GUI Updates GUI displays:

-   Zone boundaries
-   Fire locations (center of zone)
-   Drone state
-   Number of active fires

============================================================ File
Structure ============================================================

1.1 Main System

DroneCommand.java: Command object used by the scheduler to send instructions to drones (task assignment, return to base, or shutdown).
DroneResult.java: Stores the outcome of a drone’s task execution (if the task is completed).
DroneState.java: Enum defining drone states (IDLE, EN_ROUTE, DROPPING_AGENT, RETURNING_BASE, SHUTDOWN).
DroneSubsystem.java: Represents a drone worker thread. Requests tasks from the scheduler and processes assigned fire events.
FireEvent.java: Object representing a single fire incident, including attributes such as zone, severity, and time.
FireIncidentSubsystem.java: Reads fire incidents from the input file and submits them to the scheduler.
Main.java: Entry point of the program. Initializes the scheduler and subsystems, then starts the simulation.
Scheduler.java: Manages shared task queues and synchronizes communication between fire events and drones.
fire_events.csv: Input file containing a list of fire incidents used to drive the simulation.

1.2 GUI
GuiMain.java: Launches the graphical interface and initializes GUI components.
GuiModel.java: Thread-safe shared model that the scheduler updates and the GUI reads to track drone states and active fire zones.
MapPanel.java: Displays the map and visual representation of zones, fires, and drone activity.
Zone.java: Represents a geographical zone on the map, including its boundaries and identifiers.
ZoneParser.java: Reads and parses zone data from a CSV file into Zone objects.
sample_zone_file.csv: Input file defining zone layouts and coordinates for the GUI map.

1.3 Tests
DroneSubsystemTest.java: Tests drone subsystem behavior and state transitions.
FireEventTest.java: Ensures fire event objects are created and interpreted correctly.
SchedulerTest.java: Tests task coordination, queue handling, and synchronization logic.

============================================================ How to Run
============================================================

Java Version:
Minimum: Java 21
Maximum: Java 25

Download the repository before running the system.
Main System:Run the Main.java class.
GUI:Run the GuiMain.java class (GUI view only).
Tests: Run the test files located in the "Test/" directory.

============================================================ Scheduling
Policy (Iteration 2)
============================================================

-   One drone used for debugging simplicity.
-   Scheduler dispatches when drone is IDLE and has enough agent.
-   If insufficient agent, drone returns to base.
-   Once at base, refill and recharge are instantaneous.

============================================================ Assumptions
============================================================

-   Nozzle opening time included in drop time.
-   Recharge/refill not explicitly simulated.
-   One fire per zone at a time.
-   CSV ensures no overlapping zone conflicts.

============================================================ Test Cases
============================================================

Test 1 – Single Fire - Verify dispatch - Verify state transitions -
Verify fire count decrement

Test 2 – Multiple Sequential Fires - Verify queuing behavior - Verify
drone processes in order

Test 3 – Agent Capacity - Verify return-to-base behavior - Verify refill
logic

============================================================ Conclusion
============================================================

Iteration 2 successfully introduces scheduling logic, drone state
management, and GUI tracking. The system is now prepared for multi-drone
expansion in Iteration 3.
