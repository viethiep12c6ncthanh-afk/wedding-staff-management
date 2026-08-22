import { useEffect, useMemo, useState } from 'react';

import {
  createReplacementRequest,
  getMyReplacementEligibleAssignments,
  getMyReplacementInvitations,
  getMyReplacementRequests,
  getReplacementCandidates,
  getReplacementRequests,
  inviteReplacementEmployee,
  respondReplacementInvitation,
  reviewReplacementRequest,
} from '../../api/replacementApi';
import StatusBadge from '../../components/common/StatusBadge';
import { getApiErrorMessage } from '../../utils/apiError';
import { formatDateTime } from '../../utils/formatters';

const getCurrentRole = () => {
  try {
    const raw = localStorage.getItem('currentUser');
    return raw ? JSON.parse(raw)?.role : null;
  } catch {
    return null;
  }
};

const requestStatusLabels = {
  PENDING: 'Chờ duyệt',
  OPEN: 'Đang tìm người thay',
  FILLED: 'Đã có người thay',
  REJECTED: 'Đã từ chối',
  CANCELLED: 'Đã đóng',
};

const invitationStatusLabels = {
  PENDING: 'Chờ phản hồi',
  ACCEPTED: 'Đã nhận thay',
  DECLINED: 'Đã từ chối',
  CANCELLED: 'Đã hủy',
};

