# PHÂN TÍCH CHÊNH LỆCH SCHEMA VÀ BACKEND — ĐỒ ÁN CƠ SỞ

**Phiên bản:** Draft v0.1  
**Nguồn đối chiếu:**  
- `01_SRS_DACS_CANDIDATE_v0.2.md`
- `04_DATA_DICTIONARY_DACS_DRAFT_v0.1.md`
- `database/01_schema.sql`
- Danh sách Entity, Enum, Controller, Service và Repository hiện tại

---

## 1. Kết luận tổng quan

Schema và backend hiện tại là bộ khung chạy thử ban đầu. Các phần đăng nhập, địa điểm, sự kiện, ca và đăng ký đã có nền tảng, nhưng mô hình dữ liệu chưa đồng nhất với yêu cầu đã chốt.

Không xóa backend hiện tại. Cần refactor có kiểm soát theo từng nhóm:

1. chuẩn hóa tài khoản và hồ sơ nhân viên;
2. chuẩn hóa trạng thái địa điểm, sự kiện và ca;
3. bổ sung luồng đăng ký, phân công trực tiếp;
4. tách trạng thái chấm công;
5. bổ sung tiền công và đánh giá;
6. bổ sung transaction, kiểm tra trùng lịch và chỉ tiêu.

---

## 2. Đối chiếu từng bảng

| Bảng | Trạng thái hiện tại | Chênh lệch chính | Hướng xử lý |
|---|---|---|---|
| `roles` | Đã có | Thiếu mô tả và timestamps | Bổ sung hoặc giữ tối giản nếu không cần audit |
| `users` | Đã có | Dùng `password`, `enabled`; chứa `full_name`, `email`, `phone`; thiếu `account_status`, `must_change_password`, `last_login_at`, `created_by` | Chuẩn hóa thành tài khoản thuần; chuyển hồ sơ nghiệp vụ sang `employees` |
| `employees` | Đã có | Thiếu họ tên, điện thoại, email, ngày sinh, địa chỉ; có `experience_level` chưa nằm trong SRS | Bổ sung hồ sơ; quyết định giữ hoặc loại `experience_level` |
| `venues` | Đã có | Thiếu `contact_name`, `note`, `created_by`; tên cột trạng thái chưa thống nhất | Chuẩn hóa cột và bổ sung audit cần thiết |
| `events` | Đã có | Thiếu người tạo, thông tin hủy; tên trạng thái chưa thống nhất | Bổ sung trường hủy và audit |
| `shifts` | Đã có | Dùng đồng thời `registration_open` và `status`; thiếu `registration_deadline`, thông tin hủy | Chỉ dùng `shift_status` làm nguồn trạng thái; cân nhắc bỏ boolean dư thừa |
| `shift_registrations` | Đã có | Thiếu thông tin hủy; `created_at` chưa thể hiện rõ `registered_at`; tên trạng thái chưa thống nhất | Bổ sung trường hủy và chuẩn hóa tên |
| `shift_assignments` | Đã có | Thiếu `registration_id`, `assignment_source`; dùng `role_in_shift`, `task`; thiếu thông tin hủy | Bổ sung nguồn phân công và quan hệ đăng ký tùy chọn |
| `attendances` | Đã có | Chỉ có một trường `status`; thiếu tách quy trình/kết quả, phút trễ/về sớm, người ghi nhận, thời điểm xác nhận | Refactor thành `process_status` và `attendance_result` |
| `payrolls` | Chưa có | Thiếu toàn bộ | Tạo mới sau khi chấm công ổn định |
| `evaluations` | Chưa có | Thiếu toàn bộ | Tạo mới sau khi phân công/chấm công ổn định |

---

## 3. Các mâu thuẫn cần xử lý trước

### GAP-01. `users` đang chứa dữ liệu hồ sơ nhân viên

Schema hiện tại đặt:

- `full_name`
- `email`
- `phone`

trong `users`.

