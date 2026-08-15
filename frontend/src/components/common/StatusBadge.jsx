const labels = {
  ACTIVE: 'Hoạt động',
  INACTIVE: 'Ngừng hoạt động',
  LOCKED: 'Đã khóa',
  ON_LEAVE: 'Tạm nghỉ',

  DRAFT: 'Nháp',
  CONFIRMED: 'Đã xác nhận',
  OPEN: 'Đang mở',
  CLOSED: 'Đã đóng',
  IN_PROGRESS: 'Đang diễn ra',
  COMPLETED: 'Hoàn thành',
  CANCELLED: 'Đã hủy',

  PENDING: 'Chờ duyệt',
  APPROVED: 'Đã duyệt',
  REJECTED: 'Từ chối',

  ASSIGNED: 'Đã phân công',
  ABSENT: 'Vắng mặt',

  PRESENT: 'Có mặt',
  LATE: 'Đi trễ',
  EARLY_LEAVE: 'Về sớm',
  LATE_AND_EARLY_LEAVE: 'Trễ và về sớm',
};

function StatusBadge({ value }) {
  if (!value) {
    return <span className="status-badge">—</span>;
  }

  return (
    <span className={`status-badge status-${value.toLowerCase()}`}>
      {labels[value] || value}
    </span>
  );
}

export default StatusBadge;
