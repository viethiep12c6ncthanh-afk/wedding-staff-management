import axiosClient from './axiosClient';

export const getAttendanceDemoConfig = async () => {
  const response = await axiosClient.get('/qr-attendance/demo-config');
  return response.data;
};

export const createAttendanceCheckSession = async (payload) => {
  const response = await axiosClient.post('/qr-attendance/sessions', payload);
  return response.data;
};

export const revokeAttendanceCheckSession = async (id) => {
  await axiosClient.post(`/qr-attendance/sessions/${id}/revoke`);
};

export const selfCheckIn = async (payload) => {
  const response = await axiosClient.post('/qr-attendance/check-in', payload);
  return response.data;
};

export const selfCheckOut = async (payload) => {
  const response = await axiosClient.post('/qr-attendance/check-out', payload);
  return response.data;
};
