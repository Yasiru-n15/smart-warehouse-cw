import { useState, useEffect } from 'react';
import axios from 'axios';
import './App.css';

export default function App() {
  const [robots, setRobots] = useState([]);
  const [nodes, setNodes] = useState([]);
  const [notifications, setNotifications] = useState([]);

  const BASE_LOC = { x: 0, y: 0 };

  const [newRobot, setNewRobot] = useState({ name: '', targetX: 0, targetY: 0, battery: 100 });
  const [reassignCommand, setReassignCommand] = useState({ robotId: '', targetX: 0, targetY: 0 });
  const [fireLoc, setFireLoc] = useState({ x: 0, y: 0 });

  const fetchData = async () => {
    try {
      const [robotRes, nodeRes] = await Promise.all([
        axios.get('http://localhost:8080/api/robots'),
        axios.get('http://localhost:8080/api/nodes')
      ]);
      if (Array.isArray(robotRes.data)) setRobots(robotRes.data);
      if (Array.isArray(nodeRes.data)) setNodes(nodeRes.data);
    } catch (error) {
      console.error("Error fetching data:", error);
    }
  };

  useEffect(() => {
    fetchData(); 
    const interval = setInterval(fetchData, 1000); 
    return () => clearInterval(interval); 
  }, []);

  const handleDeploy = async (e) => {
    e.preventDefault();
    const payload = {
      name: newRobot.name,
      currentX: BASE_LOC.x,
      currentY: BASE_LOC.y,
      targetX: parseInt(newRobot.targetX) || 0,
      targetY: parseInt(newRobot.targetY) || 0,
      jobQueue: "", // Start with empty queue
      batteryPercentage: parseInt(newRobot.battery) || 100,
      status: 'MOVING' 
    };

    try {
      await axios.post('http://localhost:8080/api/robots', payload);
      setNotifications(prev => [`🚀 Deployed ${payload.name}`, ...prev]);
      setNewRobot({ name: '', targetX: 0, targetY: 0, battery: 100 });
      fetchData();
    } catch (err) {
      setNotifications(prev => [`❌ Failed to deploy robot`, ...prev]);
    }
  };

  // Now acts as an "Add to Queue" button!
  const handleReassign = async (e) => {
    e.preventDefault();
    if (!reassignCommand.robotId) return alert("Select a robot first!");
    const payload = { targetX: parseInt(reassignCommand.targetX) || 0, targetY: parseInt(reassignCommand.targetY) || 0 };
    
    try {
      await axios.put(`http://localhost:8080/api/robots/${reassignCommand.robotId}/target`, payload);
      const rName = robots.find(r => r.id === parseInt(reassignCommand.robotId))?.name;
      setNotifications(prev => [`📋 Added job (${payload.targetX}, ${payload.targetY}) to ${rName}'s queue`, ...prev]);
      setReassignCommand({ robotId: '', targetX: 0, targetY: 0 });
      fetchData();
    } catch (err) {
      setNotifications(prev => [`❌ Failed to assign job`, ...prev]);
    }
  };

  const handleToggleFire = async (e) => {
    e.preventDefault();
    try {
      await axios.post('http://localhost:8080/api/fire', { x: parseInt(fireLoc.x), y: parseInt(fireLoc.y) });
      setNotifications(prev => [`🔥 Toggled fire at (${fireLoc.x}, ${fireLoc.y})`, ...prev]);
      fetchData();
    } catch (err) {
      setNotifications(prev => [`❌ Failed to toggle fire`, ...prev]);
    }
  };

  const handleDelete = async (id) => {
    await axios.delete(`http://localhost:8080/api/robots/${id}`);
    fetchData(); 
  };

  const renderGrid = () => {
    let cells = [];
    const activeFires = nodes.filter(n => n.fire || n.isFire); // Get all fires

    for (let y = 0; y < 10; y++) {
      for (let x = 0; x < 10; x++) {
        const isObstacle = (x === 2 || x === 5 || x === 8) && (y >= 1 && y <= 8);
        const isBase = (x === BASE_LOC.x && y === BASE_LOC.y);
        
        // Check if exact tile is fire
        const isFire = activeFires.some(f => (f.xCoord === x || f.xcoord === x) && (f.yCoord === y || f.ycoord === y));
        
        // NEW: Check if tile is in the 3x3 Danger Zone!
        const isDangerZone = !isFire && activeFires.some(f => {
            const fx = f.xCoord !== undefined ? f.xCoord : f.xcoord;
            const fy = f.yCoord !== undefined ? f.yCoord : f.ycoord;
            return Math.abs(fx - x) <= 1 && Math.abs(fy - y) <= 1;
        });

        const robotHere = robots.find(r => r.currentX === x && r.currentY === y);

        let cellClass = 'floor';
        if (isObstacle) cellClass = 'obstacle';
        if (isBase) cellClass = 'base-station';
        if (isFire) cellClass = 'fire';
        if (isDangerZone) cellClass = 'danger-zone'; // Apply new CSS class

        cells.push(
          <div key={`${x}-${y}`} className={`grid-cell ${cellClass}`}>
            <span className="coord-label">{x},{y}</span>
            {isBase && <span className="zone-label">BASE ⚡</span>}
            {isFire && <span className="fire-icon">🔥</span>}
            {isDangerZone && <span className="danger-icon">⚠️</span>}

            {robotHere && (
              <div className={`robot-marker ${robotHere.status?.toLowerCase() || 'idle'}`}>
                {robotHere.fireWarning && (
                  <div className="robot-warning-bubble">
                    {robotHere.fireWarning}
                  </div>
                )}
                <div className="robot-name">{robotHere.name}</div>
                <div className="robot-battery">🔋{robotHere.batteryPercentage}%</div>
              </div>
            )}
          </div>
        );
      }
    }
    return cells;
  };

  // Helper to format the queue string for the UI
  const formatQueue = (queueString) => {
    if (!queueString) return "Empty";
    const jobs = queueString.split(';').filter(j => j.trim() !== '');
    return jobs.length > 0 ? `[${jobs.map(j => `(${j})`).join(', ')}]` : "Empty";
  };

  return (
    <div className="dashboard-container">
      <header><h1>Smart Warehouse Path Planner</h1></header>
      <div className="main-content">
        <section className="map-section">
          <div className="warehouse-grid">{renderGrid()}</div>
        </section>

        <aside className="side-panels">
          
          <form className="control-card hazard" onSubmit={handleToggleFire} style={{borderColor: '#d63031'}}>
            <h3 style={{color: '#ff7675'}}>🔥 Report Hazard (Fire)</h3>
            <p className="subtext" style={{color: '#fab1a0'}}>Enter coordinates to start a fire, or to extinguish an existing one.</p>
            <div className="input-row">
              <label>Fire X: <input type="number" min="0" max="9" value={fireLoc.x} onChange={e => setFireLoc({...fireLoc, x: e.target.value})} required/></label>
              <label>Fire Y: <input type="number" min="0" max="9" value={fireLoc.y} onChange={e => setFireLoc({...fireLoc, y: e.target.value})} required/></label>
            </div>
            <button type="submit" style={{backgroundColor: '#d63031'}}>Toggle (Add / Remove) Fire</button>
          </form>

          <form className="control-card dispatch" onSubmit={handleDeploy}>
            <h3>🚀 Deploy New Robot</h3>
            <label>Name: <input type="text" value={newRobot.name} onChange={e => setNewRobot({...newRobot, name: e.target.value})} required/></label>
            <div className="input-row">
              <label>Target X: <input type="number" min="0" max="9" value={newRobot.targetX} onChange={e => setNewRobot({...newRobot, targetX: e.target.value})} required/></label>
              <label>Target Y: <input type="number" min="0" max="9" value={newRobot.targetY} onChange={e => setNewRobot({...newRobot, targetY: e.target.value})} required/></label>
            </div>
            <label>Starting Battery %: <input type="number" min="0" max="100" value={newRobot.battery} onChange={e => setNewRobot({...newRobot, battery: e.target.value})} required/></label>
            <button type="submit">Spawn & Dispatch</button>
          </form>

          <form className="control-card dispatch" onSubmit={handleReassign}>
            <h3>📍 Assign / Queue Job</h3>
            <p className="subtext">If robot is busy, this adds the job to its queue!</p>
            <label>Select Robot:
              <select value={reassignCommand.robotId} onChange={e => setReassignCommand({...reassignCommand, robotId: e.target.value})} required>
                <option value="">-- Choose a Robot --</option>
                {robots.map(r => <option key={r.id} value={r.id}>{r.name} (At: {r.currentX},{r.currentY})</option>)}
              </select>
            </label>
            <div className="input-row">
              <label>Target X: <input type="number" min="0" max="9" value={reassignCommand.targetX} onChange={e => setReassignCommand({...reassignCommand, targetX: e.target.value})} required/></label>
              <label>Target Y: <input type="number" min="0" max="9" value={reassignCommand.targetY} onChange={e => setReassignCommand({...reassignCommand, targetY: e.target.value})} required/></label>
            </div>
            <button type="submit">Add Job to Queue</button>
          </form>

          <div className="telemetry-panel">
            <h3>📊 Live Robot Telemetry</h3>
            <div className="telemetry-list">
              {robots.map((r, index) => (
                <div key={r.id || index} className="telemetry-card">
                  <div className="tel-header">
                    <div>
                      <strong>{r.name || 'Unnamed'}</strong>
                      <span className={`status-badge ${r.status?.toLowerCase() || 'idle'}`} style={{marginLeft: '8px'}}>{r.status}</span>
                    </div>
                    <button type="button" className="delete-btn" onClick={() => handleDelete(r.id)}>✖</button>
                  </div>
                  <div className="tel-stats">
                    <span>📍 Loc: ({r.currentX ?? 0}, {r.currentY ?? 0}) | 🎯 Dest: ({r.targetX ?? 0}, {r.targetY ?? 0})</span>
                    <span style={{color: '#81ecec'}}>📋 Queue: {formatQueue(r.jobQueue)}</span>
                    {r.fireWarning && <span style={{color: '#ff7675', fontWeight: 'bold'}}>{r.fireWarning}</span>}
                  </div>
                </div>
              ))}
            </div>
          </div>
        </aside>
      </div>
    </div>
  );
}