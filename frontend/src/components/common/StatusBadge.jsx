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
