import axiosClient from './axiosClient';

export const getReplacementRequests = async () => {
  const response = await axiosClient.get('/replacements/requests');
  return response.data;
};


export const getReplacementCandidates = async (id) => {
  const response = await axiosClient.get(`/replacements/requests/${id}/candidates`);
  return response.data;
};

export const getAiReplacementRecommendation = async (id) => {
  const response = await axiosClient.post(`/replacements/requests/${id}/ai-recommendation`);
  return response.data;
};

export const getMyReplacementRequests = async () => {
  const response = await axiosClient.get('/replacements/requests/mine');
  return response.data;
};

export const getMyReplacementInvitations = async () => {
  const response = await axiosClient.get('/replacements/invitations/mine');
  return response.data;
};

export const getMyReplacementEligibleAssignments = async () => {
  const response = await axiosClient.get('/replacements/eligible-assignments/mine');
  return response.data;
};

export const createReplacementRequest = async (payload) => {
  const response = await axiosClient.post('/replacements/requests', payload);
  return response.data;
};

export const reviewReplacementRequest = async (id, payload) => {
  const response = await axiosClient.put(`/replacements/requests/${id}/review`, payload);
  return response.data;
};

export const inviteReplacementEmployee = async (id, employeeId) => {
  const response = await axiosClient.post(`/replacements/requests/${id}/invitations`, {
    employeeId,
  });
  return response.data;
};

export const respondReplacementInvitation = async (id, payload) => {
  const response = await axiosClient.put(`/replacements/invitations/${id}/respond`, payload);
  return response.data;
};
