import { useEffect, useMemo, useState } from 'react';

import { getEvents } from '../../api/eventApi';
import {
  changeShiftStatus,
  createShift,
  getShifts,
  updateShift,
} from '../../api/shiftApi';
import Modal from '../../components/common/Modal';
import StatusBadge from '../../components/common/StatusBadge';
import { getApiErrorMessage } from '../../utils/apiError';
import {
  formatDateTime,
  formatMoney,
  toDateTimeLocalValue,
} from '../../utils/formatters';

const emptyForm = {
  eventId: '',
  name: '',
  startAt: '',
  endAt: '',
  requiredStaff: 1,
  payAmount: 0,
  registrationDeadline: '',
  description: '',
};

const shiftTransitions = {
  DRAFT: ['OPEN', 'CANCELLED'],
  OPEN: ['CLOSED', 'CANCELLED'],
  CLOSED: ['IN_PROGRESS', 'CANCELLED'],
  IN_PROGRESS: ['COMPLETED', 'CANCELLED'],
  COMPLETED: [],
  CANCELLED: [],
};

const transitionLabels = {
  OPEN: 'Mở đăng ký',
  CLOSED: 'Đóng đăng ký',
  IN_PROGRESS: 'Bắt đầu',
  COMPLETED: 'Hoàn thành',
  CANCELLED: 'Hủy',
};

