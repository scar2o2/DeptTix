import { useEffect, useState } from "react";
import EventCard from "../components/EventCard";
import { useAuth } from "../auth/AuthProvider";
import api from "../services/api";

function isCompletedEvent(event) {
  return new Date(event.dateTime).getTime() < Date.now();
}

export default function EventListPage() {
  const { appUser } = useAuth();
  const [events, setEvents] = useState([]);
  const [error, setError] = useState("");

  useEffect(() => {
    api.get("/events")
      .then(({ data }) => setEvents(data))
      .catch((err) => setError(err.response?.data?.message || "Unable to load events."));
  }, []);

  const liveEvents = events.filter((event) => !isCompletedEvent(event));
  const completedEvents = events.filter(isCompletedEvent);
  const canBookLiveEvents = appUser.role === "USER";

  return (
    <section>
      <div className="section-banner">
        <div>
          <p className="eyebrow">Vel Tech Live Events</p>
          <h1>Browse campus happenings</h1>
        </div>
        <div className="stats-panel">
          <span>{liveEvents.length} live events</span>
          <span>{completedEvents.length} completed events</span>
          <span>{appUser.role === "USER" ? "School-filtered access" : "Admin review mode"}</span>
        </div>
      </div>
      {error ? <p className="error-text">{error}</p> : null}
      <div className="event-section">
        <div className="section-heading">
          <h2>Upcoming Events</h2>
          <span>{liveEvents.length}</span>
        </div>
        <div className="event-grid">
          {liveEvents.length ? liveEvents.map((event) => (
            <EventCard key={event.id} event={event} canBook={canBookLiveEvents} />
          )) : <p className="muted-text">No upcoming events right now.</p>}
        </div>
      </div>
      <div className="event-section">
        <div className="section-heading">
          <h2>Completed Events</h2>
          <span>{completedEvents.length}</span>
        </div>
        <div className="event-grid">
          {completedEvents.length ? completedEvents.map((event) => (
            <EventCard key={event.id} event={event} canBook={false} completed />
          )) : <p className="muted-text">Finished events will appear here.</p>}
        </div>
      </div>
    </section>
  );
}
