import { useState } from "react";

const credentials = [
  {
    role: "Admin",
    email: "manojcherukuri202@gmail.com",
    password: "12345678"
  },
  {
    role: "Sample User",
    email: "vtu25721@veltech.edu.in",
    password: "123456"
  }
];

export default function CredentialsButton() {
  const [open, setOpen] = useState(false);

  return (
    <>
      <button className="credentials-trigger" type="button" onClick={() => setOpen(true)}>
        Credentials
      </button>
      {open ? (
        <div className="modal-overlay" role="dialog" aria-modal="true" aria-labelledby="credentials-title">
          <div className="modal-card credentials-modal-card">
            <p className="eyebrow">Demo Access</p>
            <h2 id="credentials-title">Credentials</h2>
            <div className="credentials-list">
              {credentials.map((credential) => (
                <article className="credential-card" key={credential.role}>
                  <strong>{credential.role}</strong>
                  <span>Email: {credential.email}</span>
                  <span>Password: {credential.password}</span>
                </article>
              ))}
            </div>
            <div className="modal-actions">
              <button type="button" className="brutal-button secondary" onClick={() => setOpen(false)}>
                Close
              </button>
            </div>
          </div>
        </div>
      ) : null}
    </>
  );
}
