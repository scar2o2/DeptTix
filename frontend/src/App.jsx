import { lazy, Suspense } from "react";
import { Navigate, Route, Routes } from "react-router-dom";
import ProtectedRoute from "./auth/ProtectedRoute";
import AppLayout from "./layouts/AppLayout";

const LandingPage = lazy(() => import("./pages/LandingPage"));
const LoginPage = lazy(() => import("./pages/LoginPage"));
const RegisterPage = lazy(() => import("./pages/RegisterPage"));
const ProfileDetailsPage = lazy(() => import("./pages/ProfileDetailsPage"));
const EventListPage = lazy(() => import("./pages/EventListPage"));
const EventDetailPage = lazy(() => import("./pages/EventDetailPage"));
const BookingPage = lazy(() => import("./pages/BookingPage"));
const MyBookingsPage = lazy(() => import("./pages/MyBookingsPage"));
const SavedUsersPage = lazy(() => import("./pages/SavedUsersPage"));
const AdminDashboardPage = lazy(() => import("./pages/AdminDashboardPage"));
const ManageEventsPage = lazy(() => import("./pages/ManageEventsPage"));

function RouteFallback() {
  return (
    <section className="auth-loading">
      <div className="brutal-card loading-card skeleton-card" aria-hidden="true">
        <p className="eyebrow">Loading</p>
        <div className="skeleton-group">
          <span className="skeleton-line skeleton-title" />
          <span className="skeleton-line skeleton-title short" />
        </div>
      </div>
    </section>
  );
}

export default function App() {
  return (
    <Suspense fallback={<RouteFallback />}>
      <Routes>
        <Route path="/" element={<LandingPage />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
        <Route element={<ProtectedRoute profileSetup />}>
          <Route path="/complete-profile" element={<ProfileDetailsPage />} />
        </Route>

        <Route element={<ProtectedRoute />}>
          <Route element={<AppLayout />}>
            <Route element={<ProtectedRoute role="USER" />}>
              <Route path="/events" element={<EventListPage />} />
              <Route path="/events/:id" element={<EventDetailPage />} />
              <Route path="/events/:id/book" element={<BookingPage />} />
              <Route path="/my-bookings" element={<MyBookingsPage />} />
              <Route path="/saved-users" element={<SavedUsersPage />} />
            </Route>

            <Route element={<ProtectedRoute role="ADMIN" />}>
              <Route path="/admin/dashboard" element={<AdminDashboardPage />} />
              <Route path="/admin/events" element={<ManageEventsPage />} />
            </Route>
          </Route>
        </Route>

        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </Suspense>
  );
}
