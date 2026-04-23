package com.pack.project;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;



/**
 * The Entry Point for the Smart Warehouse Path Planner Backend.
 * This class starts the Spring Boot application and handles component scanning.
 */
@SpringBootApplication
@EnableScheduling
public class Main {

    public static void main(String[] args) {
        // This launches the embedded Tomcat server on port 8080
        
        SpringApplication.run(Main.class, args);
    }
}

