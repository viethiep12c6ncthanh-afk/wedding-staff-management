import { useEffect, useState } from 'react';
import QRCode from 'qrcode';

function QrCodeImage({ value, size = 260 }) {
  const [dataUrl, setDataUrl] = useState('');
  const [error, setError] = useState('');

  useEffect(() => {
    let cancelled = false;

    if (!value) {
      setDataUrl('');
      setError('');
      return undefined;
    }

    QRCode.toDataURL(value, {
      errorCorrectionLevel: 'M',
      margin: 2,
      width: size,
    })
      .then((url) => {
        if (!cancelled) {
          setDataUrl(url);
          setError('');
        }
      })
      .catch(() => {
        if (!cancelled) {
          setDataUrl('');
          setError('Không thể tạo mã QR.');
        }
      });

    return () => {
      cancelled = true;
    };
  }, [size, value]);

  if (error) {
    return <div className="error-box attendance-inline-error">{error}</div>;
  }

  if (!dataUrl) {
    return <div className="attendance-qr-placeholder">Đang tạo mã QR...</div>;
  }

  return (
    <img
      className="attendance-qr-image"
      src={dataUrl}
      alt="Mã QR chấm công"
      width={size}
      height={size}
    />
  );
}

export default QrCodeImage;
