# ĐẶC TẢ YÊU CẦU PHẦN MỀM — ĐỒ ÁN CƠ SỞ

**Mã tài liệu:** SRS-DACS  
**Phiên bản:** Candidate v0.2  
**Trạng thái:** Chờ giảng viên hướng dẫn xác nhận  
**Tên đề tài:** Xây dựng hệ thống quản lý ca làm và điều phối nhân sự phục vụ tiệc cưới, sự kiện đa địa điểm tích hợp AI

## 1. Phạm vi

Tài liệu này chỉ mô tả phạm vi **Đồ án cơ sở**.

### 1.1 Trong phạm vi

- đăng nhập, đăng xuất và phân quyền;
- quản lý tài khoản và hồ sơ nhân viên;
- quản lý địa điểm, sự kiện và ca làm;
- xem, đăng ký và hủy đăng ký ca;
- duyệt, từ chối và phân công nhân viên;
- chấm công thủ công;
- tính tiền công cơ bản;
- đánh giá sau ca;
- thống kê cơ bản.

### 1.2 Ngoài phạm vi

- AI gợi ý nhân sự hoặc lịch làm;
- chấm công QR;
- điểm uy tín;
- tự động tìm người thay thế;
- điều phối đa địa điểm nâng cao;
- dashboard và báo cáo phân tích nâng cao;
- đăng ký tài khoản công khai.

## 2. Mô tả bài toán

Các đơn vị tổ chức tiệc cưới và sự kiện thường sử dụng nhân viên phục vụ bán thời gian theo ca. Việc quản lý bằng tin nhắn hoặc bảng tính dễ phát sinh:

- khó theo dõi người đã đăng ký và được duyệt;
- dễ phân công trùng giờ;
- khó kiểm soát số lượng nhân viên còn thiếu;
- khó quản lý đi làm, đi trễ, về sớm và vắng mặt;
- khó tổng hợp số ca và tiền công;
- dữ liệu địa điểm, sự kiện, ca làm và nhân sự bị phân tán.

Hệ thống quản lý tập trung toàn bộ luồng từ tạo sự kiện, tạo ca, đăng ký, duyệt, phân công, chấm công đến tính công và thống kê.

## 3. Actor và phân quyền

### 3.1 Quản trị viên — `ADMIN`

- quản lý tài khoản;
- gán quyền;
- khóa hoặc mở khóa;
- quản lý dữ liệu nền;
- xem thống kê tổng quan.

### 3.2 Điều phối viên — `COORDINATOR`

- quản lý hồ sơ nhân viên;
- quản lý địa điểm, sự kiện và ca;
- duyệt hoặc từ chối đăng ký;
- phân công trực tiếp hoặc từ đăng ký;
- xác nhận chấm công;
- tính tiền công;
- đánh giá và xem thống kê.

### 3.3 Nhân viên — `EMPLOYEE`

- đăng nhập;
- xem thông tin cá nhân;
- xem, đăng ký và hủy đăng ký ca theo điều kiện;
- xem kết quả duyệt;
- xem lịch sử tham gia và tiền công.

### 3.4 Trưởng ca

Trưởng ca không phải role tài khoản hệ thống. Trưởng ca là nhân viên có `shiftRole = LEADER` trong một ca cụ thể. Quyền ghi nhận chấm công chỉ có hiệu lực đối với ca đó.

## 4. Yêu cầu chức năng

### FR-01. Đăng nhập

- tài khoản hợp lệ và đang hoạt động: cấp JWT;
- sai thông tin hoặc tài khoản bị khóa: từ chối đăng nhập.

### FR-02. Đăng xuất

Frontend xóa token và thông tin phiên.

### FR-03. Quản lý tài khoản

Quản trị viên có thể xem, tạo, cập nhật, đặt lại mật khẩu, khóa/mở khóa và gán role `ADMIN`, `COORDINATOR`, `EMPLOYEE`.

Nhân viên không tự đăng ký tài khoản công khai trong ĐACS.

### FR-04. Quản lý hồ sơ nhân viên

Quản trị viên hoặc điều phối viên có thể thêm, cập nhật, tìm kiếm, lọc và đổi trạng thái làm việc.

### FR-05. Quản lý địa điểm

Quản trị viên hoặc điều phối viên có thể thêm, cập nhật, xem và ngừng hoạt động địa điểm. Địa điểm đã phát sinh dữ liệu không bị xóa vật lý.

### FR-06. Quản lý sự kiện

