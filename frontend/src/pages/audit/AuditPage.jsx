import { useCallback, useEffect, useState } from 'react';
import { getAuditLogs } from '../../api/auditApi';
import { getApiErrorMessage } from '../../utils/apiError';

const actionLabels = { POST: 'Tạo / xử lý', PUT: 'Cập nhật', PATCH: 'Chuyển trạng thái', DELETE: 'Xóa' };

function AuditPage() {
  const [data, setData] = useState({ content: [], totalElements: 0, totalPages: 0, page: 0 });
  const [filters, setFilters] = useState({ actor: '', action: '' });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const load = useCallback(async (page = 0) => {
    setLoading(true);
    setError('');
    try {
      setData(await getAuditLogs({ ...filters, page, size: 25 }));
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, 'Không tải được nhật ký vận hành.'));
    } finally {
      setLoading(false);
    }
  }, [filters]);

  useEffect(() => { load(0); }, [load]);

  return (
    <section className="page-section">
      <div className="page-header">
        <div><span className="eyebrow">Truy vết hệ thống</span><h1>Nhật ký vận hành</h1>
          <p>Theo dõi các thao tác thay đổi dữ liệu, người thực hiện và kết quả xử lý.</p></div>
        <button className="secondary-button" type="button" onClick={() => load(data.page)}>Làm mới</button>
      </div>

      <div className="filter-panel audit-filter-panel">
        <label>Tài khoản<input value={filters.actor} placeholder="Ví dụ: admin"
          onChange={(event) => setFilters((current) => ({ ...current, actor: event.target.value }))} /></label>
        <label>Thao tác<select value={filters.action}
          onChange={(event) => setFilters((current) => ({ ...current, action: event.target.value }))}>
          <option value="">Tất cả</option><option value="POST">POST</option>
          <option value="PUT">PUT</option><option value="PATCH">PATCH</option><option value="DELETE">DELETE</option>
        </select></label>
      </div>

      {error && <div className="error-banner">{error}</div>}
      <div className="content-card table-card">
        <div className="card-heading"><div><h2>Lịch sử gần nhất</h2><p>{data.totalElements} bản ghi</p></div></div>
        {loading ? <div className="empty-state">Đang tải nhật ký...</div> : data.content.length === 0 ?
          <div className="empty-state">Chưa có thao tác phù hợp bộ lọc.</div> :
          <div className="table-scroll"><table><thead><tr><th>Thời gian</th><th>Người thực hiện</th>
            <th>Thao tác</th><th>Tài nguyên</th><th>Kết quả</th><th>IP</th></tr></thead><tbody>
            {data.content.map((item) => <tr key={item.id}>
              <td>{new Date(item.occurredAt).toLocaleString('vi-VN')}</td>
              <td><strong>{item.actorUsername}</strong><br/><small>{item.actorRole || '—'}</small></td>
              <td><span className="status-badge neutral">{actionLabels[item.action] || item.action}</span></td>
              <td><code>{item.resourcePath}</code></td>
              <td><span className={`status-badge ${item.success ? 'success' : 'danger'}`}>
                {item.success ? 'Thành công' : `Lỗi ${item.httpStatus}`}</span></td><td>{item.clientIp || '—'}</td>
            </tr>)}</tbody></table></div>}
        {data.totalPages > 1 && <div className="pagination-row">
          <button type="button" disabled={data.page === 0} onClick={() => load(data.page - 1)}>Trang trước</button>
          <span>Trang {data.page + 1}/{data.totalPages}</span>
          <button type="button" disabled={data.page + 1 >= data.totalPages} onClick={() => load(data.page + 1)}>Trang sau</button>
        </div>}
      </div>
    </section>
  );
}

export default AuditPage;
