import { useEffect, useState } from 'react';

import Modal from './Modal';

function ActionDialog({
  open,
  title,
  message,
  confirmLabel = 'Xác nhận',
  cancelLabel = 'Hủy',
  danger = false,
  inputLabel = '',
  inputPlaceholder = '',
  initialValue = '',
  required = false,
  maxLength = 500,
  saving = false,
  error = '',
  onClose,
  onConfirm,
}) {
  const [value, setValue] = useState(initialValue);
  const [validationError, setValidationError] = useState('');

  useEffect(() => {
    if (open) {
      setValue(initialValue || '');
      setValidationError('');
    }
  }, [initialValue, open]);

  const handleClose = () => {
    if (!saving) {
      onClose?.();
    }
  };

  const handleConfirm = () => {
    const normalizedValue = value.trim();

    if (inputLabel && required && !normalizedValue) {
      setValidationError(`Bắt buộc nhập ${inputLabel.toLowerCase()}.`);
      return;
    }

    setValidationError('');
    onConfirm?.(inputLabel ? normalizedValue : null);
  };

  return (
    <Modal
      open={open}
      title={title}
      onClose={handleClose}
      error={error}
      footer={(
        <>
          <button
            type="button"
            className="secondary-button"
            onClick={handleClose}
            disabled={saving}
          >
            {cancelLabel}
          </button>

          <button
            type="button"
            className={danger ? 'danger-button' : 'primary-button'}
            onClick={handleConfirm}
            disabled={saving}
          >
            {saving ? 'Đang xử lý...' : confirmLabel}
          </button>
        </>
      )}
    >
      <div className="action-dialog-content">
        {message && <p className="action-dialog-message">{message}</p>}

        {inputLabel && (
          <label className="action-dialog-field">
            {inputLabel}{required ? ' *' : ''}
            <textarea
              rows="4"
              maxLength={maxLength}
              value={value}
              onChange={(event) => {
                setValue(event.target.value);
                if (validationError) {
                  setValidationError('');
                }
              }}
              placeholder={inputPlaceholder}
            />
          </label>
        )}

        {validationError && (
          <div className="error-box action-dialog-error" role="alert">
            {validationError}
          </div>
        )}
      </div>
    </Modal>
  );
}

export default ActionDialog;