Có thể tạo, liên kết địa điểm, cập nhật, xác nhận, hủy, hoàn thành và xem sự kiện.

### FR-07. Quản lý ca làm

Có thể tạo ca thuộc sự kiện, thiết lập thời gian, số người cần, tiền công cố định, mở/đóng đăng ký và cập nhật trạng thái.

### FR-08. Nhân viên xem ca

Nhân viên xem danh sách ca đang mở, lọc theo thời gian/địa điểm/sự kiện và xem chi tiết.

### FR-09. Đăng ký ca

Hệ thống kiểm tra:

- ca tồn tại và đang mở;
- nhân viên đang hoạt động;
- chưa đăng ký ca đó;
- không trùng với ca đã được duyệt hoặc phân công.

Đăng ký hợp lệ có trạng thái `PENDING`.

### FR-10. Kiểm tra trùng lịch

Hai khoảng thời gian trùng khi:

```text
newStart < existingEnd
AND
newEnd > existingStart
```

Khoảng thời gian được hiểu theo dạng `[start, end)`. Ca kết thúc đúng lúc ca khác bắt đầu không bị xem là trùng.

### FR-11. Hủy đăng ký

Nhân viên được tự hủy khi:

```text
currentTime <= shiftStartTime - 24 giờ
```

Quy tắc:

- chuyển sang `CANCELLED`;
- không xóa vật lý;
- lưu người hủy, thời điểm và lý do;
- quá hạn thì phải liên hệ điều phối viên;
- không dùng `CANCELLED_LATE` trong ĐACS.

### FR-12. Duyệt hoặc từ chối đăng ký

Điều phối viên duyệt hoặc từ chối đăng ký `PENDING`. Trước khi duyệt phải kiểm tra trạng thái, trùng lịch và chỉ tiêu. Duyệt thành công tạo phân công có nguồn `REGISTRATION`.

### FR-13. Phân công trực tiếp

Điều phối viên có thể phân công trực tiếp nhân viên chưa đăng ký.

- không tạo đăng ký giả;
- `registrationId = NULL`;
- `assignmentSource = DIRECT`;
- vẫn kiểm tra trùng lịch, chỉ tiêu và trạng thái nhân viên.

### FR-14. Quản lý phân công

Phân công gồm nhân viên, ca, nguồn phân công, `shiftRole` (`LEADER`/`STAFF`), khu vực, mô tả nhiệm vụ và trạng thái. Không quản lý đến từng bàn.

### FR-15. Ghi nhận chấm công

Trưởng ca của ca đó có thể ghi nhận giờ vào, giờ ra, có mặt, vắng mặt và ghi chú. Điều phối viên cũng có thể nhập/chỉnh sửa.

### FR-16. Xác nhận chấm công

Chỉ điều phối viên được xác nhận. Sau xác nhận, dữ liệu chuyển `CONFIRMED`, mặc định chỉ đọc và mới được phép tính công.

### FR-17. Kết quả chấm công

Trạng thái quy trình:

- `DRAFT`;
- `CONFIRMED`.

Kết quả đi làm:

- `PRESENT`;
- `LATE`;
- `EARLY_LEAVE`;
- `LATE_AND_EARLY_LEAVE`;
- `ABSENT`.

### FR-18. Tính tiền công

```text
finalAmount = baseAmount + adjustmentAmount
```

- `baseAmount` lấy từ `shift.payAmount`;
- điều chỉnh có thể âm hoặc dương;
- khác 0 phải có lý do;
- điều phối viên nhập thủ công;
- `ABSENT` mặc định `finalAmount = 0`;
- không tự động phạt theo tỷ lệ cố định.

### FR-19. Đánh giá sau ca

Điều phối viên hoặc trưởng ca của ca đó có thể đánh giá điểm 1–5, nhận xét, người đánh giá và thời điểm.

### FR-20. Hủy sự kiện

Chạy trong một transaction:

- sự kiện chưa hoàn thành → `CANCELLED`;
- ca chưa hoàn thành → `CANCELLED`;
- đăng ký chưa kết thúc → `CANCELLED`;
- phân công chưa hoàn thành → `CANCELLED`;
- lưu người hủy, thời điểm và lý do;
- không thay đổi bản ghi `COMPLETED`.

### FR-21. Thống kê cơ bản

- số ca theo trạng thái;
- đăng ký chờ duyệt;
- nhân viên đã phân công;
- số ca đã tham gia;
- tình trạng chấm công;
- tổng tiền công theo nhân viên hoặc thời gian.

### FR-22. Lịch sử trạng thái

