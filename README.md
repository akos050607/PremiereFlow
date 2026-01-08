# 🎬 PremiereFlow - Real-Time Cinema Booking System

**PremiereFlow** is a full-stack, real-time cinema seat reservation application designed to handle concurrent user interactions seamlessly. It features live seat updates, user presence tracking ("ghost" cursors), and a robust locking mechanism to prevent double-bookings.

![Project Status](https://img.shields.io/badge/status-active-success.svg)
![Tech Stack](https://img.shields.io/badge/stack-Spring%20Boot%20%7C%20React-blue)

---
## DEMO
[Screencast from 2026-01-08 14-18-05.webm](https://github.com/user-attachments/assets/06c47c2f-e7e8-48ab-881f-767f5e3cfd24)

<img height="500" alt="Screenshot_20260108-142242 (1)" src="https://github.com/user-attachments/assets/2f7415b8-96ae-4c44-ae1f-e04770cc5b64" />


## Features

* **Real-Time Interactivity:** Uses WebSockets to synchronize state instantly across all connected clients.
* **Live User Presence:** See where other users are hovering their mouse (displayed as an 👁️ icon) in real-time.
* **Concurrency Protection:**
    * **Locking:** Clicking a seat locks it for the user (Green) and shows it as locked (Yellow) to others.
    * **Conflict Resolution:** Prevents two users from booking the same seat.
* **Smart Disconnect Handling:** Automatically releases locked seats if a user closes the browser or loses connection.
* **Ticket Purchase:** Simulates a transaction and generates a QR code for the booked seats.

---

## Tech Stack

### Backend
* **Java 21**
* **Spring Boot**
* **Spring WebSocket**
* **H2 Database** (for development simplicity)

### Frontend
* **React (Vite)**
* **JavaScript**
* **@stomp/stompjs** (WebSocket client)
* **CSS3** (Custom animations & responsive design)

---

## Architecture & Development Workflow

### Dual IDE Environment

* **Backend:** Developed in **IntelliJ IDEA** for its superior Java/Spring Boot support.
* **Frontend:** Developed in **VS Code** for its lightweight and efficient React ecosystem extensions.
* **Challenge:** Managing a monorepo structure while switching contexts between two IDEs.
* **Solution:** Strict separation of concerns in the directory structure (`/backend` vs `/frontend`) and a unified Git workflow handled via terminal.

### WebSocket Communication Flow
The app uses an **Event-Driven Architecture** instead of traditional HTTP polling.

1.  **Connection:** Client connects to `ws://localhost:8080/ws/websocket`.
2.  **Topics:**
    * `/topic/seat-updates`: Handles critical state changes (Lock, Unlock, Buy).
    * `/topic/seat-hover`: Handles high-frequency, transient data (Mouse movement).
3.  **Events:** When User A acts, a JSON payload is sent to the server, processed (saved to DB if needed), and broadcasted to all subscribed clients.

---

## Key Challenges & Solutions

### 1. The "Yellow Seat" Concurrency Bug
**The Problem:**
In the initial implementation, when a user successfully purchased tickets, the frontend triggered a full data reload (`loadData()`) from the REST API.
* *Scenario:* User A buys tickets. User B is selecting seats (Green) in another window.
* *Conflict:* User A's purchase forced User B's browser to reload data. Since the server only knew User B's seats were "LOCKED" (but not strictly *who* owned the lock in the REST response context), User B's local state was overwritten. Their selected seats turned **Yellow** (Locked by "someone"), preventing them from finishing the purchase.

**The Solution: Granular State Updates**
Instead of a "Pull" strategy (reloading everything), I implemented a "Push" strategy using specific WebSocket events.
* **Fix:** When a purchase occurs, the server broadcasts specific messages for *only* the sold seats (e.g., `{seatId: 5, status: 'RESERVED'}`).
* **Result:** The frontend updates only the specific seats that changed status. User B's local selection remains intact, allowing for a smooth, interruption-free experience even during high traffic.

### 2. Orphaned Locks
**The Problem:**
If a user selected seats and then closed the browser without buying, the seats remained "Locked" (Yellow) for everyone else indefinitely.
**The Solution:**
Implemented a `SessionDisconnectEvent` listener in Spring Boot. The server maps WebSocket Session IDs to User IDs. On disconnect, it identifies the user's locked seats, releases them in the database, and broadcasts a `FREE` event to all clients immediately.
