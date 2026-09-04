import { useEffect, useMemo, useState } from 'react';

import {
  getAiAnalytics,
  getDashboardSummary,
  getDashboardVenues,
  getOperationsAnalytics,
  getWorkforceAnalytics,
} from '../../api/dashboardApi';
import { getPayrollReport } from '../../api/reportApi';

import '../../styles/dashboard.css';

const formatDateInput = (date) => {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
};

const createDefaultRange = () => {
  const to = new Date();
  const from = new Date(to);
  from.setDate(from.getDate() - 29);

  return {
    from: formatDateInput(from),
    to: formatDateInput(to),
  };
};

const formatMoney = (value) =>
  `${new Intl.NumberFormat('vi-VN').format(Number(value ?? 0))} đ`;

const formatPercent = (value) =>
  `${Number(value ?? 0).toLocaleString('vi-VN', {
    maximumFractionDigits: 2,
  })}%`;

function StatCard({ label, value, detail }) {
  return (
    <article className="stat-card">
      <span className="stat-label">{label}</span>
      <strong className="stat-value">{value ?? 0}</strong>
      {detail && <span className="stat-detail">{detail}</span>}
    </article>
  );
}

function SectionHeader({ title, subtitle }) {
  return (
    <div className="dashboard-section-heading">
      <div>
        <h2>{title}</h2>
        {subtitle && <p>{subtitle}</p>}
      </div>
    </div>
  );
}

function BarList({ items, emptyText }) {
  const maxValue = Math.max(
    1,
    ...items.map((item) => Number(item.value ?? 0)),
  );

  if (items.length === 0) {
    return <div className="dashboard-empty">{emptyText}</div>;
  }

  return (
    <div className="dashboard-bar-list">
      {items.map((item) => {
        const width = Math.max(
          0,
          Math.min(100, (Number(item.value ?? 0) / maxValue) * 100),
        );

        return (
          <div className="dashboard-bar-row" key={item.key}>
            <div className="dashboard-bar-label">
              <div>
                <strong>{item.label}</strong>
                {item.detail && <span>{item.detail}</span>}
              </div>
              <b>{item.displayValue ?? item.value ?? 0}</b>
            </div>
            <div className="dashboard-bar-track">
              <span style={{ width: `${width}%` }} />
            </div>
          </div>
        );
      })}
    </div>
  );
}

