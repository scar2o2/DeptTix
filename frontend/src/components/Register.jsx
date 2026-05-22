import { useState } from "react";
import { Link } from "react-router-dom";
import { useAuth } from "../auth/AuthProvider";
import CredentialsButton from "./CredentialsButton";
import { useToast } from "../context/ToastContext";

export default function Register() {
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
      <div className="auth-utility">
        <CredentialsButton />
      </div>
      <div className="hero-panel">
        <p className="eyebrow">Secure Vel Tech Onboarding</p>
        <h1>Create your campus event access</h1>
        <p>Register with any email, verify it, and then enter the protected Vel Tech event booking system.</p>
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
        />
        <input
          placeholder="Password"
          type="password"
          value={form.password}
          onChange={(e) => setForm({ ...form, password: e.target.value })}
        />
        <div className="summary-box">
          <span>Any valid email address can create a new account.</span>
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
