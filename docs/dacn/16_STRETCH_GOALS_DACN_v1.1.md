# DACN v1.1 — Stretch Goals Status

## Mục tiêu

Phiên bản 1.1 tiếp tục tạo khác biệt rõ ràng so với DACS mà không làm suy yếu
các luồng bắt buộc đã nghiệm thu ở DACN v1.0.

## Đã triển khai

### Operational audit log

- Tự động ghi mọi request `POST`, `PUT`, `PATCH`, `DELETE` dưới `/api/**`.
- Lưu tài khoản, vai trò, action, resource path, HTTP status, kết quả, IP và thời điểm.
- Audit chạy transaction riêng và không làm hỏng nghiệp vụ chính nếu việc ghi log thất bại.
- Chỉ `ADMIN` được xem toàn bộ audit log.
- Có lọc theo tài khoản/action, phân trang và sắp xếp mới nhất trước.
- Schema: `V013__operational_audit.sql`.

### Thông báo vận hành

- Chuông thông báo xuất hiện trên topbar.
- Tự làm mới mỗi 10 giây và báo có hoạt động mới.
- `ADMIN`/`COORDINATOR` xem hoạt động hệ thống gần nhất.
- `EMPLOYEE` chỉ xem hoạt động do chính tài khoản của mình thực hiện.
- Đây là near-real-time polling; chưa tuyên bố là WebSocket/SSE.

### Xuất báo cáo

- Xuất CSV UTF-8 có BOM, mở trực tiếp bằng Excel và giữ đúng tiếng Việt.
- Tên file chứa khoảng thời gian báo cáo.
- CSS riêng cho chế độ in, cho phép lưu PDF từ hộp thoại in của trình duyệt.

### Đóng gói triển khai

- Multi-stage Dockerfile cho Spring Boot Java 21.
- Multi-stage Dockerfile cho React/Vite, phục vụ bằng Nginx.
- Nginx reverse proxy `/api` sang backend để tránh hard-code hostname.
- Docker Compose gồm MySQL 8.4, backend và frontend; có healthcheck database,
  volume dữ liệu và cấu hình secret qua environment.
- File `.env.docker.example` chỉ chứa placeholder an toàn.

## Chưa triển khai trong v1.1 hiện tại

- Mutual shift swap (hai nhân viên đổi hai phân công cho nhau).
- AI incident summary độc lập với AI recommendation hiện có.
- WebSocket/SSE realtime thật sự.
- File `.xlsx` native và PDF sinh ở backend.
- Chưa publish lên hạ tầng production công khai; hiện đã có gói Docker triển khai được.

Các mục này không được ghi là hoàn thành cho đến khi có schema, API, UI, test và
kịch bản demo end-to-end tương ứng.

## Quy trình áp dụng

1. Backup database phát triển.
2. Chạy `database/migrations/V013__operational_audit.sql` đúng một lần.
3. Khởi động backend với `JPA_DDL_AUTO=validate`.
4. Đăng nhập ADMIN, thực hiện một thao tác thay đổi dữ liệu.
5. Mở **Nhật ký vận hành** và xác nhận log mới.
6. Kiểm tra chuông thông báo tự cập nhật.
7. Mở **Báo cáo tiền công**, kiểm tra xuất Excel (CSV) và PDF/In.

## Verification hiện tại

- Frontend `npm run build`: PASS.
- Frontend `npm run lint`: PASS.
- `git diff --check`: PASS.
- Backend có thêm unit test cho audit service, nhưng môi trường đóng gói hiện tại
  không tải được Maven dependency từ Maven Central; cần chạy `mvn clean test`
  trên máy phát triển đã có dependency/network trước khi gắn tag release.
