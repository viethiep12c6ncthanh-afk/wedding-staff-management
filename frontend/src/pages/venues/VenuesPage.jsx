import { useEffect, useMemo, useState } from 'react';

import {
  changeVenueStatus,
  createVenue,
  deactivateVenue,
  getVenues,
  updateVenue,
} from '../../api/venueApi';
import Modal from '../../components/common/Modal';
import StatusBadge from '../../components/common/StatusBadge';
import { getApiErrorMessage } from '../../utils/apiError';

const emptyForm = {
  name: '',
  address: '',
  contactName: '',
  contactPhone: '',
  note: '',
};

const getCurrentRole = () => {
  try {
    const raw = localStorage.getItem('currentUser');
    return raw ? JSON.parse(raw)?.role : null;
  } catch {
    return null;
  }
};

function VenuesPage() {
  const isAdmin = getCurrentRole() === 'ADMIN';

  const [venues, setVenues] = useState([]);
  const [keyword, setKeyword] = useState('');

  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');

  const [createOpen, setCreateOpen] = useState(false);
  const [form, setForm] = useState(emptyForm);
  const [editing, setEditing] = useState(null);

  const loadVenues = async () => {
    try {
      setLoading(true);
      setError('');
      setVenues(await getVenues());
    } catch (err) {
      setError(
        getApiErrorMessage(err, 'Không thể tải danh sách địa điểm.'),
      );
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadVenues();
  }, []);

  const filteredVenues = useMemo(() => {
    const q = keyword.trim().toLowerCase();

    if (!q) {
      return venues;
    }

    return venues.filter((venue) =>
      [
        venue.name,
        venue.address,
        venue.contactName,
        venue.contactPhone,
      ]
        .filter(Boolean)
        .some((value) => String(value).toLowerCase().includes(q)),
    );
  }, [venues, keyword]);

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
    setCreateOpen(true);
  };

  const openEdit = (venue) => {
    setEditing(venue);
    setForm({
      name: venue.name || '',
      address: venue.address || '',
      contactName: venue.contactName || '',
      contactPhone: venue.contactPhone || '',
      note: venue.note || '',
    });
    setCreateOpen(true);
  };

  const handleSubmit = async (event) => {
    event.preventDefault();

    try {
      setSaving(true);
      setError('');
      setNotice('');

      const payload = {
        ...form,
        contactName: form.contactName || null,
        contactPhone: form.contactPhone || null,
        note: form.note || null,
      };

      if (editing) {
        await updateVenue(editing.id, payload);
        setNotice('Đã cập nhật địa điểm.');
      } else {
        await createVenue(payload);
        setNotice('Đã tạo địa điểm.');
      }

      setCreateOpen(false);
      setEditing(null);
      setForm(emptyForm);
      await loadVenues();
    } catch (err) {
      setError(
        getApiErrorMessage(
          err,
          editing
            ? 'Không thể cập nhật địa điểm.'
            : 'Không thể tạo địa điểm.',
        ),
      );
    } finally {
      setSaving(false);
    }
  };

  const handleStatus = async (venue, status) => {
    try {
      setError('');
      setNotice('');
      await changeVenueStatus(venue.id, status);
      setNotice('Đã cập nhật trạng thái địa điểm.');
      await loadVenues();
    } catch (err) {
      setError(
        getApiErrorMessage(
          err,
          'Không thể cập nhật trạng thái địa điểm.',
        ),
      );
    }
  };

  const handleDeactivate = async (venue) => {
    if (!window.confirm(`Ngừng hoạt động địa điểm "${venue.name}"?`)) {
      return;
    }

    try {
      setError('');
      setNotice('');
      await deactivateVenue(venue.id);
      setNotice('Địa điểm đã chuyển sang ngừng hoạt động.');
      await loadVenues();
    } catch (err) {
      setError(
        getApiErrorMessage(err, 'Không thể ngừng hoạt động địa điểm.'),
      );
    }
  };

  return (
    <section>
      <div className="page-heading page-heading-actions">
        <div>
          <h1>Quản lý địa điểm</h1>
          <p>Quản lý nơi tổ chức tiệc cưới và sự kiện.</p>
        </div>

        <button
          type="button"
          className="primary-button"
          onClick={openCreate}
        >
          + Thêm địa điểm
        </button>
      </div>

      <div className="toolbar-card">
        <input
          className="control"
          type="search"
          value={keyword}
          onChange={(event) => setKeyword(event.target.value)}
          placeholder="Tìm tên, địa chỉ, liên hệ..."
        />
      </div>

      {notice && <div className="notice-box">{notice}</div>}
      {error && <div className="error-box">{error}</div>}

      <div className="table-card">
        {loading ? (
          <div className="table-state">Đang tải địa điểm...</div>
        ) : (
          <div className="table-scroll">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Tên địa điểm</th>
                  <th>Địa chỉ</th>
                  <th>Liên hệ</th>
                  <th>Trạng thái</th>
                  <th>Thao tác</th>
                </tr>
              </thead>

              <tbody>
                {filteredVenues.length === 0 ? (
                  <tr>
                    <td colSpan="5" className="empty-cell">
                      Chưa có địa điểm phù hợp.
                    </td>
                  </tr>
                ) : (
                  filteredVenues.map((venue) => (
                    <tr key={venue.id}>
                      <td>
                        <div className="cell-title">{venue.name}</div>
                        <div className="cell-subtitle">
                          {venue.note || 'Không có ghi chú'}
                        </div>
                      </td>

                      <td>{venue.address}</td>

                      <td>
                        <div>{venue.contactName || '—'}</div>
                        <div className="cell-subtitle">
                          {venue.contactPhone || '—'}
                        </div>
                      </td>

                      <td>
                        {isAdmin ? (
                          <select
                            className="compact-select"
                            value={venue.venueStatus}
                            onChange={(event) =>
                              handleStatus(venue, event.target.value)
                            }
                          >
                            <option value="ACTIVE">Hoạt động</option>
                            <option value="INACTIVE">Ngừng hoạt động</option>
                          </select>
                        ) : (
                          <StatusBadge value={venue.venueStatus} />
                        )}
                      </td>

                      <td>
                        <div className="row-actions">
                          <button
                            type="button"
                            className="secondary-button compact-button"
                            onClick={() => openEdit(venue)}
                          >
                            Sửa
                          </button>

                          {isAdmin && venue.venueStatus === 'ACTIVE' && (
                            <button
                              type="button"
                              className="danger-button compact-button"
                              onClick={() => handleDeactivate(venue)}
                            >
                              Ngừng
                            </button>
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
        open={createOpen}
        error={error}
        title={editing ? 'Cập nhật địa điểm' : 'Thêm địa điểm'}
        onClose={() => !saving && setCreateOpen(false)}
        footer={(
          <>
            <button
              type="button"
              className="secondary-button"
              onClick={() => setCreateOpen(false)}
              disabled={saving}
            >
              Hủy
            </button>

            <button
              type="submit"
              form="venue-form"
              className="primary-button"
              disabled={saving}
            >
              {saving ? 'Đang lưu...' : 'Lưu'}
            </button>
          </>
        )}
      >
        <form
          id="venue-form"
          className="form-grid form-grid-one"
          onSubmit={handleSubmit}
        >
          <label>
            Tên địa điểm *
            <input
              name="name"
              value={form.name}
              onChange={handleChange}
              required
              maxLength="150"
            />
          </label>

          <label>
            Địa chỉ *
            <input
              name="address"
              value={form.address}
              onChange={handleChange}
              required
              maxLength="300"
            />
          </label>

          <label>
            Người liên hệ
            <input
              name="contactName"
              value={form.contactName}
              onChange={handleChange}
              maxLength="120"
            />
          </label>

          <label>
            Điện thoại liên hệ
            <input
              name="contactPhone"
              value={form.contactPhone}
              onChange={handleChange}
              maxLength="20"
            />
          </label>

          <label>
            Ghi chú
            <textarea
              name="note"
              value={form.note}
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

export default VenuesPage;
