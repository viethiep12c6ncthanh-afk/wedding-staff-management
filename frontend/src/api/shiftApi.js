import axiosClient from './axiosClient';

export const getShifts = async () => {
  const response = await axiosClient.get('/shifts');
  return response.data;
};

export const createShift = async (payload) => {
  const response = await axiosClient.post('/shifts', payload);
  return response.data;
};

export const updateShift = async (id, payload) => {
  const response = await axiosClient.put(`/shifts/${id}`, payload);
  return response.data;
};

export const changeShiftStatus = async (
  id,
  status,
  cancellationReason = null,
) => {
  const response = await axiosClient.patch(`/shifts/${id}/status`, {
    status,
    cancellationReason,
  });
  return response.data;
};
