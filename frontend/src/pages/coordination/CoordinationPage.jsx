import { useEffect, useMemo, useState } from 'react';

import { getCoordinationOverview } from '../../api/coordinationApi';
import { getVenues } from '../../api/venueApi';
import StatusBadge from '../../components/common/StatusBadge';
import { getApiErrorMessage } from '../../utils/apiError';
import { formatDateTime } from '../../utils/formatters';

const staffingLabels = {
  UNDERSTAFFED: 'Thiếu nhân sự',
  FULL: 'Đủ nhân sự',
  OVERSTAFFED: 'Dư nhân sự',
};

const issueLabels = {
  OVERLAP_CONFLICT: 'Trùng lịch',
  INSUFFICIENT_TRANSITION_TIME: 'Thiếu thời gian di chuyển',
};

const getTodayValue = () => {
  const now = new Date();
  const local = new Date(
    now.getTime() - now.getTimezoneOffset() * 60 * 1000,
  );
  return local.toISOString().slice(0, 10);
};

function CoordinationPage() {
  const [date, setDate] = useState(getTodayValue());
  const [fromTime, setFromTime] = useState('');
  const [toTime, setToTime] = useState('');
  const [venueId, setVenueId] = useState('');
  const [transitionBufferMinutes, setTransitionBufferMinutes] =
    useState('60');
  const [venues, setVenues] = useState([]);
  const [overview, setOverview] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const loadOverview = async (filters = {}) => {
    try {
      setLoading(true);
      setError('');

      const data = await getCoordinationOverview({
        date: filters.date ?? date,
        fromTime: filters.fromTime ?? fromTime,
        toTime: filters.toTime ?? toTime,
        venueId: filters.venueId ?? venueId,
        transitionBufferMinutes: Number(
          filters.transitionBufferMinutes ?? transitionBufferMinutes,
        ),
      });

      setOverview(data);
    } catch (err) {
      setError(
        getApiErrorMessage(
          err,
          'Không thể tải dữ liệu điều phối.',
        ),
      );
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    const initialize = async () => {
      try {
        const venueData = await getVenues();
        setVenues(venueData);
      } catch (err) {
        setError(
          getApiErrorMessage(
            err,
            'Không thể tải danh sách địa điểm.',
          ),
        );
      }

      await loadOverview();
    };

    initialize();
    // Initial load only. Filters are applied explicitly by the user.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const selectedVenueName = useMemo(() => {
    if (!venueId) {
      return 'Tất cả địa điểm';
    }

    return (
      venues.find((venue) => String(venue.id) === String(venueId))
        ?.name || 'Địa điểm đã chọn'
    );
  }, [venues, venueId]);

  const handleSubmit = async (event) => {
    event.preventDefault();
    await loadOverview();
  };

  return (
    <section className="coordination-page">
      <div className="page-heading">
        <h1>Điều phối đa ca / đa địa điểm</h1>
        <p>
          Theo dõi mức độ đủ nhân sự, trùng lịch và khoảng đệm
          di chuyển giữa các ca trong cùng ngày.
        </p>
      </div>

      <form className="coordination-filter-card" onSubmit={handleSubmit}>
        <label>
          Ngày điều phối
          <input
            type="date"
            value={date}
            onChange={(event) => setDate(event.target.value)}
            required
          />
        </label>

        <label>
          Từ giờ
          <input
            type="time"
            value={fromTime}
            onChange={(event) => setFromTime(event.target.value)}
          />
        </label>

        <label>
          Đến giờ
          <input
            type="time"
            value={toTime}
            onChange={(event) => setToTime(event.target.value)}
          />
        </label>

        <label>
          Địa điểm
          <select
            value={venueId}
            onChange={(event) => setVenueId(event.target.value)}
          >
            <option value="">Tất cả địa điểm</option>
            {venues.map((venue) => (
              <option key={venue.id} value={venue.id}>
                {venue.name}
              </option>
            ))}
          </select>
        </label>

        <label>
          Khoảng đệm giữa hai địa điểm
          <select
            value={transitionBufferMinutes}
            onChange={(event) =>
              setTransitionBufferMinutes(event.target.value)
            }
          >
            <option value="30">30 phút</option>
            <option value="45">45 phút</option>
            <option value="60">60 phút</option>
            <option value="90">90 phút</option>
            <option value="120">120 phút</option>
          </select>
        </label>

        <button
          className="primary-button coordination-filter-button"
          type="submit"
          disabled={loading}
        >
          {loading ? 'Đang tải...' : 'Áp dụng'}
        </button>
      </form>

      {error && <div className="error-box">{error}</div>}

      <div className="coordination-context">
        <strong>{selectedVenueName}</strong>
        <span>
          Khoảng đệm cảnh báo:{' '}
          {overview?.transitionBufferMinutes ?? transitionBufferMinutes} phút
        </span>
      </div>

      <div className="stats-grid">
        <div className="stat-card">
          <span className="stat-label">Tổng ca</span>
          <strong className="stat-value">
            {overview?.totalShifts ?? 0}
          </strong>
        </div>

        <div className="stat-card">
          <span className="stat-label">Ca thiếu nhân sự</span>
          <strong className="stat-value">
            {overview?.understaffedShifts ?? 0}
          </strong>
        </div>

        <div className="stat-card">
          <span className="stat-label">Ca đủ nhân sự</span>
          <strong className="stat-value">
            {overview?.fullShifts ?? 0}
          </strong>
        </div>

        <div className="stat-card">
          <span className="stat-label">Ca dư nhân sự</span>
          <strong className="stat-value">
            {overview?.overstaffedShifts ?? 0}
          </strong>
        </div>

        <div className="stat-card">
          <span className="stat-label">Cảnh báo lịch</span>
          <strong className="stat-value">
            {overview?.scheduleIssues?.length ?? 0}
          </strong>
        </div>
      </div>

      <h2 className="section-title">Tình trạng nhân sự theo ca</h2>

      <div className="table-card">
        <div className="table-scroll">
          <table className="data-table coordination-table">
            <thead>
              <tr>
                <th>Ca / sự kiện</th>
                <th>Địa điểm</th>
                <th>Thời gian</th>
                <th>Trạng thái ca</th>
                <th>Nhân sự</th>
                <th>Tình trạng</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr>
                  <td colSpan="6" className="table-state">
                    Đang tải dữ liệu điều phối...
                  </td>
                </tr>
              ) : overview?.shifts?.length ? (
                overview.shifts.map((shift) => (
                  <tr key={shift.shiftId}>
                    <td>
                      <div className="cell-title">{shift.shiftName}</div>
                      <div className="cell-subtitle">
                        {shift.eventName}
                      </div>
                    </td>
                    <td>
                      <div className="cell-title">{shift.venueName}</div>
                      <div className="cell-subtitle">
                        {shift.venueAddress}
                      </div>
                    </td>
                    <td>
                      <div>{formatDateTime(shift.startAt)}</div>
                      <div className="cell-subtitle">
                        → {formatDateTime(shift.endAt)}
                      </div>
                    </td>
                    <td>
                      <StatusBadge value={shift.shiftStatus} />
                    </td>
                    <td>
                      <strong>
                        {shift.effectiveStaffCount}/{shift.requiredStaff}
                      </strong>
                      {shift.missingStaffCount > 0 && (
                        <div className="coordination-missing">
                          Thiếu {shift.missingStaffCount}
                        </div>
                      )}
                      {shift.extraStaffCount > 0 && (
                        <div className="coordination-extra">
                          Dư {shift.extraStaffCount}
                        </div>
                      )}
                    </td>
                    <td>
                      <span
                        className={`coordination-staffing-badge staffing-${shift.staffingStatus.toLowerCase()}`}
                      >
                        {staffingLabels[shift.staffingStatus] ||
                          shift.staffingStatus}
                      </span>
                    </td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td colSpan="6" className="empty-cell">
                    Không có ca phù hợp với bộ lọc.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>

      <h2 className="section-title">Cảnh báo lịch nhân viên</h2>

      <div className="coordination-issues">
        {!loading && !overview?.scheduleIssues?.length ? (
          <div className="placeholder-card">
            Không phát hiện trùng lịch hoặc khoảng di chuyển quá ngắn.
          </div>
        ) : (
          overview?.scheduleIssues?.map((issue, index) => (
            <article
              className={`coordination-issue-card issue-${issue.issueType.toLowerCase()}`}
              key={`${issue.employeeId}-${issue.firstShiftId}-${issue.secondShiftId}-${issue.issueType}-${index}`}
            >
              <div className="coordination-issue-header">
                <div>
                  <strong>{issue.employeeName}</strong>
                  <span>
                    {issueLabels[issue.issueType] || issue.issueType}
                  </span>
                </div>

                {issue.issueType === 'OVERLAP_CONFLICT' ? (
                  <b>Trùng {issue.overlapMinutes} phút</b>
                ) : (
                  <b>Chỉ có {issue.gapMinutes} phút</b>
                )}
              </div>

              <div className="coordination-issue-route">
                <div>
                  <strong>{issue.firstShiftName}</strong>
                  <span>{issue.firstVenueName}</span>
                  <small>
                    {formatDateTime(issue.firstStartAt)} →{' '}
                    {formatDateTime(issue.firstEndAt)}
                  </small>
                </div>

                <div className="coordination-route-arrow">→</div>

                <div>
                  <strong>{issue.secondShiftName}</strong>
                  <span>{issue.secondVenueName}</span>
                  <small>
                    {formatDateTime(issue.secondStartAt)} →{' '}
                    {formatDateTime(issue.secondEndAt)}
                  </small>
                </div>
              </div>

              {issue.issueType ===
                'INSUFFICIENT_TRANSITION_TIME' && (
                <p>
                  Quy tắc đang dùng yêu cầu tối thiểu{' '}
                  {issue.requiredBufferMinutes} phút giữa hai ca ở
                  hai địa điểm khác nhau. Đây là cảnh báo điều phối,
                  không tự động hủy phân công.
                </p>
              )}
            </article>
          ))
        )}
      </div>
    </section>
  );
}

export default CoordinationPage;
