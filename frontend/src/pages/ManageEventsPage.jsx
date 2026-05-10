import { useEffect, useState } from "react";
import Modal from "../components/Modal";
import { EVENT_DEPARTMENTS, getDepartmentLabel } from "../constants/departments";
import { CAMPUS_VENUES } from "../constants/venues";
import { useToast } from "../context/ToastContext";
import api from "../services/api";

const initialForm = {
  name: "",
  department: "ALL",
  dateTime: "",
  venue: "",
  ticketPrice: "",
  availableTickets: ""
};

export default function ManageEventsPage() {
  const { showToast } = useToast();
  const [events, setEvents] = useState([]);
  const [form, setForm] = useState(initialForm);
  const [editId, setEditId] = useState(null);
  const [error, setError] = useState("");
  const [deleteTarget, setDeleteTarget] = useState(null);
  const [statusModal, setStatusModal] = useState({ open: false, title: "", message: "" });

  const loadEvents = () => {
    api.get("/admin/events")
      .then(({ data }) => setEvents(data))
      .catch((err) => {
        const message = err.response?.data?.message || "Unable to load admin events.";
        setError(message);
        showToast({ title: "Dashboard sync failed", message, variant: "error" });
      });
  };

  useEffect(() => {
    loadEvents();
  }, []);

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError("");
    const payload = {
      ...form,
      ticketPrice: Number(form.ticketPrice),
      availableTickets: Number(form.availableTickets)
    };

    try {
      if (editId) {
        await api.put(`/admin/events/${editId}`, payload);
        showToast({ title: "Event updated", message: `${form.name} was updated.`, variant: "success" });
        setStatusModal({ open: true, title: "Event updated", message: `${form.name} was updated successfully.` });
      } else {
        await api.post("/admin/events", payload);
        showToast({ title: "Event created", message: `${form.name} is now live.`, variant: "success" });
        setStatusModal({ open: true, title: "Event created", message: `${form.name} was added successfully.` });
      }
      setForm(initialForm);
      setEditId(null);
      loadEvents();
    } catch (err) {
      const message = err.response?.data?.message || "Unable to save event.";
      setError(message);
      showToast({ title: "Save failed", message, variant: "error" });
    }
  };

  const startEdit = (event) => {
    setEditId(event.id);
    setForm({
      name: event.name,
      department: event.department,
      dateTime: event.dateTime.slice(0, 16),
      venue: event.venue,
      ticketPrice: event.ticketPrice,
      availableTickets: event.totalTickets
    });
  };

  const removeEvent = async () => {
    if (!deleteTarget) {
      return;
    }
    try {
      await api.delete(`/admin/events/${deleteTarget.id}`);
      showToast({ title: "Event deleted", message: `${deleteTarget.name} was removed.`, variant: "success" });
      setStatusModal({ open: true, title: "Event deleted", message: `${deleteTarget.name} was deleted successfully.` });
      setDeleteTarget(null);
      loadEvents();
    } catch (err) {
      const message = err.response?.data?.message || "Unable to delete event.";
      setError(message);
      showToast({ title: "Delete failed", message, variant: "error" });
    }
  };

  return (
    <section className="manage-layout">
      <form className="brutal-card manage-form" onSubmit={handleSubmit}>
        <p className="eyebrow">Vel Tech Admin Only</p>
        <h1>{editId ? "Edit campus event" : "Create campus event"}</h1>
        <input placeholder="Event name" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} />
        <select value={form.department} onChange={(e) => setForm({ ...form, department: e.target.value })}>
          {EVENT_DEPARTMENTS.map((option) => (
            <option key={option} value={option}>{getDepartmentLabel(option)}</option>
          ))}
        </select>
        <input type="datetime-local" value={form.dateTime} onChange={(e) => setForm({ ...form, dateTime: e.target.value })} />
        <select value={form.venue} onChange={(e) => setForm({ ...form, venue: e.target.value })}>
          <option value="">Select venue</option>
          {CAMPUS_VENUES.map((venue) => (
            <option key={venue} value={venue}>{venue}</option>
          ))}
        </select>
        <input type="number" placeholder="Ticket price" value={form.ticketPrice} onChange={(e) => setForm({ ...form, ticketPrice: e.target.value })} />
        <input type="number" placeholder="Total capacity" value={form.availableTickets} onChange={(e) => setForm({ ...form, availableTickets: e.target.value })} />
        {error ? <p className="error-text">{error}</p> : null}
        <button className="brutal-button" type="submit">{editId ? "Update Event" : "Add Event"}</button>
      </form>

      <div className="list-stack">
        {events.map((event) => (
          <article className="brutal-card list-item" key={event.id}>
            <div>
              <p className="eyebrow">{getDepartmentLabel(event.department)}</p>
              <h3>{event.name}</h3>
              <div className="meta-grid">
                <span>{new Date(event.dateTime).toLocaleString()}</span>
                <span>{event.venue}</span>
                <span>Price: Rs. {event.ticketPrice}</span>
                <span>Available: {event.availableTickets}</span>
              </div>
            </div>
            <div className="card-actions">
              <button type="button" className="brutal-button secondary" onClick={() => startEdit(event)}>Edit</button>
              <button type="button" className="brutal-button danger" onClick={() => setDeleteTarget(event)}>Delete</button>
            </div>
          </article>
        ))}
      </div>
      <Modal
        open={Boolean(deleteTarget)}
        title="Delete event"
        message={deleteTarget ? `Remove ${deleteTarget.name} from the event list?` : ""}
        confirmLabel="Delete"
        variant="danger"
        onConfirm={removeEvent}
        onClose={() => setDeleteTarget(null)}
      />
      <Modal
        open={statusModal.open}
        title={statusModal.title}
        message={statusModal.message}
        confirmLabel="Close"
        hideCancel
        onConfirm={() => setStatusModal({ open: false, title: "", message: "" })}
        onClose={() => setStatusModal({ open: false, title: "", message: "" })}
      />
    </section>
  );
}
