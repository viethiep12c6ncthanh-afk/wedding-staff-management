import axiosClient from './axiosClient';

export const getAttendances = async () => {
  const response = await axiosClient.get('/attendances');
  return response.data;
};

export const getMyAttendances = async () => {
  const response = await axiosClient.get('/attendances/mine');
  return response.data;
};

export const createAttendance = async (payload) => {
  const response = await axiosClient.post('/attendances', payload);
  return response.data;
};

export const updateAttendance = async (id, payload) => {
  const response = await axiosClient.put(`/attendances/${id}`, payload);
  return response.data;
};

export const confirmAttendance = async (id) => {
  const response = await axiosClient.post(`/attendances/${id}/confirm`);
  return response.data;
};
