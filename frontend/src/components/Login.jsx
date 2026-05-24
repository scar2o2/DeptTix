import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../auth/AuthProvider";
import { useToast } from "../context/ToastContext";

export default function Login() {
  const navigate = useNavigate();
  const { login, loginWithGoogle, loading } = useAuth();
  const { showToast } = useToast();
  const [form, setForm] = useState({ email: "", password: "" });
  const [error, setError] = useState("");

  const moveToHome = (user) => {
    if (!user?.profileComplete) {
      navigate("/complete-profile");
      return;
    }
    navigate(user.role === "ADMIN" ? "/admin/dashboard" : "/events");
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError("");
    try {
      const data = await login(form);
      showToast({
        title: "Login successful",
        message: data?.name
          ? `Welcome back, ${data.name}.`
          : "Email verified. Complete your profile to continue.",
        variant: "success"
      });
      moveToHome(data);
    } catch (err) {
      const message = err.response?.data?.message || err.message || "Unable to login right now.";
      setError(message);
      showToast({ title: "Login failed", message, variant: "error" });
    }
  };

  const handleGoogleLogin = async () => {
    setError("");
    try {
      const data = await loginWithGoogle();
      showToast({
        title: "Google sign-in complete",
        message: data?.name
          ? `Workspace ready for ${data.name}.`
          : "Complete your profile to continue.",
        variant: "success"
      });
      moveToHome(data);
    } catch (err) {
      const message = err.response?.data?.message || err.message || "Google login failed.";
      setError(message);
      showToast({ title: "Sign-in failed", message, variant: "error" });
    }
  };

  return (
    <section className="auth-page">
      <div className="hero-panel">
        <p className="eyebrow">Vel Tech Events Access</p>
        <h1>Sign in to Vel Tech Events</h1>
        <p>Use your verified email account to access college events, school-level workshops, auditorium programs, and booking tools.</p>
      </div>

      <form className="brutal-card auth-card" onSubmit={handleSubmit}>
        <h2>Login</h2>
        <input
          type="email"
          placeholder="Email"
          value={form.email}
          onChange={(e) => setForm({ ...form, email: e.target.value })}
        />
        <input
          type="password"
          placeholder="Password"
          value={form.password}
          onChange={(e) => setForm({ ...form, password: e.target.value })}
        />
        {error ? <p className="error-text">{error}</p> : null}
        <button className="brutal-button" type="submit" disabled={loading}>Enter Vel Tech Events</button>
        <button className="brutal-button secondary" type="button" onClick={handleGoogleLogin} disabled={loading}>
          Continue With Google
        </button>
        <div className="summary-box">
          <span>Email login requires verified email before campus event access.</span>
          <span>Google login syncs your event profile on first sign-in.</span>
        </div>
        <p>Need an account? <Link to="/register">Register</Link></p>
      </form>
    </section>
  );
}
