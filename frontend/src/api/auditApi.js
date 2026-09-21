import axiosClient from './axiosClient';

export const getAuditLogs = async (params = {}) => {
  const response = await axiosClient.get('/audit-logs', { params });
  return response.data;
};
