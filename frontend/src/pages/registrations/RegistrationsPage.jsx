import { useEffect, useMemo, useState } from 'react';

import { getShifts } from '../../api/shiftApi';
import {
  cancelMyRegistration,
  getMyRegistrations,
  getRegistrations,
  registerShift,
  reviewRegistration,
} from '../../api/registrationApi';
import ActionDialog from '../../components/common/ActionDialog';
import Modal from '../../components/common/Modal';
import StatusBadge from '../../components/common/StatusBadge';
import { getApiErrorMessage } from '../../utils/apiError';
import {
  formatDateTime,
  formatMoney,
} from '../../utils/formatters';

const getCurrentRole = () => {
  try {
    const raw = localStorage.getItem('currentUser');
    return raw ? JSON.parse(raw)?.role : null;
  } catch {
    return null;
  }
};

function RegistrationsPage() {
  const role = getCurrentRole();
  const isEmployee = role === 'EMPLOYEE';

  const [registrations, setRegistrations] = useState([]);
  const [shifts, setShifts] = useState([]);

  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');

  const [reviewing, setReviewing] = useState(null);
  const [cancelTarget, setCancelTarget] = useState(null);
  const [reviewForm, setReviewForm] = useState({
    approved: true,
    shiftRole: 'STAFF',
    task: '',
    rejectionReason: '',
  });

  const loadData = async () => {
    try {
      setLoading(true);
      setError('');

      if (isEmployee) {
        const [registrationData, shiftData] = await Promise.all([
          getMyRegistrations(),
          getShifts(),
        ]);

        setRegistrations(registrationData);
        setShifts(shiftData);
      } else {
        setRegistrations(await getRegistrations());
      }
    } catch (err) {
      setError(
        getApiErrorMessage(
          err,
          'Không thể tải dữ liệu đăng ký ca.',
        ),
      );
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isEmployee]);

  const registrationByShift = useMemo(
    () =>
      new Map(
        registrations.map((registration) => [
          registration.shiftId,
          registration,
        ]),
      ),
    [registrations],
  );

  const availableShifts = useMemo(
    () =>
      shifts
        .filter((shift) => {
          if (shift.shiftStatus !== 'OPEN') {
            return false;
          }

          if (!shift.registrationDeadline) {
            return true;
          }

          const deadline = new Date(shift.registrationDeadline);
          return !Number.isNaN(deadline.getTime()) &&
            deadline.getTime() > Date.now();
        })
        .sort(
          (first, second) =>
            new Date(first.startAt).getTime() -
            new Date(second.startAt).getTime(),
        ),
    [shifts],
  );

  const handleRegister = async (shift) => {
    try {
      setSaving(true);
      setError('');
      setNotice('');

      await registerShift(shift.id);
      setNotice(`Đã đăng ký ca "${shift.name}".`);
      await loadData();
    } catch (err) {
      setError(
        getApiErrorMessage(err, 'Không thể đăng ký ca.'),
      );
    } finally {
      setSaving(false);
    }
  };

  const handleCancelMine = (registration) => {
    setCancelTarget(registration);
  };

  const confirmCancelMine = async (reason) => {
    if (!cancelTarget) {
      return;
    }

    try {
      setSaving(true);
      setError('');
      setNotice('');

      await cancelMyRegistration(cancelTarget.id, reason);

      setCancelTarget(null);
      setNotice('Đã hủy đăng ký ca.');
      await loadData();
    } catch (err) {
      setError(
        getApiErrorMessage(err, 'Không thể hủy đăng ký ca.'),
      );
    } finally {
      setSaving(false);
    }
  };

  const openReview = (registration) => {
    setReviewing(registration);
    setReviewForm({
      approved: true,
      shiftRole: 'STAFF',
        task: '',
      rejectionReason: '',
    });
  };

  const handleReviewChange = (event) => {
    const { name, value } = event.target;

    setReviewForm((previous) => ({
      ...previous,
      [name]:
        name === 'approved'
          ? value === 'true'
          : value,
    }));
  };

  const handleReview = async (event) => {
    event.preventDefault();

    try {
      setSaving(true);
      setError('');
      setNotice('');

      const payload = reviewForm.approved
        ? {
            approved: true,
            rejectionReason: null,
            shiftRole: reviewForm.shiftRole,
            task: reviewForm.task || null,
          }
        : {
            approved: false,
            rejectionReason: reviewForm.rejectionReason,
            shiftRole: null,
            task: null,
          };

      await reviewRegistration(reviewing.id, payload);

      setReviewing(null);
      setNotice(
        payload.approved
          ? 'Đã duyệt đăng ký và tạo phân công.'
          : 'Đã từ chối đăng ký.',
      );
      await loadData();
    } catch (err) {
      setError(
        getApiErrorMessage(err, 'Không thể xử lý đăng ký.'),
      );
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return (
      <div className="table-state">
        Đang tải đăng ký ca...
      </div>
    );
  }

  return (
    <section>
      <div className="page-heading">
        <div>
          <h1>Đăng ký ca</h1>
          <p>
            {isEmployee
              ? 'Chọn ca đang mở và theo dõi trạng thái đăng ký của bạn.'
              : 'Duyệt hoặc từ chối đăng ký ca của nhân viên.'}
          </p>
        </div>
      </div>

      {notice && <div className="notice-box">{notice}</div>}
      {error && <div className="error-box">{error}</div>}

      {isEmployee && (
        <>
          <h2 className="section-title">Ca đang mở</h2>

          <div className="workflow-card-grid">
            {availableShifts.length === 0 ? (
              <div className="table-state">
                Hiện không có ca đang mở đăng ký.
              </div>
            ) : (
              availableShifts.map((shift) => {
                const existing = registrationByShift.get(shift.id);

                return (
                  <article className="workflow-card" key={shift.id}>
                    <div className="workflow-card-top">
                      <div>
                        <h3>{shift.name}</h3>
                        <p>{shift.eventName} — {shift.venueName}</p>
                      </div>

                      <StatusBadge value={shift.shiftStatus} />
                    </div>

                    <dl className="detail-list">
                      <div>
                        <dt>Thời gian</dt>
                        <dd>
                          {formatDateTime(shift.startAt)}
                          {' → '}
                          {formatDateTime(shift.endAt)}
                        </dd>
                      </div>

                      <div>
                        <dt>Tiền công</dt>
                        <dd>{formatMoney(shift.payAmount)}</dd>
                      </div>

                      <div>
                        <dt>Hạn đăng ký</dt>
                        <dd>
                          {formatDateTime(shift.registrationDeadline)}
                        </dd>
                      </div>
                    </dl>

                    {existing ? (
                      <div className="workflow-card-footer">
                        <StatusBadge value={existing.status} />
                        <span className="muted-text">
                          Bạn đã có lịch sử đăng ký ca này.
                        </span>
                      </div>
                    ) : (
                      <button
                        type="button"
                        className="primary-button"
                        disabled={saving}
                        onClick={() => handleRegister(shift)}
                      >
                        Đăng ký ca
                      </button>
                    )}
                  </article>
                );
              })
            )}
          </div>

          <h2 className="section-title">
            Đăng ký của tôi
          </h2>
        </>
      )}

      <div className="table-card">
        <div className="table-scroll">
          <table className="data-table">
            <thead>
              <tr>
                {!isEmployee && <th>Nhân viên</th>}
                <th>Ca làm</th>
                <th>Trạng thái</th>
                <th>Ngày đăng ký</th>
                <th>Thông tin xử lý</th>
                <th>Thao tác</th>
              </tr>
            </thead>

            <tbody>
              {registrations.length === 0 ? (
                <tr>
                  <td
                    colSpan={isEmployee ? 5 : 6}
                    className="empty-cell"
                  >
                    Chưa có đăng ký ca.
                  </td>
                </tr>
              ) : (
                registrations.map((registration) => (
                  <tr key={registration.id}>
                    {!isEmployee && (
                      <td>{registration.employeeName}</td>
                    )}

                    <td>
                      <div className="cell-title">
                        {registration.shiftName}
                      </div>
                      <div className="cell-subtitle">
                        ID ca: {registration.shiftId}
                      </div>
                    </td>

                    <td>
                      <StatusBadge value={registration.status} />
                    </td>

                    <td>
                      {formatDateTime(registration.createdAt)}
                    </td>

                    <td>
                      {registration.rejectionReason ||
                        registration.cancellationReason ||
                        (registration.reviewedAt
                          ? `Đã xử lý ${formatDateTime(
                              registration.reviewedAt,
                            )}`
                          : '—')}
                    </td>

                    <td>
                      {isEmployee ? (
                        ['PENDING', 'APPROVED'].includes(
                          registration.status,
                        ) ? (
                          <button
                            type="button"
                            className="danger-button compact-button"
                            disabled={saving}
                            onClick={() =>
                              handleCancelMine(registration)
                            }
                          >
                            Hủy đăng ký
                          </button>
                        ) : (
                          '—'
                        )
                      ) : registration.status === 'PENDING' ? (
                        <button
                          type="button"
                          className="primary-button compact-button"
                          onClick={() => openReview(registration)}
                        >
                          Xử lý
                        </button>
                      ) : (
                        '—'
                      )}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      <Modal
        open={Boolean(reviewing)}
        error={error}
        title={`Xử lý đăng ký #${reviewing?.id ?? ''}`}
        onClose={() => !saving && setReviewing(null)}
        footer={(
          <>
            <button
              type="button"
              className="secondary-button"
              onClick={() => setReviewing(null)}
              disabled={saving}
            >
              Hủy
            </button>

            <button
              type="submit"
              form="review-registration-form"
              className="primary-button"
              disabled={saving}
            >
              {saving ? 'Đang xử lý...' : 'Xác nhận'}
            </button>
          </>
        )}
      >
        <form
          id="review-registration-form"
          className="form-grid form-grid-one"
          onSubmit={handleReview}
        >
          <label>
            Quyết định
            <select
              name="approved"
              value={String(reviewForm.approved)}
              onChange={handleReviewChange}
            >
              <option value="true">Duyệt đăng ký</option>
              <option value="false">Từ chối</option>
            </select>
          </label>

          {reviewForm.approved ? (
            <>
              <label>
                Vai trò trong ca
                <select
                  name="shiftRole"
                  value={reviewForm.shiftRole}
                  onChange={handleReviewChange}
                >
                  <option value="STAFF">Nhân viên</option>
                  <option value="LEADER">Trưởng ca</option>
                </select>
              </label>

              <label>
                Nhiệm vụ
                <textarea
                  name="task"
                  value={reviewForm.task}
                  onChange={handleReviewChange}
                  maxLength="300"
                  rows="3"
                />
              </label>
            </>
          ) : (
            <label>
              Lý do từ chối *
              <textarea
                name="rejectionReason"
                value={reviewForm.rejectionReason}
                onChange={handleReviewChange}
                maxLength="500"
                rows="4"
                required
              />
            </label>
          )}
        </form>
      </Modal>

      <ActionDialog
        open={Boolean(cancelTarget)}
        title="Hủy đăng ký ca"
        message={cancelTarget
          ? `Hủy đăng ký ca "${cancelTarget.shiftName}"? Hệ thống vẫn áp dụng giới hạn thời gian tự hủy hiện có.`
          : ''}
        inputLabel="Lý do hủy"
        required
        danger
        confirmLabel="Hủy đăng ký"
        saving={saving}
        error={error}
        onClose={() => setCancelTarget(null)}
        onConfirm={confirmCancelMine}
      />
    </section>
  );
}

export default RegistrationsPage;
