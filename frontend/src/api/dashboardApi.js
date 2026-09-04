import axiosClient from './axiosClient';

const buildDateParams = (from, to) => ({
  from,
  to,
});

export const getDashboardSummary = async () => {
  const response = await axiosClient.get('/dashboard/summary');
  return response.data;
};

export const getAttendanceSummary = async () => {
  const response = await axiosClient.get('/dashboard/attendance-summary');
  return response.data;
};

export const getOperationsAnalytics = async ({
  from,
  to,
  venueId = null,
}) => {
  const response = await axiosClient.get('/dashboard/operations', {
    params: {
      ...buildDateParams(from, to),
      venueId: venueId || undefined,
    },
  });

  return response.data;
};

export const getWorkforceAnalytics = async ({ from, to }) => {
  const response = await axiosClient.get('/dashboard/workforce', {
    params: buildDateParams(from, to),
  });

  return response.data;
};

export const getAiAnalytics = async ({ from, to }) => {
  const response = await axiosClient.get('/dashboard/ai', {
    params: buildDateParams(from, to),
  });

  return response.data;
};

export const getDashboardVenues = async () => {
  const response = await axiosClient.get('/venues');
  return response.data;
};
