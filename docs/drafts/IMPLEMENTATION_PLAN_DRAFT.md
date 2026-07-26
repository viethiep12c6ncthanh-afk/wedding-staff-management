# Kế hoạch thực tế: tối nay và ba tuần còn lại

## Kết luận

Có khả năng hoàn thành một Đồ án cơ sở đạt yêu cầu nếu làm theo thứ tự dọc nghiệp vụ, không mở rộng sang chức năng chuyên ngành và duy trì trung bình 4–6 giờ tập trung mỗi ngày. Không thể hoàn thành toàn bộ hệ thống sạch, đẹp, kiểm thử kỹ chỉ trong một tối.

## Tối nay — Sprint 0

1. Chốt điểm neo phạm vi Đồ án cơ sở.
2. Tạo repository và cấu trúc `backend`, `frontend`, `database`, `docs`.
3. Tạo lược đồ dữ liệu nền tảng.
4. Khởi tạo Spring Boot, JWT và ba vai trò.
5. Tạo API nền cho địa điểm, sự kiện, ca và đăng ký/duyệt ca.
6. Chạy MySQL, sửa `application.yml`, đăng nhập thử bằng Postman.
7. Commit: `chore: initialize DACS baseline and backend foundation`.

## Tuần còn lại 1 — Luồng quản trị nền

- Hoàn thiện tài khoản và hồ sơ nhân viên.
- CRUD địa điểm, sự kiện, ca.
- Kiểm tra dữ liệu đầu vào.
- Viết Postman Collection.
- Frontend đăng nhập và danh sách CRUD cơ bản.

**Mốc demo:** điều phối viên đăng nhập, tạo địa điểm → sự kiện → ca.

## Tuần còn lại 2 — Luồng đăng ký và phân công

- Nhân viên xem ca đang mở.
- Đăng ký/hủy đăng ký.
- Điều phối viên duyệt/từ chối.
- Chặn trùng lịch và chặn vượt số người.
- Tạo phân công sau khi duyệt.
- Giao diện hai vai trò.

**Mốc demo:** hoàn thành quy trình từ tạo ca đến nhân viên được phân công.

## Tuần còn lại 3 — Chấm công, tính công, báo cáo và hoàn thiện

- Chấm công thủ công, ghi trễ/về sớm/vắng.
- Tính tiền công cơ bản từ phân công đã hoàn thành.
- Đánh giá nhân viên cơ bản.
- Thống kê số ca, nhân sự, tiền công.
- Kiểm thử, dữ liệu demo, sửa lỗi.
- Hoàn thiện báo cáo và ảnh giao diện.

**Mốc demo cuối:** chạy được luồng nghiệp vụ hoàn chỉnh và có dữ liệu thống kê.

## Quy tắc giữ tiến độ

- Không làm AI, QR, điểm uy tín hoặc tìm người thay trong Đồ án cơ sở.
- Mỗi ngày phải có ít nhất một commit có ý nghĩa.
- Không chỉnh giao diện đẹp khi API chính chưa chạy.
- Không thêm bảng/chức năng ngoài điểm neo nếu chưa hoàn thành luồng chính.
