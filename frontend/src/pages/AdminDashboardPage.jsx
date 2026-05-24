import { useEffect, useState } from "react";
import {
  BarChart, Bar, CartesianGrid, LabelList, Legend, Line, LineChart,
  ResponsiveContainer, Tooltip, XAxis, YAxis
} from "recharts";
import api from "../services/api";

const toNumber = (value) => Number(value || 0);

const splitLabel = (value = "", limit = 24, maxLines = 2) => {
  const words = value.split(" ");
  const lines = [""];
  words.forEach((word) => {
    const current = lines[lines.length - 1];
    if (`${current} ${word}`.trim().length <= limit || lines.length >= maxLines) {
      lines[lines.length - 1] = `${current} ${word}`.trim();
      return;
    }
    lines.push(word);
  });
  return lines;
};

function EventNameTick({ x, y, payload, limit = 24, fontSize = 12, maxLines = 2 }) {
  const lines = splitLabel(payload.value, limit, maxLines);
  return (
    <g transform={`translate(${x},${y})`}>
      {lines.map((line, index) => (
        <text key={`${line}-${index}`} x={0} y={index * (fontSize + 3)} textAnchor="end" fill="#111" fontSize={fontSize}>
          {line}
        </text>
      ))}
    </g>
  );
}

function MobileBarLabel({ x, y, width, height, value }) {
  if (!value) {
    return null;
  }
  const label = String(value);
  const maxChars = Math.max(12, Math.floor((width || 0) / 7));
  const text = label.length > maxChars ? `${label.slice(0, Math.max(9, maxChars - 3))}...` : label;
  return (
    <text
      x={x + 8}
      y={y + height / 2 + 4}
      fill="#111"
      fontSize={10}
      fontWeight={900}
      pointerEvents="none"
    >
      {text}
    </text>
  );
}

function useChartLayout() {
  const getLayout = () => {
    if (typeof window === "undefined") {
      return { isMobile: false, labelWidth: 180, left: 130, minWidth: 720, labelLimit: 24, fontSize: 12, rowHeight: 66, heightBase: 90 };
    }
    if (window.innerWidth <= 560) {
      return { isMobile: true, labelWidth: 88, left: 42, right: 30, minWidth: 0, labelLimit: 10, fontSize: 9, rowHeight: 72, heightBase: 90 };
    }
    if (window.innerWidth <= 900) {
      return { isMobile: false, labelWidth: 150, left: 102, right: 30, minWidth: 680, labelLimit: 20, fontSize: 11, rowHeight: 72, heightBase: 100 };
    }
    return { isMobile: false, labelWidth: 180, left: 130, right: 30, minWidth: 720, labelLimit: 24, fontSize: 12, rowHeight: 66, heightBase: 90 };
  };

  const [layout, setLayout] = useState(getLayout);

  useEffect(() => {
    const handleResize = () => setLayout(getLayout());
    window.addEventListener("resize", handleResize);
    return () => window.removeEventListener("resize", handleResize);
  }, []);

  return layout;
}

const chartHeight = (items, layout) => Math.max(340, items.length * layout.rowHeight + layout.heightBase);

