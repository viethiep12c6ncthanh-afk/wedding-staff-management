import {
  BrowserRouter,
  Navigate,
  Route,
  Routes,
} from 'react-router-dom';

import ProtectedRoute from './components/common/ProtectedRoute';
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
            <Route
                path="/dashboard"
                element={<DashboardPage />}
            />
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