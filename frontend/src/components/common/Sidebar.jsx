import { NavLink } from 'react-router-dom';

const menuItems = [
    { path: '/dashboard', label: 'Tổng quan' },
    { path: '/employees', label: 'Nhân viên' },
    { path: '/venues', label: 'Địa điểm' },
    { path: '/events', label: 'Sự kiện' },
    { path: '/shifts', label: 'Ca làm' },
    { path: '/registrations', label: 'Đăng ký ca' },
    { path: '/assignments', label: 'Phân công' },
    { path: '/attendance', label: 'Chấm công' },
    { path: '/reports', label: 'Báo cáo' },
];

function Sidebar() {
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