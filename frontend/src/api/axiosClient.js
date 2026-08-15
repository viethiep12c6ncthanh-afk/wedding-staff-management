import axios from 'axios';

const apiBaseUrl = (
    import.meta.env.VITE_API_BASE_URL ||
    'http://localhost:8080/api'
).replace(/\/+$/, '');

const clearSession = () => {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('currentUser');
};

const axiosClient = axios.create({
    baseURL: apiBaseUrl,
    headers: {
        'Content-Type': 'application/json',
    },
});

axiosClient.interceptors.request.use(
    (config) => {
        const token = localStorage.getItem('accessToken');

        if (token) {
            config.headers.Authorization = `Bearer ${token}`;
        }

        return config;
    },
    (error) => Promise.reject(error),
);

axiosClient.interceptors.response.use(
    (response) => response,
    (error) => {
        if (error.response?.status === 401) {
            clearSession();

            if (window.location.pathname !== '/login') {
                window.location.replace('/login');
            }
        }

        return Promise.reject(error);
    },
);

export default axiosClient;
