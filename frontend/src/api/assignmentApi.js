import axiosClient from './axiosClient';

export const getAssignments = async () => {
  const response = await axiosClient.get('/assignments');
  return response.data;
};

export const getMyAssignments = async () => {
  const response = await axiosClient.get('/assignments/mine');
  return response.data;
};

export const createDirectAssignment = async (payload) => {
  const response = await axiosClient.post('/assignments/direct', payload);
  return response.data;
};

export const updateAssignmentPlacement = async (id, payload) => {
  const response = await axiosClient.patch(
    `/assignments/${id}/placement`,
    payload,
  );
  return response.data;
};

export const cancelAssignment = async (id, reason) => {
  const response = await axiosClient.patch(
    `/assignments/${id}/cancel`,
    { reason },
  );
  return response.data;
};
