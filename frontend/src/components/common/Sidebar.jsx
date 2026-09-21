import { NavLink } from 'react-router-dom';

const managementGroups = [
  {
    label: 'Tổng quan',
    items: [
      { path: '/dashboard', label: 'Tổng quan' },
      { path: '/coordination', label: 'Điều phối' },
    ],
  },
  {
    label: 'Nhân sự',
    items: [
      { path: '/employees', label: 'Nhân viên' },
      { path: '/reputation', label: 'Uy tín nhân viên' },
      { path: '/replacements', label: 'Thay thế nhân sự' },
    ],
  },
  {
    label: 'Vận hành',
    items: [
      { path: '/venues', label: 'Địa điểm' },
      { path: '/events', label: 'Sự kiện' },
      { path: '/shifts', label: 'Ca làm' },
      { path: '/registrations', label: 'Đăng ký ca' },
      { path: '/assignments', label: 'Phân công' },
      { path: '/attendance', label: 'Chấm công' },
    ],
  },
  {
    label: 'Báo cáo',
    items: [
      { path: '/reports', label: 'Báo cáo tiền công' },
      { path: '/audit', label: 'Nhật ký vận hành', roles: ['ADMIN'] },
    ],
  },
];

const employeeGroups = [
  {
    label: 'Công việc',
    items: [
      { path: '/registrations', label: 'Đăng ký ca' },
      { path: '/my-assignments', label: 'Phân công của tôi' },
      { path: '/replacements', label: 'Yêu cầu thay ca' },
      { path: '/attendance', label: 'Chấm công của tôi' },
    ],
  },
  {
    label: 'Cá nhân',
    items: [
      { path: '/reputation', label: 'Uy tín của tôi' },
      { path: '/reports', label: 'Tiền công của tôi' },
    ],
  },
];

const getCurrentRole = () => {
  try {
    const raw = localStorage.getItem('currentUser');
    return raw ? JSON.parse(raw)?.role : null;
  } catch {
    return null;
  }
};

function Sidebar({ open = false, onNavigate, onClose }) {
  const role = getCurrentRole();
  const groups = role === 'EMPLOYEE' ? employeeGroups : managementGroups;

  return (
    <aside className={`sidebar ${open ? 'open' : ''}`} aria-label="Điều hướng chính">
      <div className="sidebar-brand">
        <div className="brand-mark">WS</div>

        <div className="sidebar-brand-copy">
          <strong>Wedding Staff</strong>
          <span>Management</span>
        </div>

        <button
          type="button"
          className="sidebar-close-button"
          onClick={onClose}
          aria-label="Đóng menu"
        >
          ×
        </button>
      </div>

      <nav className="sidebar-nav">
        {groups.map((group) => (
          <div className="sidebar-group" key={group.label}>
            <div className="sidebar-group-label">{group.label}</div>

            <div className="sidebar-group-links">
              {group.items.filter((item) => !item.roles || item.roles.includes(role)).map((item) => (
                <NavLink
                  key={item.path}
                  to={item.path}
                  onClick={onNavigate}
                  className={({ isActive }) =>
                    `sidebar-link ${isActive ? 'active' : ''}`
                  }
                >
                  {item.label}
                </NavLink>
              ))}
            </div>
          </div>
        ))}
      </nav>
    </aside>
  );
}

export default Sidebar;