Chưa bắt buộc bảng `StatusHistory` trong lõi ĐACS. Trước mắt lưu các mốc như `createdAt`, `updatedAt`, `reviewedAt`, `cancelledAt`, `confirmedAt`, người thực hiện và lý do.

## 5. Quy tắc nghiệp vụ

- BR-01: Một địa điểm có nhiều sự kiện; một sự kiện thuộc một địa điểm.
- BR-02: Một sự kiện có nhiều ca; một ca thuộc một sự kiện.
- BR-03: Một nhân viên chỉ có một đăng ký cho cùng một ca.
- BR-04: Đăng ký mới có trạng thái `PENDING`.
- BR-05: Phân công được tạo từ đăng ký đã duyệt hoặc phân công trực tiếp.
- BR-06: Nguồn `REGISTRATION` thì `registrationId` bắt buộc có giá trị.
- BR-07: Nguồn `DIRECT` thì `registrationId` phải là `NULL`.
- BR-08: Số phân công còn hiệu lực không vượt `requiredStaff`.
- BR-09: Một nhân viên không có hai phân công còn hiệu lực bị trùng giờ.
- BR-10: Chấm công gắn với phân công, không gắn trực tiếp với đăng ký.
- BR-11: Chỉ tính công sau khi chấm công được xác nhận.
- BR-12: Dữ liệu đã phát sinh không xóa vật lý.
- BR-13: Mọi thời điểm phải thỏa `startAt < endAt`.
- BR-14: Trưởng ca chỉ có quyền trên ca mà mình được gán `LEADER`.
- BR-15: AI không tham gia vào quyết định nào trong ĐACS.

## 6. Trạng thái dự kiến

- Tài khoản: `ACTIVE`, `LOCKED`
- Nhân viên: `AVAILABLE`, `INACTIVE`
- Sự kiện: `DRAFT`, `CONFIRMED`, `CANCELLED`, `COMPLETED`
- Ca: `DRAFT`, `OPEN`, `CLOSED`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED`
- Đăng ký: `PENDING`, `APPROVED`, `REJECTED`, `CANCELLED`
- Phân công: `ASSIGNED`, `CANCELLED`, `COMPLETED`
- Nguồn phân công: `REGISTRATION`, `DIRECT`
- Vai trò trong ca: `LEADER`, `STAFF`
- Chấm công quy trình: `DRAFT`, `CONFIRMED`
- Kết quả chấm công: `PRESENT`, `LATE`, `EARLY_LEAVE`, `LATE_AND_EARLY_LEAVE`, `ABSENT`
- Tiền công: `CALCULATED`, `CONFIRMED`, `PAID`

## 7. Yêu cầu phi chức năng

### NFR-01. Bảo mật

- mật khẩu được băm;
- API dùng JWT;
- kiểm tra quyền ở backend;
- không trả password hash cho frontend.

### NFR-02. Toàn vẹn dữ liệu

- dùng khóa chính, khóa ngoại, unique constraint;
- dùng transaction khi duyệt, tạo phân công và hủy sự kiện;
- kiểm tra dữ liệu đầu vào;
- chống phân công vượt chỉ tiêu khi xử lý đồng thời.

### NFR-03. Khả năng sử dụng

- giao diện tiếng Việt;
- thông báo lỗi rõ ràng;
- tìm kiếm, lọc, phân trang khi cần.

### NFR-04. Khả năng bảo trì

- kiến trúc `Controller → Service → Repository`;
- DTO tách khỏi Entity;
- tên API, class và bảng thống nhất;
- quản lý mã nguồn bằng Git.

### NFR-05. Hiệu năng

Trong môi trường thử nghiệm, mục tiêu là 95% thao tác CRUD thông thường phản hồi không quá 2 giây, không tính thời gian mạng bên ngoài.

## 8. Công nghệ dự kiến

- Java 21, Spring Boot;
- React;
- MySQL;
- Spring Security và JWT;
- Spring Data JPA/Hibernate;
- Swagger/OpenAPI;
- Postman;
- Git/GitHub.

## 9. Điều kiện chuyển thành Baseline 1.0

Chỉ đổi tên thành `SRS_DACS_BASELINE_v1.0` khi:

- GVHD xác nhận phạm vi;
- các quyết định trong Decision Log được xác nhận;
- không còn mâu thuẫn giữa yêu cầu và quy tắc;
- mô hình dữ liệu sơ bộ được rà soát;
- thuật ngữ và trạng thái được dùng thống nhất.
