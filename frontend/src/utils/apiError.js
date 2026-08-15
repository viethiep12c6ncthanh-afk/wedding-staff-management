export const getApiErrorMessage = (
  error,
  fallback = 'Đã xảy ra lỗi. Vui lòng thử lại.',
) => {
  const data = error?.response?.data;

  if (data?.fieldErrors && typeof data.fieldErrors === 'object') {
    const firstMessage = Object.values(data.fieldErrors)[0];

    if (firstMessage) {
      return firstMessage;
    }
  }

  return data?.message || fallback;
};
