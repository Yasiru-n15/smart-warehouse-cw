package com.pack.project;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class RobotSimulationService {

    @Autowired private RobotRepository robotRepository;
    @Autowired private NodeRepository nodeRepository;
    @Autowired private PathfindingService pathfindingService;

    private boolean pullNextJob(Robot robot) {
        String q = robot.getJobQueue();
        if (q != null && !q.isEmpty()) {
            String[] jobs = q.split(";");
            if (jobs.length > 0 && !jobs[0].isEmpty()) {
                String[] coords = jobs[0].split(",");
                robot.setTargetX(Integer.parseInt(coords[0].trim()));
                robot.setTargetY(Integer.parseInt(coords[1].trim()));

                StringBuilder newQ = new StringBuilder();
                for (int i = 1; i < jobs.length; i++) {
                    if (!jobs[i].trim().isEmpty()) newQ.append(jobs[i].trim()).append(";");
                }
                robot.setJobQueue(newQ.toString());
                robot.setStatus("MOVING");
                return true;
            }
        }
        return false;
    }

    @Transactional
    @Scheduled(fixedRate = 1000)
    public void runSimulationLoop() {
        List<Robot> activeRobots = robotRepository.findAll();
        List<Node> allNodes = nodeRepository.findAll();

        for (Robot robot : activeRobots) {

            // --- NEW: Persistent Hazard Memory Check ---
            // If the robot is carrying a warning, check if that specific fire has been manually removed by the user.
            if (robot.getFireWarning() != null && robot.getFireWarning().startsWith("[WARNING] (")) {
                try {
                    int start = robot.getFireWarning().indexOf("(") + 1;
                    int end = robot.getFireWarning().indexOf(")");
                    String[] coords = robot.getFireWarning().substring(start, end).split(",");
                    int hazardX = Integer.parseInt(coords[0].trim());
                    int hazardY = Integer.parseInt(coords[1].trim());

                    Node hazardNode = findNodeAt(hazardX, hazardY, allNodes);
                    // If the node is no longer a danger zone, the user put out the fire! We can clear the message.
                    if (hazardNode != null && !pathfindingService.isDangerZone(hazardNode, allNodes)) {
                        robot.setFireWarning("");
                    }
                } catch (Exception e) {
                    // Ignore parse errors, keep message active
                }
            }

            // --- NEW: Wake up IDLE robots ---
            // Check if jobs were assigned to a robot while it was sitting at the base
            if ("IDLE".equals(robot.getStatus())) {
                pullNextJob(robot);
            }

            // 1. Predictive Battery Check
            if ("MOVING".equals(robot.getStatus())) {
                Node curr = findNodeAt(robot.getCurrentX(), robot.getCurrentY(), allNodes);
                Node dest = findNodeAt(robot.getTargetX(), robot.getTargetY(), allNodes);
                Node base = findNodeAt(0, 0, allNodes);
                if (curr != null && dest != null && base != null) {
                    int cost = pathfindingService.getPathLength(curr, dest, allNodes) +
                            pathfindingService.getPathLength(dest, base, allNodes) + 2;
                    if (robot.getBatteryPercentage() < cost) {
                        robot.setSavedTargetX(robot.getTargetX());
                        robot.setSavedTargetY(robot.getTargetY());
                        robot.setTargetX(0); robot.setTargetY(0);
                        robot.setStatus(robot.getCurrentX() == 0 && robot.getCurrentY() == 0 ? "CHARGING" : "CHARGING_BOUND");
                    }
                }
            }

            // 2. Pathfinding, Movement, and Heat Sensor Trigger
            if ("MOVING".equals(robot.getStatus()) || "RETURNING".equals(robot.getStatus()) || "CHARGING_BOUND".equals(robot.getStatus())) {
                Node targetNode = findNodeAt(robot.getTargetX(), robot.getTargetY(), allNodes);

                if (targetNode != null) {
                    List<Node> path = pathfindingService.getSmartPath(robot, targetNode, allNodes, activeRobots);
                    boolean targetIsHazard = pathfindingService.isDangerZone(targetNode, allNodes);
                    boolean arrivedAtTarget = (robot.getCurrentX() == robot.getTargetX() && robot.getCurrentY() == robot.getTargetY());

                    // SENSOR TRIGGER: If not at destination, but path is exhausted (we got as close as we could)
                    if (!arrivedAtTarget && (path == null || path.size() <= 1)) {

                        if (targetIsHazard) {
                            robot.setFireWarning("[WARNING] (" + robot.getTargetX() + "," + robot.getTargetY() + ") job can't do cause there is a fire!");
                        } else {
                            robot.setFireWarning("[WARNING] (" + robot.getTargetX() + "," + robot.getTargetY() + ") is unreachable!");
                        }

                        // Skip this job
                        robot.setTargetX(robot.getCurrentX());
                        robot.setTargetY(robot.getCurrentY());

                        // Immediately pull the next job, or return to base
                        if (!pullNextJob(robot)) {
                            robot.setStatus(robot.getCurrentX() == 0 && robot.getCurrentY() == 0 ? "IDLE" : "RETURNING");
                            robot.setTargetX(0); robot.setTargetY(0);
                        }
                        robotRepository.save(robot);
                        continue;
                    }

                    // Normal movement step
                    if (!arrivedAtTarget && path != null && path.size() > 1) {
                        Node nextStep = path.get(1);
                        robot.setCurrentX(nextStep.getXCoord());
                        robot.setCurrentY(nextStep.getYCoord());
                        robot.setBatteryPercentage(Math.max(0, robot.getBatteryPercentage() - 1));
                    }
                }

                // 3. Destination Reached Logic
                if (robot.getCurrentX() == robot.getTargetX() && robot.getCurrentY() == robot.getTargetY()) {
                    if ("MOVING".equals(robot.getStatus())) {
                        if (!pullNextJob(robot)) {
                            robot.setStatus("RETURNING");
                            robot.setTargetX(0); robot.setTargetY(0);
                        }
                    } else if ("RETURNING".equals(robot.getStatus())) {
                        // Check if a new job came in while we were returning!
                        if (!pullNextJob(robot)) {
                            robot.setStatus("IDLE");
                        }
                    } else if ("CHARGING_BOUND".equals(robot.getStatus())) {
                        robot.setStatus("CHARGING");
                    }
                }
            }

            // 4. Charging Logic
            if ("CHARGING".equals(robot.getStatus())) {
                robot.setBatteryPercentage(Math.min(100, robot.getBatteryPercentage() + 10));
                if (robot.getBatteryPercentage() == 100) {
                    if (robot.getSavedTargetX() != 0 || robot.getSavedTargetY() != 0) {
                        robot.setTargetX(robot.getSavedTargetX());
                        robot.setTargetY(robot.getSavedTargetY());
                        robot.setSavedTargetX(0);
                        robot.setSavedTargetY(0);
                        robot.setStatus("MOVING");
                    } else if (!pullNextJob(robot)) {
                        robot.setStatus("IDLE");
                    }
                }
            }
            robotRepository.save(robot);
        }
    }

    private Node findNodeAt(int x, int y, List<Node> nodes) {
        return nodes.stream().filter(n -> n.getXCoord() == x && n.getYCoord() == y).findFirst().orElse(null);
    }
}