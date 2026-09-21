import { useState } from 'react';
import { Outlet, useNavigate } from 'react-router-dom';

import Sidebar from '../components/common/Sidebar';
import NotificationBell from '../components/common/NotificationBell';

const roleLabels = {
  ADMIN: 'Quản trị viên',
  COORDINATOR: 'Điều phối viên',
  EMPLOYEE: 'Nhân viên',
};

function MainLayout() {
  const navigate = useNavigate();
  const [sidebarOpen, setSidebarOpen] = useState(false);

  const currentUser = (() => {
    try {
      const raw = localStorage.getItem('currentUser');
      return raw ? JSON.parse(raw) : null;
    } catch {
      return null;
    }
  })();

  const handleLogout = () => {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('currentUser');

    navigate('/login', {
      replace: true,
    });
  };

  return (
    <div className="app-shell">
      <Sidebar
        open={sidebarOpen}
        onNavigate={() => setSidebarOpen(false)}
        onClose={() => setSidebarOpen(false)}
      />

      {sidebarOpen && (
        <button
          type="button"
          className="sidebar-overlay"
          onClick={() => setSidebarOpen(false)}
          aria-label="Đóng menu điều hướng"
        />
      )}

      <div className="app-main">
        <header className="topbar">
          <div className="topbar-start">
            <button
              type="button"
              className="mobile-menu-button"
              onClick={() => setSidebarOpen(true)}
              aria-label="Mở menu điều hướng"
              aria-expanded={sidebarOpen}
            >
              ☰
            </button>

            <div className="topbar-title">
              Hệ thống quản lý nhân sự sự kiện
            </div>
          </div>

          <div className="topbar-user">
            <NotificationBell />
            <div className="user-info">
              <strong>
                {currentUser?.fullName ||
                  currentUser?.username ||
                  'Người dùng'}
              </strong>

              {currentUser?.role && (
                <span>
                  {roleLabels[currentUser.role] || currentUser.role}
                </span>
              )}
            </div>

            <button
              type="button"
              className="logout-button"
              onClick={handleLogout}
            >
              Đăng xuất
            </button>
          </div>
        </header>

        <main className="content-area">
          <Outlet />
        </main>
      </div>
    </div>
  );
}

export default MainLayout;
