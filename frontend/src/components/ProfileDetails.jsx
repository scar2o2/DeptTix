import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../auth/AuthProvider";
import { useToast } from "../context/ToastContext";
import { USER_DEPARTMENTS, getDepartmentLabel } from "../constants/departments";

export default function ProfileDetails() {
  const navigate = useNavigate();
  const { firebaseUser, completeProfile, loading } = useAuth();
  const { showToast } = useToast();
  const [department, setDepartment] = useState("");
  const [error, setError] = useState("");

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError("");

    try {
      const data = await completeProfile({ department });
      showToast({
        title: "Profile completed",
        message: `Welcome, ${data.name}. Your account is ready.`,
        variant: "success"
      });
      navigate(data.role === "ADMIN" ? "/admin/dashboard" : "/events", { replace: true });
    } catch (err) {
      const message = err.response?.data?.message || err.message || "Unable to save your details.";
      setError(message);
      showToast({ title: "Profile save failed", message, variant: "error" });
    }
  };

  return (
    <section className="auth-page">
      <div className="hero-panel">
        <p className="eyebrow">One More Step</p>
        <h1>Complete your Vel Tech profile</h1>
        <p>Your sign-in is verified. Choose your Vel Tech school once so the app can unlock the right campus events for you.</p>
      </div>

      <form className="brutal-card auth-card" onSubmit={handleSubmit}>
        <h2>Details</h2>
        <div className="summary-box">
          <span>Name: {firebaseUser?.displayName || firebaseUser?.email?.split("@")[0]}</span>
          <span>Email: {firebaseUser?.email}</span>
        </div>
        <select value={department} onChange={(e) => setDepartment(e.target.value)}>
          <option value="">Select Vel Tech school</option>
          {USER_DEPARTMENTS.map((option) => (
            <option key={option} value={option}>{getDepartmentLabel(option)}</option>
          ))}
        </select>
        {error ? <p className="error-text">{error}</p> : null}
        <button className="brutal-button" type="submit" disabled={loading || !department}>
          Save Details
        </button>
      </form>
    </section>
  );
}
