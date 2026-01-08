import { useEffect, useState, useRef } from 'react';
import { Client } from '@stomp/stompjs'; // ÚJ IMPORT!
import './App.css';

function App() {
  const [screening, setScreening] = useState(null);
  const [loading, setLoading] = useState(true);
  const stompClientRef = useRef(null);

  useEffect(() => {
    fetch('http://localhost:8080/api/screenings/1')
      .then(response => response.json())
      .then(data => {
        setScreening(data);
        setLoading(false);
        setupWebSocket();
      })
      .catch(error => {
        console.error("Hiba:", error);
        setLoading(false);
      });

    return () => {
      if (stompClientRef.current) {
        stompClientRef.current.deactivate();
      }
    };
  }, []);

  const setupWebSocket = () => {
    const client = new Client({
      brokerURL: 'ws://localhost:8080/ws/websocket',
      
      onConnect: () => {
        console.log('--- WebSocket Connected ---');

        // Feliratkozás
        client.subscribe('/topic/seat-updates', (message) => {
          const event = JSON.parse(message.body);
          handleRealTimeUpdate(event);
        });
      },
      onStompError: (frame) => {
        console.error('Broker error: ' + frame.headers['message']);
      },
    });

    client.activate();
    stompClientRef.current = client;
  };

  const handleRealTimeUpdate = (event) => {
    console.log("Update jött:", event.seatId, event.status);
    setScreening((prev) => {
      if (!prev) return prev;
      const updatedSeats = prev.seats.map((seat) => {
        if (seat.id === event.seatId) {
            return { ...seat, status: event.status };
        }
        return seat;
      });
      return { ...prev, seats: updatedSeats };
    });
  };

  const handleSeatClick = (seat) => {
    if (seat.status === 'RESERVED' || seat.status === 'LOCKED') return;

    if (stompClientRef.current && stompClientRef.current.connected) {
        const payload = {
            screeningId: screening.id,
            seatId: seat.id,
            status: 'LOCKED'
        };
        
        stompClientRef.current.publish({
            destination: "/app/lock-seat",
            body: JSON.stringify(payload)
        });
    }
  };

  if (loading) return <div className="container">Loading...</div>;
  if (!screening) return <div className="container">Error in loading the data.</div>;

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
            style={{ gridRow: seat.rowNum, gridColumn: seat.seatNum }}
            onClick={() => handleSeatClick(seat)}
            title={`Sor: ${seat.rowNum}, Szék: ${seat.seatNum}`}
          >
            {seat.seatNum}
          </div>
        ))}
      </div>
      <div className="legend">
        <div className="legend-item"><div className="legend-box" style={{background: '#444'}}></div> Szabad</div>
        <div className="legend-item"><div className="legend-box" style={{background: '#d32f2f'}}></div> Eladva</div>
        <div className="legend-item"><div className="legend-box" style={{background: '#fbc02d'}}></div> Zárolt</div>
      </div>
    </div>
  );
}

export default App;