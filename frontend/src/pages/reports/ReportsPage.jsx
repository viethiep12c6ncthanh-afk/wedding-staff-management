import { useEffect, useMemo, useState } from 'react';

import {
  getMyPayrollReport,
  getPayrollReport,
} from '../../api/reportApi';
import { getApiErrorMessage } from '../../utils/apiError';
import { formatMoney } from '../../utils/formatters';

const getCurrentRole = () => {
  try {
    const raw = localStorage.getItem('currentUser');
    return raw ? JSON.parse(raw)?.role : null;
  } catch {
    return null;
  }
};

function PayrollStatCard({ label, value, detail }) {
  return (
    <article className="payroll-stat-card">
      <span className="payroll-stat-label">{label}</span>
      <strong className="payroll-stat-value">{value}</strong>

      {detail && (
        <span className="payroll-stat-detail">
          {detail}
        </span>
      )}
    </article>
  );
}

function ReportsPage() {
  const role = getCurrentRole();
  const isEmployee = role === 'EMPLOYEE';

  const [filters, setFilters] = useState({
    from: '',
    to: '',
  });

  const [appliedFilters, setAppliedFilters] = useState({
    from: '',
    to: '',
  });

  const [report, setReport] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const loadReport = async (from, to) => {
    try {
      setLoading(true);
      setError('');

      const data = isEmployee
        ? await getMyPayrollReport(from, to)
        : await getPayrollReport(from, to);

      setReport(data);
    } catch (err) {
      setError(
        getApiErrorMessage(
          err,
          isEmployee
            ? 'Không thể tải tiền công của bạn.'
            : 'Không thể tải báo cáo tiền công.',
        ),
      );
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadReport(appliedFilters.from, appliedFilters.to);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [appliedFilters, isEmployee]);

  const handleFilterChange = (event) => {
    const { name, value } = event.target;

    setFilters((previous) => ({
      ...previous,
      [name]: value,
    }));
  };

  const handleFilterSubmit = (event) => {
    event.preventDefault();

    if (
      filters.from &&
      filters.to &&
      filters.from > filters.to
    ) {
      setError('Ngày bắt đầu không được sau ngày kết thúc.');
      return;
    }

    setError('');
    setAppliedFilters(filters);
  };

  const handleReset = () => {
    const empty = {
      from: '',
      to: '',
    };

    setFilters(empty);
    setError('');
    setAppliedFilters(empty);
  };

  const totalDifference = useMemo(() => {
    const base = Number(report?.totalBasePay ?? 0);
    const payable = Number(report?.totalPayable ?? 0);

    return Math.max(base - payable, 0);
  }, [report]);

  const employeeRows = report?.employees ?? [];

  return (
    <section>
      <div className="page-heading">
        <div>
          <h1>
            {isEmployee
              ? 'Tiền công của tôi'
              : 'Báo cáo tiền công'}
          </h1>

          <p>
            {isEmployee
              ? 'Theo dõi tiền công từ các ca đã được xác nhận chấm công.'
              : 'Tổng hợp tiền công từ các bản chấm công đã xác nhận.'}
          </p>
        </div>
      </div>

      <form
        className="report-filter-card"
        onSubmit={handleFilterSubmit}
      >
        <label>
          Từ ngày
          <input
            name="from"
            type="date"
            value={filters.from}
            onChange={handleFilterChange}
          />
        </label>

        <label>
          Đến ngày
          <input
            name="to"
            type="date"
            value={filters.to}
            onChange={handleFilterChange}
          />
        </label>

        <div className="report-filter-actions">
          <button
            type="submit"
            className="primary-button"
            disabled={loading}
          >
            Lọc báo cáo
          </button>

          <button
            type="button"
            className="secondary-button"
            disabled={loading}
            onClick={handleReset}
          >
            Xóa bộ lọc
          </button>
        </div>
      </form>

      <div className="report-note">
        Báo cáo chỉ tính các bản chấm công đã xác nhận.
        Khoảng ngày được lọc theo ngày bắt đầu ca làm.
      </div>

      {error && <div className="error-box">{error}</div>}

      {loading ? (
        <div className="table-state">
          Đang tải báo cáo tiền công...
        </div>
      ) : report ? (
        <>
          <div className="payroll-stats-grid">
            <PayrollStatCard
              label="Ca đã xác nhận"
              value={report.confirmedAttendanceCount ?? 0}
              detail="Bản chấm công đã chốt"
            />

            <PayrollStatCard
              label="Ca được trả công"
              value={report.paidShiftCount ?? 0}
              detail="Không tính ca vắng mặt"
            />

            <PayrollStatCard
              label="Ca vắng mặt"
              value={report.absentShiftCount ?? 0}
              detail="Tiền thực trả bằng 0"
            />

            <PayrollStatCard
              label="Tiền công cơ bản"
              value={formatMoney(report.totalBasePay)}
              detail="Tổng basePaySnapshot"
            />

            <PayrollStatCard
              label="Tiền thực trả"
              value={formatMoney(report.totalPayable)}
              detail="Tổng payableAmount"
            />

            <PayrollStatCard
              label="Chênh lệch"
              value={formatMoney(totalDifference)}
              detail="Cơ bản trừ thực trả"
            />
          </div>

          <div className="report-period">
            <strong>Khoảng báo cáo:</strong>{' '}
            {report.from || 'Không giới hạn'}
            {' → '}
            {report.to || 'Không giới hạn'}
          </div>

          <div className="table-card">
            <div className="table-scroll">
              <table className="data-table payroll-table">
                <thead>
                  <tr>
                    {!isEmployee && <th>Mã NV</th>}
                    <th>Nhân viên</th>
                    <th>Ca xác nhận</th>
                    <th>Ca có trả công</th>
                    <th>Ca vắng</th>
                    <th>Tiền cơ bản</th>
                    <th>Thực trả</th>
                  </tr>
                </thead>

                <tbody>
                  {employeeRows.length === 0 ? (
                    <tr>
                      <td
                        colSpan={isEmployee ? 6 : 7}
                        className="empty-cell"
                      >
                        Không có dữ liệu tiền công trong khoảng đã chọn.
                      </td>
                    </tr>
                  ) : (
                    employeeRows.map((employee) => (
                      <tr key={employee.employeeId}>
                        {!isEmployee && (
                          <td>
                            <strong>
                              {employee.employeeCode}
                            </strong>
                          </td>
                        )}

                        <td>
                          <div className="cell-title">
                            {employee.fullName}
                          </div>

                          {isEmployee && (
                            <div className="cell-subtitle">
                              {employee.employeeCode}
                            </div>
                          )}
                        </td>

                        <td>{employee.confirmedShiftCount}</td>
                        <td>{employee.paidShiftCount}</td>
                        <td>{employee.absentShiftCount}</td>

                        <td>
                          {formatMoney(employee.totalBasePay)}
                        </td>

                        <td>
                          <strong className="payable-money">
                            {formatMoney(employee.totalPayable)}
                          </strong>
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
          </div>
        </>
      ) : null}
    </section>
  );
}

export default ReportsPage;
