import { useEffect, useMemo, useState } from 'react';

import {
  cancelAssignment,
  createDirectAssignment,
  getAssignments,
} from '../../api/assignmentApi';
import { getEmployees } from '../../api/employeeApi';
import { getShifts } from '../../api/shiftApi';
import Modal from '../../components/common/Modal';
import StatusBadge from '../../components/common/StatusBadge';
import { getApiErrorMessage } from '../../utils/apiError';
import { formatDateTime } from '../../utils/formatters';

const emptyForm = {
  shiftId: '',
  employeeId: '',
  shiftRole: 'STAFF',
  area: '',
  task: '',
};

function AssignmentsPage() {
  const [assignments, setAssignments] = useState([]);
  const [shifts, setShifts] = useState([]);
  const [employees, setEmployees] = useState([]);

  const [keyword, setKeyword] = useState('');
  const [statusFilter, setStatusFilter] = useState('');

  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');

  const [modalOpen, setModalOpen] = useState(false);
  const [form, setForm] = useState(emptyForm);

  const loadData = async () => {
    try {
      setLoading(true);
      setError('');

      const [assignmentData, shiftData, employeeData] =
        await Promise.all([
          getAssignments(),
          getShifts(),
          getEmployees({
            employmentStatus: 'ACTIVE',
            accountStatus: 'ACTIVE',
          }),
        ]);

      setAssignments(assignmentData);
      setShifts(shiftData);
      setEmployees(employeeData);
    } catch (err) {
      setError(
        getApiErrorMessage(
          err,
          'Không thể tải dữ liệu phân công.',
        ),
      );
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const assignableShifts = useMemo(
    () =>
      shifts.filter((shift) =>
        ['OPEN', 'CLOSED'].includes(shift.shiftStatus),
      ),
    [shifts],
  );

  const filteredAssignments = useMemo(() => {
    const q = keyword.trim().toLowerCase();

    return assignments.filter((assignment) => {
      const matchKeyword =
        !q ||
        [
          assignment.shiftName,
          assignment.employeeName,
          assignment.area,
          assignment.task,
        ]
          .filter(Boolean)
          .some((value) =>
            String(value).toLowerCase().includes(q),
          );

      const matchStatus =
        !statusFilter || assignment.status === statusFilter;

      return matchKeyword && matchStatus;
    });
  }, [assignments, keyword, statusFilter]);

  const handleChange = (event) => {
    const { name, value } = event.target;

    setForm((previous) => ({
      ...previous,
      [name]: value,
    }));
  };

  const handleSubmit = async (event) => {
    event.preventDefault();

    try {
      setSaving(true);
      setError('');
      setNotice('');

      await createDirectAssignment({
        shiftId: Number(form.shiftId),
        employeeId: Number(form.employeeId),
        shiftRole: form.shiftRole,
        area: form.area || null,
        task: form.task || null,
      });

      setModalOpen(false);
      setForm(emptyForm);
      setNotice('Đã phân công trực tiếp nhân viên vào ca.');
      await loadData();
    } catch (err) {
      setError(
        getApiErrorMessage(err, 'Không thể tạo phân công.'),
      );
    } finally {
      setSaving(false);
    }
  };

  const handleCancel = async (assignment) => {
    const reason = window.prompt('Nhập lý do hủy phân công:');

    if (reason === null) {
      return;
    }

    if (!reason.trim()) {
      setError('Bắt buộc nhập lý do hủy phân công.');
      return;
    }

    try {
      setSaving(true);
      setError('');
      setNotice('');

      await cancelAssignment(assignment.id, reason.trim());
      setNotice('Đã hủy phân công.');
      await loadData();
    } catch (err) {
      setError(
        getApiErrorMessage(err, 'Không thể hủy phân công.'),
      );
    } finally {
      setSaving(false);
    }
  };

  return (
    <section>
      <div className="page-heading page-heading-actions">
        <div>
          <h1>Phân công</h1>
          <p>
            Theo dõi nhân sự trong ca và phân công trực tiếp khi cần.
          </p>
        </div>

        <button
          type="button"
          className="primary-button"
          onClick={() => setModalOpen(true)}
        >
          + Phân công trực tiếp
        </button>
      </div>

      <div className="toolbar-card">
        <input
          className="control"
          type="search"
          value={keyword}
          onChange={(event) => setKeyword(event.target.value)}
          placeholder="Tìm ca, nhân viên, khu vực..."
        />

        <select
          className="control"
          value={statusFilter}
          onChange={(event) => setStatusFilter(event.target.value)}
        >
          <option value="">Mọi trạng thái</option>
          <option value="ASSIGNED">Đã phân công</option>
          <option value="CONFIRMED">Đã xác nhận</option>
          <option value="COMPLETED">Hoàn thành</option>
          <option value="ABSENT">Vắng mặt</option>
          <option value="CANCELLED">Đã hủy</option>
        </select>
      </div>

      {notice && <div className="notice-box">{notice}</div>}
      {error && <div className="error-box">{error}</div>}

      <div className="table-card">
        {loading ? (
          <div className="table-state">Đang tải phân công...</div>
        ) : (
          <div className="table-scroll">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Ca làm</th>
                  <th>Nhân viên</th>
                  <th>Nguồn</th>
                  <th>Vai trò</th>
                  <th>Khu vực / nhiệm vụ</th>
                  <th>Trạng thái</th>
                  <th>Thao tác</th>
                </tr>
              </thead>

              <tbody>
                {filteredAssignments.length === 0 ? (
                  <tr>
                    <td colSpan="7" className="empty-cell">
                      Chưa có phân công phù hợp.
                    </td>
                  </tr>
                ) : (
                  filteredAssignments.map((assignment) => (
                    <tr key={assignment.id}>
                      <td>
                        <div className="cell-title">
                          {assignment.shiftName}
                        </div>
                        <div className="cell-subtitle">
                          #{assignment.shiftId}
                        </div>
                      </td>

                      <td>{assignment.employeeName}</td>

                      <td>
                        {assignment.assignmentSource === 'DIRECT'
                          ? 'Trực tiếp'
                          : assignment.assignmentSource === 'REPLACEMENT'
                            ? 'Thay ca'
                            : 'Từ đăng ký'}
                      </td>

                      <td>
                        {assignment.shiftRole === 'LEADER'
                          ? 'Trưởng ca'
                          : 'Nhân viên'}
                      </td>

                      <td>
                        <div>{assignment.area || '—'}</div>
                        <div className="cell-subtitle">
                          {assignment.task || 'Không có nhiệm vụ riêng'}
                        </div>
                      </td>

                      <td>
                        <StatusBadge value={assignment.status} />
                      </td>

                      <td>
                        {['ASSIGNED', 'CONFIRMED'].includes(
                          assignment.status,
                        ) ? (
                          <button
                            type="button"
                            className="danger-button compact-button"
                            disabled={saving}
                            onClick={() => handleCancel(assignment)}
                          >
                            Hủy
                          </button>
                        ) : (
                          <span className="muted-text">
                            {assignment.cancelledAt
                              ? formatDateTime(assignment.cancelledAt)
                              : '—'}
                          </span>
                        )}
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
        error={error}
        title="Phân công trực tiếp"
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
              form="direct-assignment-form"
              className="primary-button"
              disabled={saving}
            >
              {saving ? 'Đang lưu...' : 'Tạo phân công'}
            </button>
          </>
        )}
      >
        <form
          id="direct-assignment-form"
          className="form-grid"
          onSubmit={handleSubmit}
        >
          <label>
            Ca làm *
            <select
              name="shiftId"
              value={form.shiftId}
              onChange={handleChange}
              required
            >
              <option value="">Chọn ca</option>
              {assignableShifts.map((shift) => (
                <option key={shift.id} value={shift.id}>
                  {shift.name} — {shift.eventName}
                </option>
              ))}
            </select>
          </label>

          <label>
            Nhân viên *
            <select
              name="employeeId"
              value={form.employeeId}
              onChange={handleChange}
              required
            >
              <option value="">Chọn nhân viên</option>
              {employees.map((employee) => (
                <option key={employee.id} value={employee.id}>
                  {employee.employeeCode} — {employee.fullName}
                </option>
              ))}
            </select>
          </label>

          <label>
            Vai trò *
            <select
              name="shiftRole"
              value={form.shiftRole}
              onChange={handleChange}
              required
            >
              <option value="STAFF">Nhân viên</option>
              <option value="LEADER">Trưởng ca</option>
            </select>
          </label>

          <label>
            Khu vực
            <input
              name="area"
              value={form.area}
              onChange={handleChange}
              maxLength="100"
            />
          </label>

          <label className="form-span-2">
            Nhiệm vụ
            <textarea
              name="task"
              value={form.task}
              onChange={handleChange}
              maxLength="300"
              rows="3"
            />
          </label>
        </form>
      </Modal>
    </section>
  );
}

export default AssignmentsPage;
