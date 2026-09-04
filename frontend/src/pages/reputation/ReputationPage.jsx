import { useEffect, useMemo, useState } from 'react';

import { getAssignments } from '../../api/assignmentApi';
import {
  createEmployeeEvaluation,
  getMyReputation,
  getReputation,
  getReputations,
} from '../../api/reputationApi';
import { getApiErrorMessage } from '../../utils/apiError';
import { formatDateTime } from '../../utils/formatters';

const eventLabels = {
  BASELINE_INITIALIZED: 'Khởi tạo uy tín',
  ATTENDANCE_PRESENT: 'Hoàn thành đúng giờ',
  ATTENDANCE_LATE: 'Đi trễ',
  ATTENDANCE_EARLY_LEAVE: 'Về sớm',
  ATTENDANCE_LATE_AND_EARLY_LEAVE: 'Trễ và về sớm',
  ATTENDANCE_ABSENT: 'Vắng mặt',
  EVALUATION_RATING: 'Đánh giá nhân viên',
};

const getCurrentRole = () => {
  try {
    const raw = localStorage.getItem('currentUser');
    return raw ? JSON.parse(raw)?.role : null;
  } catch {
    return null;
  }
};

const formatDelta = (value) => {
  const number = Number(value ?? 0);
  return number > 0 ? `+${number}` : String(number);
};

const scoreLevel = (score) => {
  if (score >= 90) return 'Rất tốt';
  if (score >= 75) return 'Tốt';
  if (score >= 60) return 'Cần theo dõi';
  return 'Rủi ro';
};

