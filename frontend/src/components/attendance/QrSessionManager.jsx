import { useEffect, useMemo, useState } from 'react';

import {
  createAttendanceCheckSession,
  revokeAttendanceCheckSession,
} from '../../api/qrAttendanceApi';
import { getShifts } from '../../api/shiftApi';
import ActionDialog from '../common/ActionDialog';
import { getApiErrorMessage } from '../../utils/apiError';
import { formatDateTime } from '../../utils/formatters';
import { buildAttendanceQrPayload } from '../../utils/qrAttendance';
import QrCodeImage from './QrCodeImage';
import '../../styles/attendance-qr.css';

const initialForm = {
  shiftId: '',
  action: 'CHECK_IN',
  validMinutes: 10,
  gpsRequired: false,
  latitude: '',
  longitude: '',
  radiusMeters: 150,
};

const actionLabels = {
  CHECK_IN: 'Check-in',
  CHECK_OUT: 'Check-out',
};

const isWithinActionWindow = (shift, action, now = Date.now()) => {
  const start = new Date(shift.startAt).getTime();
  const end = new Date(shift.endAt).getTime();

  if (!Number.isFinite(start) || !Number.isFinite(end)) {
    return false;
  }

  if (action === 'CHECK_IN') {
    return now >= start - 120 * 60 * 1000 && now <= end;
  }

  return now >= start && now <= end + 240 * 60 * 1000;
};

