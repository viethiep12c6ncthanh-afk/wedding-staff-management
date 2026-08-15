import { Navigate, Outlet } from 'react-router-dom';

const VALID_ROLES = new Set([
    'ADMIN',
    'COORDINATOR',
    'EMPLOYEE',
]);

const readCurrentUser = () => {
    try {
        const raw = localStorage.getItem('currentUser');
        return raw ? JSON.parse(raw) : null;
    } catch {
        return null;
    }
};

function ProtectedRoute() {
    const token = localStorage.getItem('accessToken');
    const currentUser = readCurrentUser();

    if (
        !token ||
        !currentUser?.role ||
        !VALID_ROLES.has(currentUser.role)
    ) {
        localStorage.removeItem('accessToken');
        localStorage.removeItem('currentUser');

        return <Navigate to="/login" replace />;
    }

    return <Outlet />;
}

export default ProtectedRoute;
