export default function Modal({
  open,
  title,
  message,
  confirmLabel = "Confirm",
  cancelLabel = "Cancel",
  onConfirm,
  onClose,
  variant = "default",
  hideCancel = false
}) {
  if (!open) {
    return null;
  }

  return (
    <div className="modal-overlay" role="dialog" aria-modal="true">
      <div className={`modal-card ${variant}`}>
        <p className="eyebrow">Confirmation</p>
        <h2>{title}</h2>
        {message ? <p>{message}</p> : null}
        <div className="modal-actions">
          {!hideCancel ? (
            <button type="button" className="brutal-button secondary" onClick={onClose}>
              {cancelLabel}
            </button>
          ) : null}
          <button type="button" className={`brutal-button ${variant === "danger" ? "danger" : ""}`} onClick={onConfirm}>
            {confirmLabel}
          </button>
        </div>
      </div>
    </div>
  );
}