Data Dictionary đặt các trường này trong `employees`.

**Quyết định đề xuất:** `users` chỉ lưu thông tin xác thực và phân quyền; hồ sơ cá nhân của nhân viên nằm trong `employees`.

### GAP-02. `registration_open` trùng ý nghĩa với trạng thái ca

Schema hiện tại có cả:

- `registration_open BOOLEAN`
- `status VARCHAR`

Trong SRS, trạng thái `OPEN` và `CLOSED` đã thể hiện việc mở hoặc đóng đăng ký.

**Quyết định đề xuất:** bỏ `registration_open`; chỉ dùng `shift_status`.

### GAP-03. Phân công chưa biết nguồn

Schema hiện tại không thể phân biệt:

- phân công từ đăng ký được duyệt;
- phân công trực tiếp.

**Cần bổ sung:**

- `registration_id` nullable;
- `assignment_source`;
- `shift_role`.

### GAP-04. Chấm công đang trộn hai khái niệm

`attendances.status` hiện tại chưa phân biệt:

- trạng thái quy trình: `DRAFT`, `CONFIRMED`;
- kết quả đi làm: `PRESENT`, `LATE`, `ABSENT`, ...

**Cần tách thành hai cột riêng.**

### GAP-05. Chưa có tiền công và đánh giá

Backend/schema chưa có:

- `payrolls`;
- `evaluations`;
- entity, repository, service và API tương ứng.

---

## 4. Phạm vi backend hiện có

### Đã có Entity

- Attendance
- Employee
- Event
- Role
- ShiftAssignment
- ShiftRegistration
- UserAccount
- Venue
- WorkShift

### Đã có Controller

- AuthController
- EmployeeController
- EventController
- RegistrationController
- ShiftController
- VenueController

### Đã có Service

- EventService
- RegistrationService
- ShiftService
- VenueService

### Chưa thấy Service riêng

- Account/User service
- Employee service
- Assignment service
- Attendance service
- Payroll service
- Evaluation service

### Chưa có Entity

- Payroll
- Evaluation

---

## 5. Thứ tự refactor đề xuất

### Giai đoạn A — Chuẩn hóa nền tảng

1. Role và UserAccount.
2. Employee.
3. Venue.
4. Event.
5. WorkShift.
6. Đồng bộ enum và schema.

### Giai đoạn B — Đăng ký và phân công

1. ShiftRegistration.
2. ShiftAssignment.
3. Kiểm tra trùng lịch.
4. Kiểm tra chỉ tiêu.
5. Transaction khi duyệt/phân công trực tiếp.

### Giai đoạn C — Chấm công

1. Tách `process_status`.
2. Tách `attendance_result`.
3. Bổ sung người ghi nhận/xác nhận.
4. Kiểm tra quyền trưởng ca theo ca.

### Giai đoạn D — Tiền công và đánh giá

1. Payroll.
2. Evaluation.
3. Thống kê cơ bản.

---

## 6. Nguyên tắc thực hiện

- Tạo branch refactor riêng.
- Không sửa hàng loạt khi chưa kiểm tra Entity và Enum.
- Mỗi nhóm thay đổi phải chạy lại backend.
- Mỗi nhóm thay đổi phải kiểm thử API.
- Chỉ vẽ ERD sau khi schema và Entity đồng nhất.
- Không gọi tài liệu là đã được GVHD phê duyệt; chỉ ghi “baseline nội bộ”.

---

## 7. Dữ liệu còn cần thu thập

Để lập bản sửa chính xác, cần xem nội dung:

- toàn bộ Entity;
- toàn bộ Enum;
- các Service hiện có;
- Repository có truy vấn trùng lịch/chỉ tiêu;
- DTO liên quan;
- `application.properties` hoặc `application.yml`;
- cấu hình JPA `ddl-auto`;
- `DataInitializer`;
- file seed SQL.

Sau khi có các file này mới quyết định sửa schema trước hay Entity trước.
