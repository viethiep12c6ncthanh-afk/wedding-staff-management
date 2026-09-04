const PREFIX = 'WSMATTEND';
const VERSION = '1';

export const buildAttendanceQrPayload = ({ shiftId, action, qrToken }) =>
  [PREFIX, VERSION, action, shiftId, qrToken].join('|');

export const parseAttendanceQrPayload = (rawValue) => {
  const value = String(rawValue || '').trim();

  if (!value) {
    return null;
  }

  const parts = value.split('|');

  if (parts.length === 5 && parts[0] === PREFIX && parts[1] === VERSION) {
    const [, , action, shiftIdRaw, qrToken] = parts;
    const shiftId = Number(shiftIdRaw);

    if (
      !['CHECK_IN', 'CHECK_OUT'].includes(action) ||
      !Number.isInteger(shiftId) ||
      shiftId <= 0 ||
      !qrToken
    ) {
      return null;
    }

    return {
      action,
      shiftId,
      qrToken,
      wrapped: true,
    };
  }

  // Raw-token fallback for manual/demo use. Shift/action must be supplied separately.
  return {
    action: null,
    shiftId: null,
    qrToken: value,
    wrapped: false,
  };
};
