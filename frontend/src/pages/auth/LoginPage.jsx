import { useState } from 'react';
import { useNavigate } from 'react-router-dom';

import { login } from '../../api/authApi';

function LoginPage() {
  const navigate = useNavigate();

  const [formData, setFormData] = useState({
    username: '',
    password: '',
  });

  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleChange = (event) => {
    const { name, value } = event.target;

    setFormData((previous) => ({
      ...previous,
      [name]: value,
    }));
  };

  const handleSubmit = async (event) => {
    event.preventDefault();

    setError('');
    setLoading(true);

    try {
      const data = await login(formData);

      localStorage.setItem('accessToken', data.token);
      localStorage.setItem(
        'currentUser',
        JSON.stringify({
          username: data.username ?? formData.username,
          fullName: data.fullName ?? null,
          role: data.role ?? null,
          mustChangePassword: data.mustChangePassword ?? false,
        }),
      );

      navigate(
        data.role === 'EMPLOYEE'
          ? '/registrations'
          : '/dashboard',
        {
          replace: true,
        },
      );
    } catch (err) {
      setError(
        err.response?.data?.message ||
          'Không thể đăng nhập. Vui lòng kiểm tra lại thông tin.',
      );
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="login-page">
      <main className="login-card">
        <div className="login-header">
          <h1>Quản lý nhân sự sự kiện</h1>
          <p>Đăng nhập để tiếp tục</p>
        </div>

        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label htmlFor="username">Tên đăng nhập</label>

            <input
              id="username"
              name="username"
              type="text"
              value={formData.username}
              onChange={handleChange}
              autoComplete="username"
              placeholder="Nhập tên đăng nhập"
              required
            />
          </div>

          <div className="form-group">
            <label htmlFor="password">Mật khẩu</label>

            <input
              id="password"
              name="password"
              type="password"
              value={formData.password}
              onChange={handleChange}
              autoComplete="current-password"
              placeholder="Nhập mật khẩu"
              required
            />
          </div>

          {error && (
            <div className="error-message">
              {error}
            </div>
          )}

          <button
            className="login-button"
            type="submit"
            disabled={loading}
          >
            {loading ? 'Đang đăng nhập...' : 'Đăng nhập'}
          </button>
        </form>
      </main>
    </div>
  );
}

export default LoginPage;