function DashboardPage() {
  const defaultRange = useMemo(() => createDefaultRange(), []);

  const [from, setFrom] = useState(defaultRange.from);
  const [to, setTo] = useState(defaultRange.to);
  const [venueId, setVenueId] = useState('');

  const [summary, setSummary] = useState(null);
  const [operations, setOperations] = useState(null);
  const [workforce, setWorkforce] = useState(null);
  const [ai, setAi] = useState(null);
  const [payroll, setPayroll] = useState(null);
  const [venues, setVenues] = useState([]);

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const loadDashboard = async ({
    nextFrom = from,
    nextTo = to,
    nextVenueId = venueId,
  } = {}) => {
    try {
      setLoading(true);
      setError('');

      const [
        summaryData,
        operationsData,
        workforceData,
        aiData,
        payrollData,
        venueData,
      ] = await Promise.all([
        getDashboardSummary(),
        getOperationsAnalytics({
          from: nextFrom,
          to: nextTo,
          venueId: nextVenueId,
        }),
        getWorkforceAnalytics({
          from: nextFrom,
          to: nextTo,
        }),
        getAiAnalytics({
          from: nextFrom,
          to: nextTo,
        }),
        getPayrollReport(nextFrom, nextTo),
        getDashboardVenues(),
      ]);

      setSummary(summaryData);
      setOperations(operationsData);
      setWorkforce(workforceData);
      setAi(aiData);
      setPayroll(payrollData);
      setVenues(venueData);
    } catch (err) {
      setError(
        err.response?.data?.message ||
          'Không thể tải dữ liệu Dashboard.',
      );
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadDashboard({
      nextFrom: defaultRange.from,
      nextTo: defaultRange.to,
      nextVenueId: '',
    });
    // Initial load only. Filters are applied explicitly by the form.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const handleSubmit = (event) => {
    event.preventDefault();

    if (!from || !to) {
      setError('Vui lòng chọn đầy đủ khoảng ngày.');
      return;
    }

    if (from > to) {
      setError('Ngày bắt đầu không được sau ngày kết thúc.');
      return;
    }

    loadDashboard();
  };

  const trendItems = (workforce?.attendanceTrend ?? []).map((item) => ({
    key: item.date,
    label: item.date,
    detail: `Đi trễ: ${item.lateCount} · Vắng: ${item.absentCount}`,
    value: item.confirmedCount,
  }));

  const workloadItems = (workforce?.topWorkload ?? []).map((item) => ({
    key: item.employeeId,
    label: `${item.employeeCode} · ${item.fullName}`,
    detail: `${item.confirmedShiftCount} ca xác nhận`,
    value: item.paidShiftCount,
    displayValue: `${item.paidShiftCount} ca`,
  }));

  const reputationItems = (
    workforce?.reputationDistribution ?? []
  ).map((item) => ({
    key: item.code,
    label: item.label,
    detail: `${item.minScore}–${item.maxScore} điểm`,
    value: item.employeeCount,
    displayValue: `${item.employeeCount} NV`,
  }));

  const providerItems = (ai?.providerUsage ?? []).map((item, index) => ({
    key: `${item.provider}-${item.model}-${index}`,
    label: item.provider,
    detail: item.model,
    value: item.runCount,
    displayValue: `${item.runCount} lượt`,
  }));

  const fallbackItems = (ai?.fallbackReasons ?? []).map(
    (item, index) => ({
      key: `${item.reason}-${index}`,
      label: item.reason,
      value: item.runCount,
      displayValue: `${item.runCount} lượt`,
    }),
  );

  if (loading && !summary) {
    return (
      <div className="dashboard-state">
        Đang tải dữ liệu Dashboard...
      </div>
    );
  }

  return (
    <section className="dashboard-page">
      <div className="page-heading">
        <div>
          <h1>Tổng quan vận hành</h1>
          <p>
            Theo dõi nhân sự, điều phối, chấm công, thay thế, AI và
            tiền công theo cùng một khoảng thời gian.
          </p>
        </div>
      </div>

      <form className="dashboard-filter-card" onSubmit={handleSubmit}>
        <label>
          Từ ngày
          <input
            type="date"
            value={from}
            onChange={(event) => setFrom(event.target.value)}
          />
        </label>

        <label>
          Đến ngày
          <input
            type="date"
            value={to}
            onChange={(event) => setTo(event.target.value)}
          />
        </label>

        <label>
          Địa điểm vận hành
          <select
            value={venueId}
            onChange={(event) => setVenueId(event.target.value)}
          >
            <option value="">Tất cả địa điểm</option>
            {venues.map((venue) => (
              <option value={venue.id} key={venue.id}>
                {venue.name}
              </option>
            ))}
          </select>
        </label>

        <button
          className="primary-button dashboard-filter-button"
          type="submit"
          disabled={loading}
        >
          {loading ? 'Đang tải...' : 'Cập nhật Dashboard'}
        </button>
      </form>

      <div className="dashboard-filter-note">
        Khoảng ngày áp dụng cho toàn Dashboard. Bộ lọc địa điểm chỉ
        áp dụng cho vận hành và thay thế nhân sự.
      </div>

      {error && <div className="error-box">{error}</div>}

      <SectionHeader
        title="Tổng quan hệ thống"
        subtitle="Các chỉ số tổng thể, không phụ thuộc khoảng ngày."
      />

      <div className="stats-grid dashboard-stats-grid">
        <StatCard
          label="Nhân viên"
          value={summary?.totalEmployees}
          detail={`${summary?.activeEmployees ?? 0} đang hoạt động`}
        />
        <StatCard
          label="Địa điểm"
          value={summary?.totalVenues}
          detail={`${summary?.activeVenues ?? 0} đang hoạt động`}
        />
        <StatCard
          label="Sự kiện"
          value={summary?.totalEvents}
          detail={`${summary?.confirmedEvents ?? 0} đã xác nhận`}
        />
        <StatCard
          label="Ca làm"
          value={summary?.totalShifts}
          detail={`${summary?.openShifts ?? 0} đang mở`}
        />
        <StatCard
          label="Đăng ký chờ duyệt"
          value={summary?.pendingRegistrations}
        />
        <StatCard
          label="Phân công đang hoạt động"
          value={summary?.activeAssignments}
        />
      </div>

      <SectionHeader
        title="Vận hành ca làm"
        subtitle="Tình trạng đủ người được tính theo phân công hiệu lực."
      />

      <div className="stats-grid dashboard-stats-grid">
        <StatCard label="Tổng ca" value={operations?.totalShifts} />
        <StatCard
          label="Ca thiếu người"
          value={operations?.understaffedShifts}
          detail={`Thiếu tổng ${operations?.missingStaffTotal ?? 0} vị trí`}
        />
        <StatCard label="Ca đủ người" value={operations?.fullShifts} />
        <StatCard
          label="Ca dư người"
          value={operations?.overstaffedShifts}
        />
      </div>

      <SectionHeader
        title="Thay thế nhân sự"
        subtitle="Tỷ lệ lấp đầy chỉ tính các yêu cầu đã kết thúc."
      />

      <div className="stats-grid dashboard-stats-grid">
        <StatCard
          label="Tổng yêu cầu"
          value={operations?.replacementTotal}
        />
        <StatCard
          label="Chờ duyệt"
          value={operations?.replacementPending}
        />
        <StatCard
          label="Đang tìm người"
          value={operations?.replacementOpen}
        />
        <StatCard
          label="Đã lấp đầy"
          value={operations?.replacementFilled}
          detail={`${operations?.replacementResolved ?? 0} yêu cầu đã kết thúc`}
        />
        <StatCard
          label="Tỷ lệ lấp đầy"
          value={formatPercent(operations?.replacementFillRate)}
          detail={`${operations?.replacementRejected ?? 0} từ chối · ${operations?.replacementCancelled ?? 0} hủy`}
        />
      </div>

      <SectionHeader
        title="Chấm công"
        subtitle="Chỉ tính các bản chấm công đã xác nhận."
      />

      <div className="stats-grid dashboard-stats-grid">
        <StatCard
          label="Đã xác nhận"
          value={workforce?.confirmedAttendances}
        />
        <StatCard label="Có mặt đúng giờ" value={workforce?.presentCount} />
        <StatCard
          label="Tỷ lệ đi trễ"
          value={formatPercent(workforce?.lateRate)}
          detail={`${(workforce?.lateCount ?? 0) + (workforce?.lateAndEarlyLeaveCount ?? 0)} bản ghi có đi trễ`}
        />
        <StatCard
          label="Tỷ lệ vắng"
          value={formatPercent(workforce?.absenceRate)}
          detail={`${workforce?.absentCount ?? 0} bản ghi vắng`}
        />
        <StatCard
          label="Về sớm"
          value={
            (workforce?.earlyLeaveCount ?? 0) +
            (workforce?.lateAndEarlyLeaveCount ?? 0)
          }
          detail={`${workforce?.lateAndEarlyLeaveCount ?? 0} trường hợp vừa trễ vừa về sớm`}
        />
      </div>

      <div className="dashboard-panel">
        <SectionHeader
          title="Xu hướng chấm công"
          subtitle="Thanh biểu diễn số bản chấm công xác nhận theo ngày."
        />
        <BarList
          items={trendItems}
          emptyText="Không có dữ liệu chấm công trong khoảng đã chọn."
        />
      </div>

      <div className="dashboard-two-column">
        <div className="dashboard-panel">
          <SectionHeader
            title="Khối lượng làm việc"
            subtitle="Top nhân viên theo số ca được trả công trong khoảng."
          />
          <BarList
            items={workloadItems}
            emptyText="Chưa có dữ liệu khối lượng làm việc."
          />
        </div>

        <div className="dashboard-panel">
          <SectionHeader
            title="Phân bố uy tín"
            subtitle="Điểm uy tín hiện tại của nhân viên."
          />
          <BarList
            items={reputationItems}
            emptyText="Chưa có dữ liệu uy tín."
          />
        </div>
      </div>

      <SectionHeader
        title="AI recommendation"
        subtitle="Chỉ thống kê các lần recommendation đã được lưu thành run."
      />

      <div className="stats-grid dashboard-stats-grid">
        <StatCard label="Tổng lượt chạy" value={ai?.totalRuns} />
        <StatCard label="AI-assisted" value={ai?.aiAssistedRuns} />
        <StatCard label="Fallback" value={ai?.fallbackRuns} />
        <StatCard
          label="Tỷ lệ fallback"
          value={formatPercent(ai?.fallbackRate)}
        />
      </div>

      <div className="dashboard-two-column">
        <div className="dashboard-panel">
          <SectionHeader title="Provider / model" />
          <BarList
            items={providerItems}
            emptyText="Chưa có lượt AI recommendation trong khoảng."
          />
        </div>

        <div className="dashboard-panel">
          <SectionHeader title="Nguyên nhân fallback" />
          <BarList
            items={fallbackItems}
            emptyText="Không có fallback trong khoảng đã chọn."
          />
        </div>
      </div>

      <SectionHeader
        title="Tiền công"
        subtitle="Tái sử dụng snapshot payroll đã xác nhận ở Commit 10."
      />

      <div className="stats-grid dashboard-stats-grid">
        <StatCard
          label="Tiền công cơ bản"
          value={formatMoney(payroll?.totalBasePay)}
        />
        <StatCard
          label="Phụ cấp trưởng ca"
          value={formatMoney(payroll?.totalLeaderAllowance)}
        />
        <StatCard
          label="Tiền tăng ca"
          value={formatMoney(payroll?.totalOvertimePay)}
        />
        <StatCard
          label="Tổng khấu trừ"
          value={formatMoney(
            Number(payroll?.totalLateDeduction ?? 0) +
              Number(payroll?.totalEarlyLeaveDeduction ?? 0),
          )}
        />
        <StatCard
          label="Tiền thực trả"
          value={formatMoney(payroll?.totalPayable)}
          detail={`${payroll?.confirmedAttendanceCount ?? 0} bản chấm công đã xác nhận`}
        />
      </div>
    </section>
  );
}

export default DashboardPage;
