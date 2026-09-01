import { NavLink } from 'react-router-dom';

const managementItems = [
  { path: '/dashboard', label: 'Tổng quan' },
  { path: '/coordination', label: 'Điều phối' },
  { path: '/replacements', label: 'Thay thế nhân sự' },
  { path: '/reputation', label: 'Uy tín nhân viên' },
  { path: '/employees', label: 'Nhân viên' },
  { path: '/venues', label: 'Địa điểm' },
  { path: '/events', label: 'Sự kiện' },
  { path: '/shifts', label: 'Ca làm' },
  { path: '/registrations', label: 'Đăng ký ca' },
  { path: '/assignments', label: 'Phân công' },
  { path: '/attendance', label: 'Chấm công' },
  { path: '/reports', label: 'Báo cáo tiền công' },
];

const employeeItems = [
  { path: '/registrations', label: 'Đăng ký ca' },
  { path: '/my-assignments', label: 'Phân công của tôi' },
  { path: '/replacements', label: 'Yêu cầu thay ca' },
  { path: '/attendance', label: 'Chấm công của tôi' },
  { path: '/reputation', label: 'Uy tín của tôi' },
  { path: '/reports', label: 'Tiền công của tôi' },
];

const getCurrentRole = () => {
  try {
    const raw = localStorage.getItem('currentUser');
    return raw ? JSON.parse(raw)?.role : null;
  } catch {
    return null;
  }
};

function Sidebar() {
  const role = getCurrentRole();
  const menuItems =
    role === 'EMPLOYEE' ? employeeItems : managementItems;

  return (
    <aside className="sidebar">
      <div className="sidebar-brand">
        <div className="brand-mark">WS</div>

        <div>
          <strong>Wedding Staff</strong>
          <span>Management</span>
        </div>
      </div>

      <nav className="sidebar-nav">
        {menuItems.map((item) => (
          <NavLink
            key={item.path}
            to={item.path}
            className={({ isActive }) =>
              `sidebar-link ${isActive ? 'active' : ''}`
            }
          >
            {item.label}
          </NavLink>
        ))}
      </nav>
    </aside>
  );
}

export default Sidebar;
