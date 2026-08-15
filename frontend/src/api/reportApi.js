import axiosClient from './axiosClient';

const buildDateParams = (from, to) => ({
  ...(from ? { from } : {}),
  ...(to ? { to } : {}),
});

export const getPayrollReport = async (from, to) => {
  const response = await axiosClient.get('/reports/payroll', {
    params: buildDateParams(from, to),
  });

  return response.data;
};

export const getMyPayrollReport = async (from, to) => {
  const response = await axiosClient.get('/reports/payroll/mine', {
    params: buildDateParams(from, to),
  });

  return response.data;
};
