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

Core Simulation: - Main.java - Scheduler.java - DroneSubsystem.java -
DroneCommand.java - DroneState.java - DroneResult.java -
FireIncidentSubsystem.java - FireEvent.java

GUI: - GuiMain.java - MapPanel.java - GuiModel.java - Zone.java -
ZoneParser.java

Test Files: - src/fire_events.csv - src/sample_zone_file.csv

============================================================ How to Run
============================================================

Run Full System (Simulation + GUI): Run Main.java

Default configuration: - Input file: src/fire_events.csv - Number of
drones: 1

Optional: java Main

Run GUI Only: Run GuiMain.java

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
