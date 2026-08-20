import axiosClient from './axiosClient';

export const getReputations = async () => {
  const response = await axiosClient.get('/reputations');
  return response.data;
};

export const getReputation = async (employeeId) => {
  const response = await axiosClient.get(`/reputations/${employeeId}`);
  return response.data;
};

export const getMyReputation = async () => {
  const response = await axiosClient.get('/reputations/mine');
  return response.data;
};

export const createEmployeeEvaluation = async (payload) => {
  const response = await axiosClient.post(
    '/reputations/evaluations',
    payload,
  );
  return response.data;
};
