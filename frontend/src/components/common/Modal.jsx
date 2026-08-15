function Modal({
  open,
  title,
  onClose,
  children,
  footer,
  wide = false,
}) {
  if (!open) {
    return null;
  }

  return (
    <div
      className="modal-backdrop"
      role="presentation"
      onMouseDown={onClose}
    >
      <section
        className={`modal-card ${wide ? 'modal-wide' : ''}`}
        role="dialog"
        aria-modal="true"
        aria-label={title}
        onMouseDown={(event) => event.stopPropagation()}
      >
        <header className="modal-header">
          <h2>{title}</h2>

          <button
            type="button"
            className="icon-button"
            onClick={onClose}
            aria-label="Đóng"
          >
            ×
          </button>
        </header>

        <div className="modal-body">
          {children}
        </div>

        {footer && (
          <footer className="modal-footer">
            {footer}
          </footer>
        )}
      </section>
    </div>
  );
}

export default Modal;
