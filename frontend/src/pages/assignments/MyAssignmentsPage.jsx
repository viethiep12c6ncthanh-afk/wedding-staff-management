import { useEffect, useMemo, useState } from 'react';

import { getMyAssignments } from '../../api/assignmentApi';
import StatusBadge from '../../components/common/StatusBadge';
import { getApiErrorMessage } from '../../utils/apiError';
import { formatDateTime } from '../../utils/formatters';
import '../../styles/assignment-placement.css';

function MyAssignmentsPage() {
  const [assignments, setAssignments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    let active = true;

    getMyAssignments()
      .then((data) => {
        if (active) {
          setAssignments(data);
        }
      })
      .catch((err) => {
        if (active) {
          setError(
            getApiErrorMessage(
              err,
              'Không thể tải phân công của bạn.',
            ),
          );
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

  const activeAssignments = useMemo(
    () =>
      assignments.filter((item) =>
        ['ASSIGNED', 'CONFIRMED'].includes(item.status),
      ),
    [assignments],
  );

  if (loading) {
    return (
      <div className="table-state">
        Đang tải phân công của bạn...
      </div>
    );
  }

  return (
    <section>
      <div className="page-heading">
        <div>
          <h1>Phân công của tôi</h1>
          <p>
            Xem ca làm, vai trò, khu vực, bàn và nhiệm vụ đã
            được điều phối.
          </p>
        </div>
      </div>

      {error && <div className="error-box">{error}</div>}

      <div className="assignment-summary-strip">
        <strong>{activeAssignments.length}</strong>
        <span>phân công đang hoạt động</span>
      </div>

      <div className="table-card">
        <div className="table-scroll">
          <table className="data-table">
            <thead>
              <tr>
                <th>Ca làm</th>
                <th>Vai trò</th>
                <th>Khu vực</th>
                <th>Bàn</th>
                <th>Nhiệm vụ</th>
                <th>Trạng thái</th>
              </tr>
            </thead>

            <tbody>
              {assignments.length === 0 ? (
                <tr>
                  <td colSpan="6" className="empty-cell">
                    Chưa có phân công.
                  </td>
                </tr>
              ) : (
                assignments.map((assignment) => (
                  <tr key={assignment.id}>
                    <td>
                      <div className="cell-title">
                        {assignment.shiftName}
                      </div>
                      <div className="cell-subtitle">
                        Phân công #{assignment.id}
                      </div>
                    </td>

                    <td>
                      {assignment.shiftRole === 'LEADER'
                        ? 'Trưởng ca'
                        : 'Nhân viên'}
                    </td>

                    <td>{assignment.area || 'Chưa phân'}</td>

                    <td>
                      {(assignment.tableCodes || []).length > 0
                        ? assignment.tableCodes.join(', ')
                        : '—'}
                    </td>

                    <td>
                      {assignment.task ||
                        'Không có nhiệm vụ riêng'}
                    </td>

                    <td>
                      <StatusBadge value={assignment.status} />
                      {assignment.cancelledAt && (
                        <div className="cell-subtitle">
                          {formatDateTime(
                            assignment.cancelledAt,
                          )}
                        </div>
                      )}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>
    </section>
  );
}

export default MyAssignmentsPage;