export default function AdminDashboardPage() {
  const [revenueData, setRevenueData] = useState([]);
  const [stats, setStats] = useState(null);
  const [events, setEvents] = useState([]);
  const [error, setError] = useState("");
  const chartLayout = useChartLayout();

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
          <div className="chart-scroll">
            <div className="chart-canvas" style={{ minWidth: chartLayout.minWidth, height: chartHeight(normalizedRevenueData, chartLayout) }}>
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={normalizedRevenueData} layout="vertical" margin={{ top: 12, right: chartLayout.right, bottom: 12, left: chartLayout.left }}>
                  <CartesianGrid stroke="#111" />
                  <XAxis type="number" allowDecimals={false} />
                  {chartLayout.isMobile ? (
                    <YAxis dataKey="eventName" type="category" width={0} tick={false} axisLine={false} tickLine={false} />
                  ) : (
                    <YAxis
                      dataKey="eventName"
                      type="category"
                      width={chartLayout.labelWidth}
                      tick={<EventNameTick limit={chartLayout.labelLimit} fontSize={chartLayout.fontSize} />}
                      interval={0}
                    />
                  )}
                  <Tooltip />
                  <Bar dataKey="revenue" name="Revenue" fill="#ff6b00">
                    {chartLayout.isMobile ? <LabelList dataKey="eventName" content={<MobileBarLabel />} /> : null}
                  </Bar>
                </BarChart>
              </ResponsiveContainer>
            </div>
          </div>
        ) : <p className="muted-text">No confirmed booking revenue yet.</p>}
      </article>

      <article className="brutal-card chart-card">
        <h2>Ticket Distribution</h2>
        {ticketDistribution.length ? (
          <div className="chart-scroll">
            <div className="chart-canvas" style={{ minWidth: chartLayout.minWidth, height: chartHeight(ticketDistribution, chartLayout) }}>
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={ticketDistribution} layout="vertical" margin={{ top: 12, right: chartLayout.right, bottom: 12, left: chartLayout.left }}>
                  <CartesianGrid stroke="#111" />
                  <XAxis type="number" allowDecimals={false} />
                  {chartLayout.isMobile ? (
                    <YAxis dataKey="name" type="category" width={0} tick={false} axisLine={false} tickLine={false} />
                  ) : (
                    <YAxis
                      dataKey="name"
                      type="category"
                      width={chartLayout.labelWidth}
                      tick={<EventNameTick limit={chartLayout.labelLimit} fontSize={chartLayout.fontSize} />}
                      interval={0}
                    />
                  )}
                  <Tooltip />
                  <Bar dataKey="tickets" name="Tickets Sold" fill="#ff3b7a">
                    {chartLayout.isMobile ? <LabelList dataKey="name" content={<MobileBarLabel />} /> : null}
                  </Bar>
                </BarChart>
              </ResponsiveContainer>
            </div>
          </div>
        ) : <p className="muted-text">No confirmed ticket sales yet.</p>}
      </article>

      <article className="brutal-card chart-card">
        <h2>Seat Capacity by Event</h2>
        {soldVsRemaining.length ? (
          <div className="chart-scroll">
            <div className="chart-canvas" style={{ minWidth: chartLayout.minWidth, height: chartHeight(soldVsRemaining, chartLayout) }}>
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={soldVsRemaining} layout="vertical" margin={{ top: 12, right: chartLayout.right, bottom: 12, left: chartLayout.left }}>
                  <CartesianGrid stroke="#111" />
                  <XAxis type="number" allowDecimals={false} />
                  {chartLayout.isMobile ? (
                    <YAxis dataKey="name" type="category" width={0} tick={false} axisLine={false} tickLine={false} />
                  ) : (
                    <YAxis
                      dataKey="name"
                      type="category"
                      width={chartLayout.labelWidth}
                      tick={<EventNameTick limit={chartLayout.labelLimit} fontSize={chartLayout.fontSize} />}
                      interval={0}
                    />
                  )}
                  <Tooltip />
                  <Legend />
                  <Bar dataKey="booked" name="Booked" stackId="capacity" fill="#111">
                    {chartLayout.isMobile ? <LabelList dataKey="name" content={<MobileBarLabel />} /> : null}
                  </Bar>
                  <Bar dataKey="held" name="Held" stackId="capacity" fill="#f4dd00" />
                  <Bar dataKey="remaining" name="Remaining" stackId="capacity" fill="#00d4ff" />
                </BarChart>
              </ResponsiveContainer>
            </div>
          </div>
        ) : <p className="muted-text">No event capacity data yet.</p>}
      </article>

      <article className="brutal-card chart-card">
        <h2>Bookings Over Time</h2>
        {bookingsOverTime.length ? (
          <div className="chart-scroll">
            <div className="chart-canvas" style={{ minWidth: chartLayout.minWidth, height: 320 }}>
              <ResponsiveContainer width="100%" height="100%">
                <LineChart data={bookingsOverTime} margin={{ top: 12, right: 24, bottom: 12, left: 10 }}>
                  <CartesianGrid stroke="#111" />
                  <XAxis dataKey="date" />
                  <YAxis allowDecimals={false} />
                  <Tooltip />
                  <Line type="monotone" dataKey="bookings" name="Bookings" stroke="#ff3b7a" strokeWidth={4} />
                </LineChart>
              </ResponsiveContainer>
            </div>
          </div>
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
