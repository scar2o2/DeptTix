import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { useToast } from "../context/ToastContext";
import api from "../services/api";

export default function SavedUsersPage() {
  const { showToast } = useToast();
  const [savedPassengers, setSavedPassengers] = useState([]);
  const [error, setError] = useState("");
  const [form, setForm] = useState({ name: "", age: "", gender: "" });
  const [savingPassenger, setSavingPassenger] = useState(false);
  const [deletingPassengerId, setDeletingPassengerId] = useState(null);

  const loadSavedPassengers = async () => {
    try {
      const { data } = await api.get("/users/me/passengers");
      setSavedPassengers(data);
    } catch (err) {
      const message = err.response?.data?.message || "Unable to load saved users.";
      setError(message);
      showToast({ title: "Could not load saved users", message, variant: "error" });
    }
  };

  useEffect(() => {
    loadSavedPassengers();
  }, []);

  const updateForm = (field, value) => {
    setForm((current) => ({ ...current, [field]: value }));
  };

  const addPassenger = async (event) => {
    event.preventDefault();
    setError("");

    if (!form.name.trim() || !form.age || Number(form.age) <= 0 || !form.gender) {
      const message = "Enter name, age, and gender to save a user.";
      setError(message);
      showToast({ title: "Details missing", message, variant: "error" });
      return;
    }

    setSavingPassenger(true);
    try {
      const payload = {
        name: form.name.trim(),
        age: Number(form.age),
        gender: form.gender
      };
      const { data } = await api.post("/users/me/passengers", payload);
      setSavedPassengers((current) => [data, ...current]);
      setForm({ name: "", age: "", gender: "" });
      showToast({
        title: "Saved user added",
        message: `${payload.name} is now ready to import during booking.`,
        variant: "success"
      });
    } catch (err) {
      const message = err.response?.data?.message || "Unable to save user.";
      setError(message);
      showToast({ title: "Save failed", message, variant: "error" });
    } finally {
      setSavingPassenger(false);
    }
  };

  const deletePassenger = async (passengerId) => {
    setDeletingPassengerId(passengerId);
    try {
      await api.delete(`/users/me/passengers/${passengerId}`);
      setSavedPassengers((current) => current.filter((passenger) => passenger.id !== passengerId));
      showToast({ title: "Saved user removed", message: "The saved details were deleted.", variant: "info" });
    } catch (err) {
      const message = err.response?.data?.message || "Unable to remove saved user.";
      showToast({ title: "Delete failed", message, variant: "error" });
    } finally {
      setDeletingPassengerId(null);
    }
  };

  return (
    <section>
      <div className="section-banner">
        <div>
          <p className="eyebrow">Quick Fill</p>
          <h1>Saved users</h1>
          <p className="muted-text">Manage the people whose details you reuse while booking tickets.</p>
        </div>
        <Link className="brutal-button" to="/events">
          Back to Events
        </Link>
      </div>
      {error ? <p className="error-text">{error}</p> : null}
      <form className="brutal-card manage-form" onSubmit={addPassenger}>
        <h2>Add saved user</h2>
        <p className="muted-text">Create reusable ticket-holder details here so they are ready on the booking page.</p>
        <input
          type="text"
          placeholder="Full name"
          value={form.name}
          onChange={(event) => updateForm("name", event.target.value)}
        />
        <input
          type="number"
          min="1"
          max="120"
          placeholder="Age"
          value={form.age}
          onChange={(event) => updateForm("age", event.target.value)}
        />
        <select
          value={form.gender}
          onChange={(event) => updateForm("gender", event.target.value)}
        >
          <option value="">Select gender</option>
          <option value="Male">Male</option>
          <option value="Female">Female</option>
          <option value="Other">Other</option>
        </select>
        <div className="card-actions">
          <button className="brutal-button" type="submit" disabled={savingPassenger}>
            {savingPassenger ? "Saving..." : "Add Saved User"}
          </button>
        </div>
      </form>
      <div className="list-stack">
        {savedPassengers.length ? savedPassengers.map((passenger) => (
          <article key={passenger.id} className="brutal-card list-item">
            <div>
              <h3>{passenger.name}</h3>
              <div className="meta-grid">
                <span>Age: {passenger.age}</span>
                <span>Gender: {passenger.gender}</span>
                <span>Saved: {new Date(passenger.createdAt).toLocaleString()}</span>
              </div>
            </div>
            <button
              type="button"
              className="brutal-button danger"
              onClick={() => deletePassenger(passenger.id)}
              disabled={deletingPassengerId === passenger.id}
            >
              {deletingPassengerId === passenger.id ? "Removing..." : "Remove"}
            </button>
          </article>
        )) : (
          <article className="brutal-card">
            <p className="muted-text">
              No saved users yet. Open a booking form, fill a passenger, and use the `Save Person` button to store them.
            </p>
          </article>
        )}
      </div>
    </section>
  );
}
