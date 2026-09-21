import axiosClient from './axiosClient';

export const getRecentNotifications = async () => {
  const response = await axiosClient.get('/notifications/recent');
  return response.data;
};
