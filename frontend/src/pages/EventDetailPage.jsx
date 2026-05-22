import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { getDepartmentLabel } from "../constants/departments";
import api from "../services/api";

export default function EventDetailPage() {
  const { id } = useParams();
  const [event, setEvent] = useState(null);
  const [error, setError] = useState("");

  useEffect(() => {
    api.get(`/events/${id}`)
      .then(({ data }) => setEvent(data))
      .catch((err) => setError(err.response?.data?.message || "Unable to load event."));
  }, [id]);

  if (error) {
    return <p className="error-text">{error}</p>;
  }

  if (!event) {
    return <p>Loading event details...</p>;
  }

  const completed = new Date(event.dateTime).getTime() < Date.now();

  return (
    <section className="detail-panel brutal-card">
      <p className="eyebrow">{getDepartmentLabel(event.department)}</p>
      <h1>{event.name}</h1>
      <div className="detail-grid">
        <span>Date: {new Date(event.dateTime).toLocaleString()}</span>
        <span>Venue: {event.venue}</span>
        <span>Base price: Rs. {event.ticketPrice}</span>
        <span>Current price: Rs. {event.currentTicketPrice}</span>
        <span>Available: {event.availableTickets}</span>
        <span>Held right now: {event.heldSeats}</span>
        <span>Sold seats: {event.soldTickets}</span>
      </div>
      <div className="summary-box">
        <span>Pricing multiplier: {event.pricingMultiplier}x</span>
        <span>Dynamic pricing updates when demand rises and in the final 48 hours.</span>
      </div>
      <p>This Vel Tech booking experience is limited to authenticated student and faculty users only.</p>
      <div className="card-actions">
        {completed ? (
          <span className="role-chip completed-chip">Completed</span>
        ) : (
          <Link className="brutal-button" to={`/events/${event.id}/book`}>Continue to Booking</Link>
        )}
      </div>
    </section>
  );
}
