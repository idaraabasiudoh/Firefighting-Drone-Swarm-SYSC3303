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
Same as Iteration1