function ReputationPage() {
  const role = getCurrentRole();
  const isManager = role === 'ADMIN' || role === 'COORDINATOR';

  const [summaries, setSummaries] = useState([]);
  const [detail, setDetail] = useState(null);
  const [assignments, setAssignments] = useState([]);
  const [selectedEmployeeId, setSelectedEmployeeId] = useState(null);
  const [loading, setLoading] = useState(true);
  const [detailLoading, setDetailLoading] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [rating, setRating] = useState('5');
  const [assignmentId, setAssignmentId] = useState('');
  const [comment, setComment] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const loadManagerData = async () => {
    const [reputationData, assignmentData] = await Promise.all([
      getReputations(),
      getAssignments(),
    ]);
    setSummaries(reputationData);
    setAssignments(assignmentData);

    if (reputationData.length > 0) {
      const firstId = selectedEmployeeId || reputationData[0].employeeId;
      setSelectedEmployeeId(firstId);
      const detailData = await getReputation(firstId);
      setDetail(detailData);
    } else {
      setDetail(null);
    }
  };

  const loadEmployeeData = async () => {
    const data = await getMyReputation();
    setDetail(data);
  };

  const loadInitial = async () => {
    try {
      setLoading(true);
      setError('');
      if (isManager) {
        await loadManagerData();
      } else {
        await loadEmployeeData();
      }
    } catch (err) {
      setError(
        getApiErrorMessage(err, 'Không thể tải dữ liệu uy tín.'),
      );
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadInitial();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const loadDetail = async (employeeId) => {
    try {
      setDetailLoading(true);
      setError('');
      setNotice('');
      setSelectedEmployeeId(employeeId);
      setAssignmentId('');
      const data = await getReputation(employeeId);
      setDetail(data);
    } catch (err) {
      setError(
        getApiErrorMessage(err, 'Không thể tải chi tiết uy tín.'),
      );
    } finally {
      setDetailLoading(false);
    }
  };

  const availableAssignments = useMemo(() => {
    if (!isManager || !detail?.summary) return [];

    const evaluatedAssignmentIds = new Set(
      (detail.evaluations || []).map((item) => item.assignmentId),
    );

    return assignments.filter(
      (item) =>
        item.employeeId === detail.summary.employeeId &&
        item.status === 'COMPLETED' &&
        !evaluatedAssignmentIds.has(item.id),
    );
  }, [assignments, detail, isManager]);

  const handleEvaluation = async (event) => {
    event.preventDefault();
    if (!assignmentId) {
      setError('Hãy chọn một phân công đã hoàn thành.');
      return;
    }

    try {
      setSubmitting(true);
      setError('');
      setNotice('');
      await createEmployeeEvaluation({
        assignmentId: Number(assignmentId),
        rating: Number(rating),
        comment: comment.trim() || null,
      });

      const [detailData, summaryData] = await Promise.all([
        getReputation(detail.summary.employeeId),
        getReputations(),
      ]);
      setDetail(detailData);
      setSummaries(summaryData);
      setAssignmentId('');
      setRating('5');
      setComment('');
      setNotice('Đã lưu đánh giá và cập nhật điểm uy tín.');
    } catch (err) {
      setError(
        getApiErrorMessage(err, 'Không thể lưu đánh giá.'),
      );
    } finally {
      setSubmitting(false);
    }
  };

  const summary = detail?.summary;

  return (
    <section className="reputation-page">
      <div className="page-heading">
        <h1>{isManager ? 'Uy tín nhân viên' : 'Uy tín của tôi'}</h1>
        <p>
          Điểm uy tín được cập nhật từ chấm công đã xác nhận và đánh
          giá sau khi hoàn thành ca. Mọi thay đổi đều có lịch sử truy vết.
        </p>
      </div>

      {error && <div className="error-box">{error}</div>}
      {notice && <div className="notice-box">{notice}</div>}

      {loading ? (
        <div className="table-card reputation-loading">Đang tải...</div>
      ) : (
        <div className={`reputation-layout ${isManager ? '' : 'single'}`}>
          {isManager && (
            <div className="table-card reputation-list-card">
              <div className="reputation-card-heading">
                <div>
                  <strong>Danh sách nhân viên</strong>
                  <span>{summaries.length} hồ sơ uy tín</span>
                </div>
              </div>

              <div className="reputation-list">
                {summaries.map((item) => (
                  <button
                    type="button"
                    key={item.employeeId}
                    className={`reputation-list-item ${
                      selectedEmployeeId === item.employeeId ? 'active' : ''
                    }`}
                    onClick={() => loadDetail(item.employeeId)}
                  >
                    <div>
                      <strong>{item.fullName}</strong>
                      <span>{item.employeeCode}</span>
                    </div>
                    <div className="reputation-list-score">
                      {item.currentScore}
                      <small>/100</small>
                    </div>
                  </button>
                ))}

                {summaries.length === 0 && (
                  <div className="reputation-empty">Chưa có nhân viên.</div>
                )}
              </div>
            </div>
          )}

          <div className="reputation-detail-column">
            {detailLoading ? (
              <div className="table-card reputation-loading">
                Đang tải chi tiết...
              </div>
            ) : summary ? (
              <>
                <div className="reputation-score-card">
                  <div className="reputation-person">
                    <div>
                      <span className="reputation-eyebrow">
                        {summary.employeeCode}
                      </span>
                      <h2>{summary.fullName}</h2>
                      <p>{scoreLevel(summary.currentScore)}</p>
                    </div>

                    <div className="reputation-score-value">
                      <strong>{summary.currentScore}</strong>
                      <span>/100</span>
                    </div>
                  </div>

                  <div className="reputation-progress">
                    <span
                      style={{ width: `${summary.currentScore}%` }}
                    />
                  </div>

                  <div className="reputation-metrics">
                    <Metric
                      label="Hoàn thành"
                      value={summary.completedShiftCount}
                    />
                    <Metric label="Đi trễ" value={summary.lateCount} />
                    <Metric
                      label="Về sớm"
                      value={summary.earlyLeaveCount}
                    />
                    <Metric label="Vắng" value={summary.absentCount} />
                    <Metric
                      label="Đánh giá"
                      value={
                        summary.averageRating == null
                          ? '—'
                          : `${summary.averageRating}/5`
                      }
                    />
                  </div>
                </div>

                {isManager && (
                  <form
                    className="table-card reputation-evaluation-card"
                    onSubmit={handleEvaluation}
                  >
                    <div className="reputation-card-heading">
                      <div>
                        <strong>Đánh giá sau ca</strong>
                        <span>
                          Chỉ áp dụng cho phân công đã hoàn thành và chưa
                          được đánh giá.
                        </span>
                      </div>
                    </div>

                    <div className="reputation-form-grid">
                      <label>
                        Phân công
                        <select
                          value={assignmentId}
                          onChange={(event) =>
                            setAssignmentId(event.target.value)
                          }
                        >
                          <option value="">Chọn phân công</option>
                          {availableAssignments.map((item) => (
                            <option key={item.id} value={item.id}>
                              #{item.id} - {item.shiftName}
                            </option>
                          ))}
                        </select>
                      </label>

                      <label>
                        Điểm đánh giá
                        <select
                          value={rating}
                          onChange={(event) => setRating(event.target.value)}
                        >
                          <option value="5">5 - Rất tốt</option>
                          <option value="4">4 - Tốt</option>
                          <option value="3">3 - Đạt</option>
                          <option value="2">2 - Chưa tốt</option>
                          <option value="1">1 - Kém</option>
                        </select>
                      </label>
                    </div>

                    <label className="reputation-comment-field">
                      Nhận xét
                      <textarea
                        rows="3"
                        maxLength="500"
                        value={comment}
                        onChange={(event) => setComment(event.target.value)}
                        placeholder="Nhận xét ngắn về chất lượng làm việc..."
                      />
                    </label>

                    <div className="reputation-form-actions">
                      <span>
                        {availableAssignments.length === 0
                          ? 'Không có phân công đủ điều kiện đánh giá.'
                          : `${availableAssignments.length} phân công có thể đánh giá`}
                      </span>
                      <button
                        type="submit"
                        className="primary-button"
                        disabled={submitting || availableAssignments.length === 0}
                      >
                        {submitting ? 'Đang lưu...' : 'Lưu đánh giá'}
                      </button>
                    </div>
                  </form>
                )}

                <div className="table-card reputation-history-card">
                  <div className="reputation-card-heading">
                    <div>
                      <strong>Lịch sử điểm uy tín</strong>
                      <span>
                        Mỗi nguồn nghiệp vụ chỉ được ghi nhận một lần.
                      </span>
                    </div>
                  </div>

                  <div className="reputation-history-list">
                    {(detail.events || []).map((item) => (
                      <div className="reputation-history-item" key={item.id}>
                        <div className="reputation-history-main">
                          <strong>
                            {eventLabels[item.eventType] || item.eventType}
                          </strong>
                          <span>{item.reason}</span>
                          <small>
                            {formatDateTime(item.occurredAt)}
                            {item.actorUsername
                              ? ` · ${item.actorUsername}`
                              : ' · Hệ thống'}
                          </small>
                        </div>

                        <div
                          className={`reputation-delta ${
                            item.scoreDelta > 0
                              ? 'positive'
                              : item.scoreDelta < 0
                                ? 'negative'
                                : 'neutral'
                          }`}
                        >
                          {formatDelta(item.scoreDelta)}
                          <small>
                            {item.scoreBefore} → {item.scoreAfter}
                          </small>
                        </div>
                      </div>
                    ))}

                    {(detail.events || []).length === 0 && (
                      <div className="reputation-empty">
                        Chưa có lịch sử uy tín.
                      </div>
                    )}
                  </div>
                </div>

                <div className="table-card reputation-history-card">
                  <div className="reputation-card-heading">
                    <div>
                      <strong>Lịch sử đánh giá</strong>
                      <span>{summary.evaluationCount} đánh giá</span>
                    </div>
                  </div>

                  <div className="reputation-history-list">
                    {(detail.evaluations || []).map((item) => (
                      <div className="reputation-history-item" key={item.id}>
                        <div className="reputation-history-main">
                          <strong>
                            {item.rating}/5 · {item.shiftName}
                          </strong>
                          <span>{item.comment || 'Không có nhận xét.'}</span>
                          <small>
                            {item.venueName} · {formatDateTime(item.evaluatedAt)}
                            {' · '}{item.evaluatedBy}
                          </small>
                        </div>
                      </div>
                    ))}

                    {(detail.evaluations || []).length === 0 && (
                      <div className="reputation-empty">
                        Chưa có đánh giá sau ca.
                      </div>
                    )}
                  </div>
                </div>
              </>
            ) : (
              <div className="table-card reputation-loading">
                Chưa có hồ sơ uy tín để hiển thị.
              </div>
            )}
          </div>
        </div>
      )}
    </section>
  );
}

function Metric({ label, value }) {
  return (
    <div className="reputation-metric">
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  );
}

export default ReputationPage;
