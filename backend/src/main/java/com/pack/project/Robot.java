package com.pack.project;

import jakarta.persistence.*;

@Entity
@Table(name = "robots")
public class Robot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(name = "current_x")
    private int currentX;

    @Column(name = "current_y")
    private int currentY;

    @Column(name = "target_x")
    private int targetX;

    @Column(name = "target_y")
    private int targetY;

    // --- NEW: Mission Memory ---
    @Column(name = "saved_target_x")
    private int savedTargetX;

    @Column(name = "saved_target_y")
    private int savedTargetY;

    @Column(name = "battery_percentage")
    private int batteryPercentage;

    private String status;

    // --- GETTERS ---
    public Long getId() { return id; }
    public String getName() { return name; }
    public int getCurrentX() { return currentX; }
    public int getCurrentY() { return currentY; }
    public int getTargetX() { return targetX; }
    public int getTargetY() { return targetY; }
    public int getSavedTargetX() { return savedTargetX; }
    public int getSavedTargetY() { return savedTargetY; }
    public int getBatteryPercentage() { return batteryPercentage; }
    public String getStatus() { return status; }

    // --- SETTERS ---
    public void setId(Long id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setCurrentX(int currentX) { this.currentX = currentX; }
    public void setCurrentY(int currentY) { this.currentY = currentY; }
    public void setTargetX(int targetX) { this.targetX = targetX; }
    public void setTargetY(int targetY) { this.targetY = targetY; }
    public void setSavedTargetX(int savedTargetX) { this.savedTargetX = savedTargetX; }
    public void setSavedTargetY(int savedTargetY) { this.savedTargetY = savedTargetY; }
    public void setBatteryPercentage(int batteryPercentage) { this.batteryPercentage = batteryPercentage; }
    public void setStatus(String status) { this.status = status; }
}