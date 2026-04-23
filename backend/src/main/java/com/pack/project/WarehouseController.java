package com.pack.project;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:5173")
public class WarehouseController {

    @Autowired private NodeRepository nodeRepository;
    @Autowired private RobotRepository robotRepository;

    @GetMapping("/nodes") public List<Node> getAllNodes() { return nodeRepository.findAll(); }
    @GetMapping("/robots") public List<Robot> getAllRobots() { return robotRepository.findAll(); }

    @PostMapping("/robots")
    public ResponseEntity<?> deployRobot(@RequestBody Robot newRobot) {
        try {
            return ResponseEntity.ok(robotRepository.save(newRobot));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to save robot: " + e.getMessage());
        }
    }

    // --- NEW: FIFO Queue Appender ---
    @PutMapping("/robots/{id}/target")
    public ResponseEntity<?> reassignRobot(@PathVariable Long id, @RequestBody Map<String, Integer> payload) {
        return robotRepository.findById(id).map(robot -> {
            int tx = payload.get("targetX");
            int ty = payload.get("targetY");

            if ("IDLE".equals(robot.getStatus())) {
                // Not busy! Start immediately.
                robot.setTargetX(tx);
                robot.setTargetY(ty);
                robot.setStatus("MOVING");
                robot.setFireWarning("");
            } else {
                // Busy! Add to the end of the queue line.
                String currentQ = robot.getJobQueue() == null ? "" : robot.getJobQueue();
                robot.setJobQueue(currentQ + tx + "," + ty + ";");
            }
            robotRepository.save(robot);
            return ResponseEntity.ok(robot);
        }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/fire")
    public ResponseEntity<?> toggleFire(@RequestBody Map<String, Integer> payload) {
        Node node = nodeRepository.findAll().stream()
                .filter(n -> n.getXCoord() == payload.get("x") && n.getYCoord() == payload.get("y"))
                .findFirst().orElse(null);
        if (node != null) {
            node.setFire(!node.isFire());
            return ResponseEntity.ok(nodeRepository.save(node));
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/robots/{id}")
    public ResponseEntity<?> deleteRobot(@PathVariable Long id) {
        if (robotRepository.existsById(id)) {
            robotRepository.deleteById(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }
}