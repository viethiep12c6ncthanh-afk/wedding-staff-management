import axiosClient from './axiosClient';

export const getEvents = async () => {
  const response = await axiosClient.get('/events');
  return response.data;
};

export const createEvent = async (payload) => {
  const response = await axiosClient.post('/events', payload);
  return response.data;
};

export const updateEvent = async (id, payload) => {
  const response = await axiosClient.put(`/events/${id}`, payload);
  return response.data;
};

export const changeEventStatus = async (
  id,
  status,
  cancellationReason = null,
) => {
  const response = await axiosClient.patch(`/events/${id}/status`, {
    status,
    cancellationReason,
  });
  return response.data;
};
