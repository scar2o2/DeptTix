# Internal Department Event Ticket Booking System

Full-stack ticket booking platform with:

- React frontend using a Neo-Brutalism UI
- Spring Boot REST API
- JWT authentication
- Role-based access for `USER` and `ADMIN`
- Admin-only analytics dashboard with charts

## Core behavior

- The application lands on the login page first.
- No frontend route is accessible without authentication.
- `USER` accounts can browse events, book tickets, and view booking history.
- `ADMIN` accounts can manage events and view analytics.
- Admins cannot access the booking flow or booking API.

## Backend

Location: [backend/src/main/java/com/department/ticketsystem](/c:/Users/manoj_tiabzvj/OneDrive/Desktop/practice/web/f_s-class/project2-2/backend/src/main/java/com/department/ticketsystem)

Run with Maven:

```bash
cd backend
mvn spring-boot:run
```

Default demo accounts created on startup:

- Admin: `admin@department.edu` / `Admin@123`
- User: `student@department.edu` / `Student@123`

The backend is configured with in-memory H2 by default in [application.properties](/c:/Users/manoj_tiabzvj/OneDrive/Desktop/practice/web/f_s-class/project2-2/backend/src/main/resources/application.properties:1). You can switch to MySQL by replacing the datasource settings.

## Frontend

Location: [frontend](/c:/Users/manoj_tiabzvj/OneDrive/Desktop/practice/web/f_s-class/project2-2/frontend)

Install and run:

```bash
npm install
npm run dev
```

The frontend expects the backend at `http://localhost:8081`.

## Admin analytics

The admin dashboard includes:

- Total bookings per event
- Ticket distribution
- Remaining vs booked tickets
- Bookings over time

Charts refresh on a 15-second interval for near real-time updates.
