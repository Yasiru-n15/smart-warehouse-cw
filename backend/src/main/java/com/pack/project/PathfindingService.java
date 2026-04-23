package com.pack.project;

import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class PathfindingService {

    public List<Node> getSmartPath(Robot robot, Node targetGoal, List<Node> allNodes, List<Robot> activeRobots) {
        Node startNode = findNodeAt(robot.getCurrentX(), robot.getCurrentY(), allNodes);
        return findPath(startNode, targetGoal, allNodes, activeRobots, robot);
    }

    public int getPathLength(Node start, Node goal, List<Node> allNodes) {
        List<Node> path = findPath(start, goal, allNodes, Collections.emptyList(), null);
        return (path != null && !path.isEmpty()) ? path.size() - 1 : 999;
    }

    public boolean isDangerZone(Node node, List<Node> allNodes) {
        if (node.isFire()) return true;
        for (Node n : allNodes) {
            if (n.isFire()) {
                if (Math.abs(n.getXCoord() - node.getXCoord()) <= 1 && Math.abs(n.getYCoord() - node.getYCoord()) <= 1) {
                    return true;
                }
            }
        }
        return false;
    }

    public List<Node> findPath(Node start, Node goal, List<Node> allNodes, List<Robot> activeRobots, Robot currentRobot) {
        for (Node n : allNodes) { n.gCost = Double.MAX_VALUE; n.parent = null; }
        PriorityQueue<Node> openSet = new PriorityQueue<>(Comparator.comparingDouble(n -> n.fCost));
        Set<Node> closedSet = new HashSet<>();

        start.gCost = 0;
        start.hCost = Math.abs(start.getXCoord() - goal.getXCoord()) + Math.abs(start.getYCoord() - goal.getYCoord());
        start.fCost = start.gCost + start.hCost;
        openSet.add(start);

        // NEW: Track the closest node to the goal.
        // If the goal is blocked by fire, we return the path to this node instead!
        Node closestNode = start;
        double minHCost = start.hCost;

        while (!openSet.isEmpty()) {
            Node current = openSet.poll();
            if (current.getXCoord() == goal.getXCoord() && current.getYCoord() == goal.getYCoord()) {
                return reconstructPath(current);
            }
            closedSet.add(current);

            // NEW: Update closest node as we explore
            if (current.hCost < minHCost) {
                minHCost = current.hCost;
                closestNode = current;
            }

            for (Node neighbor : getNeighbors(current, allNodes)) {
                if (neighbor.isObstacle() || isDangerZone(neighbor, allNodes) || closedSet.contains(neighbor)) continue;

                boolean isOccupied = false;
                for (Robot otherRobot : activeRobots) {
                    if (currentRobot != null && !otherRobot.getId().equals(currentRobot.getId())) {
                        if (otherRobot.getCurrentX() == neighbor.getXCoord() && otherRobot.getCurrentY() == neighbor.getYCoord()) {
                            if (neighbor.getXCoord() == 0 && neighbor.getYCoord() == 0) continue;
                            isOccupied = true;
                            break;
                        }
                    }
                }
                if (isOccupied) continue;

                double tentativeGCost = current.gCost + 1;
                if (tentativeGCost < neighbor.gCost) {
                    neighbor.parent = current;
                    neighbor.gCost = tentativeGCost;
                    neighbor.hCost = Math.abs(neighbor.getXCoord() - goal.getXCoord()) + Math.abs(neighbor.getYCoord() - goal.getYCoord());
                    neighbor.fCost = neighbor.gCost + neighbor.hCost;
                    if (!openSet.contains(neighbor)) openSet.add(neighbor);
                }
            }
        }
        // NEW: Return the path to the closest reachable node (edges of the fire)
        return reconstructPath(closestNode);
    }

    private List<Node> reconstructPath(Node node) {
        List<Node> path = new ArrayList<>();
        while (node != null) { path.add(node); node = node.parent; }
        Collections.reverse(path);
        return path;
    }

    private List<Node> getNeighbors(Node current, List<Node> allNodes) {
        List<Node> neighbors = new ArrayList<>();
        for (Node node : allNodes) {
            if (Math.abs(node.getXCoord() - current.getXCoord()) + Math.abs(node.getYCoord() - current.getYCoord()) == 1) {
                neighbors.add(node);
            }
        }
        return neighbors;
    }

    private Node findNodeAt(int x, int y, List<Node> allNodes) {
        return allNodes.stream().filter(n -> n.getXCoord() == x && n.getYCoord() == y).findFirst().orElse(null);
    }
}