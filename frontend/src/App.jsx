import { useEffect, useState, useRef } from 'react';
import SockJS from 'sockjs-client';
import Stomp from 'stompjs';
import './App.css';

function App() {
  const [screening, setScreening] = useState(null);
  const [loading, setLoading] = useState(true);
  
  // We keep the stompClient in a ref to persist it without re-rendering
  const stompClientRef = useRef(null);

  // 1. Initial Load: Get the static map (Snapshot)
  useEffect(() => {
    fetch('http://localhost:8080/api/screenings/1')
      .then(response => response.json())
      .then(data => {
        setScreening(data);
        setLoading(false);
        // After loading data, connect to WebSocket
        connectToWebSocket();
      })
      .catch(error => {
        console.error("Error loading screening:", error);
        setLoading(false);
      });

    // Cleanup on unmount (disconnect)
    return () => {
      if (stompClientRef.current) {
        stompClientRef.current.disconnect();
      }
    };
  }, []);

  // 2. WebSocket Connection Logic
  const connectToWebSocket = () => {
    // Connect to the endpoint we defined in Java (WebSocketConfig)
    const socket = new SockJS('http://localhost:8080/ws');
    const client = Stomp.over(socket);
    
    // Disable debug logs in console (optional, keeps console clean)
    client.debug = null; 

    client.connect({}, () => {
      console.log('--- Connected to WebSocket ---');

      // SUBSCRIBE to updates (Listen to the broadcast)
      client.subscribe('/topic/seat-updates', (message) => {
        const event = JSON.parse(message.body);
        handleRealTimeUpdate(event);
      });
    });

    stompClientRef.current = client;
  };

  // 3. Handle incoming real-time messages
  const handleRealTimeUpdate = (event) => {
    console.log("Update received for seat:", event.seatId);

    setScreening((prevScreening) => {
      if (!prevScreening) return prevScreening;

      // Find the seat in the list and update its status
      const updatedSeats = prevScreening.seats.map((seat) => {
        if (seat.id === event.seatId) {
            // Return a NEW object with the new status
            return { ...seat, status: event.status };
        }
        return seat;
      });

      return { ...prevScreening, seats: updatedSeats };
    });
  };

  // 4. User Interaction: Click on a seat
  const handleSeatClick = (seat) => {
    // If already reserved/locked, do nothing
    if (seat.status === 'RESERVED' || seat.status === 'LOCKED') {
        return;
    }

    // Send a message to the Backend (Controller)
    if (stompClientRef.current) {
        const payload = {
            screeningId: screening.id,
            seatId: seat.id,
            status: 'LOCKED' // We want to lock it
        };
        // Send to "/app/lock-seat" (as defined in WebSocketController)
        stompClientRef.current.send("/app/lock-seat", {}, JSON.stringify(payload));
    }
  };

  if (loading) return <div className="container">Loading cinema...</div>;
  if (!screening) return <div className="container">Error loading data.</div>;

  return (
    <div className="container">
      <h1>{screening.movieTitle}</h1>
      <p>{screening.startTime} | {screening.roomName}</p>

      <div className="screen"></div>

      <div className="cinema-room">
        {screening.seats.map((seat) => (
          <div
            key={seat.id}
            className={`seat ${seat.status}`}
            style={{
              gridRow: seat.rowNum,
              gridColumn: seat.seatNum
            }}
            onClick={() => handleSeatClick(seat)} // Click event added!
            title={`Row: ${seat.rowNum}, Seat: ${seat.seatNum}`}
          >
            {seat.seatNum}
          </div>
        ))}
      </div>

      <div className="legend">
        <div className="legend-item"><div className="legend-box" style={{background: '#444'}}></div> Free</div>
        <div className="legend-item"><div className="legend-box" style={{background: '#d32f2f'}}></div> Sold</div>
        <div className="legend-item"><div className="legend-box" style={{background: '#fbc02d'}}></div> Locked</div>
      </div>
    </div>
  );
}

export default App;