function ShiftsPage() {
  const [shifts, setShifts] = useState([]);
  const [events, setEvents] = useState([]);
  const [keyword, setKeyword] = useState('');
  const [statusFilter, setStatusFilter] = useState('');

  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');

  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState(null);
  const [form, setForm] = useState(emptyForm);

  const loadData = async () => {
    try {
      setLoading(true);
      setError('');

      const [shiftData, eventData] = await Promise.all([
        getShifts(),
        getEvents(),
      ]);

      setShifts(shiftData);
      setEvents(eventData);
    } catch (err) {
      setError(
        getApiErrorMessage(err, 'Không thể tải dữ liệu ca làm.'),
      );
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const usableEvents = useMemo(
    () =>
      events.filter(
        (event) =>
          !['COMPLETED', 'CANCELLED'].includes(event.eventStatus),
      ),
    [events],
  );

  const filteredShifts = useMemo(() => {
    const q = keyword.trim().toLowerCase();

    return shifts.filter((shift) => {
      const matchKeyword =
        !q ||
        [shift.name, shift.eventName, shift.venueName]
          .filter(Boolean)
          .some((value) => String(value).toLowerCase().includes(q));

      const matchStatus =
        !statusFilter || shift.shiftStatus === statusFilter;

      return matchKeyword && matchStatus;
    });
  }, [shifts, keyword, statusFilter]);

  const handleChange = (event) => {
    const { name, value } = event.target;

    setForm((previous) => ({
      ...previous,
      [name]: value,
    }));
  };

  const openCreate = () => {
    setEditing(null);
    setForm(emptyForm);
    setModalOpen(true);
  };

  const openEdit = (shift) => {
    setEditing(shift);
    setForm({
      eventId: String(shift.eventId),
      name: shift.name || '',
      startAt: toDateTimeLocalValue(shift.startAt),
      endAt: toDateTimeLocalValue(shift.endAt),
      requiredStaff: shift.requiredStaff ?? 1,
      payAmount: shift.payAmount ?? 0,
      registrationDeadline: toDateTimeLocalValue(
        shift.registrationDeadline,
      ),
      description: shift.description || '',
    });
    setModalOpen(true);
  };

  const handleSubmit = async (submitEvent) => {
    submitEvent.preventDefault();

    try {
      setSaving(true);
      setError('');
      setNotice('');

      const payload = {
        eventId: Number(form.eventId),
        name: form.name,
        startAt: form.startAt,
        endAt: form.endAt,
        requiredStaff: Number(form.requiredStaff),
        payAmount: Number(form.payAmount),
        registrationDeadline: form.registrationDeadline || null,
        description: form.description || null,
      };

      if (editing) {
        await updateShift(editing.id, payload);
        setNotice('Đã cập nhật ca làm.');
      } else {
        await createShift(payload);
        setNotice('Đã tạo ca làm ở trạng thái nháp.');
      }

      setModalOpen(false);
      setEditing(null);
      setForm(emptyForm);
      await loadData();
    } catch (err) {
      setError(
        getApiErrorMessage(
          err,
          editing
            ? 'Không thể cập nhật ca làm.'
            : 'Không thể tạo ca làm.',
        ),
      );
    } finally {
      setSaving(false);
    }
  };

  const handleTransition = async (shift, target) => {
    let reason = null;

    if (target === 'CANCELLED') {
      reason = window.prompt('Nhập lý do hủy ca:');

      if (reason === null) {
        return;
      }

      if (!reason.trim()) {
        setError('Bắt buộc nhập lý do hủy ca.');
        return;
      }
    }

    try {
      setError('');
      setNotice('');

      await changeShiftStatus(
        shift.id,
        target,
        reason?.trim() || null,
      );

      setNotice(`Đã cập nhật trạng thái ca: ${transitionLabels[target]}.`);
      await loadData();
    } catch (err) {
      setError(
        getApiErrorMessage(
          err,
          'Không thể chuyển trạng thái ca.',
        ),
      );
    }
  };

  return (
    <section>
      <div className="page-heading page-heading-actions">
        <div>
          <h1>Quản lý ca làm</h1>
          <p>
            Tạo ca, mở đăng ký và theo dõi vòng đời ca làm.
          </p>
        </div>

        <button
          type="button"
          className="primary-button"
          onClick={openCreate}
        >
          + Thêm ca làm
        </button>
      </div>

      <div className="toolbar-card">
        <input
          className="control"
          type="search"
          value={keyword}
          onChange={(event) => setKeyword(event.target.value)}
          placeholder="Tìm ca, sự kiện hoặc địa điểm..."
        />

        <select
          className="control"
          value={statusFilter}
          onChange={(event) => setStatusFilter(event.target.value)}
        >
          <option value="">Mọi trạng thái</option>
          <option value="DRAFT">Nháp</option>
          <option value="OPEN">Đang mở</option>
          <option value="CLOSED">Đã đóng</option>
          <option value="IN_PROGRESS">Đang diễn ra</option>
          <option value="COMPLETED">Hoàn thành</option>
          <option value="CANCELLED">Đã hủy</option>
        </select>
      </div>

      {notice && <div className="notice-box">{notice}</div>}
      {error && <div className="error-box">{error}</div>}

      <div className="table-card">
        {loading ? (
          <div className="table-state">Đang tải ca làm...</div>
        ) : (
          <div className="table-scroll">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Ca làm</th>
                  <th>Sự kiện</th>
                  <th>Thời gian</th>
                  <th>Nhân sự</th>
                  <th>Tiền công</th>
                  <th>Trạng thái</th>
                  <th>Thao tác</th>
                </tr>
              </thead>

              <tbody>
                {filteredShifts.length === 0 ? (
                  <tr>
                    <td colSpan="7" className="empty-cell">
                      Chưa có ca làm phù hợp.
                    </td>
                  </tr>
                ) : (
                  filteredShifts.map((shift) => (
                    <tr key={shift.id}>
                      <td>
                        <div className="cell-title">{shift.name}</div>
                        <div className="cell-subtitle">
                          {shift.venueName}
                        </div>
                      </td>

                      <td>{shift.eventName}</td>

                      <td>
                        <div>{formatDateTime(shift.startAt)}</div>
                        <div className="cell-subtitle">
                          đến {formatDateTime(shift.endAt)}
                        </div>
                      </td>

                      <td>{shift.requiredStaff}</td>

                      <td>{formatMoney(shift.payAmount)}</td>

                      <td>
                        <StatusBadge value={shift.shiftStatus} />
                      </td>

                      <td>
                        <div className="row-actions row-actions-wrap">
                          {!['IN_PROGRESS', 'COMPLETED', 'CANCELLED'].includes(
                            shift.shiftStatus,
                          ) && (
                            <button
                              type="button"
                              className="secondary-button compact-button"
                              onClick={() => openEdit(shift)}
                            >
                              Sửa
                            </button>
                          )}

                          {shiftTransitions[shift.shiftStatus]?.map(
                            (target) => (
                              <button
                                key={target}
                                type="button"
                                className={
                                  target === 'CANCELLED'
                                    ? 'danger-button compact-button'
                                    : 'secondary-button compact-button'
                                }
                                onClick={() =>
                                  handleTransition(shift, target)
                                }
                              >
                                {transitionLabels[target]}
                              </button>
                            ),
                          )}
                        </div>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        )}
      </div>

      <Modal
        open={modalOpen}
        title={editing ? 'Cập nhật ca làm' : 'Thêm ca làm'}
        onClose={() => !saving && setModalOpen(false)}
        wide
        footer={(
          <>
            <button
              type="button"
              className="secondary-button"
              onClick={() => setModalOpen(false)}
              disabled={saving}
            >
              Hủy
            </button>

            <button
              type="submit"
              form="shift-form"
              className="primary-button"
              disabled={saving}
            >
              {saving ? 'Đang lưu...' : 'Lưu'}
            </button>
          </>
        )}
      >
        <form
          id="shift-form"
          className="form-grid"
          onSubmit={handleSubmit}
        >
          <label>
            Sự kiện *
            <select
              name="eventId"
              value={form.eventId}
              onChange={handleChange}
              required
            >
              <option value="">Chọn sự kiện</option>
              {usableEvents.map((event) => (
                <option key={event.id} value={event.id}>
                  {event.name} — {event.venueName}
                </option>
              ))}
            </select>
          </label>

          <label>
            Tên ca *
            <input
              name="name"
              value={form.name}
              onChange={handleChange}
              required
              maxLength="120"
            />
          </label>

          <label>
            Bắt đầu *
            <input
              name="startAt"
              type="datetime-local"
              value={form.startAt}
              onChange={handleChange}
              required
            />
          </label>

          <label>
            Kết thúc *
            <input
              name="endAt"
              type="datetime-local"
              value={form.endAt}
              onChange={handleChange}
              required
            />
          </label>

          <label>
            Số nhân viên cần *
            <input
              name="requiredStaff"
              type="number"
              min="1"
              value={form.requiredStaff}
              onChange={handleChange}
              required
            />
          </label>

          <label>
            Tiền công / ca *
            <input
              name="payAmount"
              type="number"
              min="0"
              step="1000"
              value={form.payAmount}
              onChange={handleChange}
              required
            />
          </label>

          <label className="form-span-2">
            Hạn đăng ký
            <input
              name="registrationDeadline"
              type="datetime-local"
              value={form.registrationDeadline}
              onChange={handleChange}
            />
          </label>

          <label className="form-span-2">
            Mô tả
            <textarea
              name="description"
              value={form.description}
              onChange={handleChange}
              maxLength="500"
              rows="3"
            />
          </label>
        </form>
      </Modal>
    </section>
  );
}

export default ShiftsPage;
