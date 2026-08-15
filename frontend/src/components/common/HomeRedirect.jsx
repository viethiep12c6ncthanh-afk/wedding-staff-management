import { Navigate } from 'react-router-dom';

function HomeRedirect() {
  let role = null;

  try {
    const raw = localStorage.getItem('currentUser');
    role = raw ? JSON.parse(raw)?.role : null;
  } catch {
    role = null;
  }

  return (
    <Navigate
      to={role === 'EMPLOYEE' ? '/registrations' : '/dashboard'}
      replace
    />
  );
}

export default HomeRedirect;