function QrSessionManager() {
  const [shifts, setShifts] = useState([]);
  const [form, setForm] = useState(initialForm);
  const [session, setSession] = useState(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [locating, setLocating] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [revokeOpen, setRevokeOpen] = useState(false);

  useEffect(() => {
    let active = true;

    getShifts()
      .then((data) => {
        if (active) {
          setShifts(data);
        }
      })
      .catch((err) => {
        if (active) {
          setError(getApiErrorMessage(err, 'Không thể tải danh sách ca làm.'));
        }
      })
      .finally(() => {
        if (active) {
          setLoading(false);
        }
      });

    return () => {
      active = false;
    };
  }, []);

  const availableShifts = useMemo(
    () =>
      shifts.filter(
        (shift) =>
          ['OPEN', 'CLOSED', 'IN_PROGRESS'].includes(shift.shiftStatus) &&
          isWithinActionWindow(shift, form.action),
      ),
    [form.action, shifts],
  );

  useEffect(() => {
    if (
      form.shiftId &&
      !availableShifts.some((shift) => String(shift.id) === String(form.shiftId))
    ) {
      setForm((previous) => ({ ...previous, shiftId: '' }));
    }
  }, [availableShifts, form.shiftId]);

  const qrPayload = session
    ? buildAttendanceQrPayload({
        shiftId: session.shiftId,
        action: session.action,
        qrToken: session.qrToken,
      })
    : '';

  const handleChange = (event) => {
    const { name, value, checked, type } = event.target;

    setForm((previous) => ({
      ...previous,
      [name]: type === 'checkbox' ? checked : value,
    }));
  };

  const useCurrentLocation = () => {
    setError('');

    if (!navigator.geolocation) {
      setError('Trình duyệt không hỗ trợ định vị GPS.');
      return;
    }

    setLocating(true);

    navigator.geolocation.getCurrentPosition(
      (position) => {
        setForm((previous) => ({
          ...previous,
          latitude: position.coords.latitude.toFixed(6),
          longitude: position.coords.longitude.toFixed(6),
        }));
        setLocating(false);
      },
      () => {
        setLocating(false);
        setError('Không thể lấy vị trí hiện tại. Hãy kiểm tra quyền vị trí của trình duyệt.');
      },
      {
        enableHighAccuracy: true,
        timeout: 10000,
        maximumAge: 30000,
      },
    );
  };

  const handleCreate = async (event) => {
    event.preventDefault();
    setError('');
    setNotice('');

    if (form.gpsRequired && (!form.latitude || !form.longitude)) {
      setError('Phiên yêu cầu GPS phải có đủ vĩ độ và kinh độ trung tâm.');
      return;
    }

    try {
      setSaving(true);

      const created = await createAttendanceCheckSession({
        shiftId: Number(form.shiftId),
        action: form.action,
        validMinutes: Number(form.validMinutes),
        latitude: form.gpsRequired ? Number(form.latitude) : null,
        longitude: form.gpsRequired ? Number(form.longitude) : null,
        radiusMeters: form.gpsRequired ? Number(form.radiusMeters) : null,
      });

      setSession(created);
      setNotice(
        `Đã tạo phiên ${actionLabels[created.action]}. Mã QR và OTP chỉ được giữ trong phiên màn hình này.`,
      );
    } catch (err) {
      setError(getApiErrorMessage(err, 'Không thể tạo phiên chấm công.'));
    } finally {
      setSaving(false);
    }
  };

  const handleRevoke = async () => {
    if (!session) {
      return;
    }

    try {
      setSaving(true);
      setError('');
      setNotice('');
      await revokeAttendanceCheckSession(session.sessionId);
      setSession(null);
      setRevokeOpen(false);
      setNotice('Đã thu hồi phiên chấm công.');
    } catch (err) {
      setError(getApiErrorMessage(err, 'Không thể thu hồi phiên chấm công.'));
    } finally {
      setSaving(false);
    }
  };

  const copyQrPayload = async () => {
    try {
      await navigator.clipboard.writeText(qrPayload);
      setNotice('Đã sao chép nội dung QR. Dùng chức năng này cho kiểm thử hoặc thiết bị không có camera.');
    } catch {
      setError('Không thể sao chép nội dung QR vào clipboard.');
    }
  };

  return (
    <section className="attendance-feature-section">
      <div className="attendance-feature-heading">
        <div>
          <h2>Tạo phiên QR / OTP</h2>
          <p>
            Tạo mã ngắn hạn cho check-in hoặc check-out. Phiên mới cùng ca và cùng thao tác sẽ làm phiên cũ mất hiệu lực.
          </p>
        </div>
      </div>

      {notice && <div className="notice-box">{notice}</div>}
      {error && <div className="error-box">{error}</div>}

      <div className="attendance-manager-grid">
        <form className="attendance-session-form" onSubmit={handleCreate}>
          <label>
            Thao tác *
            <select name="action" value={form.action} onChange={handleChange}>
              <option value="CHECK_IN">Check-in</option>
              <option value="CHECK_OUT">Check-out</option>
            </select>
          </label>

          <label>
            Ca làm *
            <select
              name="shiftId"
              value={form.shiftId}
              onChange={handleChange}
              required
              disabled={loading}
            >
              <option value="">Chọn ca đang trong cửa sổ chấm công</option>
              {availableShifts.map((shift) => (
                <option key={shift.id} value={shift.id}>
                  #{shift.id} — {shift.name} — {shift.eventName}
                </option>
              ))}
            </select>
          </label>

          <label>
            Hiệu lực (phút) *
            <input
              name="validMinutes"
              type="number"
              min="1"
              max="30"
              value={form.validMinutes}
              onChange={handleChange}
              required
            />
          </label>

          <label className="checkbox-label attendance-gps-toggle">
            <input
              name="gpsRequired"
              type="checkbox"
              checked={form.gpsRequired}
              onChange={handleChange}
            />
            Yêu cầu GPS trong bán kính
          </label>

          {form.gpsRequired && (
            <div className="attendance-gps-fields">
              <div className="attendance-location-row">
                <button
                  type="button"
                  className="secondary-button"
                  onClick={useCurrentLocation}
                  disabled={locating}
                >
                  {locating ? 'Đang lấy vị trí...' : 'Dùng vị trí hiện tại'}
                </button>
                <span className="muted-text">
                  GPS chỉ dùng để kiểm tra bán kính tại thời điểm thao tác, không theo dõi liên tục.
                </span>
              </div>

              <label>
                Vĩ độ *
                <input
                  name="latitude"
                  type="number"
                  step="0.000001"
                  min="-90"
                  max="90"
                  value={form.latitude}
                  onChange={handleChange}
                  required
                />
              </label>

              <label>
                Kinh độ *
                <input
                  name="longitude"
                  type="number"
                  step="0.000001"
                  min="-180"
                  max="180"
                  value={form.longitude}
                  onChange={handleChange}
                  required
                />
              </label>

              <label>
                Bán kính (m) *
                <input
                  name="radiusMeters"
                  type="number"
                  min="20"
                  max="1000"
                  value={form.radiusMeters}
                  onChange={handleChange}
                  required
                />
              </label>
            </div>
          )}

          <button
            type="submit"
            className="primary-button"
            disabled={saving || loading || availableShifts.length === 0}
          >
            {saving ? 'Đang tạo...' : 'Tạo phiên chấm công'}
          </button>

          {!loading && availableShifts.length === 0 && (
            <div className="muted-text">
              Không có ca nào đang nằm trong cửa sổ {actionLabels[form.action]} theo đồng hồ trình duyệt.
            </div>
          )}
        </form>

        <div className="attendance-session-preview">
          {!session ? (
            <div className="attendance-empty-panel">
              <strong>Chưa có phiên đang hiển thị</strong>
              <span>Tạo phiên để nhận QR và OTP dùng một lần theo thời gian hiệu lực.</span>
            </div>
          ) : (
            <>
              <div className="attendance-session-meta">
                <div>
                  <span>Ca</span>
                  <strong>#{session.shiftId} — {session.shiftName}</strong>
                </div>
                <div>
                  <span>Thao tác</span>
                  <strong>{actionLabels[session.action]}</strong>
                </div>
                <div>
                  <span>Hết hạn</span>
                  <strong>{formatDateTime(session.expiresAt)}</strong>
                </div>
                <div>
                  <span>GPS</span>
                  <strong>
                    {session.gpsRequired
                      ? `Bắt buộc · ${session.radiusMeters} m`
                      : 'Không bắt buộc'}
                  </strong>
                </div>
              </div>

              <div className="attendance-qr-box">
                <QrCodeImage value={qrPayload} />
              </div>

              <div className="attendance-otp-box">
                <span>OTP dự phòng</span>
                <strong>{session.otp}</strong>
              </div>

              <div className="row-actions row-actions-wrap attendance-session-actions">
                <button
                  type="button"
                  className="secondary-button"
                  onClick={copyQrPayload}
                >
                  Sao chép nội dung QR
                </button>
                <button
                  type="button"
                  className="danger-button"
                  onClick={() => setRevokeOpen(true)}
                  disabled={saving}
                >
                  Thu hồi phiên
                </button>
              </div>

              <p className="muted-text attendance-secret-note">
                Không lưu QR token hoặc OTP vào localStorage. Sau khi tải lại trang, hãy tạo phiên mới nếu cần; backend sẽ thu hồi phiên cũ cùng ca/thao tác khi tạo phiên mới.
              </p>
            </>
          )}
        </div>
      </div>

      <ActionDialog
        open={revokeOpen}
        title="Thu hồi phiên chấm công"
        message={session
          ? `Thu hồi phiên ${actionLabels[session.action]} của ca #${session.shiftId}? QR và OTP hiện tại sẽ mất hiệu lực ngay.`
          : ''}
        danger
        confirmLabel="Thu hồi phiên"
        saving={saving}
        error={error}
        onClose={() => setRevokeOpen(false)}
        onConfirm={handleRevoke}
      />
    </section>
  );
}

export default QrSessionManager;
