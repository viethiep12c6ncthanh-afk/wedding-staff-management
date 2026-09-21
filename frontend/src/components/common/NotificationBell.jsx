import { useEffect, useRef, useState } from 'react';
import { getRecentNotifications } from '../../api/notificationApi';

const actionText = { POST: 'đã tạo/xử lý', PUT: 'đã cập nhật', PATCH: 'đã đổi trạng thái', DELETE: 'đã xóa' };

function NotificationBell() {
  const [items, setItems] = useState([]);
  const [open, setOpen] = useState(false);
  const [unread, setUnread] = useState(0);
  const latestId = useRef(null);

  useEffect(() => {
    let active = true;
    const load = async () => {
      try {
        const data = await getRecentNotifications();
        if (!active) return;
        if (latestId.current && data[0]?.id && data[0].id !== latestId.current) setUnread((value) => value + 1);
        latestId.current = data[0]?.id || latestId.current;
        setItems(data);
      } catch {
        // Notification polling is non-blocking; page workflows remain available.
      }
    };
    load();
    const timer = window.setInterval(load, 10000);
    return () => { active = false; window.clearInterval(timer); };
  }, []);

  const toggle = () => { setOpen((value) => !value); setUnread(0); };

  return <div className="notification-center">
    <button type="button" className="notification-button" onClick={toggle} aria-label="Thông báo vận hành">
      🔔{unread > 0 && <span>{unread > 9 ? '9+' : unread}</span>}
    </button>
    {open && <div className="notification-popover">
      <div className="notification-heading"><strong>Thông báo vận hành</strong><small>Tự cập nhật mỗi 10 giây</small></div>
      {items.length === 0 ? <p className="notification-empty">Chưa có hoạt động mới.</p> : items.map((item) =>
        <article key={item.id}><div><strong>{item.actorUsername}</strong> {actionText[item.action] || item.action}</div>
          <code>{item.resourcePath}</code><small>{new Date(item.occurredAt).toLocaleString('vi-VN')}</small></article>)}
    </div>}
  </div>;
}

export default NotificationBell;