function ReplacementPage() {
  const role = getCurrentRole();
  const isManager = role === 'ADMIN' || role === 'COORDINATOR';

  const [requests, setRequests] = useState([]);
  const [invitations, setInvitations] = useState([]);
  const [eligibleAssignments, setEligibleAssignments] = useState([]);
  const [candidatesByRequest, setCandidatesByRequest] = useState({});
  const [candidateErrorsByRequest, setCandidateErrorsByRequest] = useState({});
  const [assignmentId, setAssignmentId] = useState('');
  const [reason, setReason] = useState('');
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');

  const loadData = async () => {
    try {
      setLoading(true);
      setError('');

      if (isManager) {
        const requestData = await getReplacementRequests();
        const openRequests = requestData.filter((item) => item.status === 'OPEN');
        const candidateResults = await Promise.all(
          openRequests.map(async (item) => {
            try {
              return {
                requestId: item.id,
                candidates: await getReplacementCandidates(item.id),
                error: '',
              };
            } catch (candidateError) {
              return {
                requestId: item.id,
                candidates: [],
                error: getApiErrorMessage(
                  candidateError,
                  'Không thể tải gợi ý ứng viên cho yêu cầu này.',
                ),
              };
            }
          }),
        );
        setRequests(requestData);
        setCandidatesByRequest(Object.fromEntries(
          candidateResults.map((item) => [item.requestId, item.candidates]),
        ));
        setCandidateErrorsByRequest(Object.fromEntries(
          candidateResults
            .filter((item) => item.error)
            .map((item) => [item.requestId, item.error]),
        ));
      } else {
        const [requestData, invitationData, assignmentData] = await Promise.all([
          getMyReplacementRequests(),
          getMyReplacementInvitations(),
          getMyReplacementEligibleAssignments(),
        ]);
        setRequests(requestData);
        setInvitations(invitationData);
        setEligibleAssignments(assignmentData);
      }
    } catch (err) {
      setError(getApiErrorMessage(err, 'Không thể tải dữ liệu thay ca.'));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isManager]);

  const pendingInvitations = useMemo(
    () => invitations.filter((item) => item.status === 'PENDING'),
    [invitations],
  );

  const handleCreateRequest = async (event) => {
    event.preventDefault();
    if (!assignmentId) {
      setError('Hãy chọn phân công cần xin thay ca.');
      return;
    }
    if (!reason.trim()) {
      setError('Bắt buộc nhập lý do xin thay ca.');
      return;
    }

    try {
      setSaving(true);
      setError('');
      setNotice('');
      await createReplacementRequest({
        assignmentId: Number(assignmentId),
        reason: reason.trim(),
      });
      setAssignmentId('');
      setReason('');
      setNotice('Đã gửi yêu cầu thay ca. Phân công hiện tại vẫn giữ nguyên cho tới khi quản lý duyệt.');
      await loadData();
    } catch (err) {
      setError(getApiErrorMessage(err, 'Không thể gửi yêu cầu thay ca.'));
    } finally {
      setSaving(false);
    }
  };

  const handleReview = async (request, approved) => {
    let reviewNote = null;
    if (!approved) {
      reviewNote = window.prompt('Nhập lý do từ chối yêu cầu thay ca:');
      if (reviewNote === null) return;
      if (!reviewNote.trim()) {
        setError('Bắt buộc nhập lý do từ chối.');
        return;
      }
    } else if (!window.confirm(
      `Duyệt yêu cầu #${request.id}? Phân công hiện tại của ${request.originalEmployeeName} sẽ được hủy và mở vị trí cần người thay.`,
    )) {
      return;
    }

    try {
      setSaving(true);
      setError('');
      setNotice('');
      await reviewReplacementRequest(request.id, {
        approved,
        reviewNote: reviewNote?.trim() || null,
      });
      setNotice(approved
        ? 'Đã duyệt yêu cầu và mở vị trí cần người thay.'
        : 'Đã từ chối yêu cầu thay ca.');
      await loadData();
    } catch (err) {
      setError(getApiErrorMessage(err, 'Không thể xử lý yêu cầu thay ca.'));
    } finally {
      setSaving(false);
    }
  };

  const handleInvite = async (request, employeeId) => {
    try {
      setSaving(true);
      setError('');
      setNotice('');
      await inviteReplacementEmployee(request.id, Number(employeeId));
      setNotice('Đã gửi lời mời thay ca cho nhân viên được chọn.');
      await loadData();
    } catch (err) {
      setError(getApiErrorMessage(err, 'Không thể gửi lời mời thay ca.'));
    } finally {
      setSaving(false);
    }
  };

  const handleInvitation = async (invitation, accepted) => {
    let responseNote = null;
    if (!accepted) {
      responseNote = window.prompt('Lý do từ chối (có thể để trống):');
      if (responseNote === null) return;
    } else if (!window.confirm(
      `Nhận thay ca "${invitation.shiftName}"? Hệ thống sẽ kiểm tra lại trùng lịch trước khi tạo phân công.`,
    )) {
      return;
    }

    try {
      setSaving(true);
      setError('');
      setNotice('');
      await respondReplacementInvitation(invitation.id, {
        accepted,
        responseNote: responseNote?.trim() || null,
      });
      setNotice(accepted
        ? 'Đã nhận thay ca và tạo phân công mới.'
        : 'Đã từ chối lời mời thay ca.');
      await loadData();
    } catch (err) {
      setError(getApiErrorMessage(err, 'Không thể phản hồi lời mời thay ca.'));
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return <div className="table-state">Đang tải dữ liệu thay ca...</div>;
  }

  return (
    <section className="replacement-page">
      <div className="page-heading">
        <div>
          <h1>{isManager ? 'Hủy ca & thay thế nhân sự' : 'Yêu cầu thay ca'}</h1>
          <p>
            {isManager
              ? 'Duyệt yêu cầu rút khỏi ca, mời người thay và giữ đầy đủ lịch sử điều phối.'
              : 'Yêu cầu thay ca không tự hủy phân công. Quản lý phải duyệt trước khi vị trí được mở để tìm người thay.'}
          </p>
        </div>
      </div>

      {notice && <div className="notice-box">{notice}</div>}
      {error && <div className="error-box">{error}</div>}

      {!isManager && (
        <>
          <div className="table-card replacement-form-card">
            <div className="replacement-card-heading">
              <div>
                <strong>Gửi yêu cầu thay ca</strong>
                <span>Không thay đổi quy tắc tự hủy đăng ký trước 24 giờ của hệ thống cũ.</span>
              </div>
            </div>

            <form className="replacement-request-form" onSubmit={handleCreateRequest}>
              <label>
                Phân công cần thay *
                <select
                  value={assignmentId}
                  onChange={(event) => setAssignmentId(event.target.value)}
                  required
                >
                  <option value="">-- Chọn phân công --</option>
                  {eligibleAssignments.map((item) => (
                    <option key={item.assignmentId} value={item.assignmentId}>
                      {item.shiftName} · {formatDateTime(item.startAt)}
                    </option>
                  ))}
                </select>
              </label>

              <label>
                Lý do *
                <textarea
                  rows="3"
                  maxLength="500"
                  value={reason}
                  onChange={(event) => setReason(event.target.value)}
                  placeholder="Ví dụ: Có việc gia đình đột xuất..."
                  required
                />
              </label>

              <div className="replacement-form-actions">
                <span>{eligibleAssignments.length} phân công có thể gửi yêu cầu</span>
                <button
                  type="submit"
                  className="primary-button"
                  disabled={saving || eligibleAssignments.length === 0}
                >
                  {saving ? 'Đang gửi...' : 'Gửi yêu cầu'}
                </button>
              </div>
            </form>
          </div>

          <h2 className="section-title">Lời mời thay ca</h2>
          <div className="replacement-invitation-grid">
            {pendingInvitations.length === 0 ? (
              <div className="table-card replacement-empty">Không có lời mời đang chờ phản hồi.</div>
            ) : pendingInvitations.map((item) => (
              <article className="replacement-invitation-card" key={item.id}>
                <div className="replacement-card-top">
                  <div>
                    <h3>{item.shiftName}</h3>
                    <p>{item.eventName} · {item.venueName}</p>
                  </div>
                  <StatusBadge value={item.status} />
                </div>
                <div className="replacement-meta">
                  <span>{formatDateTime(item.startAt)} → {formatDateTime(item.endAt)}</span>
                  <span>Thay cho: <strong>{item.originalEmployeeName}</strong></span>
                  <span>Người mời: {item.invitedBy}</span>
                </div>
                <div className="replacement-actions">
                  <button
                    type="button"
                    className="secondary-button"
                    disabled={saving}
                    onClick={() => handleInvitation(item, false)}
                  >
                    Từ chối
                  </button>
                  <button
                    type="button"
                    className="primary-button"
                    disabled={saving}
                    onClick={() => handleInvitation(item, true)}
                  >
                    Nhận thay ca
                  </button>
                </div>
              </article>
            ))}
          </div>
        </>
      )}

      <h2 className="section-title">
        {isManager ? 'Danh sách yêu cầu thay ca' : 'Yêu cầu của tôi'}
      </h2>

      <div className="replacement-request-list">
        {requests.length === 0 ? (
          <div className="table-card replacement-empty">Chưa có yêu cầu thay ca.</div>
        ) : requests.map((request) => {
          const recommendedCandidates = candidatesByRequest[request.id] || [];
          const candidateError = candidateErrorsByRequest[request.id] || '';

          return (
            <article className="table-card replacement-request-card" key={request.id}>
              <div className="replacement-card-heading replacement-card-heading-row">
                <div>
                  <strong>Yêu cầu #{request.id} · {request.shiftName}</strong>
                  <span>{request.eventName} · {request.venueName}</span>
                </div>
                <div className={`replacement-request-status status-${request.status.toLowerCase()}`}>
                  {requestStatusLabels[request.status] || request.status}
                </div>
              </div>

              <div className="replacement-request-body">
                <div className="replacement-request-summary">
                  <div><span>Nhân viên gốc</span><strong>{request.originalEmployeeName}</strong></div>
                  <div><span>Thời gian</span><strong>{formatDateTime(request.startAt)}</strong></div>
                  <div><span>Lý do</span><strong>{request.reason}</strong></div>
                  <div><span>Gửi lúc</span><strong>{formatDateTime(request.createdAt)}</strong></div>
                  {request.replacementEmployeeName && (
                    <div><span>Người thay</span><strong>{request.replacementEmployeeName}</strong></div>
                  )}
                  {request.reviewNote && (
                    <div><span>Ghi chú duyệt</span><strong>{request.reviewNote}</strong></div>
                  )}
                  {request.closedReason && (
                    <div><span>Lý do đóng</span><strong>{request.closedReason}</strong></div>
                  )}
                </div>

                {isManager && request.status === 'PENDING' && (
                  <div className="replacement-actions">
                    <button
                      type="button"
                      className="secondary-button"
                      disabled={saving}
                      onClick={() => handleReview(request, false)}
                    >
                      Từ chối
                    </button>
                    <button
                      type="button"
                      className="primary-button"
                      disabled={saving}
                      onClick={() => handleReview(request, true)}
                    >
                      Duyệt & mở thay ca
                    </button>
                  </div>
                )}

                {isManager && request.status === 'OPEN' && (
                  <div className="replacement-candidate-panel">
                    <div className="replacement-candidate-heading">
                      <div>
                        <strong>Gợi ý ứng viên phù hợp</strong>
                        <span>
                          Xếp hạng theo quy tắc xác định: uy tín, độ ổn định,
                          kinh nghiệm và kinh nghiệm đúng vai trò. Không sử dụng AI ở bước này.
                        </span>
                      </div>
                      <span className="replacement-rule-badge">Rule-based</span>
                    </div>

                    {candidateError ? (
                      <div className="replacement-candidate-empty">
                        {candidateError}
                      </div>
                    ) : recommendedCandidates.length === 0 ? (
                      <div className="replacement-candidate-empty">
                        Chưa có ứng viên hợp lệ chưa được mời và không trùng lịch.
                      </div>
                    ) : (
                      <div className="replacement-candidate-list">
                        {recommendedCandidates.map((candidate) => (
                          <article
                            key={candidate.employeeId}
                            className="replacement-candidate-row"
                          >
                            <div className="replacement-candidate-rank">
                              #{candidate.rank}
                            </div>
                            <div className="replacement-candidate-main">
                              <div className="replacement-candidate-name">
                                <strong>
                                  {candidate.employeeCode} · {candidate.fullName}
                                </strong>
                                <span>{candidate.totalScore}/100 điểm phù hợp</span>
                              </div>
                              <div className="replacement-candidate-metrics">
                                <span>Uy tín {candidate.reputationScore}/100</span>
                                <span>Ổn định {candidate.reliabilityPercent}/100</span>
                                <span>{candidate.completedShiftCount} ca hoàn thành</span>
                                <span>
                                  {candidate.sameRoleCompletedCount} ca cùng vai trò
                                </span>
                              </div>
                              <details className="replacement-candidate-reasons">
                                <summary>Giải thích điểm</summary>
                                <ul>
                                  {(candidate.reasons || []).map((reasonItem) => (
                                    <li key={reasonItem}>{reasonItem}</li>
                                  ))}
                                </ul>
                              </details>
                            </div>
                            <button
                              type="button"
                              className="primary-button"
                              disabled={saving}
                              onClick={() => handleInvite(request, candidate.employeeId)}
                            >
                              Mời thay ca
                            </button>
                          </article>
                        ))}
                      </div>
                    )}
                  </div>
                )}

                {(request.invitations || []).length > 0 && (
                  <div className="replacement-invitation-history">
                    <strong>Lời mời đã gửi</strong>
                    {(request.invitations || []).map((item) => (
                      <div key={item.id} className="replacement-invitation-row">
                        <span>{item.employeeName}</span>
                        <span>{invitationStatusLabels[item.status] || item.status}</span>
                        <small>{formatDateTime(item.invitedAt)}</small>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </article>
          );
        })}
      </div>

      {!isManager && invitations.length > pendingInvitations.length && (
        <div className="replacement-response-history">
          <h2 className="section-title">Lịch sử lời mời</h2>
          {invitations
            .filter((item) => item.status !== 'PENDING')
            .map((item) => (
              <div className="table-card replacement-history-row" key={item.id}>
                <div>
                  <strong>{item.shiftName}</strong>
                  <span>{item.eventName} · {formatDateTime(item.startAt)}</span>
                </div>
                <span>{invitationStatusLabels[item.status] || item.status}</span>
              </div>
            ))}
        </div>
      )}
    </section>
  );
}

export default ReplacementPage;
