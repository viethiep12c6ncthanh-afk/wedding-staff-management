export const formatDateTime = (value) => {
  if (!value) {
    return '—';
  }

  const date = new Date(value);

  if (Number.isNaN(date.getTime())) {
    return value;
  }

  return new Intl.DateTimeFormat('vi-VN', {
    dateStyle: 'short',
    timeStyle: 'short',
  }).format(date);
};

export const formatMoney = (value) => {
  const number = Number(value ?? 0);

  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
    maximumFractionDigits: 0,
  }).format(number);
};

export const toDateTimeLocalValue = (value) => {
  if (!value) {
    return '';
  }

  return String(value).slice(0, 16);
};
