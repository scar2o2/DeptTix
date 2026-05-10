import { useState } from "react";
import { Link } from "react-router-dom";
import { useAuth } from "../auth/AuthProvider";
import { useToast } from "../context/ToastContext";

export default function Register() {
  const allowedEmailDomain = "@veltech.edu.in";
  const { register, loading } = useAuth();
  const { showToast } = useToast();
  const [form, setForm] = useState({
    name: "",
    email: "",
    password: ""
  });
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");

  const handleSubmit = async (event) => {
    event.preventDefault();
    setMessage("");
    setError("");

    if (!form.email.trim().toLowerCase().endsWith(allowedEmailDomain)) {
      const failure = `Use your ${allowedEmailDomain} email address to register.`;
      setError(failure);
      showToast({ title: "Vel Tech email required", message: failure, variant: "error" });
      return;
    }

    try {
      await register(form);
      const successMessage = "Verify your email before login.";
      setMessage(successMessage);
      showToast({
        title: "Verification email sent",
        message: successMessage,
        variant: "success"
      });
    } catch (err) {
      const failure = err.response?.data?.message || err.message || "Unable to register right now.";
      setError(failure);
      showToast({ title: "Registration failed", message: failure, variant: "error" });
    }
  };

  return (
    <section className="auth-page">
      <div className="hero-panel">
        <p className="eyebrow">Secure Vel Tech Onboarding</p>
        <h1>Create your campus event access</h1>
        <p>Students and faculty can register, verify email, and then enter the protected Vel Tech event booking system.</p>
      </div>

      <form className="brutal-card auth-card" onSubmit={handleSubmit}>
        <h2>Register</h2>
        <input
          placeholder="Full name"
          value={form.name}
          onChange={(e) => setForm({ ...form, name: e.target.value })}
        />
        <input
          placeholder="Email"
          type="email"
          value={form.email}
          onChange={(e) => setForm({ ...form, email: e.target.value })}
          pattern="^[^@\s]+@veltech\.edu\.in$"
          title="Use your @veltech.edu.in email address"
        />
        <input
          placeholder="Password"
          type="password"
          value={form.password}
          onChange={(e) => setForm({ ...form, password: e.target.value })}
        />
        <div className="summary-box">
          <span>Only @veltech.edu.in email addresses can create new accounts.</span>
          <span>You will choose your department after your first verified sign-in.</span>
          <span>Admin access is provisioned separately.</span>
        </div>
        {message ? <p className="success-text">{message}</p> : null}
        {error ? <p className="error-text">{error}</p> : null}
        <button className="brutal-button" type="submit" disabled={loading}>Create Account</button>
        <p>Already registered? <Link to="/login">Login</Link></p>
      </form>
    </section>
  );
}
