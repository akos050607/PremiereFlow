import { useEffect, useState } from 'react';
import './App.css';

function App() {
  const [screening, setScreening] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetch('http://localhost:8080/api/screenings/1') // For example: fetching screening with ID 1
      .then(response => response.json())
      .then(data => {
        setScreening(data);
        setLoading(false);
      })
      .catch(error => {
        console.error("Hiba a backend elérésében:", error);
        setLoading(false);
      });
  }, []);

  if (loading) {
    return <div className="container">Loading...</div>;
  }

  if (!screening) {
    return <div className="container">Couldn't load the screening.</div>;
  }

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
            title={`Sor: ${seat.rowNum}, Szék: ${seat.seatNum}, Ár: ${seat.price} Ft`}
          >
            {seat.seatNum}
          </div>
        ))}
      </div>

      <div className="legend">
        <div className="legend-item">
          <div className="legend-box" style={{background: '#444'}}></div> Free
        </div>
        <div className="legend-item">
          <div className="legend-box" style={{background: '#d32f2f'}}></div> Not free
        </div>
        <div className="legend-item">
          <div className="legend-box" style={{background: '#fbc02d'}}></div> Reserved
        </div>
      </div>
    </div>
  );
}

export default App;