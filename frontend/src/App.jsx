import { useEffect, useState, useRef } from 'react';
import { Client } from '@stomp/stompjs';
import './App.css';

// Generate a unique ID for the browser session
const generateUserId = () => 'user-' + Math.random().toString(36).substr(2, 9);

function App() {
  const [screening, setScreening] = useState(null);
  const [loading, setLoading] = useState(true);
  
  // User's own ID
  const [myUserId] = useState(generateUserId());
  
  // Track other users' mouse movements: { seatId: userId }
  const [activeHovers, setActiveHovers] = useState({});
  
  // Purchase and QR code states
  const [showQrModal, setShowQrModal] = useState(false);
  const [purchasedSeats, setPurchasedSeats] = useState([]);

  const stompClientRef = useRef(null);

  useEffect(() => {
    loadData();
    return () => {
      if (stompClientRef.current) stompClientRef.current.deactivate();
    };
  }, []);

  const loadData = () => {
    fetch('http://localhost:8080/api/screenings/1')
      .then(response => response.json())
      .then(data => {
        setScreening(data);
        setLoading(false);
        // Only connect if not already connected
        if (!stompClientRef.current) setupWebSocket();
      })
      .catch(error => console.error("Error loading data:", error));
  };

  const setupWebSocket = () => {
    const client = new Client({
      brokerURL: 'ws://localhost:8080/ws/websocket',
      onConnect: () => {
        console.log('WebSocket Connected');
        
        // Channel 1: Booking and status updates
        client.subscribe('/topic/seat-updates', (message) => {
          const event = JSON.parse(message.body);
          handleRealTimeUpdate(event);
        });

        // Channel 2: Hover (mouse) events
        client.subscribe('/topic/seat-hover', (message) => {
            const event = JSON.parse(message.body);
            handleHoverUpdate(event);
        });
      },
    });
    client.activate();
    stompClientRef.current = client;
  };

  // --- EVENT HANDLERS ---

  const handleRealTimeUpdate = (event) => {
    if (event.status === 'RESET_ALL') {
        loadData(); 
        setPurchasedSeats([]);
        setShowQrModal(false);
        setActiveHovers({});
        return;
    }

    if (event.status === 'PURCHASE_SUCCESS') {
        if (event.userId === myUserId) {
             setScreening(prev => {
                 const mySeats = prev.seats
                    .filter(s => s.lockedBy === myUserId)
                    .map(s => `${s.rowNum}/${s.seatNum}`);
                 
                 setPurchasedSeats(mySeats);
                 setShowQrModal(true);
                 return prev;
             });
        }
        return;
    }

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

    // PROTECTION: If locked, and NOT by me
    if (seat.status === 'LOCKED' && seat.lockedBy !== myUserId) {
        alert("This seat is currently selected by someone else!");
        return;
    }

    // Send Lock or Unlock request
    if (stompClientRef.current?.connected) {
        const payload = {
            screeningId: screening?.id,
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

  // Reset Button Handler
  const handleResetAll = () => {
    if (stompClientRef.current?.connected) {
        if(window.confirm("Are you sure you want to reset all bookings?")) {
            stompClientRef.current.publish({
                destination: "/app/reset",
                body: JSON.stringify({ screeningId: screening.id })
            });
        }
    }
  };

  // Buy Button Handler
  const handleBuyTickets = () => {
    const mySeats = screening.seats.filter(s => s.status === 'LOCKED' && s.lockedBy === myUserId);
    if (mySeats.length === 0) {
        alert("No seats selected!");
        return;
    }

    if (stompClientRef.current?.connected) {
        stompClientRef.current.publish({
            destination: "/app/buy",
            body: JSON.stringify({ screeningId: screening.id, userId: myUserId })
        });
    }
  };

  if (loading) return <div className="container">Loading...</div>;

  // Count my selected seats
  const mySelectionCount = screening?.seats 
    ? screening.seats.filter(s => s.status === 'LOCKED' && s.lockedBy === myUserId).length
    : 0;

  return (
    <div className="container">
      <h1>{screening?.movieTitle || "Cinema"}</h1>
      
      {/* --- CONTROL BUTTONS --- */}
      <div className="controls">
          <button className="reset-btn" onClick={handleResetAll}>
            ⚠️ Reset Demo
          </button>
          
          {mySelectionCount > 0 && (
              <button className="buy-btn" onClick={handleBuyTickets}>
                  Buy Tickets ({mySelectionCount})
              </button>
          )}
      </div>

      <div className="screen"></div>
      
      {/* --- THEATER ROOM --- */}
      <div className="cinema-room">
        {screening?.seats.map((seat) => {
           // Is someone else watching this seat?
           const isBeingWatched = activeHovers[seat.id] && activeHovers[seat.id] !== myUserId;
           // Is this seat locked by me?
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

      {/* --- LEGEND --- */}
      <div className="legend">
        <div className="legend-item"><div className="seat" style={{width:20, height:20}}></div> Free</div>
        <div className="legend-item"><div className="seat LOCKED MY-LOCK" style={{width:20, height:20}}></div> Your Selection</div>
        <div className="legend-item"><div className="seat LOCKED" style={{width:20, height:20, backgroundColor: '#fbc02d'}}></div> Locked</div>
        <div className="legend-item"><div className="seat RESERVED" style={{width:20, height:20}}></div> Sold</div>
      </div>

      {/* --- QR CODE MODAL --- */}
      {showQrModal && (
          <div className="modal-overlay">
              <div className="modal-content">
                  <h2>Purchase Successful!</h2>
                  <p>Your tickets are ready:</p>
                  
                  <img 
                    src={`https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=TICKET:${screening.movieTitle}:${purchasedSeats.join(',')}`} 
                    alt="Ticket QR Code" 
                  />
                  
                  <p><strong>Seats:</strong> {purchasedSeats.join(', ')}</p>
                  <button onClick={() => setShowQrModal(false)}>Close</button>
              </div>
          </div>
      )}
    </div>
  );
}

export default App;