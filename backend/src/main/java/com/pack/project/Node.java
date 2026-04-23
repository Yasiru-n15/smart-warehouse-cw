package com.pack.project;

import jakarta.persistence.*;

@Entity
@Table(name = "nodes")
public class Node {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "x_coord") private int xCoord;
    @Column(name = "y_coord") private int yCoord;
    @Column(name = "is_obstacle") private boolean isObstacle;

    // NEW: Fire Hazard
    @Column(name = "is_fire") private boolean isFire;

    @Transient public double gCost = Double.MAX_VALUE;
    @Transient public double hCost;
    @Transient public double fCost;
    @Transient public Node parent;

    public int getXCoord() { return xCoord; }
    public int getYCoord() { return yCoord; }
    public boolean isObstacle() { return isObstacle; }
    public boolean isFire() { return isFire; }
    public void setFire(boolean isFire) { this.isFire = isFire; }
}