import { Link } from "react-router-dom";
import velTechLogo from "../assets/veltech-logo.png";
import { CAMPUS_VENUES } from "../constants/venues";

const schools = [
  "School of Computing",
  "School of Electrical and Communication",
  "School of Mechanical and Construction",
  "School of Management",
  "School of Law",
  "School of Media Technology & Communication"
];

export default function LandingPage() {
  return (
    <main className="landing-page">
      <section className="landing-hero">
        <nav className="landing-nav" aria-label="Landing navigation">
          <span className="brand-mark">
            <img src={velTechLogo} alt="Vel Tech logo" />
            Vel Tech Events
          </span>
          <a href="https://www.veltech.edu.in/" target="_blank" rel="noreferrer">
            Official Vel Tech
          </a>
        </nav>

        <div className="landing-hero-grid">
          <div className="landing-copy">
            <p className="eyebrow">Vel Tech Rangarajan Dr. Sagunthala R&D Institute of Science and Technology</p>
            <h1>Campus event booking for Vel Tech, Avadi</h1>
            <p>
              A college-specific event hub for students and faculty to discover school programs,
              campus activities, workshops, seminars, and auditorium events from one authenticated place.
            </p>
            <div className="landing-actions">
              <Link className="brutal-button" to="/login">Get Started</Link>
              <Link className="brutal-button secondary" to="/register">Create Account</Link>
            </div>
          </div>

          <div className="campus-card">
            <p className="eyebrow">Campus Address</p>
            <strong>No.42, Avadi-Vel Tech Road</strong>
            <span>Vel Nagar, Avadi, Chennai - 600 062</span>
            <span>Tamil Nadu, India</span>
          </div>
        </div>
      </section>

      <section className="landing-band light">
        <div className="landing-section-title">
          <p className="eyebrow">Schools & Activities</p>
          <h2>Events can be mapped to Vel Tech academics and campus facilities</h2>
        </div>
        <div className="school-grid">
          {schools.map((school) => (
            <span key={school}>{school}</span>
          ))}
        </div>
      </section>

      <section className="landing-band">
        <div className="landing-section-title">
          <p className="eyebrow">Campus Venues</p>
          <h2>Available Vel Tech event venues</h2>
        </div>
        <div className="venue-table-wrap">
          <table className="venue-table">
            <thead>
              <tr>
                <th>No.</th>
                <th>Venue</th>
              </tr>
            </thead>
            <tbody>
              {CAMPUS_VENUES.map((venue, index) => (
                <tr key={venue}>
                  <td>{index + 1}</td>
                  <td>{venue}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>
    </main>
  );
}
