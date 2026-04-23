# 🚛 Smart Warehouse Path Planner
**A Full-Stack Autonomous AGV Management System**

The **Smart Warehouse Path Planner** is a high-fidelity simulation designed to manage a fleet of Autonomous Guided Vehicles (AGVs) within a 10x10 warehouse grid. This system bridges the gap between software logic and physical constraints by integrating real-time pathfinding, predictive battery management, and localized hazard detection.

---

## 🛠️ Tech Stack

* **Frontend:** React.js, Axios, Custom CSS (Grid Visualization)
* **Backend:** Java Spring Boot, Spring Data JPA, Scheduled Tasks
* **Database:** MySQL (Persistence for Map, Robots, and Queues)

---

## 🧠 Core Intelligence

### 1. Advanced Navigation (A* Algorithm)
The system utilizes a modified **A-Star (A*)** algorithm to calculate the most efficient path.

* **Heuristic Calculation:** Uses **Manhattan Distance** for efficiency:
  $$h(n) = |x_1 - x_2| + |y_1 - y_2|$$
* **Collision Avoidance:** Active robots are treated as dynamic, temporary obstacles.
* **Dynamic Rerouting:** If a target is blocked, the robot identifies and moves to the closest reachable "Safe Node."

### 2. Predictive Battery Management
To ensure zero "dead-on-floor" incidents, the backend runs a predictive loop every second:

* **Calculated Threshold:** Before any movement, the system estimates the energy cost: 
  > **Cost = (Path to Target) + (Path from Target to Base)**
* **Safety Protocol:** If the cost exceeds current battery levels, the mission is aborted, the job is saved, and the AGV enters the `CHARGING_BOUND` state.

### 3. Localized Hazard Response ("Fire Sensor")
This project simulates **Physical Sensors** rather than a "global knowledge" system:

* **Danger Zones:** Fire hazards create a 3x3 AOE (Area of Effect).
* **Sensor Trigger:** AGVs only detect the hazard when they reach the edge of the Danger Zone.
* **Job Skipping:** Upon detection, the robot logs a warning, skips the blocked task, and moves to the next item in its FIFO queue.

---

## 🏗️ Architectural Components

### FIFO Job Queueing
The system implements a true **First-In-First-Out** queue, allowing users to stack multiple tasks for a single robot.
* **Data Structure:** Semicolon-separated coordinates stored in MySQL (e.g., `5,9; 2,1; 0,4;`).
* **Auto-Wake:** Robots in an `IDLE` state automatically detect new queue entries and begin execution.

### Robot State Machine
Robots transition through distinct logical states:
* 🟢 **IDLE**: Awaiting tasks at base.
* 🔵 **MOVING**: Actively executing a path.
* 🟡 **CHARGING_BOUND**: Low battery; returning to base.
* ⚡ **CHARGING**: Reclaiming power at (0,0).
* 🟠 **RETURNING**: All tasks complete; heading home.

---

## 🚀 Key Achievements

* **Database Robustness:** Implemented standard character encoding to handle 4-byte UTF-8 warning strings.
* **Realistic Simulation:** Focused on localized sensor-range detection rather than global awareness.
* **Persistent Memory:** AGVs retain "knowledge" of failed tasks and environmental status across sessions.

---

## 💻 Setup Instructions

1. **Database:** Import the provided `.sql` schema into your MySQL instance.
2. **Backend:** Configure `application.properties` with your DB credentials.
3. **Frontend:** ```bash
   npm install
   npm run dev
