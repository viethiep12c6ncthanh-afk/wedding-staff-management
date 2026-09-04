import { useEffect, useRef, useState } from 'react';

const SCAN_INTERVAL_MS = 450;

function QrScanner({ disabled = false, onDetected }) {
  const videoRef = useRef(null);
  const streamRef = useRef(null);
  const timerRef = useRef(null);
  const detectingRef = useRef(false);

  const [scanning, setScanning] = useState(false);
  const [error, setError] = useState('');

  const stopScanner = () => {
    if (timerRef.current) {
      window.clearInterval(timerRef.current);
      timerRef.current = null;
    }

    streamRef.current?.getTracks().forEach((track) => track.stop());
    streamRef.current = null;

    if (videoRef.current) {
      videoRef.current.srcObject = null;
    }

    detectingRef.current = false;
    setScanning(false);
  };

  useEffect(() => () => stopScanner(), []);

  const startScanner = async () => {
    setError('');

    if (!('BarcodeDetector' in window)) {
      setError(
        'Trình duyệt này chưa hỗ trợ quét QR trực tiếp. Hãy dùng Chrome mới hoặc dán nội dung QR vào ô bên dưới.',
      );
      return;
    }

    if (!navigator.mediaDevices?.getUserMedia) {
      setError('Trình duyệt không cho phép truy cập camera.');
      return;
    }

    try {
      const supportedFormats = window.BarcodeDetector.getSupportedFormats
        ? await window.BarcodeDetector.getSupportedFormats()
        : ['qr_code'];

      if (!supportedFormats.includes('qr_code')) {
        setError('Trình duyệt không hỗ trợ định dạng QR.');
        return;
      }

      const detector = new window.BarcodeDetector({ formats: ['qr_code'] });
      const stream = await navigator.mediaDevices.getUserMedia({
        video: {
          facingMode: { ideal: 'environment' },
        },
        audio: false,
      });

      streamRef.current = stream;

      if (!videoRef.current) {
        stream.getTracks().forEach((track) => track.stop());
        return;
      }

      videoRef.current.srcObject = stream;
      await videoRef.current.play();
      setScanning(true);

      timerRef.current = window.setInterval(async () => {
        if (
          detectingRef.current ||
          !videoRef.current ||
          videoRef.current.readyState < 2
        ) {
          return;
        }

        detectingRef.current = true;

        try {
          const barcodes = await detector.detect(videoRef.current);
          const rawValue = barcodes?.[0]?.rawValue;

          if (rawValue) {
            stopScanner();
            onDetected(rawValue);
          }
        } catch {
          // Keep scanning; transient frame decode errors are expected.
        } finally {
          detectingRef.current = false;
        }
      }, SCAN_INTERVAL_MS);
    } catch (err) {
      stopScanner();
      setError(
        err?.name === 'NotAllowedError'
          ? 'Bạn chưa cấp quyền camera cho trình duyệt.'
          : 'Không thể mở camera để quét QR.',
      );
    }
  };

  return (
    <div className="attendance-scanner">
      <div className={`attendance-video-wrap ${scanning ? 'active' : ''}`}>
        <video
          ref={videoRef}
          className="attendance-video"
          muted
          playsInline
        />
      </div>

      <div className="row-actions row-actions-wrap">
        {!scanning ? (
          <button
            type="button"
            className="secondary-button"
            onClick={startScanner}
            disabled={disabled}
          >
            Mở camera quét QR
          </button>
        ) : (
          <button
            type="button"
            className="secondary-button"
            onClick={stopScanner}
          >
            Dừng camera
          </button>
        )}
      </div>

      {error && <div className="error-box attendance-inline-error">{error}</div>}
    </div>
  );
}

export default QrScanner;
