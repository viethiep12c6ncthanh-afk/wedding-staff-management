import { Navigate, Outlet } from 'react-router-dom';

const getCurrentRole = () => {
  try {
    const raw = localStorage.getItem('currentUser');
    return raw ? JSON.parse(raw)?.role : null;
  } catch {
    return null;
  }
};

function RoleRoute({ allowedRoles }) {
  const role = getCurrentRole();

  if (allowedRoles.includes(role)) {
    return <Outlet />;
  }

  return (
    <Navigate
      to={role === 'EMPLOYEE' ? '/registrations' : '/dashboard'}
      replace
    />
  );
}

export default RoleRoute;
