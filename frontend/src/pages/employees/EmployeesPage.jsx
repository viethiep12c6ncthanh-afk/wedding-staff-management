import { useEffect, useMemo, useState } from 'react';

import {
  changeAccountStatus,
  changeEmployeeStatus,
  createEmployee,
  getEmployees,
  updateEmployee,
} from '../../api/employeeApi';
import Modal from '../../components/common/Modal';
import StatusBadge from '../../components/common/StatusBadge';
import { getApiErrorMessage } from '../../utils/apiError';

const emptyCreateForm = {
  username: '',
  temporaryPassword: '',
  fullName: '',
  email: '',
  phone: '',
  employeeCode: '',
  dateOfBirth: '',
  address: '',
  experienceLevel: '',
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

function EmployeesPage() {
  const role = getCurrentRole();
  const isAdmin = role === 'ADMIN';

  const [employees, setEmployees] = useState([]);
  const [keyword, setKeyword] = useState('');
  const [employmentStatus, setEmploymentStatus] = useState('');
  const [accountStatus, setAccountStatus] = useState('');

  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');

  const [createOpen, setCreateOpen] = useState(false);
  const [createForm, setCreateForm] = useState(emptyCreateForm);

  const [editing, setEditing] = useState(null);
  const [editForm, setEditForm] = useState(null);

  const query = useMemo(
    () => ({
      ...(keyword.trim() ? { keyword: keyword.trim() } : {}),
      ...(employmentStatus ? { employmentStatus } : {}),
      ...(accountStatus ? { accountStatus } : {}),
    }),
    [keyword, employmentStatus, accountStatus],
  );

  const loadEmployees = async () => {
    try {
      setLoading(true);
      setError('');
      const data = await getEmployees(query);
      setEmployees(data);
    } catch (err) {
      setError(
        getApiErrorMessage(
          err,
          'Không thể tải danh sách nhân viên.',
        ),
      );
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadEmployees();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [query]);

  const handleCreateChange = (event) => {
    const { name, value } = event.target;
    setCreateForm((previous) => ({
      ...previous,
      [name]: value,
    }));
  };

  const handleCreate = async (event) => {
    event.preventDefault();

    try {
      setSaving(true);
      setError('');
      setNotice('');

      await createEmployee({
        ...createForm,
        email: createForm.email || null,
        phone: createForm.phone || null,
        dateOfBirth: createForm.dateOfBirth || null,
        address: createForm.address || null,
        experienceLevel: createForm.experienceLevel || null,
        note: createForm.note || null,
      });

      setCreateOpen(false);
      setCreateForm(emptyCreateForm);
      setNotice('Đã tạo nhân viên và tài khoản.');
      await loadEmployees();
    } catch (err) {
      setError(
        getApiErrorMessage(
          err,
          'Không thể tạo nhân viên.',
        ),
      );
    } finally {
      setSaving(false);
    }
  };

  const openEdit = (employee) => {
    setEditing(employee);
    setEditForm({
      fullName: employee.fullName || '',
      email: employee.email || '',
      phone: employee.phone || '',
      dateOfBirth: employee.dateOfBirth || '',
      address: employee.address || '',
      experienceLevel: employee.experienceLevel || '',
      note: employee.note || '',
    });
  };

  const handleEditChange = (event) => {
    const { name, value } = event.target;
    setEditForm((previous) => ({
      ...previous,
      [name]: value,
    }));
  };

  const handleUpdate = async (event) => {
    event.preventDefault();

    try {
      setSaving(true);
      setError('');
      setNotice('');

      await updateEmployee(editing.id, {
        ...editForm,
        email: editForm.email || null,
        phone: editForm.phone || null,
        dateOfBirth: editForm.dateOfBirth || null,
        address: editForm.address || null,
        experienceLevel: editForm.experienceLevel || null,
        note: editForm.note || null,
      });

      setEditing(null);
      setEditForm(null);
      setNotice('Đã cập nhật hồ sơ nhân viên.');
      await loadEmployees();
    } catch (err) {
      setError(
        getApiErrorMessage(
          err,
          'Không thể cập nhật nhân viên.',
        ),
      );
    } finally {
      setSaving(false);
    }
  };

  const handleEmploymentStatus = async (employee, status) => {
    try {
      setError('');
      setNotice('');
      await changeEmployeeStatus(employee.id, status);
      setNotice('Đã cập nhật trạng thái làm việc.');
      await loadEmployees();
    } catch (err) {
      setError(
        getApiErrorMessage(
          err,
          'Không thể cập nhật trạng thái làm việc.',
        ),
      );
    }
  };

  const handleAccountStatus = async (employee, status) => {
    try {
      setError('');
      setNotice('');
      await changeAccountStatus(employee.id, status);
      setNotice('Đã cập nhật trạng thái tài khoản.');
      await loadEmployees();
    } catch (err) {
      setError(
        getApiErrorMessage(
          err,
          'Không thể cập nhật trạng thái tài khoản.',
        ),
      );
    }
  };

  return (
    <section>
      <div className="page-heading page-heading-actions">
        <div>
          <h1>Quản lý nhân viên</h1>
          <p>
            Quản lý hồ sơ, trạng thái làm việc và tài khoản nhân viên.
          </p>
        </div>

        <button
          type="button"
          className="primary-button"
          onClick={() => setCreateOpen(true)}
        >
          + Thêm nhân viên
        </button>
      </div>

      <div className="toolbar-card">
        <input
          className="control"
          type="search"
          value={keyword}
          onChange={(event) => setKeyword(event.target.value)}
          placeholder="Tìm mã, tên, username..."
        />

        <select
          className="control"
          value={employmentStatus}
          onChange={(event) => setEmploymentStatus(event.target.value)}
        >
          <option value="">Mọi trạng thái làm việc</option>
          <option value="ACTIVE">Hoạt động</option>
          <option value="ON_LEAVE">Tạm nghỉ</option>
          <option value="INACTIVE">Ngừng hoạt động</option>
        </select>

        <select
          className="control"
          value={accountStatus}
          onChange={(event) => setAccountStatus(event.target.value)}
        >
          <option value="">Mọi trạng thái tài khoản</option>
          <option value="ACTIVE">Hoạt động</option>
          <option value="INACTIVE">Ngừng hoạt động</option>
          <option value="LOCKED">Đã khóa</option>
        </select>
      </div>

      {notice && <div className="notice-box">{notice}</div>}
      {error && <div className="error-box">{error}</div>}

      <div className="table-card">
        {loading ? (
          <div className="table-state">Đang tải nhân viên...</div>
        ) : (
          <div className="table-scroll">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Mã NV</th>
                  <th>Họ tên</th>
                  <th>Tài khoản</th>
                  <th>Liên hệ</th>
                  <th>Làm việc</th>
                  <th>Tài khoản</th>
                  <th>Thao tác</th>
                </tr>
              </thead>

              <tbody>
                {employees.length === 0 ? (
                  <tr>
                    <td colSpan="7" className="empty-cell">
                      Không có nhân viên phù hợp.
                    </td>
                  </tr>
                ) : (
                  employees.map((employee) => (
                    <tr key={employee.id}>
                      <td>
                        <strong>{employee.employeeCode}</strong>
                      </td>

                      <td>
                        <div className="cell-title">
                          {employee.fullName}
                        </div>
                        <div className="cell-subtitle">
                          {employee.experienceLevel || 'Chưa cập nhật kinh nghiệm'}
                        </div>
                      </td>

                      <td>{employee.username}</td>

                      <td>
                        <div>{employee.phone || '—'}</div>
                        <div className="cell-subtitle">
                          {employee.email || '—'}
                        </div>
                      </td>

                      <td>
                        <select
                          className="compact-select"
                          value={employee.employmentStatus}
                          onChange={(event) =>
                            handleEmploymentStatus(
                              employee,
                              event.target.value,
                            )
                          }
                        >
                          <option value="ACTIVE">Hoạt động</option>
                          <option value="ON_LEAVE">Tạm nghỉ</option>
                          <option value="INACTIVE">Ngừng hoạt động</option>
                        </select>
                      </td>

                      <td>
                        {isAdmin ? (
                          <select
                            className="compact-select"
                            value={employee.accountStatus}
                            onChange={(event) =>
                              handleAccountStatus(
                                employee,
                                event.target.value,
                              )
                            }
                          >
                            <option value="ACTIVE">Hoạt động</option>
                            <option value="INACTIVE">Ngừng hoạt động</option>
                            <option value="LOCKED">Đã khóa</option>
                          </select>
                        ) : (
                          <StatusBadge value={employee.accountStatus} />
                        )}
                      </td>

                      <td>
                        <button
                          type="button"
                          className="secondary-button compact-button"
                          onClick={() => openEdit(employee)}
                        >
                          Sửa
                        </button>
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
        title="Thêm nhân viên"
        onClose={() => !saving && setCreateOpen(false)}
        wide
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
              form="create-employee-form"
              className="primary-button"
              disabled={saving}
            >
              {saving ? 'Đang lưu...' : 'Tạo nhân viên'}
            </button>
          </>
        )}
      >
        <form
          id="create-employee-form"
          className="form-grid"
          onSubmit={handleCreate}
        >
          <label>
            Username *
            <input
              name="username"
              value={createForm.username}
              onChange={handleCreateChange}
              required
              maxLength="50"
            />
          </label>

          <label>
            Mật khẩu tạm *
            <input
              name="temporaryPassword"
              type="password"
              value={createForm.temporaryPassword}
              onChange={handleCreateChange}
              required
              minLength="8"
              maxLength="72"
            />
          </label>

          <label>
            Mã nhân viên *
            <input
              name="employeeCode"
              value={createForm.employeeCode}
              onChange={handleCreateChange}
              required
              maxLength="30"
            />
          </label>

          <label>
            Họ tên *
            <input
              name="fullName"
              value={createForm.fullName}
              onChange={handleCreateChange}
              required
              maxLength="120"
            />
          </label>

          <label>
            Email
            <input
              name="email"
              type="email"
              value={createForm.email}
              onChange={handleCreateChange}
              maxLength="120"
            />
          </label>

          <label>
            Điện thoại
            <input
              name="phone"
              value={createForm.phone}
              onChange={handleCreateChange}
              maxLength="20"
            />
          </label>

          <label>
            Ngày sinh
            <input
              name="dateOfBirth"
              type="date"
              value={createForm.dateOfBirth}
              onChange={handleCreateChange}
            />
          </label>

          <label>
            Kinh nghiệm
            <input
              name="experienceLevel"
              value={createForm.experienceLevel}
              onChange={handleCreateChange}
              maxLength="50"
            />
          </label>

          <label className="form-span-2">
            Địa chỉ
            <input
              name="address"
              value={createForm.address}
              onChange={handleCreateChange}
              maxLength="255"
            />
          </label>

          <label className="form-span-2">
            Ghi chú
            <textarea
              name="note"
              value={createForm.note}
              onChange={handleCreateChange}
              maxLength="500"
              rows="3"
            />
          </label>
        </form>
      </Modal>

      <Modal
        open={Boolean(editing && editForm)}
        error={error}
        title={`Cập nhật ${editing?.employeeCode || ''}`}
        onClose={() => !saving && setEditing(null)}
        wide
        footer={(
          <>
            <button
              type="button"
              className="secondary-button"
              onClick={() => setEditing(null)}
              disabled={saving}
            >
              Hủy
            </button>

            <button
              type="submit"
              form="edit-employee-form"
              className="primary-button"
              disabled={saving}
            >
              {saving ? 'Đang lưu...' : 'Lưu thay đổi'}
            </button>
          </>
        )}
      >
        {editForm && (
          <form
            id="edit-employee-form"
            className="form-grid"
            onSubmit={handleUpdate}
          >
            <label className="form-span-2">
              Họ tên *
              <input
                name="fullName"
                value={editForm.fullName}
                onChange={handleEditChange}
                required
                maxLength="120"
              />
            </label>

            <label>
              Email
              <input
                name="email"
                type="email"
                value={editForm.email}
                onChange={handleEditChange}
                maxLength="120"
              />
            </label>

            <label>
              Điện thoại
              <input
                name="phone"
                value={editForm.phone}
                onChange={handleEditChange}
                maxLength="20"
              />
            </label>

            <label>
              Ngày sinh
              <input
                name="dateOfBirth"
                type="date"
                value={editForm.dateOfBirth}
                onChange={handleEditChange}
              />
            </label>

            <label>
              Kinh nghiệm
              <input
                name="experienceLevel"
                value={editForm.experienceLevel}
                onChange={handleEditChange}
                maxLength="50"
              />
            </label>

            <label className="form-span-2">
              Địa chỉ
              <input
                name="address"
                value={editForm.address}
                onChange={handleEditChange}
                maxLength="255"
              />
            </label>

            <label className="form-span-2">
              Ghi chú
              <textarea
                name="note"
                value={editForm.note}
                onChange={handleEditChange}
                maxLength="500"
                rows="3"
              />
            </label>
          </form>
        )}
      </Modal>
    </section>
  );
}

export default EmployeesPage;
