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
import EmployeesPage from './pages/employees/EmployeesPage';
import VenuesPage from './pages/venues/VenuesPage';
import EventsPage from './pages/events/EventsPage';
import ShiftsPage from './pages/shifts/ShiftsPage';

import './styles/crud.css';

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
              element={<EmployeesPage />}
            />

            <Route
              path="/venues"
              element={<VenuesPage />}
            />

            <Route
              path="/events"
              element={<EventsPage />}
            />

            <Route
              path="/shifts"
              element={<ShiftsPage />}
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
