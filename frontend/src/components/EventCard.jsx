import { Link } from "react-router-dom";
import { getDepartmentLabel } from "../constants/departments";

export default function EventCard({ event, canBook }) {
  return (
    <article className="brutal-card">
      <p className="eyebrow">{getDepartmentLabel(event.department)}</p>
      <h3>{event.name}</h3>
      <div className="meta-grid">
        <span>{new Date(event.dateTime).toLocaleString()}</span>
        <span>{event.venue}</span>
        <span>Base: Rs. {event.ticketPrice}</span>
        <span>Live price: Rs. {event.currentTicketPrice}</span>
        <span>{event.availableTickets} tickets left</span>
        <span>{event.heldSeats} seats on hold</span>
      </div>
      <div className="card-actions">
        <Link className="brutal-button secondary" to={`/events/${event.id}`}>
          Details
        </Link>
        {canBook ? (
          <Link className="brutal-button" to={`/events/${event.id}/book`}>
            Book Ticket
          </Link>
        ) : (
          <span className="role-chip">Admin view only</span>
        )}
      </div>
    </article>
  );
}
