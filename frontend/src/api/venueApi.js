import axiosClient from './axiosClient';

export const getVenues = async () => {
  const response = await axiosClient.get('/venues');
  return response.data;
};

export const createVenue = async (payload) => {
  const response = await axiosClient.post('/venues', payload);
  return response.data;
};

export const updateVenue = async (id, payload) => {
  const response = await axiosClient.put(`/venues/${id}`, payload);
  return response.data;
};

export const changeVenueStatus = async (id, status) => {
  const response = await axiosClient.patch(`/venues/${id}/status`, {
    status,
  });
  return response.data;
};

export const deactivateVenue = async (id) => {
  await axiosClient.delete(`/venues/${id}`);
};
