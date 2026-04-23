CREATE DATABASE warehouse_db;
-- 1. Create the Database

USE warehouse_db;

-- 2. Create the Nodes Table (The Map)
-- Represents the warehouse floor as a graph of coordinates.
CREATE TABLE IF NOT EXISTS nodes (
    id INT AUTO_INCREMENT PRIMARY KEY,
    x_coord INT NOT NULL,
    y_coord INT NOT NULL,
    is_obstacle BOOLEAN DEFAULT FALSE,
    UNIQUE KEY unique_coords (x_coord, y_coord)
);

-- 3. Create the Robots Table
-- Tracks robot positions and their battery status.
CREATE TABLE IF NOT EXISTS robots (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    current_x INT NOT NULL,
    current_y INT NOT NULL,
    battery_percentage INT DEFAULT 100,
    status VARCHAR(20) DEFAULT 'IDLE', -- IDLE, MOVING, CHARGING, ERROR
    CONSTRAINT check_battery CHECK (battery_percentage >= 0 AND battery_percentage <= 100)
);

-- 4. Populate a 10x10 Grid (Nodes)
-- We use a procedure to avoid writing 100 individual INSERT statements.
DELIMITER //
CREATE PROCEDURE PopulateGrid()
BEGIN
    DECLARE x INT DEFAULT 0;
    DECLARE y INT DEFAULT 0;
    WHILE x < 10 DO
        SET y = 0;
        WHILE y < 10 DO
            INSERT INTO nodes (x_coord, y_coord, is_obstacle) VALUES (x, y, FALSE);
            SET y = y + 1;
        END WHILE;
        SET x = x + 1;
    END WHILE;
END //
DELIMITER ;

CALL PopulateGrid();


-- 5. Add Obstacles (Simulating Warehouse Aisles)
-- Let's turn columns of nodes into "shelves" (obstacles).
-- This creates two vertical aisles/shelves in the middle.
UPDATE nodes SET is_obstacle = TRUE WHERE x_coord = 2 AND y_coord BETWEEN 1 AND 8;
UPDATE nodes SET is_obstacle = TRUE WHERE x_coord = 5 AND y_coord BETWEEN 1 AND 8;
UPDATE nodes SET is_obstacle = TRUE WHERE x_coord = 8 AND y_coord BETWEEN 1 AND 8;

-- 6. Insert Initial Robots
INSERT INTO robots (name, current_x, current_y, battery_percentage, status) 
VALUES 
('Robot_Alpha', 0, 0, 100, 'IDLE'),
('Robot_Beta', 9, 9, 85, 'IDLE'),
('Robot_Gamma', 4, 4, 15, 'IDLE'); -- Low battery example

-- 7. Verify Data
SELECT * FROM nodes WHERE is_obstacle = TRUE;
SELECT * FROM robots;

-- 8. update 
USE warehouse_db;

-- 1. Turn off Safe Updates temporarily
SET SQL_SAFE_UPDATES = 0;

ALTER TABLE robots ADD COLUMN target_x INT DEFAULT 0;
ALTER TABLE robots ADD COLUMN target_y INT DEFAULT 0;

-- 2. Run our fix

UPDATE robots SET target_x = 0 WHERE target_x IS NULL;
UPDATE robots SET target_y = 0 WHERE target_y IS NULL;

-- 3. Turn Safe Updates back on to protect your database
SET SQL_SAFE_UPDATES = 1;

-- 9. update 

ALTER TABLE robots ADD COLUMN saved_target_x INT DEFAULT 0;
ALTER TABLE robots ADD COLUMN saved_target_y INT DEFAULT 0;

-- 10. Update 

USE warehouse_db;

ALTER TABLE nodes ADD COLUMN is_fire BOOLEAN DEFAULT FALSE;
ALTER TABLE robots ADD COLUMN fire_warning VARCHAR(255) DEFAULT '';
ALTER TABLE robots ADD COLUMN queued_target_x INT DEFAULT -1;
ALTER TABLE robots ADD COLUMN queued_target_y INT DEFAULT -1;

-- 11 update

USE warehouse_db;
ALTER TABLE robots ADD COLUMN job_queue VARCHAR(500) DEFAULT '';