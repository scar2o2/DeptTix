import { useEffect, useState } from "react";
import { useLocation } from "react-router-dom";
import { useToast } from "../context/ToastContext";
import api from "../services/api";

export default function MyBookingsPage() {
  const location = useLocation();
  const { showToast } = useToast();
  const [bookings, setBookings] = useState([]);
  const [error, setError] = useState("");

  const loadBookings = () => {
    api.get("/bookings/me")
      .then(({ data }) => setBookings(data))
      .catch((err) => {
        const message = err.response?.data?.message || "Unable to load bookings.";
        setError(message);
        showToast({ title: "Could not load bookings", message, variant: "error" });
      });
  };

  useEffect(() => {
    if (location.state?.message) {
      showToast({ title: "Booking updated", message: location.state.message, variant: "success" });
    }
  }, [location.state, showToast]);

  useEffect(() => {
    loadBookings();
  }, [showToast]);

  const cancelBooking = async (bookingId) => {
    try {
      await api.patch(`/bookings/${bookingId}/cancel`);
      showToast({ title: "Booking cancelled", message: "Your seats have been released.", variant: "info" });
      loadBookings();
    } catch (err) {
      const message = err.response?.data?.message || "Unable to cancel booking.";
      showToast({ title: "Cancellation failed", message, variant: "error" });
    }
  };

  return (
    <section>
      <div className="section-banner">
        <div>
          <p className="eyebrow">Booking Summary</p>
          <h1>My bookings</h1>
        </div>
      </div>
      {error ? <p className="error-text">{error}</p> : null}
      <div className="list-stack">
        {bookings.map((booking) => (
          <article key={booking.id} className="brutal-card list-item">
            <div>
              <h3>{booking.eventName}</h3>
              <div className="meta-grid">
                <span>Tickets: {booking.tickets}</span>
                <span>Seat(s): {booking.seatNumbers.join(", ") || "Auto-assigned"}</span>
                <span>Price per ticket: Rs. {booking.pricePerTicket}</span>
                <span>Total: Rs. {booking.totalAmount}</span>
                <span>Status: {booking.status}</span>
                <span>Booked: {new Date(booking.bookingDate).toLocaleString()}</span>
              </div>
              <div className="ticket-holder-summary">
                <strong>Ticket holder details</strong>
                {booking.ticketHolders?.length ? (
                  <div className="ticket-holder-booking-list">
                    {booking.ticketHolders.map((holder, index) => (
                      <span key={`${booking.id}-${holder.name}-${index}`}>
                        {index + 1}. {holder.name} | Age: {holder.age} | Gender: {holder.gender}
                      </span>
                    ))}
                  </div>
                ) : <p className="muted-text">No ticket holder details available.</p>}
              </div>
            </div>
            {booking.status === "CONFIRMED" ? (
              <button type="button" className="brutal-button danger" onClick={() => cancelBooking(booking.id)}>
                Cancel Booking
              </button>
            ) : null}
          </article>
        ))}
      </div>
    </section>
  );
}
