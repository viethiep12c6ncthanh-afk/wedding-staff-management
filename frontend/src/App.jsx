import {
  BrowserRouter,
  Navigate,
  Route,
  Routes,
} from 'react-router-dom';

import PlaceholderPage from './components/common/PlaceholderPage';
import ProtectedRoute from './components/common/ProtectedRoute';

import MainLayout from './layouts/MainLayout';

import LoginPage from './pages/auth/LoginPage';
import DashboardPage from './pages/dashboard/DashboardPage';

function App() {
  return (
      <BrowserRouter>
        <Routes>
          <Route
              path="/login"
              element={<LoginPage />}
          />

          <Route element={<ProtectedRoute />}>
            <Route element={<MainLayout />}>
              <Route
                  path="/dashboard"
                  element={<DashboardPage />}
              />

              <Route
                  path="/employees"
                  element={
                    <PlaceholderPage
                        title="Quản lý nhân viên"
                        description="Quản lý hồ sơ và tài khoản nhân viên."
                    />
                  }
              />

              <Route
                  path="/venues"
                  element={
                    <PlaceholderPage
                        title="Quản lý địa điểm"
                        description="Quản lý địa điểm tổ chức sự kiện."
                    />
                  }
              />

              <Route
                  path="/events"
                  element={
                    <PlaceholderPage
                        title="Quản lý sự kiện"
                        description="Theo dõi và quản lý sự kiện."
                    />
                  }
              />

              <Route
                  path="/shifts"
                  element={
                    <PlaceholderPage
                        title="Quản lý ca làm"
                        description="Quản lý các ca làm thuộc sự kiện."
                    />
                  }
              />

              <Route
                  path="/registrations"
                  element={
                    <PlaceholderPage
                        title="Đăng ký ca"
                        description="Theo dõi đăng ký ca của nhân viên."
                    />
                  }
              />

              <Route
                  path="/assignments"
                  element={
                    <PlaceholderPage
                        title="Phân công"
                        description="Quản lý nhân sự được phân công vào ca."
                    />
                  }
              />

              <Route
                  path="/attendance"
                  element={
                    <PlaceholderPage
                        title="Chấm công"
                        description="Theo dõi kết quả chấm công."
                    />
                  }
              />

              <Route
                  path="/reports"
                  element={
                    <PlaceholderPage
                        title="Báo cáo"
                        description="Theo dõi tiền công và báo cáo."
                    />
                  }
              />
            </Route>
          </Route>

          <Route
              path="/"
              element={<Navigate to="/dashboard" replace />}
          />

          <Route
              path="*"
              element={<Navigate to="/dashboard" replace />}
          />
        </Routes>
      </BrowserRouter>
  );
}

export default App;