import { useEffect, useState } from "react";
import EventCard from "../components/EventCard";
import { useAuth } from "../auth/AuthProvider";
import api from "../services/api";

export default function EventListPage() {
  const { appUser } = useAuth();
  const [events, setEvents] = useState([]);
  const [error, setError] = useState("");

  useEffect(() => {
    api.get("/events")
      .then(({ data }) => setEvents(data))
      .catch((err) => setError(err.response?.data?.message || "Unable to load events."));
  }, []);

  return (
    <section>
      <div className="section-banner">
        <div>
          <p className="eyebrow">Vel Tech Live Events</p>
          <h1>Browse campus happenings</h1>
        </div>
        <div className="stats-panel">
          <span>{events.length} events</span>
          <span>{appUser.role === "USER" ? "School-filtered access" : "Admin review mode"}</span>
        </div>
      </div>
      {error ? <p className="error-text">{error}</p> : null}
      <div className="event-grid">
        {events.map((event) => (
          <EventCard key={event.id} event={event} canBook={appUser.role === "USER"} />
        ))}
      </div>
    </section>
  );
}
