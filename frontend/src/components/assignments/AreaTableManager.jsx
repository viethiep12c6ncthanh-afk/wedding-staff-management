import { useEffect, useMemo, useState } from 'react';

import { updateAssignmentPlacement } from '../../api/assignmentApi';
import {
  changeShiftAreaStatus,
  changeShiftTableStatus,
  createShiftArea,
  createShiftTable,
  getShiftAreas,
  updateShiftArea,
  updateShiftTable,
} from '../../api/placementApi';
import { getApiErrorMessage } from '../../utils/apiError';
import '../../styles/assignment-placement.css';

const emptyAreaForm = {
  name: '',
  requiredStaff: '',
  description: '',
};

const emptyTableForm = {
  tableCode: '',
  displayName: '',
  note: '',
};

const emptyPlacementForm = {
  assignmentId: '',
  areaId: '',
  tableIds: [],
  task: '',
};

function AreaTableManager({
  shifts,
  assignments,
  onChanged,
}) {
  const editableShifts = useMemo(
    () =>
      shifts.filter(
        (shift) =>
          !['COMPLETED', 'CANCELLED'].includes(shift.shiftStatus),
      ),
    [shifts],
  );

  const [shiftId, setShiftId] = useState('');
  const [areas, setAreas] = useState([]);
  const [selectedAreaId, setSelectedAreaId] = useState('');

  const [areaForm, setAreaForm] = useState(emptyAreaForm);
  const [tableForm, setTableForm] = useState(emptyTableForm);
  const [placementForm, setPlacementForm] =
    useState(emptyPlacementForm);

  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');

  useEffect(() => {
    if (!shiftId && editableShifts.length > 0) {
      setShiftId(String(editableShifts[0].id));
    }
  }, [editableShifts, shiftId]);

  const loadAreas = async (targetShiftId = shiftId) => {
    if (!targetShiftId) {
      setAreas([]);
      return;
    }

    try {
      setLoading(true);
      setError('');
      const data = await getShiftAreas(Number(targetShiftId));
      setAreas(data);

      setSelectedAreaId((current) => {
        if (
          current &&
          data.some((area) => String(area.id) === String(current))
        ) {
          return current;
        }

        const firstActive =
          data.find((area) => area.status === 'ACTIVE') || data[0];
        return firstActive ? String(firstActive.id) : '';
      });
    } catch (err) {
      setError(
        getApiErrorMessage(
          err,
          'Không thể tải cấu hình khu vực và bàn.',
        ),
      );
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    setPlacementForm(emptyPlacementForm);
    setSelectedAreaId('');
    setNotice('');
    setError('');
    loadAreas(shiftId);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [shiftId]);

  const selectedArea = useMemo(
    () =>
      areas.find(
        (area) => String(area.id) === String(selectedAreaId),
      ) || null,
    [areas, selectedAreaId],
  );

  const activeAreas = useMemo(
    () => areas.filter((area) => area.status === 'ACTIVE'),
    [areas],
  );

  const shiftAssignments = useMemo(
    () =>
      assignments.filter(
        (assignment) =>
          String(assignment.shiftId) === String(shiftId) &&
          ['ASSIGNED', 'CONFIRMED'].includes(assignment.status),
      ),
    [assignments, shiftId],
  );

  const placementArea = useMemo(
    () =>
      activeAreas.find(
        (area) =>
          String(area.id) === String(placementForm.areaId),
      ) || null,
    [activeAreas, placementForm.areaId],
  );

  const handleCreateArea = async (event) => {
    event.preventDefault();

    try {
      setSaving(true);
      setError('');
      setNotice('');

      await createShiftArea({
        shiftId: Number(shiftId),
        name: areaForm.name.trim(),
        requiredStaff: areaForm.requiredStaff
          ? Number(areaForm.requiredStaff)
          : null,
        description: areaForm.description.trim() || null,
      });

      setAreaForm(emptyAreaForm);
      setNotice('Đã tạo khu vực cho ca.');
      await loadAreas();
    } catch (err) {
      setError(
        getApiErrorMessage(err, 'Không thể tạo khu vực.'),
      );
    } finally {
      setSaving(false);
    }
  };

  const handleEditArea = async (area) => {
    const name = window.prompt('Tên khu vực:', area.name);
    if (name === null) {
      return;
    }

    const capacity = window.prompt(
      'Số nhân viên tối đa của khu vực (để trống nếu không đặt giới hạn):',
      area.requiredStaff ?? '',
    );
    if (capacity === null) {
      return;
    }

    const description = window.prompt(
      'Mô tả khu vực:',
      area.description || '',
    );
    if (description === null) {
      return;
    }

    try {
      setSaving(true);
      setError('');
      setNotice('');

      await updateShiftArea(area.id, {
        name: name.trim(),
        requiredStaff: capacity.trim()
          ? Number(capacity)
          : null,
        description: description.trim() || null,
      });

      setNotice('Đã cập nhật khu vực.');
      await loadAreas();
      await onChanged?.();
    } catch (err) {
      setError(
        getApiErrorMessage(err, 'Không thể cập nhật khu vực.'),
      );
    } finally {
      setSaving(false);
    }
  };

  const handleToggleArea = async (area) => {
    const target =
      area.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';

    try {
      setSaving(true);
      setError('');
      setNotice('');
      await changeShiftAreaStatus(area.id, target);
      setNotice(
        target === 'ACTIVE'
          ? 'Đã kích hoạt khu vực.'
          : 'Đã ngừng sử dụng khu vực.',
      );
      await loadAreas();
      await onChanged?.();
    } catch (err) {
      setError(
        getApiErrorMessage(
          err,
          'Không thể thay đổi trạng thái khu vực.',
        ),
      );
    } finally {
      setSaving(false);
    }
  };

  const handleCreateTable = async (event) => {
    event.preventDefault();

    if (!selectedAreaId) {
      setError('Hãy chọn khu vực trước khi tạo bàn.');
      return;
    }

    try {
      setSaving(true);
      setError('');
      setNotice('');

      await createShiftTable({
        areaId: Number(selectedAreaId),
        tableCode: tableForm.tableCode.trim(),
        displayName: tableForm.displayName.trim() || null,
        note: tableForm.note.trim() || null,
      });

      setTableForm(emptyTableForm);
      setNotice('Đã thêm bàn.');
      await loadAreas();
    } catch (err) {
      setError(
        getApiErrorMessage(err, 'Không thể tạo bàn.'),
      );
    } finally {
      setSaving(false);
    }
  };

  const handleEditTable = async (tableItem) => {
    const tableCode = window.prompt(
      'Mã bàn:',
      tableItem.tableCode,
    );
    if (tableCode === null) {
      return;
    }

    const displayName = window.prompt(
      'Tên hiển thị:',
      tableItem.displayName || '',
    );
    if (displayName === null) {
      return;
    }

    const note = window.prompt(
      'Ghi chú:',
      tableItem.note || '',
    );
    if (note === null) {
      return;
    }

    try {
      setSaving(true);
      setError('');
      setNotice('');

      await updateShiftTable(tableItem.id, {
        tableCode: tableCode.trim(),
        displayName: displayName.trim() || null,
        note: note.trim() || null,
      });

      setNotice('Đã cập nhật bàn.');
      await loadAreas();
      await onChanged?.();
    } catch (err) {
      setError(
        getApiErrorMessage(err, 'Không thể cập nhật bàn.'),
      );
    } finally {
      setSaving(false);
    }
  };

  const handleToggleTable = async (tableItem) => {
    const target =
      tableItem.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';

    try {
      setSaving(true);
      setError('');
      setNotice('');
      await changeShiftTableStatus(tableItem.id, target);
      setNotice(
        target === 'ACTIVE'
          ? 'Đã kích hoạt bàn.'
          : 'Đã ngừng sử dụng bàn.',
      );
      await loadAreas();
      await onChanged?.();
    } catch (err) {
      setError(
        getApiErrorMessage(
          err,
          'Không thể thay đổi trạng thái bàn.',
        ),
      );
    } finally {
      setSaving(false);
    }
  };

  const handleAssignmentChange = (event) => {
    const nextId = event.target.value;
    const assignment = shiftAssignments.find(
      (item) => String(item.id) === String(nextId),
    );

    setPlacementForm({
      assignmentId: nextId,
      areaId: assignment?.areaId
        ? String(assignment.areaId)
        : '',
      tableIds: assignment?.tableIds || [],
      task: assignment?.task || '',
    });
  };

  const handlePlacementAreaChange = (event) => {
    setPlacementForm((previous) => ({
      ...previous,
      areaId: event.target.value,
      tableIds: [],
    }));
  };

  const handleTableSelection = (tableId) => {
    setPlacementForm((previous) => {
      const selected = new Set(previous.tableIds.map(Number));

      if (selected.has(Number(tableId))) {
        selected.delete(Number(tableId));
      } else {
        selected.add(Number(tableId));
      }

      return {
        ...previous,
        tableIds: Array.from(selected),
      };
    });
  };

  const handleSavePlacement = async (event) => {
    event.preventDefault();

    if (!placementForm.assignmentId) {
      setError('Hãy chọn nhân viên cần phân khu vực/bàn.');
      return;
    }

    try {
      setSaving(true);
      setError('');
      setNotice('');

      await updateAssignmentPlacement(
        Number(placementForm.assignmentId),
        {
          areaId: placementForm.areaId
            ? Number(placementForm.areaId)
            : null,
          tableIds: placementForm.areaId
            ? placementForm.tableIds.map(Number)
            : [],
          task: placementForm.task.trim() || null,
        },
      );

      setNotice('Đã cập nhật khu vực/bàn cho nhân viên.');
      await onChanged?.();
      await loadAreas();
    } catch (err) {
      setError(
        getApiErrorMessage(
          err,
          'Không thể cập nhật khu vực/bàn.',
        ),
      );
    } finally {
      setSaving(false);
    }
  };

  if (editableShifts.length === 0) {
    return (
      <section className="placement-panel">
        <div className="placement-heading">
          <div>
            <h2>Khu vực & bàn</h2>
            <p>Chưa có ca có thể cấu hình khu vực/bàn.</p>
          </div>
        </div>
      </section>
    );
  }

  return (
    <section className="placement-panel">
      <div className="placement-heading">
        <div>
          <h2>Khu vực & bàn</h2>
          <p>
            Cấu hình khu vực, danh sách bàn và phân vị trí cho
            nhân viên trong ca.
          </p>
        </div>

        <label className="placement-shift-select">
          Ca làm
          <select
            value={shiftId}
            onChange={(event) => setShiftId(event.target.value)}
          >
            {editableShifts.map((shift) => (
              <option key={shift.id} value={shift.id}>
                {shift.name} — {shift.eventName}
              </option>
            ))}
          </select>
        </label>
      </div>

      {notice && <div className="notice-box">{notice}</div>}
      {error && <div className="error-box">{error}</div>}

      <div className="placement-grid">
        <div className="placement-card">
          <div className="placement-card-title">
            <div>
              <strong>Khu vực</strong>
              <span>
                {loading
                  ? 'Đang tải...'
                  : `${areas.length} khu vực`}
              </span>
            </div>
          </div>

          <form
            className="placement-inline-form"
            onSubmit={handleCreateArea}
          >
            <input
              value={areaForm.name}
              onChange={(event) =>
                setAreaForm((previous) => ({
                  ...previous,
                  name: event.target.value,
                }))
              }
              placeholder="Tên khu vực"
              maxLength="100"
              required
            />
            <input
              type="number"
              min="1"
              value={areaForm.requiredStaff}
              onChange={(event) =>
                setAreaForm((previous) => ({
                  ...previous,
                  requiredStaff: event.target.value,
                }))
              }
              placeholder="Số NV tối đa"
            />
            <input
              value={areaForm.description}
              onChange={(event) =>
                setAreaForm((previous) => ({
                  ...previous,
                  description: event.target.value,
                }))
              }
              placeholder="Mô tả"
              maxLength="500"
            />
            <button
              type="submit"
              className="primary-button compact-button"
              disabled={saving || !shiftId}
            >
              + Khu vực
            </button>
          </form>

          <div className="placement-item-list">
            {areas.length === 0 ? (
              <div className="placement-empty">
                Chưa có khu vực.
              </div>
            ) : (
              areas.map((area) => (
                <div
                  key={area.id}
                  className={`placement-area-row ${
                    String(area.id) === String(selectedAreaId)
                      ? 'active'
                      : ''
                  }`}
                >
                  <button
                    type="button"
                    className="placement-area-main-button"
                    onClick={() =>
                      setSelectedAreaId(String(area.id))
                    }
                  >
                    <strong>{area.name}</strong>
                    <small>
                      {area.assignedStaffCount}
                      {area.requiredStaff
                        ? ` / ${area.requiredStaff}`
                        : ''}{' '}
                      nhân viên · {area.tables.length} bàn
                    </small>
                  </button>

                  <span className="placement-row-actions">
                    <span
                      className={`placement-status ${area.status.toLowerCase()}`}
                    >
                      {area.status === 'ACTIVE'
                        ? 'Hoạt động'
                        : 'Ngừng'}
                    </span>

                    <button
                      type="button"
                      className="placement-link-action"
                      disabled={saving}
                      onClick={() => handleEditArea(area)}
                    >
                      Sửa
                    </button>

                    <button
                      type="button"
                      className="placement-link-action"
                      disabled={saving}
                      onClick={() => handleToggleArea(area)}
                    >
                      {area.status === 'ACTIVE'
                        ? 'Ngừng'
                        : 'Bật'}
                    </button>
                  </span>
                </div>
              ))
            )}
          </div>
        </div>

        <div className="placement-card">
          <div className="placement-card-title">
            <div>
              <strong>Bàn</strong>
              <span>
                {selectedArea
                  ? `Thuộc ${selectedArea.name}`
                  : 'Chọn khu vực'}
              </span>
            </div>
          </div>

          <form
            className="placement-inline-form"
            onSubmit={handleCreateTable}
          >
            <input
              value={tableForm.tableCode}
              onChange={(event) =>
                setTableForm((previous) => ({
                  ...previous,
                  tableCode: event.target.value,
                }))
              }
              placeholder="Mã bàn, VD: A01"
              maxLength="30"
              required
            />
            <input
              value={tableForm.displayName}
              onChange={(event) =>
                setTableForm((previous) => ({
                  ...previous,
                  displayName: event.target.value,
                }))
              }
              placeholder="Tên hiển thị"
              maxLength="100"
            />
            <input
              value={tableForm.note}
              onChange={(event) =>
                setTableForm((previous) => ({
                  ...previous,
                  note: event.target.value,
                }))
              }
              placeholder="Ghi chú"
              maxLength="300"
            />
            <button
              type="submit"
              className="primary-button compact-button"
              disabled={
                saving ||
                !selectedArea ||
                selectedArea.status !== 'ACTIVE'
              }
            >
              + Bàn
            </button>
          </form>

          <div className="placement-item-list">
            {!selectedArea ? (
              <div className="placement-empty">
                Chọn một khu vực để quản lý bàn.
              </div>
            ) : selectedArea.tables.length === 0 ? (
              <div className="placement-empty">
                Khu vực chưa có bàn.
              </div>
            ) : (
              selectedArea.tables.map((tableItem) => (
                <div
                  className="placement-table-row"
                  key={tableItem.id}
                >
                  <span>
                    <strong>{tableItem.tableCode}</strong>
                    <small>
                      {tableItem.displayName ||
                        tableItem.note ||
                        'Không có ghi chú'}
                    </small>
                  </span>

                  <span className="placement-row-actions">
                    <span
                      className={`placement-status ${tableItem.status.toLowerCase()}`}
                    >
                      {tableItem.status === 'ACTIVE'
                        ? 'Hoạt động'
                        : 'Ngừng'}
                    </span>
                    <button
                      type="button"
                      className="placement-link-action"
                      disabled={saving}
                      onClick={() =>
                        handleEditTable(tableItem)
                      }
                    >
                      Sửa
                    </button>
                    <button
                      type="button"
                      className="placement-link-action"
                      disabled={saving}
                      onClick={() =>
                        handleToggleTable(tableItem)
                      }
                    >
                      {tableItem.status === 'ACTIVE'
                        ? 'Ngừng'
                        : 'Bật'}
                    </button>
                  </span>
                </div>
              ))
            )}
          </div>
        </div>
      </div>

      <div className="placement-card placement-assignment-card">
        <div className="placement-card-title">
          <div>
            <strong>Phân khu vực / bàn cho nhân viên</strong>
            <span>
              Một nhân viên thuộc tối đa một khu vực nhưng có
              thể phụ trách nhiều bàn.
            </span>
          </div>
        </div>

        <form
          className="placement-assignment-form"
          onSubmit={handleSavePlacement}
        >
          <label>
            Nhân viên *
            <select
              value={placementForm.assignmentId}
              onChange={handleAssignmentChange}
              required
            >
              <option value="">Chọn phân công</option>
              {shiftAssignments.map((assignment) => (
                <option
                  key={assignment.id}
                  value={assignment.id}
                >
                  {assignment.employeeName} ·{' '}
                  {assignment.shiftRole === 'LEADER'
                    ? 'Trưởng ca'
                    : 'Nhân viên'}
                </option>
              ))}
            </select>
          </label>

          <label>
            Khu vực
            <select
              value={placementForm.areaId}
              onChange={handlePlacementAreaChange}
            >
              <option value="">
                Chưa phân khu vực
              </option>
              {activeAreas.map((area) => (
                <option key={area.id} value={area.id}>
                  {area.name}
                  {area.requiredStaff
                    ? ` (${area.assignedStaffCount}/${area.requiredStaff})`
                    : ''}
                </option>
              ))}
            </select>
          </label>

          <div className="placement-table-picker">
            <span className="placement-field-label">
              Bàn phụ trách
            </span>

            {!placementArea ? (
              <span className="muted-text">
                Chọn khu vực để chọn bàn.
              </span>
            ) : (
              <div className="placement-checkbox-grid">
                {placementArea.tables
                  .filter(
                    (tableItem) =>
                      tableItem.status === 'ACTIVE',
                  )
                  .map((tableItem) => (
                    <label key={tableItem.id}>
                      <input
                        type="checkbox"
                        checked={placementForm.tableIds
                          .map(Number)
                          .includes(Number(tableItem.id))}
                        onChange={() =>
                          handleTableSelection(tableItem.id)
                        }
                      />
                      {tableItem.tableCode}
                    </label>
                  ))}

                {placementArea.tables.filter(
                  (tableItem) =>
                    tableItem.status === 'ACTIVE',
                ).length === 0 && (
                  <span className="muted-text">
                    Khu vực chưa có bàn đang hoạt động.
                  </span>
                )}
              </div>
            )}
          </div>

          <label className="placement-task-field">
            Nhiệm vụ
            <textarea
              rows="3"
              maxLength="300"
              value={placementForm.task}
              onChange={(event) =>
                setPlacementForm((previous) => ({
                  ...previous,
                  task: event.target.value,
                }))
              }
              placeholder="Nhiệm vụ riêng của nhân viên trong ca"
            />
          </label>

          <div className="placement-save-row">
            <span>
              {shiftAssignments.length} phân công đang hoạt động
              trong ca.
            </span>
            <button
              type="submit"
              className="primary-button"
              disabled={saving || !placementForm.assignmentId}
            >
              {saving ? 'Đang lưu...' : 'Lưu vị trí'}
            </button>
          </div>
        </form>
      </div>
    </section>
  );
}

export default AreaTableManager;
