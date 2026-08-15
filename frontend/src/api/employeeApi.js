import axiosClient from './axiosClient';

export const getEmployees = async (params = {}) => {
  const response = await axiosClient.get('/employees', { params });
  return response.data;
};

export const createEmployee = async (payload) => {
  const response = await axiosClient.post('/employees', payload);
  return response.data;
};

export const updateEmployee = async (id, payload) => {
  const response = await axiosClient.put(`/employees/${id}`, payload);
  return response.data;
};

export const changeEmployeeStatus = async (id, status) => {
  const response = await axiosClient.patch(
    `/employees/${id}/employment-status`,
    { status },
  );
  return response.data;
};

export const changeAccountStatus = async (id, status) => {
  const response = await axiosClient.patch(
    `/employees/${id}/account-status`,
    { status },
  );
  return response.data;
};
