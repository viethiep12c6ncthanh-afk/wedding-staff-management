import { useEffect, useState } from 'react';

import {
    getAttendanceSummary,
    getDashboardSummary,
} from '../../api/dashboardApi';

function StatCard({ label, value, detail }) {
    return (
        <article className="stat-card">
            <span className="stat-label">{label}</span>

            <strong className="stat-value">
                {value ?? 0}
            </strong>

            {detail && (
                <span className="stat-detail">
          {detail}
        </span>
            )}
        </article>
    );
}

function DashboardPage() {
    const [summary, setSummary] = useState(null);
    const [attendance, setAttendance] = useState(null);

    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');

    useEffect(() => {
        const loadDashboard = async () => {
            try {
                setLoading(true);
                setError('');

                const [summaryData, attendanceData] =
                    await Promise.all([
                        getDashboardSummary(),
                        getAttendanceSummary(),
                    ]);

                setSummary(summaryData);
                setAttendance(attendanceData);
            } catch (err) {
                setError(
                    err.response?.data?.message ||
                    'Không thể tải dữ liệu Dashboard.',
                );
            } finally {
                setLoading(false);
            }
        };

        loadDashboard();
    }, []);

    if (loading) {
        return (
            <div className="dashboard-state">
                Đang tải dữ liệu Dashboard...
            </div>
        );
    }

    if (error) {
        return (
            <div className="dashboard-error">
                <strong>Không thể tải Dashboard</strong>
                <p>{error}</p>
            </div>
        );
    }

    return (
        <section className="dashboard-page">
            <div className="page-heading">
                <div>
                    <h1>Tổng quan</h1>

                    <p>
                        Theo dõi nhanh nhân sự, sự kiện, ca làm
                        và tình trạng chấm công.
                    </p>
                </div>
            </div>

            <h2 className="section-title">
                Tổng quan hệ thống
            </h2>

            <div className="stats-grid">
                <StatCard
                    label="Tổng nhân viên"
                    value={summary?.totalEmployees}
                    detail={`${summary?.activeEmployees ?? 0} đang hoạt động`}
                />

                <StatCard
                    label="Địa điểm"
                    value={summary?.totalVenues}
                    detail={`${summary?.activeVenues ?? 0} đang hoạt động`}
                />

                <StatCard
                    label="Sự kiện"
                    value={summary?.totalEvents}
                    detail={`${summary?.confirmedEvents ?? 0} đã xác nhận`}
                />

                <StatCard
                    label="Ca làm"
                    value={summary?.totalShifts}
                    detail={`${summary?.openShifts ?? 0} đang mở`}
                />

                <StatCard
                    label="Đăng ký chờ duyệt"
                    value={summary?.pendingRegistrations}
                />

                <StatCard
                    label="Phân công đang hoạt động"
                    value={summary?.activeAssignments}
                />
            </div>

            <h2 className="section-title">
                Chấm công
            </h2>

            <div className="stats-grid">
                <StatCard
                    label="Tổng bản ghi"
                    value={attendance?.totalAttendances}
                />

                <StatCard
                    label="Đã xác nhận"
                    value={attendance?.confirmedCount}
                    detail={`${attendance?.draftCount ?? 0} bản nháp`}
                />

                <StatCard
                    label="Có mặt"
                    value={attendance?.presentCount}
                />

                <StatCard
                    label="Đi trễ"
                    value={attendance?.lateCount}
                />

                <StatCard
                    label="Về sớm"
                    value={attendance?.earlyLeaveCount}
                />

                <StatCard
                    label="Trễ và về sớm"
                    value={attendance?.lateAndEarlyLeaveCount}
                />

                <StatCard
                    label="Vắng mặt"
                    value={attendance?.absentCount}
                />
            </div>
        </section>
    );
}

export default DashboardPage;