import { Navigate, Outlet } from "react-router-dom";
import { useAuth } from "./AuthProvider";

export default function ProtectedRoute({ role, profileSetup = false }) {
  const { firebaseUser, appUser, loading } = useAuth();

  if (loading) {
    return (
      <section className="auth-loading">
        <div className="brutal-card loading-card skeleton-card" aria-hidden="true">
          <p className="eyebrow">Authorizing</p>
          <div className="skeleton-group">
            <span className="skeleton-line skeleton-title" />
            <span className="skeleton-line skeleton-title short" />
          </div>
          <div className="skeleton-group">
            <span className="skeleton-line" />
            <span className="skeleton-line" />
            <span className="skeleton-line medium" />
          </div>
        </div>
      </section>
    );
  }

  if (!firebaseUser || !appUser) {
    if (profileSetup && firebaseUser) {
      return <Outlet />;
    }
    return <Navigate to="/login" replace />;
  }

  if (profileSetup) {
    if (appUser.profileComplete) {
      return <Navigate to={appUser.role === "ADMIN" ? "/admin/dashboard" : "/events"} replace />;
    }
    return <Outlet />;
  }

  if (!appUser.profileComplete) {
    return <Navigate to="/complete-profile" replace />;
  }

  if (role && appUser.role !== role) {
    return <Navigate to={appUser.role === "ADMIN" ? "/admin/dashboard" : "/events"} replace />;
  }

  return <Outlet />;
}
