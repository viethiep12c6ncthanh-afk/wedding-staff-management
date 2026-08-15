import axiosClient from './axiosClient';

export const getDashboardSummary = async () => {
    const response = await axiosClient.get('/dashboard/summary');
    return response.data;
};

export const getAttendanceSummary = async () => {
    const response = await axiosClient.get('/dashboard/attendance-summary');
    return response.data;
};