import { useEffect, useMemo, useState } from 'react';

import { getAssignments } from '../../api/assignmentApi';
import {
  confirmAttendance,
  createAttendance,
  getAttendances,
  getMyAttendances,
  updateAttendance,
} from '../../api/attendanceApi';
import Modal from '../../components/common/Modal';
import StatusBadge from '../../components/common/StatusBadge';
import { getApiErrorMessage } from '../../utils/apiError';
import {
  formatDateTime,
  formatMoney,
  toDateTimeLocalValue,
} from '../../utils/formatters';

const emptyForm = {
  assignmentId: '',
  checkInAt: '',
  checkOutAt: '',
  absent: false,
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

function AttendancePage() {
  const role = getCurrentRole();
  const isManager = ['ADMIN', 'COORDINATOR'].includes(role);

  const [attendances, setAttendances] = useState([]);
  const [assignments, setAssignments] = useState([]);

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

      if (isManager) {
        const [attendanceData, assignmentData] =
          await Promise.all([
            getAttendances(),
            getAssignments(),
          ]);

        setAttendances(attendanceData);
        setAssignments(assignmentData);
      } else {
        setAttendances(await getMyAttendances());
        setAssignments([]);
      }
    } catch (err) {
      setError(
        getApiErrorMessage(
          err,
          'Không thể tải dữ liệu chấm công.',
        ),
      );
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isManager]);

  const attendanceAssignmentIds = useMemo(
    () => new Set(attendances.map((item) => item.assignmentId)),
    [attendances],
  );

  const availableAssignments = useMemo(
    () =>
      assignments.filter(
        (assignment) =>
          ['ASSIGNED', 'CONFIRMED'].includes(assignment.status) &&
          !attendanceAssignmentIds.has(assignment.id),
      ),
    [assignments, attendanceAssignmentIds],
  );

  const handleChange = (event) => {
    const { name, value, type, checked } = event.target;

    setForm((previous) => ({
      ...previous,
      [name]: type === 'checkbox' ? checked : value,
    }));
  };

  const openCreate = () => {
    setEditing(null);
    setForm(emptyForm);
    setModalOpen(true);
  };

  const openEdit = (attendance) => {
    setEditing(attendance);
    setForm({
      assignmentId: String(attendance.assignmentId),
      checkInAt: toDateTimeLocalValue(attendance.checkInAt),
      checkOutAt: toDateTimeLocalValue(attendance.checkOutAt),
      absent: attendance.attendanceResult === 'ABSENT',
      note: attendance.note || '',
    });
    setModalOpen(true);
  };

  const handleSubmit = async (event) => {
    event.preventDefault();

    try {
      setSaving(true);
      setError('');
      setNotice('');

      const payload = {
        ...(editing
          ? {}
          : { assignmentId: Number(form.assignmentId) }),
        checkInAt: form.absent ? null : form.checkInAt || null,
        checkOutAt: form.absent ? null : form.checkOutAt || null,
        absent: form.absent,
        note: form.note || null,
      };

      if (editing) {
        await updateAttendance(editing.id, payload);
        setNotice('Đã cập nhật bản nháp chấm công.');
      } else {
        await createAttendance(payload);
        setNotice('Đã tạo bản nháp chấm công.');
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
            ? 'Không thể cập nhật chấm công.'
            : 'Không thể tạo chấm công.',
        ),
      );
    } finally {
      setSaving(false);
    }
  };

  const handleConfirm = async (attendance) => {
    if (!window.confirm('Xác nhận chấm công này? Sau đó sẽ không thể sửa.')) {
      return;
    }

    try {
      setSaving(true);
      setError('');
      setNotice('');

      await confirmAttendance(attendance.id);
      setNotice('Đã xác nhận chấm công và chốt tiền công.');
      await loadData();
    } catch (err) {
      setError(
        getApiErrorMessage(
          err,
          'Không thể xác nhận chấm công.',
        ),
      );
    } finally {
      setSaving(false);
    }
  };

  return (
    <section>
      <div className="page-heading page-heading-actions">
        <div>
          <h1>{isManager ? 'Chấm công' : 'Chấm công của tôi'}</h1>
          <p>
            {isManager
              ? 'Ghi nhận giờ vào/ra, vắng mặt và xác nhận kết quả.'
              : 'Theo dõi các bản chấm công của bạn.'}
          </p>
        </div>

        {isManager && (
          <button
            type="button"
            className="primary-button"
            onClick={openCreate}
          >
            + Ghi nhận chấm công
          </button>
        )}
      </div>

      {notice && <div className="notice-box">{notice}</div>}
      {error && <div className="error-box">{error}</div>}

      <div className="table-card">
        {loading ? (
          <div className="table-state">Đang tải chấm công...</div>
        ) : (
          <div className="table-scroll">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Ca làm</th>
                  {isManager && <th>Nhân viên</th>}
                  <th>Giờ vào</th>
                  <th>Giờ ra</th>
                  <th>Kết quả</th>
                  <th>Xử lý</th>
                  <th>Tiền công</th>
                  {isManager && <th>Thao tác</th>}
                </tr>
              </thead>

              <tbody>
                {attendances.length === 0 ? (
                  <tr>
                    <td
                      colSpan={isManager ? 8 : 6}
                      className="empty-cell"
                    >
                      Chưa có bản ghi chấm công.
                    </td>
                  </tr>
                ) : (
                  attendances.map((attendance) => (
                    <tr key={attendance.id}>
                      <td>
                        <div className="cell-title">
                          {attendance.shiftName}
                        </div>
                        <div className="cell-subtitle">
                          {formatDateTime(attendance.shiftStartAt)}
                          {' → '}
                          {formatDateTime(attendance.shiftEndAt)}
                        </div>
                      </td>

                      {isManager && (
                        <td>{attendance.employeeName}</td>
                      )}

                      <td>{formatDateTime(attendance.checkInAt)}</td>
                      <td>{formatDateTime(attendance.checkOutAt)}</td>

                      <td>
                        <StatusBadge
                          value={attendance.attendanceResult}
                        />
                        {(attendance.lateMinutes > 0 ||
                          attendance.earlyLeaveMinutes > 0) && (
                          <div className="cell-subtitle">
                            Trễ {attendance.lateMinutes || 0} phút ·
                            Về sớm {attendance.earlyLeaveMinutes || 0} phút
                          </div>
                        )}
                      </td>

                      <td>
                        <StatusBadge value={attendance.processStatus} />
                      </td>

                      <td>
                        {attendance.processStatus === 'CONFIRMED'
                          ? formatMoney(attendance.payableAmount)
                          : 'Chưa chốt'}
                      </td>

                      {isManager && (
                        <td>
                          <div className="row-actions row-actions-wrap">
                            {attendance.processStatus === 'DRAFT' && (
                              <>
                                <button
                                  type="button"
                                  className="secondary-button compact-button"
                                  onClick={() => openEdit(attendance)}
                                >
                                  Sửa
                                </button>

                                <button
                                  type="button"
                                  className="primary-button compact-button"
                                  disabled={saving}
                                  onClick={() =>
                                    handleConfirm(attendance)
                                  }
                                >
                                  Xác nhận
                                </button>
                              </>
                            )}
                          </div>
                        </td>
                      )}
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
        title={editing ? 'Cập nhật chấm công' : 'Ghi nhận chấm công'}
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
              form="attendance-form"
              className="primary-button"
              disabled={saving}
            >
              {saving ? 'Đang lưu...' : 'Lưu bản nháp'}
            </button>
          </>
        )}
      >
        <form
          id="attendance-form"
          className="form-grid"
          onSubmit={handleSubmit}
        >
          {!editing && (
            <label className="form-span-2">
              Phân công *
              <select
                name="assignmentId"
                value={form.assignmentId}
                onChange={handleChange}
                required
              >
                <option value="">Chọn phân công</option>

                {availableAssignments.map((assignment) => (
                  <option key={assignment.id} value={assignment.id}>
                    {assignment.shiftName}
                    {' — '}
                    {assignment.employeeName}
                    {' — '}
                    {assignment.shiftRole}
                  </option>
                ))}
              </select>
            </label>
          )}

          <label className="checkbox-label form-span-2">
            <input
              name="absent"
              type="checkbox"
              checked={form.absent}
              onChange={handleChange}
            />
            Nhân viên vắng mặt
          </label>

          {!form.absent && (
            <>
              <label>
                Giờ vào
                <input
                  name="checkInAt"
                  type="datetime-local"
                  value={form.checkInAt}
                  onChange={handleChange}
                />
              </label>

              <label>
                Giờ ra
                <input
                  name="checkOutAt"
                  type="datetime-local"
                  value={form.checkOutAt}
                  onChange={handleChange}
                />
              </label>
            </>
          )}

          <label className="form-span-2">
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

export default AttendancePage;
