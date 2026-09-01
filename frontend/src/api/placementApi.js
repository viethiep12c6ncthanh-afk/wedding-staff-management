import axiosClient from './axiosClient';

export const getShiftAreas = async (shiftId) => {
  const response = await axiosClient.get('/placements/areas', {
    params: { shiftId },
  });
  return response.data;
};

export const createShiftArea = async (payload) => {
  const response = await axiosClient.post('/placements/areas', payload);
  return response.data;
};

export const updateShiftArea = async (id, payload) => {
  const response = await axiosClient.put(`/placements/areas/${id}`, payload);
  return response.data;
};

export const changeShiftAreaStatus = async (id, status) => {
  const response = await axiosClient.patch(
    `/placements/areas/${id}/status`,
    { status },
  );
  return response.data;
};

export const createShiftTable = async (payload) => {
  const response = await axiosClient.post('/placements/tables', payload);
  return response.data;
};

export const updateShiftTable = async (id, payload) => {
  const response = await axiosClient.put(`/placements/tables/${id}`, payload);
  return response.data;
};

export const changeShiftTableStatus = async (id, status) => {
  const response = await axiosClient.patch(
    `/placements/tables/${id}/status`,
    { status },
  );
  return response.data;
};
