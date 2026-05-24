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

The backend does not keep secrets in `application.properties`. For local development, create `backend/.env.properties` from `backend/.env.example` and fill in your real values. That file is ignored by git. For Render, paste the same values into the service Environment tab as `DATABASE_URL`, `DATABASE_USERNAME`, and `DATABASE_PASSWORD`.

## Frontend

Location: [frontend](/c:/Users/manoj_tiabzvj/OneDrive/Desktop/practice/web/f_s-class/project2-2/frontend)

Install and run:

```bash
npm install
npm run dev
```

The frontend uses `VITE_API_BASE_URL` for API calls and currently points to `https://depttix.onrender.com/api`. Set the backend `APP_CORS_ALLOWED_ORIGINS` to your deployed frontend origin, for example `https://your-frontend.vercel.app`.

## Admin analytics

The admin dashboard includes:

- Total bookings per event
- Ticket distribution
- Remaining vs booked tickets
- Bookings over time

Charts refresh on a 15-second interval for near real-time updates.

## Credentials to explore the admin panel

mail-manojcherukuri202@gmail.com
pass-12345678
