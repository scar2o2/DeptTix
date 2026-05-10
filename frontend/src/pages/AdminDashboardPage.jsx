import { useEffect, useState } from "react";
import {
  BarChart, Bar, CartesianGrid, Cell, Legend, Line, LineChart,
  Pie, PieChart, ResponsiveContainer, Tooltip, XAxis, YAxis
} from "recharts";
import api from "../services/api";

const colors = ["#ff6b00", "#00d4ff", "#f4dd00", "#ff3b7a", "#8bf400"];

export default function AdminDashboardPage() {
  const [revenueData, setRevenueData] = useState([]);
  const [stats, setStats] = useState(null);
  const [events, setEvents] = useState([]);
  const [error, setError] = useState("");

  useEffect(() => {
    const loadDashboard = () => {
      Promise.all([
        api.get("/admin/revenue"),
        api.get("/admin/bookings/stats"),
        api.get("/admin/events")
      ])
        .then(([revenueResponse, statsResponse, eventsResponse]) => {
          setRevenueData(revenueResponse.data);
          setStats(statsResponse.data);
          setEvents(eventsResponse.data);
        })
        .catch((err) => setError(err.response?.data?.message || "Unable to load dashboard."));
    };

    loadDashboard();
    const intervalId = window.setInterval(loadDashboard, 15000);
    return () => window.clearInterval(intervalId);
  }, []);

  const totalRevenue = revenueData.reduce((sum, item) => sum + Number(item.revenue || 0), 0);
  const totalBookings = revenueData.reduce((sum, item) => sum + Number(item.bookings || 0), 0);
  const totalTicketsSold = revenueData.reduce((sum, item) => sum + Number(item.ticketsSold || 0), 0);

  const exportReport = async (eventId) => {
    try {
      const response = await api.get(`/admin/export-report/${eventId}`, { responseType: "blob" });
      const url = window.URL.createObjectURL(new Blob([response.data], { type: "application/pdf" }));
      const link = document.createElement("a");
      link.href = url;
      link.download = `event-report-${eventId}.pdf`;
      link.click();
      window.URL.revokeObjectURL(url);
    } catch (err) {
      setError(err.response?.data?.message || "Unable to export PDF report.");
    }
  };

  if (!stats) {
    return <p>{error || "Loading dashboard..."}</p>;
  }

  return (
    <section className="dashboard-grid">
      <article className="brutal-card metric-card"><span>Campus Events</span><strong>{events.length}</strong></article>
      <article className="brutal-card metric-card"><span>Total Revenue</span><strong>Rs. {totalRevenue.toFixed(0)}</strong></article>
      <article className="brutal-card metric-card"><span>Tickets Sold</span><strong>{totalTicketsSold}</strong></article>
      <article className="brutal-card metric-card"><span>Bookings</span><strong>{totalBookings}</strong></article>

      <article className="brutal-card chart-card">
        <h2>Revenue per Event</h2>
        <ResponsiveContainer width="100%" height={280}>
          <BarChart data={revenueData}>
            <CartesianGrid stroke="#111" />
            <XAxis dataKey="eventName" />
            <YAxis />
            <Tooltip />
            <Bar dataKey="revenue" fill="#ff6b00" />
          </BarChart>
        </ResponsiveContainer>
      </article>

      <article className="brutal-card chart-card">
        <h2>Ticket Distribution</h2>
        <ResponsiveContainer width="100%" height={280}>
          <PieChart>
            <Pie data={stats.ticketDistribution} dataKey="tickets" nameKey="name" outerRadius={95}>
              {stats.ticketDistribution.map((entry, index) => (
                <Cell key={entry.name} fill={colors[index % colors.length]} />
              ))}
            </Pie>
            <Tooltip />
            <Legend />
          </PieChart>
        </ResponsiveContainer>
      </article>

      <article className="brutal-card chart-card">
        <h2>Remaining vs Booked Tickets</h2>
        <ResponsiveContainer width="100%" height={280}>
          <BarChart data={stats.soldVsRemaining}>
            <CartesianGrid stroke="#111" />
            <XAxis dataKey="name" />
            <YAxis />
            <Tooltip />
            <Legend />
            <Bar dataKey="booked" fill="#111" />
            <Bar dataKey="remaining" fill="#00d4ff" />
          </BarChart>
        </ResponsiveContainer>
      </article>

      <article className="brutal-card chart-card">
        <h2>Bookings Over Time</h2>
        <ResponsiveContainer width="100%" height={280}>
          <LineChart data={stats.bookingsOverTime}>
            <CartesianGrid stroke="#111" />
            <XAxis dataKey="date" />
            <YAxis />
            <Tooltip />
            <Line type="monotone" dataKey="tickets" stroke="#ff3b7a" strokeWidth={4} />
          </LineChart>
        </ResponsiveContainer>
      </article>

      <article className="brutal-card chart-card full-span">
        <h2>Export PDF Reports</h2>
        <div className="list-stack">
          {events.map((event) => (
            <div className="list-item" key={event.id}>
              <div>
                <strong>{event.name}</strong>
                <p className="muted-text">{new Date(event.dateTime).toLocaleString()} | {event.venue}</p>
              </div>
              <button className="brutal-button" type="button" onClick={() => exportReport(event.id)}>
                Download PDF
              </button>
            </div>
          ))}
        </div>
      </article>
    </section>
  );
}
