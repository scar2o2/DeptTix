import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import Modal from "../components/Modal";
import { useToast } from "../context/ToastContext";
import api from "../services/api";

const MAX_TICKETS_PER_BOOKING = 8;

const createEmptyHolder = () => ({
  name: "",
  age: "",
  gender: ""
});

const compareSeatNumbers = (left, right) => {
  const leftMatch = /^([A-Za-z]+)(\d+)$/.exec(left?.seatNumber?.trim() || "");
  const rightMatch = /^([A-Za-z]+)(\d+)$/.exec(right?.seatNumber?.trim() || "");
  if (leftMatch && rightMatch) {
    const rowCompare = leftMatch[1].localeCompare(rightMatch[1], undefined, { sensitivity: "base" });
    if (rowCompare !== 0) {
      return rowCompare;
    }
    return Number(leftMatch[2]) - Number(rightMatch[2]);
  }
  return (left?.seatNumber || "").localeCompare(right?.seatNumber || "", undefined, { sensitivity: "base" });
};

const sortSeats = (seatList) => [...seatList].sort(compareSeatNumbers);

export default function BookingPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { showToast } = useToast();
  const [event, setEvent] = useState(null);
  const [seats, setSeats] = useState([]);
  const [selectedSeatIds, setSelectedSeatIds] = useState([]);
  const [holdInfo, setHoldInfo] = useState(null);
  const [error, setError] = useState("");
  const [showConfirm, setShowConfirm] = useState(false);
  const [showImportModal, setShowImportModal] = useState(false);
  const [successModal, setSuccessModal] = useState({ open: false, message: "" });
  const [ticketHolders, setTicketHolders] = useState([]);
  const [savedPassengers, setSavedPassengers] = useState([]);
  const [selectedPassengerIds, setSelectedPassengerIds] = useState([]);
  const [passengersLoading, setPassengersLoading] = useState(true);
  const [savingPassengerIndex, setSavingPassengerIndex] = useState(null);
  const [deletingPassengerId, setDeletingPassengerId] = useState(null);

  useEffect(() => {
    Promise.all([
      api.get(`/events/${id}`),
      api.get(`/events/${id}/seats`)
    ])
      .then(([eventResponse, seatResponse]) => {
        setEvent(eventResponse.data);
        setSeats(sortSeats(seatResponse.data));
      })
      .catch((err) => {
        const message = err.response?.data?.message || "Unable to load event.";
        setError(message);
        showToast({ title: "Event unavailable", message, variant: "error" });
      });
  }, [id, showToast]);

  useEffect(() => {
    const loadSavedPassengers = async () => {
      setPassengersLoading(true);
      try {
        const { data } = await api.get("/users/me/passengers");
        setSavedPassengers(data);
      } catch (err) {
        const message = err.response?.data?.message || "Unable to load saved passenger details.";
        showToast({ title: "Saved passengers unavailable", message, variant: "error" });
      } finally {
        setPassengersLoading(false);
      }
    };

    loadSavedPassengers();
  }, [showToast]);

  useEffect(() => {
    setTicketHolders((current) => {
      if (selectedSeatIds.length === current.length) {
        return current;
      }
      return Array.from({ length: selectedSeatIds.length }, (_, index) => current[index] || createEmptyHolder());
    });
  }, [selectedSeatIds]);

  const toggleSeat = (seat) => {
    if (seat.status === "BOOKED" || (seat.status === "HELD" && !seat.heldByCurrentUser)) {
      return;
    }
    if (!selectedSeatIds.includes(seat.id) && selectedSeatIds.length >= MAX_TICKETS_PER_BOOKING) {
      const message = "You can book a maximum of 8 tickets for any event.";
      setError(message);
      showToast({ title: "Selection limit reached", message, variant: "error" });
      return;
    }
    setSelectedSeatIds((current) => (
      current.includes(seat.id)
        ? current.filter((seatId) => seatId !== seat.id)
        : [...current, seat.id]
    ));
  };

  const updateTicketHolder = (index, field, value) => {
    setTicketHolders((current) => current.map((holder, holderIndex) => (
      holderIndex === index ? { ...holder, [field]: value } : holder
    )));
  };

  const isHolderComplete = (holder) => (
    holder.name.trim() && holder.age && Number(holder.age) > 0 && holder.gender
  );

  const getUnfilledHolderIndexes = () => ticketHolders.reduce((indexes, holder, index) => {
    if (!isHolderComplete(holder)) {
      indexes.push(index);
    }
    return indexes;
  }, []);

  const toggleSavedPassengerSelection = (passengerId) => {
    const unfilledHolderIndexes = getUnfilledHolderIndexes();
    setSelectedPassengerIds((current) => {
      if (current.includes(passengerId)) {
        return current.filter((id) => id !== passengerId);
      }
      if (!unfilledHolderIndexes.length) {
        const message = "All seats are already filled. Clear a ticket holder first to import more details.";
        setError(message);
        showToast({ title: "All seats filled", message, variant: "info" });
        return current;
      }
      if (current.length >= unfilledHolderIndexes.length) {
        const message = "All unfilled seats are already covered by your selected saved profiles.";
        setError(message);
        showToast({ title: "All seats filled", message, variant: "info" });
        return current;
      }
      return [...current, passengerId];
    });
  };

  const importSelectedPassengers = () => {
    const unfilledHolderIndexes = getUnfilledHolderIndexes();
    const passengersToImport = savedPassengers.filter((passenger) => selectedPassengerIds.includes(passenger.id));
    if (!passengersToImport.length) {
      const message = "Select at least one saved passenger to import.";
      setError(message);
      showToast({ title: "No passengers selected", message, variant: "error" });
      return;
    }
    if (!unfilledHolderIndexes.length) {
      const message = "All selected seats already have complete details.";
      setError(message);
      showToast({ title: "Nothing to import", message, variant: "info" });
      return;
    }

    setTicketHolders((current) => {
      const nextHolders = [...current];
      unfilledHolderIndexes.forEach((holderIndex, index) => {
        const passenger = passengersToImport[index];
        if (!passenger) {
          return;
        }
        nextHolders[holderIndex] = {
          name: passenger.name,
          age: String(passenger.age),
          gender: passenger.gender
        };
      });
      return nextHolders;
    });
    setShowImportModal(false);
    setSelectedPassengerIds([]);
    showToast({
      title: "Details imported",
      message: `Loaded ${passengersToImport.length} saved passenger detail(s) into the booking form.`,
      variant: "success"
    });
  };

  const savePassenger = async (holder, index) => {
    if (!isHolderComplete(holder)) {
      const message = "Complete the passenger's name, age, and gender before saving.";
      setError(message);
      showToast({ title: "Passenger not saved", message, variant: "error" });
      return;
    }

    setSavingPassengerIndex(index);
    try {
      const payload = {
        name: holder.name.trim(),
        age: Number(holder.age),
        gender: holder.gender
      };
      const { data } = await api.post("/users/me/passengers", payload);
      setSavedPassengers((current) => [data, ...current]);
      showToast({
        title: "Passenger saved",
        message: `${payload.name} is now available from Import Details.`,
        variant: "success"
      });
    } catch (err) {
      const message = err.response?.data?.message || "Unable to save passenger details.";
      setError(message);
      showToast({ title: "Save failed", message, variant: "error" });
    } finally {
      setSavingPassengerIndex(null);
    }
  };

  const deletePassenger = async (passengerId) => {
    setDeletingPassengerId(passengerId);
    try {
      await api.delete(`/users/me/passengers/${passengerId}`);
      setSavedPassengers((current) => current.filter((passenger) => passenger.id !== passengerId));
      setSelectedPassengerIds((current) => current.filter((id) => id !== passengerId));
      showToast({ title: "Passenger removed", message: "Saved passenger details deleted.", variant: "info" });
    } catch (err) {
      const message = err.response?.data?.message || "Unable to delete passenger details.";
      setError(message);
      showToast({ title: "Delete failed", message, variant: "error" });
    } finally {
      setDeletingPassengerId(null);
    }
  };

  const validateTicketHolders = () => {
    if (!selectedSeatIds.length) {
      return "Select at least one seat to continue.";
    }
    if (selectedSeatIds.length > MAX_TICKETS_PER_BOOKING) {
      return "You can book a maximum of 8 tickets for any event.";
    }
    const hasIncompleteHolder = ticketHolders.some((holder) => (
      !holder.name.trim() || !holder.age || Number(holder.age) <= 0 || !holder.gender
    ));
    if (hasIncompleteHolder || ticketHolders.length !== selectedSeatIds.length) {
      return "Enter name, age, and gender for each ticket holder.";
    }
    return "";
  };

  const holdSeats = async () => {
    if (!selectedSeatIds.length) {
      setError("Select at least one seat to continue.");
      return;
    }
    try {
      const { data } = await api.post("/bookings/hold", { eventId: Number(id), seatIds: selectedSeatIds });
      setHoldInfo(data);
      showToast({
        title: "Seats held",
        message: `${data.seatNumbers.join(", ")} held until ${new Date(data.holdExpiresAt).toLocaleTimeString()}.`,
        variant: "info"
      });
      const { data: refreshedSeats } = await api.get(`/events/${id}/seats`);
      const { data: refreshedEvent } = await api.get(`/events/${id}`);
      setSeats(sortSeats(refreshedSeats));
      setEvent(refreshedEvent);
    } catch (err) {
      const message = err.response?.data?.message || "Unable to hold seats.";
      setError(message);
      showToast({ title: "Hold failed", message, variant: "error" });
    }
  };

  const confirmBooking = async () => {
    setError("");
    setShowConfirm(false);
    const validationMessage = validateTicketHolders();
    if (validationMessage) {
      setError(validationMessage);
      showToast({ title: "Booking details missing", message: validationMessage, variant: "error" });
      return;
    }
    try {
      const payload = {
        eventId: Number(id),
        seatIds: selectedSeatIds,
        ticketHolders: ticketHolders.map((holder) => ({
          name: holder.name.trim(),
          age: Number(holder.age),
          gender: holder.gender
        }))
      };
      const { data } = await api.post("/bookings", payload);
      setSuccessModal({
        open: true,
        message: `Booking confirmed for ${data.tickets} ticket(s). Seats: ${data.seatNumbers.join(", ")}. Total: Rs. ${data.totalAmount}`
      });
      showToast({
        title: "Tickets booked",
        message: `${data.eventName} is now in your booking history.`,
        variant: "success"
      });
      const { data: refreshedSeats } = await api.get(`/events/${id}/seats`);
      const { data: refreshedEvent } = await api.get(`/events/${id}`);
      setSeats(sortSeats(refreshedSeats));
      setEvent(refreshedEvent);
      setSelectedSeatIds([]);
      setHoldInfo(null);
      setTicketHolders([]);
    } catch (err) {
      const message = err.response?.data?.message || "Booking failed.";
      setError(message);
      showToast({ title: "Booking failed", message, variant: "error" });
    }
  };

  if (!event) {
    return <p>{error || "Loading booking form..."}</p>;
  }

  const remainingImportSlots = getUnfilledHolderIndexes().length;

  return (
    <section className="brutal-card booking-panel">
      <p className="eyebrow">Vel Tech Booking Only</p>
      <h1>{event.name}</h1>
      <div className="booking-layout">
        <div className="seat-selector brutal-card">
          <h2>Select seats</h2>
          <div className="seat-legend">
            <span><i className="seat-swatch available" /> Available</span>
            <span><i className="seat-swatch booked" /> Booked</span>
            <span><i className="seat-swatch held" /> Held</span>
            <span><i className="seat-swatch selected" /> Selected</span>
          </div>
          <div className="seat-grid">
            {seats.map((seat) => {
              const isSelected = selectedSeatIds.includes(seat.id);
              const className = [
                "seat-button",
                seat.status.toLowerCase(),
                isSelected ? "selected" : "",
                seat.heldByCurrentUser ? "mine" : ""
              ].join(" ").trim();
              return (
                <button
                  key={seat.id}
                  type="button"
                  className={className}
                  onClick={() => toggleSeat(seat)}
                >
                  {seat.seatNumber}
                </button>
              );
            })}
          </div>
        </div>
        <form
          onSubmit={(submitEvent) => {
            submitEvent.preventDefault();
            const validationMessage = validateTicketHolders();
            if (validationMessage) {
              setError(validationMessage);
              showToast({ title: "Booking details missing", message: validationMessage, variant: "error" });
              return;
            }
            setShowConfirm(true);
          }}
          className="booking-form"
        >
          <div className="summary-box">
            <span>Base price: Rs. {event.ticketPrice}</span>
            <span>Live price: Rs. {holdInfo?.pricePerTicket || event.currentTicketPrice}</span>
            <span>Remaining tickets: {event.availableTickets}</span>
            <span>Selected seats: {selectedSeatIds.length} / {MAX_TICKETS_PER_BOOKING}</span>
            <span>Unfilled details: {remainingImportSlots}</span>
            <span>Total amount: Rs. {(holdInfo?.totalAmount || Number(event.currentTicketPrice) * selectedSeatIds.length) || 0}</span>
            {holdInfo ? <span>Hold expires: {new Date(holdInfo.holdExpiresAt).toLocaleTimeString()}</span> : null}
          </div>
          <div className="ticket-holder-section brutal-card">
            <div className="ticket-holder-header">
              <div>
                <h2>Ticket holder details</h2>
                <p className="muted-text">Enter the name, age, and gender for each selected seat.</p>
              </div>
              <button
                type="button"
                className="brutal-button secondary small"
                onClick={() => {
                  if (!remainingImportSlots) {
                    const message = "All selected seats already have complete details.";
                    setError(message);
                    showToast({ title: "All seats filled", message, variant: "info" });
                    return;
                  }
                  setShowImportModal(true);
                  setSelectedPassengerIds([]);
                }}
                disabled={!selectedSeatIds.length}
              >
                Import Details
              </button>
            </div>
            {selectedSeatIds.length ? (
              <div className="ticket-holder-list">
                {ticketHolders.map((holder, index) => (
                  <div key={`${selectedSeatIds[index]}-${index}`} className="ticket-holder-card">
                    <div className="ticket-holder-card-top">
                      <strong>Ticket {index + 1}</strong>
                      <button
                        type="button"
                        className="text-button"
                        onClick={() => savePassenger(holder, index)}
                        disabled={savingPassengerIndex === index}
                      >
                        {savingPassengerIndex === index ? "Saving..." : "Save Person"}
                      </button>
                    </div>
                    <input
                      type="text"
                      placeholder="Full name"
                      value={holder.name}
                      onChange={(event) => updateTicketHolder(index, "name", event.target.value)}
                    />
                    <input
                      type="number"
                      min="1"
                      max="120"
                      placeholder="Age"
                      value={holder.age}
                      onChange={(event) => updateTicketHolder(index, "age", event.target.value)}
                    />
                    <select
                      value={holder.gender}
                      onChange={(event) => updateTicketHolder(index, "gender", event.target.value)}
                    >
                      <option value="">Select gender</option>
                      <option value="Male">Male</option>
                      <option value="Female">Female</option>
                      <option value="Other">Other</option>
                    </select>
                  </div>
                ))}
              </div>
            ) : <p className="muted-text">Choose seat(s) to unlock attendee details.</p>}
          </div>
          <div className="card-actions">
            <button className="brutal-button secondary" type="button" onClick={holdSeats}>
              Hold Seats
            </button>
            <button className="brutal-button" type="submit" disabled={!selectedSeatIds.length}>
              Confirm Booking
            </button>
          </div>
          <p className="muted-text">Yellow seats are temporarily locked. Your own held seats stay selectable. Maximum 8 seats per booking.</p>
        </form>
      </div>
      {error ? <p className="error-text">{error}</p> : null}
      <Modal
        open={showConfirm}
        title="Confirm booking"
        message={`Book ${selectedSeatIds.length} seat(s) for ${event.name} for Rs. ${holdInfo?.totalAmount || Number(event.currentTicketPrice) * selectedSeatIds.length}?`}
        confirmLabel="Book Now"
        onConfirm={confirmBooking}
        onClose={() => setShowConfirm(false)}
      />
      <Modal
        open={successModal.open}
        title="Booking confirmed"
        message={successModal.message}
        confirmLabel="View My Bookings"
        hideCancel
        onConfirm={() => {
          setSuccessModal({ open: false, message: "" });
          navigate("/my-bookings", { state: { message: `Booked ${event.name} successfully.` } });
        }}
        onClose={() => setSuccessModal({ open: false, message: "" })}
      />
      {showImportModal ? (
        <div className="modal-overlay" role="dialog" aria-modal="true">
          <div className="modal-card import-modal-card">
            <p className="eyebrow">Saved Passengers</p>
            <h2>Import details</h2>
            <p>Select up to {remainingImportSlots} saved passenger detail(s) to fill only the unfilled ticket slots.</p>
            {passengersLoading ? (
              <p>Loading saved passengers...</p>
            ) : savedPassengers.length ? (
              <div className="saved-passenger-list">
                {savedPassengers.map((passenger) => {
                  const checked = selectedPassengerIds.includes(passenger.id);
                  const selectionDisabled = !checked && selectedPassengerIds.length >= remainingImportSlots;
                  return (
                    <div key={passenger.id} className="saved-passenger-card">
                      <div className="saved-passenger-main">
                        <input
                          type="checkbox"
                          checked={checked}
                          disabled={selectionDisabled}
                          onChange={() => toggleSavedPassengerSelection(passenger.id)}
                        />
                        <div>
                          <strong>{passenger.name}</strong>
                          <p className="muted-text">
                            Age {passenger.age} • {passenger.gender}
                          </p>
                        </div>
                      </div>
                      <button
                        type="button"
                        className="text-button"
                        onClick={() => deletePassenger(passenger.id)}
                        disabled={deletingPassengerId === passenger.id}
                      >
                        {deletingPassengerId === passenger.id ? "Removing..." : "Remove"}
                      </button>
                    </div>
                  );
                })}
              </div>
            ) : (
              <p className="muted-text">No saved passengers yet. Use the Save Person button on a filled ticket card to build your reusable list.</p>
            )}
            {!remainingImportSlots ? <p className="muted-text">All selected seats already have complete details.</p> : null}
            <div className="modal-actions">
              <button type="button" className="brutal-button secondary" onClick={() => setShowImportModal(false)}>
                Close
              </button>
              <button
                type="button"
                className="brutal-button"
                onClick={importSelectedPassengers}
                disabled={!selectedPassengerIds.length}
              >
                Import Selected
              </button>
            </div>
          </div>
        </div>
      ) : null}
    </section>
  );
}
