import { useEffect, useMemo, useState } from 'react';

import {
  changeEventStatus,
  createEvent,
  getEvents,
  updateEvent,
} from '../../api/eventApi';
import { getVenues } from '../../api/venueApi';
import Modal from '../../components/common/Modal';
import StatusBadge from '../../components/common/StatusBadge';
import { getApiErrorMessage } from '../../utils/apiError';
import {
  formatDateTime,
  toDateTimeLocalValue,
} from '../../utils/formatters';

const emptyForm = {
  venueId: '',
  name: '',
  startAt: '',
  endAt: '',
  description: '',
};

const eventTransitions = {
  DRAFT: ['CONFIRMED', 'CANCELLED'],
  CONFIRMED: ['IN_PROGRESS', 'CANCELLED'],
  IN_PROGRESS: ['COMPLETED', 'CANCELLED'],
  COMPLETED: [],
  CANCELLED: [],
};

const transitionLabels = {
  CONFIRMED: 'Xác nhận',
  IN_PROGRESS: 'Bắt đầu',
  COMPLETED: 'Hoàn thành',
  CANCELLED: 'Hủy',
};

function EventsPage() {
  const [events, setEvents] = useState([]);
  const [venues, setVenues] = useState([]);
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

      const [eventData, venueData] = await Promise.all([
        getEvents(),
        getVenues(),
      ]);

      setEvents(eventData);
      setVenues(venueData);
    } catch (err) {
      setError(
        getApiErrorMessage(err, 'Không thể tải dữ liệu sự kiện.'),
      );
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const usableVenues = useMemo(
    () => venues.filter((venue) => venue.venueStatus === 'ACTIVE'),
    [venues],
  );

  const filteredEvents = useMemo(() => {
    const q = keyword.trim().toLowerCase();

    return events.filter((event) => {
      const matchKeyword =
        !q ||
        [event.name, event.venueName]
          .filter(Boolean)
          .some((value) => String(value).toLowerCase().includes(q));

      const matchStatus =
        !statusFilter || event.eventStatus === statusFilter;

      return matchKeyword && matchStatus;
    });
  }, [events, keyword, statusFilter]);

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

  const openEdit = (event) => {
    setEditing(event);
    setForm({
      venueId: String(event.venueId),
      name: event.name || '',
      startAt: toDateTimeLocalValue(event.startAt),
      endAt: toDateTimeLocalValue(event.endAt),
      description: event.description || '',
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
        venueId: Number(form.venueId),
        name: form.name,
        startAt: form.startAt,
        endAt: form.endAt,
        description: form.description || null,
      };

      if (editing) {
        await updateEvent(editing.id, payload);
        setNotice('Đã cập nhật sự kiện.');
      } else {
        await createEvent(payload);
        setNotice('Đã tạo sự kiện ở trạng thái nháp.');
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
            ? 'Không thể cập nhật sự kiện.'
            : 'Không thể tạo sự kiện.',
        ),
      );
    } finally {
      setSaving(false);
    }
  };

  const handleTransition = async (event, target) => {
    let reason = null;

    if (target === 'CANCELLED') {
      reason = window.prompt('Nhập lý do hủy sự kiện:');

      if (reason === null) {
        return;
      }

      if (!reason.trim()) {
        setError('Bắt buộc nhập lý do hủy sự kiện.');
        return;
      }
    }

    try {
      setError('');
      setNotice('');

      await changeEventStatus(
        event.id,
        target,
        reason?.trim() || null,
      );

      setNotice(`Đã cập nhật trạng thái sự kiện: ${transitionLabels[target]}.`);
      await loadData();
    } catch (err) {
      setError(
        getApiErrorMessage(
          err,
          'Không thể chuyển trạng thái sự kiện.',
        ),
      );
    }
  };

  return (
    <section>
      <div className="page-heading page-heading-actions">
        <div>
          <h1>Quản lý sự kiện</h1>
          <p>Quản lý tiệc cưới, sự kiện và vòng đời thực hiện.</p>
        </div>

        <button
          type="button"
          className="primary-button"
          onClick={openCreate}
        >
          + Thêm sự kiện
        </button>
      </div>

      <div className="toolbar-card">
        <input
          className="control"
          type="search"
          value={keyword}
          onChange={(event) => setKeyword(event.target.value)}
          placeholder="Tìm tên sự kiện hoặc địa điểm..."
        />

        <select
          className="control"
          value={statusFilter}
          onChange={(event) => setStatusFilter(event.target.value)}
        >
          <option value="">Mọi trạng thái</option>
          <option value="DRAFT">Nháp</option>
          <option value="CONFIRMED">Đã xác nhận</option>
          <option value="IN_PROGRESS">Đang diễn ra</option>
          <option value="COMPLETED">Hoàn thành</option>
          <option value="CANCELLED">Đã hủy</option>
        </select>
      </div>

      {notice && <div className="notice-box">{notice}</div>}
      {error && <div className="error-box">{error}</div>}

      <div className="table-card">
        {loading ? (
          <div className="table-state">Đang tải sự kiện...</div>
        ) : (
          <div className="table-scroll">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Sự kiện</th>
                  <th>Địa điểm</th>
                  <th>Thời gian</th>
                  <th>Trạng thái</th>
                  <th>Thao tác</th>
                </tr>
              </thead>

              <tbody>
                {filteredEvents.length === 0 ? (
                  <tr>
                    <td colSpan="5" className="empty-cell">
                      Chưa có sự kiện phù hợp.
                    </td>
                  </tr>
                ) : (
                  filteredEvents.map((event) => (
                    <tr key={event.id}>
                      <td>
                        <div className="cell-title">{event.name}</div>
                        <div className="cell-subtitle">
                          {event.description || 'Không có mô tả'}
                        </div>
                      </td>

                      <td>{event.venueName}</td>

                      <td>
                        <div>{formatDateTime(event.startAt)}</div>
                        <div className="cell-subtitle">
                          đến {formatDateTime(event.endAt)}
                        </div>
                      </td>

                      <td>
                        <StatusBadge value={event.eventStatus} />
                      </td>

                      <td>
                        <div className="row-actions row-actions-wrap">
                          {!['COMPLETED', 'CANCELLED'].includes(
                            event.eventStatus,
                          ) && (
                            <button
                              type="button"
                              className="secondary-button compact-button"
                              onClick={() => openEdit(event)}
                            >
                              Sửa
                            </button>
                          )}

                          {eventTransitions[event.eventStatus]?.map(
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
                                  handleTransition(event, target)
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
        title={editing ? 'Cập nhật sự kiện' : 'Thêm sự kiện'}
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
              form="event-form"
              className="primary-button"
              disabled={saving}
            >
              {saving ? 'Đang lưu...' : 'Lưu'}
            </button>
          </>
        )}
      >
        <form
          id="event-form"
          className="form-grid"
          onSubmit={handleSubmit}
        >
          <label>
            Địa điểm *
            <select
              name="venueId"
              value={form.venueId}
              onChange={handleChange}
              required
            >
              <option value="">Chọn địa điểm</option>
              {usableVenues.map((venue) => (
                <option key={venue.id} value={venue.id}>
                  {venue.name}
                </option>
              ))}
            </select>
          </label>

          <label>
            Tên sự kiện *
            <input
              name="name"
              value={form.name}
              onChange={handleChange}
              required
              maxLength="150"
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

          <label className="form-span-2">
            Mô tả
            <textarea
              name="description"
              value={form.description}
              onChange={handleChange}
              maxLength="1000"
              rows="4"
            />
          </label>
        </form>
      </Modal>
    </section>
  );
}

export default EventsPage;
