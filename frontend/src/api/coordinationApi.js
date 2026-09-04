import axiosClient from './axiosClient';

export const getCoordinationOverview = async ({
  date,
  fromTime = null,
  toTime = null,
  venueId = null,
  transitionBufferMinutes = 60,
}) => {
  const response = await axiosClient.get('/coordination/overview', {
    params: {
      date,
      fromTime: fromTime || undefined,
      toTime: toTime || undefined,
      venueId: venueId || undefined,
      transitionBufferMinutes,
    },
  });

  return response.data;
};
