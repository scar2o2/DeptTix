import { useEffect, useState } from "react";
import {
  BarChart, Bar, CartesianGrid, Legend, Line, LineChart,
  ResponsiveContainer, Tooltip, XAxis, YAxis
} from "recharts";
import api from "../services/api";

const toNumber = (value) => Number(value || 0);

const splitLabel = (value = "", limit = 24) => {
  const words = value.split(" ");
  const lines = [""];
  words.forEach((word) => {
    const current = lines[lines.length - 1];
    if (`${current} ${word}`.trim().length <= limit || lines.length >= 2) {
      lines[lines.length - 1] = `${current} ${word}`.trim();
      return;
    }
    lines.push(word);
  });
  return lines;
};

function EventNameTick({ x, y, payload }) {
  const lines = splitLabel(payload.value);
  return (
    <g transform={`translate(${x},${y})`}>
      {lines.map((line, index) => (
        <text key={line} x={0} y={index * 15} textAnchor="end" fill="#111" fontSize={12}>
          {line}
        </text>
      ))}
    </g>
  );
}

const chartHeight = (items) => Math.max(340, items.length * 66 + 90);

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

  const normalizedRevenueData = revenueData.map((item) => ({
    ...item,
    revenue: toNumber(item.revenue),
    bookings: toNumber(item.bookings),
    ticketsSold: toNumber(item.ticketsSold)
  }));
  const ticketDistribution = (stats?.ticketDistribution || [])
    .map((item) => ({ ...item, tickets: toNumber(item.tickets) }))
    .filter((item) => item.tickets > 0);
  const soldVsRemaining = (stats?.soldVsRemaining || []).map((item) => ({
    ...item,
    booked: toNumber(item.booked),
    held: toNumber(item.held),
    remaining: toNumber(item.remaining),
    capacity: toNumber(item.capacity)
  }));
  const bookingsOverTime = (stats?.bookingsOverTime || []).map((item) => ({
    ...item,
    bookings: toNumber(item.bookings)
  }));

  const totalRevenue = normalizedRevenueData.reduce((sum, item) => sum + item.revenue, 0);
  const totalBookings = normalizedRevenueData.reduce((sum, item) => sum + item.bookings, 0);
  const totalTicketsSold = normalizedRevenueData.reduce((sum, item) => sum + item.ticketsSold, 0);

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
        {normalizedRevenueData.length ? (
          <ResponsiveContainer width="100%" height={chartHeight(normalizedRevenueData)}>
            <BarChart data={normalizedRevenueData} layout="vertical" margin={{ top: 12, right: 30, bottom: 12, left: 130 }}>
              <CartesianGrid stroke="#111" />
              <XAxis type="number" allowDecimals={false} />
              <YAxis dataKey="eventName" type="category" width={180} tick={<EventNameTick />} interval={0} />
              <Tooltip />
              <Bar dataKey="revenue" name="Revenue" fill="#ff6b00" />
            </BarChart>
          </ResponsiveContainer>
        ) : <p className="muted-text">No confirmed booking revenue yet.</p>}
      </article>

      <article className="brutal-card chart-card">
        <h2>Ticket Distribution</h2>
        {ticketDistribution.length ? (
          <ResponsiveContainer width="100%" height={chartHeight(ticketDistribution)}>
            <BarChart data={ticketDistribution} layout="vertical" margin={{ top: 12, right: 30, bottom: 12, left: 130 }}>
              <CartesianGrid stroke="#111" />
              <XAxis type="number" allowDecimals={false} />
              <YAxis dataKey="name" type="category" width={180} tick={<EventNameTick />} interval={0} />
              <Tooltip />
              <Bar dataKey="tickets" name="Tickets Sold" fill="#ff3b7a" />
            </BarChart>
          </ResponsiveContainer>
        ) : <p className="muted-text">No confirmed ticket sales yet.</p>}
      </article>

      <article className="brutal-card chart-card">
        <h2>Seat Capacity by Event</h2>
        {soldVsRemaining.length ? (
          <ResponsiveContainer width="100%" height={chartHeight(soldVsRemaining)}>
            <BarChart data={soldVsRemaining} layout="vertical" margin={{ top: 12, right: 30, bottom: 12, left: 130 }}>
              <CartesianGrid stroke="#111" />
              <XAxis type="number" allowDecimals={false} />
              <YAxis dataKey="name" type="category" width={180} tick={<EventNameTick />} interval={0} />
              <Tooltip />
              <Legend />
              <Bar dataKey="booked" name="Booked" stackId="capacity" fill="#111" />
              <Bar dataKey="held" name="Held" stackId="capacity" fill="#f4dd00" />
              <Bar dataKey="remaining" name="Remaining" stackId="capacity" fill="#00d4ff" />
            </BarChart>
          </ResponsiveContainer>
        ) : <p className="muted-text">No event capacity data yet.</p>}
      </article>

      <article className="brutal-card chart-card">
        <h2>Bookings Over Time</h2>
        {bookingsOverTime.length ? (
          <ResponsiveContainer width="100%" height={300}>
            <LineChart data={bookingsOverTime} margin={{ top: 12, right: 18, bottom: 12, left: 10 }}>
              <CartesianGrid stroke="#111" />
              <XAxis dataKey="date" />
              <YAxis allowDecimals={false} />
              <Tooltip />
              <Line type="monotone" dataKey="bookings" name="Bookings" stroke="#ff3b7a" strokeWidth={4} />
            </LineChart>
          </ResponsiveContainer>
        ) : <p className="muted-text">No confirmed bookings in the last 30 days.</p>}
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
