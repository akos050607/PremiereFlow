import { useEffect, useState, useRef } from 'react';
import { Client } from '@stomp/stompjs';
import './App.css';

const generateUserId = () => 'user-' + Math.random().toString(36).substr(2, 9);

function App() {
  const [screening, setScreening] = useState(null);
  const [loading, setLoading] = useState(true);
  
  const [myUserId] = useState(generateUserId());
  const [activeHovers, setActiveHovers] = useState({});

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
      if (stompClientRef.current) stompClientRef.current.deactivate();
    };
  }, []);

  const setupWebSocket = () => {
    const client = new Client({
      brokerURL: 'ws://localhost:8080/ws/websocket',
      onConnect: () => {
        console.log('--- WebSocket Connected as ' + myUserId + ' ---');

        client.subscribe('/topic/seat-updates', (message) => {
          const event = JSON.parse(message.body);
          handleRealTimeUpdate(event);
        });

        client.subscribe('/topic/seat-hover', (message) => {
            const event = JSON.parse(message.body);
            handleHoverUpdate(event);
        });
      },
    });
    client.activate();
    stompClientRef.current = client;
  };

  const handleRealTimeUpdate = (event) => {
    setScreening((prev) => {
      if (!prev) return prev;
      const updatedSeats = prev.seats.map((seat) => {
        if (seat.id === event.seatId) {
            return { ...seat, status: event.status, lockedBy: event.userId };
        }
        return seat;
      });
      return { ...prev, seats: updatedSeats };
    });
  };

  const handleHoverUpdate = (event) => {
    setActiveHovers(prev => {
        const newHovers = { ...prev };
        if (event.status === 'ENTER') {
            newHovers[event.seatId] = event.userId;
        } else {
            delete newHovers[event.seatId];
        }
        return newHovers;
    });
  };

  const sendHoverEvent = (seatId, type) => {
    if (stompClientRef.current?.connected) {
        stompClientRef.current.publish({
            destination: "/app/hover-seat",
            body: JSON.stringify({
                seatId: seatId,
                userId: myUserId,
                status: type
            })
        });
    }
  };

  const handleSeatClick = (seat) => {
    if (seat.status === 'RESERVED') return;

    if (seat.status === 'LOCKED' && seat.lockedBy !== myUserId) {
        alert("Ezt a széket valaki más épp foglalja!");
        return;
    }

    if (stompClientRef.current?.connected) {
        const payload = {
            screeningId: screening.id,
            seatId: seat.id,
            userId: myUserId,
            status: 'LOCKED'
        };
        
        stompClientRef.current.publish({
            destination: "/app/lock-seat",
            body: JSON.stringify(payload)
        });
    }
  };

  if (loading) return <div className="container">Loading...</div>;

  return (
    <div className="container">
      <h1>{screening.movieTitle}</h1>
      <div className="screen"></div>
      <div className="cinema-room">
        {screening.seats.map((seat) => {
            const isBeingWatched = activeHovers[seat.id] && activeHovers[seat.id] !== myUserId;
            
            const isMyLock = seat.status === 'LOCKED' && seat.lockedBy === myUserId;

            return (
                <div
                    key={seat.id}
                    className={`seat ${seat.status} ${isMyLock ? 'MY-LOCK' : ''} ${isBeingWatched ? 'WATCHED' : ''}`}
                    style={{ gridRow: seat.rowNum, gridColumn: seat.seatNum }}
                    onClick={() => handleSeatClick(seat)}
                    onMouseEnter={() => sendHoverEvent(seat.id, 'ENTER')}
                    onMouseLeave={() => sendHoverEvent(seat.id, 'LEAVE')}
                >
                    {seat.seatNum}
                </div>
            );
        })}
      </div>
    </div>
  );
}

export default App;