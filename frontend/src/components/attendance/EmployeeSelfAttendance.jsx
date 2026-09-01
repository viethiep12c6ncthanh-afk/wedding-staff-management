import { useState } from 'react';

import { selfCheckIn, selfCheckOut } from '../../api/qrAttendanceApi';
import StatusBadge from '../common/StatusBadge';
import { getApiErrorMessage } from '../../utils/apiError';
import { formatDateTime } from '../../utils/formatters';
import { parseAttendanceQrPayload } from '../../utils/qrAttendance';
import QrScanner from './QrScanner';
import '../../styles/attendance-qr.css';

const initialForm = {
  action: 'CHECK_IN',
  shiftId: '',
  method: 'QR',
  qrValue: '',
  otp: '',
};

const actionLabels = {
  CHECK_IN: 'Check-in',
  CHECK_OUT: 'Check-out',
};

function EmployeeSelfAttendance({ onCompleted }) {
  const [form, setForm] = useState(initialForm);
  const [location, setLocation] = useState(null);
  const [locating, setLocating] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [lastResult, setLastResult] = useState(null);

  const handleChange = (event) => {
    const { name, value } = event.target;

    setForm((previous) => ({
      ...previous,
      [name]: value,
    }));
  };

  const applyQrValue = (rawValue) => {
    const parsed = parseAttendanceQrPayload(rawValue);

    if (!parsed) {
      setError('Nội dung QR không hợp lệ.');
      return;
    }

    setError('');
    setForm((previous) => ({
      ...previous,
      method: 'QR',
      qrValue: parsed.qrToken,
      action: parsed.action || previous.action,
      shiftId: parsed.shiftId ? String(parsed.shiftId) : previous.shiftId,
    }));

    if (parsed.wrapped) {
      setNotice(
        `Đã đọc QR cho ca #${parsed.shiftId} · ${actionLabels[parsed.action]}.`,
      );
    } else {
      setNotice('Đã nhận token QR thô. Hãy kiểm tra lại mã ca và thao tác trước khi gửi.');
    }
  };

  const handleQrInput = (event) => {
    const value = event.target.value;
    setForm((previous) => ({ ...previous, qrValue: value }));
  };

  const parseQrInput = () => {
    applyQrValue(form.qrValue);
  };

  const getLocation = () => {
    setError('');

    if (!navigator.geolocation) {
      setError('Trình duyệt không hỗ trợ định vị GPS.');
      return;
    }

    setLocating(true);

    navigator.geolocation.getCurrentPosition(
      (position) => {
        setLocation({
          latitude: position.coords.latitude,
          longitude: position.coords.longitude,
          accuracy: position.coords.accuracy,
        });
        setLocating(false);
        setNotice('Đã lấy vị trí hiện tại cho lần chấm công này.');
      },
      () => {
        setLocating(false);
        setError('Không thể lấy vị trí. Hãy cấp quyền vị trí nếu phiên chấm công yêu cầu GPS.');
      },
      {
        enableHighAccuracy: true,
        timeout: 10000,
        maximumAge: 30000,
      },
    );
  };

  const clearLocation = () => {
    setLocation(null);
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError('');
    setNotice('');

    let effectiveAction = form.action;
    let effectiveShiftId = Number(form.shiftId);
    let qrToken = null;
    let otp = null;

    if (form.method === 'QR') {
      const parsed = parseAttendanceQrPayload(form.qrValue);

      if (!parsed?.qrToken) {
        setError('Hãy quét hoặc nhập QR hợp lệ.');
        return;
      }

      qrToken = parsed.qrToken;
      effectiveAction = parsed.action || form.action;
      effectiveShiftId = parsed.shiftId || effectiveShiftId;
    } else {
      otp = String(form.otp || '').trim();

      if (!/^\d{6}$/.test(otp)) {
        setError('OTP phải gồm đúng 6 chữ số.');
        return;
      }
    }

    if (!Number.isInteger(effectiveShiftId) || effectiveShiftId <= 0) {
      setError('Mã ca không hợp lệ.');
      return;
    }

    const payload = {
      shiftId: effectiveShiftId,
      qrToken,
      otp,
      latitude: location?.latitude ?? null,
      longitude: location?.longitude ?? null,
    };

    try {
      setSaving(true);

      const result = effectiveAction === 'CHECK_IN'
        ? await selfCheckIn(payload)
        : await selfCheckOut(payload);

      setLastResult(result);
      setNotice(
        `${actionLabels[result.action]} thành công bằng ${result.method}${result.gpsVerified ? ' · GPS hợp lệ' : ''}.`,
      );
      setForm((previous) => ({
        ...previous,
        qrValue: '',
        otp: '',
      }));
      setLocation(null);

      await onCompleted?.(result);
    } catch (err) {
      setError(
        getApiErrorMessage(
          err,
          'Không thể chấm công. Kiểm tra QR/OTP, thời gian ca và yêu cầu GPS.',
        ),
      );
    } finally {
      setSaving(false);
    }
  };

  return (
    <section className="attendance-feature-section">
      <div className="attendance-feature-heading">
        <div>
          <h2>Check-in / Check-out</h2>
          <p>
            Quét QR hoặc dùng OTP dự phòng. GPS chỉ được gửi khi bạn chủ động lấy vị trí cho lần thao tác hiện tại.
          </p>
        </div>
      </div>

      {notice && <div className="notice-box">{notice}</div>}
      {error && <div className="error-box">{error}</div>}

      <div className="attendance-employee-grid">
        <form className="attendance-self-form" onSubmit={handleSubmit}>
          <div className="attendance-choice-grid">
            <label>
              Thao tác *
              <select name="action" value={form.action} onChange={handleChange}>
                <option value="CHECK_IN">Check-in</option>
                <option value="CHECK_OUT">Check-out</option>
              </select>
            </label>

            <label>
              Mã ca (Shift ID) *
              <input
                name="shiftId"
                type="number"
                min="1"
                value={form.shiftId}
                onChange={handleChange}
                required
              />
            </label>
          </div>

          <div className="attendance-method-tabs" role="tablist" aria-label="Phương thức chấm công">
            <button
              type="button"
              className={form.method === 'QR' ? 'active' : ''}
              onClick={() => setForm((previous) => ({ ...previous, method: 'QR' }))}
            >
              QR
            </button>
            <button
              type="button"
              className={form.method === 'OTP' ? 'active' : ''}
              onClick={() => setForm((previous) => ({ ...previous, method: 'OTP' }))}
            >
              OTP
            </button>
          </div>

          {form.method === 'QR' ? (
            <div className="attendance-method-panel">
              <QrScanner disabled={saving} onDetected={applyQrValue} />

              <label>
                Nội dung QR / token
                <textarea
                  name="qrValue"
                  rows="3"
                  maxLength="500"
                  value={form.qrValue}
                  onChange={handleQrInput}
                  placeholder="Quét camera hoặc dán nội dung QR..."
                />
              </label>

              <button
                type="button"
                className="secondary-button"
                onClick={parseQrInput}
                disabled={!form.qrValue.trim()}
              >
                Đọc nội dung QR
              </button>
            </div>
          ) : (
            <div className="attendance-method-panel">
              <label>
                OTP 6 chữ số *
                <input
                  name="otp"
                  inputMode="numeric"
                  pattern="[0-9]{6}"
                  maxLength="6"
                  value={form.otp}
                  onChange={(event) => {
                    const value = event.target.value.replace(/\D/g, '').slice(0, 6);
                    setForm((previous) => ({ ...previous, otp: value }));
                  }}
                  placeholder="000000"
                />
              </label>
            </div>
          )}

          <div className="attendance-location-card">
            <div>
              <strong>Vị trí cho lần thao tác này</strong>
              {location ? (
                <span>
                  {location.latitude.toFixed(6)}, {location.longitude.toFixed(6)} · sai số ~{Math.round(location.accuracy)} m
                </span>
              ) : (
                <span>Chưa gửi vị trí. Nếu phiên yêu cầu GPS, hãy lấy vị trí trước khi chấm công.</span>
              )}
            </div>

            <div className="row-actions row-actions-wrap">
              <button
                type="button"
                className="secondary-button"
                onClick={getLocation}
                disabled={locating || saving}
              >
                {locating ? 'Đang lấy vị trí...' : 'Lấy vị trí'}
              </button>

              {location && (
                <button
                  type="button"
                  className="secondary-button"
                  onClick={clearLocation}
                  disabled={saving}
                >
                  Bỏ vị trí
                </button>
              )}
            </div>
          </div>

          <button type="submit" className="primary-button" disabled={saving}>
            {saving ? 'Đang gửi...' : actionLabels[form.action]}
          </button>
        </form>

        <div className="attendance-result-panel">
          {!lastResult ? (
            <div className="attendance-empty-panel">
              <strong>Chưa có kết quả trong phiên này</strong>
              <span>Lịch sử đã ghi nhận vẫn hiển thị ở bảng phía dưới.</span>
            </div>
          ) : (
            <>
              <div className="attendance-result-title">
                <strong>{actionLabels[lastResult.action]} thành công</strong>
                <span>{formatDateTime(lastResult.occurredAt)}</span>
              </div>

              <dl className="detail-list">
                <div>
                  <dt>Phương thức</dt>
                  <dd>{lastResult.method}</dd>
                </div>
                <div>
                  <dt>GPS</dt>
                  <dd>
                    {lastResult.gpsVerified
                      ? `Đã xác minh · ${lastResult.distanceMeters ?? 0} m`
                      : 'Không yêu cầu / không xác minh'}
                  </dd>
                </div>
                <div>
                  <dt>Ca</dt>
                  <dd>#{lastResult.attendance?.shiftId} · {lastResult.attendance?.shiftName}</dd>
                </div>
                <div>
                  <dt>Xử lý</dt>
                  <dd><StatusBadge value={lastResult.attendance?.processStatus} /></dd>
                </div>
                <div>
                  <dt>Kết quả</dt>
                  <dd><StatusBadge value={lastResult.attendance?.attendanceResult} /></dd>
                </div>
              </dl>
            </>
          )}
        </div>
      </div>
    </section>
  );
}

export default EmployeeSelfAttendance;
