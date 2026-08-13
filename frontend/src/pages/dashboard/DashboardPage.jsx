import { useNavigate } from 'react-router-dom';

function DashboardPage() {
    const navigate = useNavigate();

    const handleLogout = () => {
        localStorage.removeItem('accessToken');
        localStorage.removeItem('currentUser');

        navigate('/login', {
            replace: true,
        });
    };

    return (
        <div className="temporary-dashboard">
            <h1>Dashboard</h1>

            <p>Frontend đã đăng nhập thành công vào hệ thống.</p>

            <button
                type="button"
                onClick={handleLogout}
            >
                Đăng xuất
            </button>
        </div>
    );
}

export default DashboardPage;