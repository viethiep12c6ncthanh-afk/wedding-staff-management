import {
  BrowserRouter,
  Route,
  Routes,
} from 'react-router-dom';

import HomeRedirect from './components/common/HomeRedirect';
import ProtectedRoute from './components/common/ProtectedRoute';
import RoleRoute from './components/common/RoleRoute';
import MainLayout from './layouts/MainLayout';

import LoginPage from './pages/auth/LoginPage';
import DashboardPage from './pages/dashboard/DashboardPage';
import CoordinationPage from './pages/coordination/CoordinationPage';
import EmployeesPage from './pages/employees/EmployeesPage';
import VenuesPage from './pages/venues/VenuesPage';
import EventsPage from './pages/events/EventsPage';
import ShiftsPage from './pages/shifts/ShiftsPage';
import RegistrationsPage from './pages/registrations/RegistrationsPage';
import AssignmentsPage from './pages/assignments/AssignmentsPage';
import MyAssignmentsPage from './pages/assignments/MyAssignmentsPage';
import AttendancePage from './pages/attendance/AttendancePage';
import ReportsPage from './pages/reports/ReportsPage';
import ReputationPage from './pages/reputation/ReputationPage';
import ReplacementPage from './pages/replacements/ReplacementPage';
import AuditPage from './pages/audit/AuditPage';

import './styles/crud.css';
import './styles/coordination.css';
import './styles/workflow.css';
import './styles/report.css';
import './styles/reputation.css';
import './styles/replacement.css';

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
              element={
                <RoleRoute
                  allowedRoles={['ADMIN', 'COORDINATOR']}
                />
              }
            >
              <Route
                path="/dashboard"
                element={<DashboardPage />}
              />

              <Route
                path="/coordination"
                element={<CoordinationPage />}
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
                path="/assignments"
                element={<AssignmentsPage />}
              />
              <Route
                path="/audit"
                element={<RoleRoute allowedRoles={['ADMIN']} />}
              >
                <Route index element={<AuditPage />} />
              </Route>
            </Route>

            <Route
              element={
                <RoleRoute
                  allowedRoles={['EMPLOYEE']}
                />
              }
            >
              <Route
                path="/my-assignments"
                element={<MyAssignmentsPage />}
              />
            </Route>

            <Route
              path="/registrations"
              element={<RegistrationsPage />}
            />

            <Route
              path="/attendance"
              element={<AttendancePage />}
            />

            <Route
              path="/reputation"
              element={<ReputationPage />}
            />

            <Route
              path="/replacements"
              element={<ReplacementPage />}
            />

            <Route
              path="/reports"
              element={<ReportsPage />}
            />
          </Route>
        </Route>

        <Route path="/" element={<HomeRedirect />} />
        <Route path="*" element={<HomeRedirect />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
