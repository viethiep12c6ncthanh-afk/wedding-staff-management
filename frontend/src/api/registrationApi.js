import axiosClient from './axiosClient';

export const getRegistrations = async () => {
  const response = await axiosClient.get('/registrations');
  return response.data;
};

export const getMyRegistrations = async () => {
  const response = await axiosClient.get('/registrations/mine');
  return response.data;
};

export const registerShift = async (shiftId) => {
  const response = await axiosClient.post('/registrations', {
    shiftId,
  });
  return response.data;
};

export const reviewRegistration = async (id, payload) => {
  const response = await axiosClient.put(
    `/registrations/${id}/review`,
    payload,
  );
  return response.data;
};

export const cancelMyRegistration = async (id, reason) => {
  const response = await axiosClient.patch(
    `/registrations/${id}/cancel`,
    { reason },
  );
  return response.data;
};
