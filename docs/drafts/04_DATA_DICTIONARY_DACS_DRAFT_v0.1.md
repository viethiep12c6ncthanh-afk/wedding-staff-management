# DATA DICTIONARY — ĐỒ ÁN CƠ SỞ

**Mã tài liệu:** DD-DACS  
**Phiên bản:** Draft v0.1  
**Trạng thái:** Đã chốt nội bộ để rà soát kỹ thuật  
**Cơ sở:** `01_SRS_DACS_CANDIDATE_v0.2.md`

---

## 1. Quy ước chung

### 1.1 Hệ quản trị

- MySQL 8.x
- Storage engine: InnoDB
- Character set: `utf8mb4`
- Collation đề xuất: `utf8mb4_unicode_ci`

### 1.2 Quy ước đặt tên

- Tên bảng và cột: `snake_case`
- Tên bảng: số nhiều
- Khóa chính: `id`
- Khóa ngoại: `<entity>_id`
- Thời điểm: hậu tố `_at`
- Người thao tác: hậu tố `_by`
- Trạng thái: hậu tố `_status` hoặc tên rõ nghĩa

### 1.3 Kiểu dữ liệu dùng chung

| Loại dữ liệu | Kiểu MySQL đề xuất |
|---|---|
| Khóa chính/khóa ngoại | `BIGINT UNSIGNED` |
| Chuỗi ngắn | `VARCHAR(n)` |
| Nội dung dài | `TEXT` |
| Thời điểm | `DATETIME(6)` |
| Số tiền | `DECIMAL(12,2)` |
| Số phút | `INT UNSIGNED` |
| Boolean | `BOOLEAN` |
| Điểm đánh giá | `TINYINT UNSIGNED` |

### 1.4 Quy ước trạng thái

Ưu tiên lưu enum dưới dạng `VARCHAR`, ánh xạ bằng `EnumType.STRING` trong JPA. Không dùng ordinal.

Ví dụ:

```java
@Enumerated(EnumType.STRING)
@Column(nullable = false, length = 30)
private ShiftStatus status;
```

### 1.5 Quy ước xóa dữ liệu

- Không xóa vật lý dữ liệu nghiệp vụ đã phát sinh.
- Dùng trạng thái `INACTIVE`, `LOCKED`, `CANCELLED` hoặc tương đương.
- Không bổ sung `deleted_at` trong giai đoạn cốt lõi để tránh trùng ý nghĩa với trạng thái.

---

# 2. Danh sách bảng

1. `roles`
2. `users`
3. `employees`
4. `venues`
5. `events`
6. `shifts`
7. `shift_registrations`
8. `shift_assignments`
9. `attendances`
10. `payrolls`
11. `evaluations`

---

# 3. Chi tiết bảng

## 3.1 `roles`

### Mục đích

Lưu các role hệ thống dùng cho RBAC/JWT.

### Dữ liệu chuẩn

- `ADMIN`
- `COORDINATOR`
- `EMPLOYEE`

`LEADER` không thuộc bảng này.

| Cột | Kiểu | Null | Ràng buộc | Mô tả |
|---|---|:---:|---|---|
| `id` | `BIGINT UNSIGNED` | Không | PK, AUTO_INCREMENT | Mã role |
| `name` | `VARCHAR(30)` | Không | UNIQUE | Tên role |
| `description` | `VARCHAR(255)` | Có |  | Mô tả |
| `created_at` | `DATETIME(6)` | Không | DEFAULT CURRENT_TIMESTAMP(6) | Thời điểm tạo |
| `updated_at` | `DATETIME(6)` | Không | Tự cập nhật | Thời điểm cập nhật |

### Index

- `uk_roles_name(name)`

---

## 3.2 `users`

### Mục đích

Lưu tài khoản đăng nhập và role hệ thống.

| Cột | Kiểu | Null | Ràng buộc | Mô tả |
|---|---|:---:|---|---|
| `id` | `BIGINT UNSIGNED` | Không | PK, AUTO_INCREMENT | Mã tài khoản |
| `role_id` | `BIGINT UNSIGNED` | Không | FK → `roles.id` | Role hệ thống |
| `username` | `VARCHAR(50)` | Không | UNIQUE | Tên đăng nhập |
| `password_hash` | `VARCHAR(255)` | Không |  | Mật khẩu đã băm |
| `account_status` | `VARCHAR(20)` | Không | DEFAULT `ACTIVE` | `ACTIVE`, `LOCKED` |
| `must_change_password` | `BOOLEAN` | Không | DEFAULT TRUE | Buộc đổi mật khẩu lần đầu |
| `last_login_at` | `DATETIME(6)` | Có |  | Lần đăng nhập gần nhất |
| `created_by` | `BIGINT UNSIGNED` | Có | FK → `users.id` | Tài khoản tạo bản ghi |
| `created_at` | `DATETIME(6)` | Không | DEFAULT CURRENT_TIMESTAMP(6) | Thời điểm tạo |
| `updated_at` | `DATETIME(6)` | Không | Tự cập nhật | Thời điểm cập nhật |

### Ràng buộc

- `username` dài 4–50 ký tự.
- Backend không trả `password_hash`.
- Không cho xóa tài khoản đã phát sinh dữ liệu.

### Index

- `uk_users_username(username)`
- `idx_users_role_id(role_id)`
- `idx_users_account_status(account_status)`

---

## 3.3 `employees`

### Mục đích

Lưu hồ sơ nghiệp vụ của nhân viên phục vụ.

Một tài khoản có thể không có hồ sơ nhân viên. Một hồ sơ nhân viên bắt buộc gắn với đúng một tài khoản.

| Cột | Kiểu | Null | Ràng buộc | Mô tả |
|---|---|:---:|---|---|
| `id` | `BIGINT UNSIGNED` | Không | PK, AUTO_INCREMENT | Mã hồ sơ |
| `user_id` | `BIGINT UNSIGNED` | Không | FK, UNIQUE → `users.id` | Tài khoản |
| `employee_code` | `VARCHAR(20)` | Không | UNIQUE | Mã nhân viên |
| `full_name` | `VARCHAR(100)` | Không |  | Họ tên |
| `phone` | `VARCHAR(20)` | Có | UNIQUE | Số điện thoại |
| `email` | `VARCHAR(100)` | Có | UNIQUE | Email |
| `date_of_birth` | `DATE` | Có |  | Ngày sinh |
| `address` | `VARCHAR(255)` | Có |  | Địa chỉ |
| `work_status` | `VARCHAR(20)` | Không | DEFAULT `AVAILABLE` | `AVAILABLE`, `INACTIVE` |
| `note` | `TEXT` | Có |  | Ghi chú |
| `created_at` | `DATETIME(6)` | Không | DEFAULT CURRENT_TIMESTAMP(6) | Thời điểm tạo |
| `updated_at` | `DATETIME(6)` | Không | Tự cập nhật | Thời điểm cập nhật |

### Ràng buộc

- `user_id` là quan hệ 1–0..1 từ `users` đến `employees`.
- Nhân viên chỉ đăng ký hoặc nhận phân công khi `work_status = AVAILABLE`.
- Tài khoản của nhân viên phải có role `EMPLOYEE`.
- Quy tắc role được kiểm tra ở Service; FK không thể bảo đảm điều này.

### Index

- `uk_employees_user_id(user_id)`
- `uk_employees_employee_code(employee_code)`
- `uk_employees_phone(phone)`
- `uk_employees_email(email)`
- `idx_employees_work_status(work_status)`
- `idx_employees_full_name(full_name)`

---

## 3.4 `venues`

### Mục đích

Lưu địa điểm tổ chức tiệc cưới hoặc sự kiện.

| Cột | Kiểu | Null | Ràng buộc | Mô tả |
|---|---|:---:|---|---|
| `id` | `BIGINT UNSIGNED` | Không | PK, AUTO_INCREMENT | Mã địa điểm |
| `name` | `VARCHAR(150)` | Không |  | Tên địa điểm |
| `address` | `VARCHAR(255)` | Không |  | Địa chỉ |
| `contact_name` | `VARCHAR(100)` | Có |  | Người liên hệ |
| `contact_phone` | `VARCHAR(20)` | Có |  | Số liên hệ |
| `venue_status` | `VARCHAR(20)` | Không | DEFAULT `ACTIVE` | `ACTIVE`, `INACTIVE` |
| `note` | `TEXT` | Có |  | Ghi chú |
| `created_by` | `BIGINT UNSIGNED` | Có | FK → `users.id` | Người tạo |
| `created_at` | `DATETIME(6)` | Không | DEFAULT CURRENT_TIMESTAMP(6) | Thời điểm tạo |
| `updated_at` | `DATETIME(6)` | Không | Tự cập nhật | Thời điểm cập nhật |

### Index

- `idx_venues_name(name)`
- `idx_venues_status(venue_status)`

---

## 3.5 `events`

### Mục đích

Lưu sự kiện tổ chức tại một địa điểm.

| Cột | Kiểu | Null | Ràng buộc | Mô tả |
|---|---|:---:|---|---|
| `id` | `BIGINT UNSIGNED` | Không | PK, AUTO_INCREMENT | Mã sự kiện |
| `venue_id` | `BIGINT UNSIGNED` | Không | FK → `venues.id` | Địa điểm |
| `name` | `VARCHAR(150)` | Không |  | Tên sự kiện |
| `description` | `TEXT` | Có |  | Mô tả |
| `start_at` | `DATETIME(6)` | Không |  | Bắt đầu |
| `end_at` | `DATETIME(6)` | Không |  | Kết thúc |
| `event_status` | `VARCHAR(20)` | Không | DEFAULT `DRAFT` | Trạng thái |
| `created_by` | `BIGINT UNSIGNED` | Không | FK → `users.id` | Người tạo |
| `cancelled_by` | `BIGINT UNSIGNED` | Có | FK → `users.id` | Người hủy |
| `cancelled_at` | `DATETIME(6)` | Có |  | Thời điểm hủy |
| `cancellation_reason` | `VARCHAR(500)` | Có |  | Lý do hủy |
| `created_at` | `DATETIME(6)` | Không | DEFAULT CURRENT_TIMESTAMP(6) | Thời điểm tạo |
| `updated_at` | `DATETIME(6)` | Không | Tự cập nhật | Thời điểm cập nhật |

### Trạng thái

- `DRAFT`
- `CONFIRMED`
- `CANCELLED`
- `COMPLETED`

### Ràng buộc

- `start_at < end_at`.
- Không tạo sự kiện tại địa điểm `INACTIVE`.
- Khi chuyển `CANCELLED`, bắt buộc có `cancelled_by`, `cancelled_at`, `cancellation_reason`.
- Hủy sự kiện và cập nhật dữ liệu liên quan phải chạy trong transaction.

### Index

- `idx_events_venue_id(venue_id)`
- `idx_events_start_at(start_at)`
- `idx_events_status(event_status)`
- `idx_events_venue_start(venue_id, start_at)`

---

## 3.6 `shifts`

### Mục đích

Lưu ca làm thuộc một sự kiện.

| Cột | Kiểu | Null | Ràng buộc | Mô tả |
|---|---|:---:|---|---|
| `id` | `BIGINT UNSIGNED` | Không | PK, AUTO_INCREMENT | Mã ca |
| `event_id` | `BIGINT UNSIGNED` | Không | FK → `events.id` | Sự kiện |
| `name` | `VARCHAR(100)` | Không |  | Tên ca |
| `start_at` | `DATETIME(6)` | Không |  | Giờ bắt đầu |
| `end_at` | `DATETIME(6)` | Không |  | Giờ kết thúc |
| `required_staff` | `SMALLINT UNSIGNED` | Không |  | Số nhân viên cần |
| `pay_amount` | `DECIMAL(12,2)` | Không |  | Tiền công cố định |
| `registration_deadline` | `DATETIME(6)` | Có |  | Hạn đăng ký |
| `shift_status` | `VARCHAR(20)` | Không | DEFAULT `DRAFT` | Trạng thái |
| `description` | `TEXT` | Có |  | Mô tả |
| `created_by` | `BIGINT UNSIGNED` | Không | FK → `users.id` | Người tạo |
| `cancelled_by` | `BIGINT UNSIGNED` | Có | FK → `users.id` | Người hủy |
| `cancelled_at` | `DATETIME(6)` | Có |  | Thời điểm hủy |
| `cancellation_reason` | `VARCHAR(500)` | Có |  | Lý do hủy |
| `created_at` | `DATETIME(6)` | Không | DEFAULT CURRENT_TIMESTAMP(6) | Thời điểm tạo |
| `updated_at` | `DATETIME(6)` | Không | Tự cập nhật | Thời điểm cập nhật |

### Trạng thái

- `DRAFT`
- `OPEN`
- `CLOSED`
- `IN_PROGRESS`
- `COMPLETED`
- `CANCELLED`

### Ràng buộc

- `start_at < end_at`.
- `required_staff > 0`.
- `pay_amount >= 0`.
- Ca phải nằm trong khoảng thời gian sự kiện.
- Chỉ ca `OPEN` mới nhận đăng ký.
- `registration_deadline <= start_at`.
- Việc đếm chỉ tiêu phải thực hiện trong transaction khi duyệt hoặc phân công trực tiếp.

### Index

- `idx_shifts_event_id(event_id)`
- `idx_shifts_start_at(start_at)`
- `idx_shifts_status(shift_status)`
- `idx_shifts_event_start(event_id, start_at)`

---

## 3.7 `shift_registrations`

### Mục đích

Lưu hành động nhân viên đăng ký một ca.

Không dùng bảng này để giả lập phân công trực tiếp.

| Cột | Kiểu | Null | Ràng buộc | Mô tả |
|---|---|:---:|---|---|
| `id` | `BIGINT UNSIGNED` | Không | PK, AUTO_INCREMENT | Mã đăng ký |
| `shift_id` | `BIGINT UNSIGNED` | Không | FK → `shifts.id` | Ca |
| `employee_id` | `BIGINT UNSIGNED` | Không | FK → `employees.id` | Nhân viên |
| `registration_status` | `VARCHAR(20)` | Không | DEFAULT `PENDING` | Trạng thái |
| `registered_at` | `DATETIME(6)` | Không | DEFAULT CURRENT_TIMESTAMP(6) | Thời điểm đăng ký |
| `reviewed_by` | `BIGINT UNSIGNED` | Có | FK → `users.id` | Điều phối viên duyệt |
| `reviewed_at` | `DATETIME(6)` | Có |  | Thời điểm duyệt |
| `rejection_reason` | `VARCHAR(500)` | Có |  | Lý do từ chối |
| `cancelled_by` | `BIGINT UNSIGNED` | Có | FK → `users.id` | Người hủy |
| `cancelled_at` | `DATETIME(6)` | Có |  | Thời điểm hủy |
| `cancellation_reason` | `VARCHAR(500)` | Có |  | Lý do hủy |
| `updated_at` | `DATETIME(6)` | Không | Tự cập nhật | Thời điểm cập nhật |

### Trạng thái

- `PENDING`
- `APPROVED`
- `REJECTED`
- `CANCELLED`

### Ràng buộc

- UNIQUE (`shift_id`, `employee_id`).
- Khi tạo mới: trạng thái luôn `PENDING`.
- Chỉ `PENDING` được duyệt hoặc từ chối.
- `REJECTED` phải có `reviewed_by`, `reviewed_at`, `rejection_reason`.
- `APPROVED` phải có `reviewed_by`, `reviewed_at`.
- Tự hủy chỉ khi còn ít nhất 24 giờ trước `shift.start_at`.
- Không xóa vật lý bản ghi.

### Index

- `uk_shift_registrations_shift_employee(shift_id, employee_id)`
- `idx_shift_registrations_shift_status(shift_id, registration_status)`
- `idx_shift_registrations_employee_status(employee_id, registration_status)`
- `idx_shift_registrations_registered_at(registered_at)`

---

## 3.8 `shift_assignments`

### Mục đích

Lưu việc một nhân viên được phân công làm một ca.

Phân công được tạo từ:

- đăng ký đã duyệt;
- điều phối viên phân công trực tiếp.

| Cột | Kiểu | Null | Ràng buộc | Mô tả |
|---|---|:---:|---|---|
| `id` | `BIGINT UNSIGNED` | Không | PK, AUTO_INCREMENT | Mã phân công |
| `shift_id` | `BIGINT UNSIGNED` | Không | FK → `shifts.id` | Ca |
| `employee_id` | `BIGINT UNSIGNED` | Không | FK → `employees.id` | Nhân viên |
| `registration_id` | `BIGINT UNSIGNED` | Có | FK, UNIQUE → `shift_registrations.id` | Đăng ký nguồn |
| `assignment_source` | `VARCHAR(20)` | Không |  | `REGISTRATION`, `DIRECT` |
| `shift_role` | `VARCHAR(20)` | Không | DEFAULT `STAFF` | `LEADER`, `STAFF` |
| `area` | `VARCHAR(100)` | Có |  | Khu vực |
| `task_description` | `VARCHAR(500)` | Có |  | Nhiệm vụ |
| `assignment_status` | `VARCHAR(20)` | Không | DEFAULT `ASSIGNED` | Trạng thái |
| `assigned_by` | `BIGINT UNSIGNED` | Không | FK → `users.id` | Người phân công |
| `assigned_at` | `DATETIME(6)` | Không | DEFAULT CURRENT_TIMESTAMP(6) | Thời điểm phân công |
| `cancelled_by` | `BIGINT UNSIGNED` | Có | FK → `users.id` | Người hủy |
| `cancelled_at` | `DATETIME(6)` | Có |  | Thời điểm hủy |
| `cancellation_reason` | `VARCHAR(500)` | Có |  | Lý do hủy |
| `updated_at` | `DATETIME(6)` | Không | Tự cập nhật | Thời điểm cập nhật |

### Trạng thái

- `ASSIGNED`
- `CANCELLED`
- `COMPLETED`

### Ràng buộc

- UNIQUE (`shift_id`, `employee_id`).
- Nếu `assignment_source = REGISTRATION`:
  - `registration_id` bắt buộc có giá trị;
  - đăng ký phải thuộc cùng ca và cùng nhân viên;
  - đăng ký phải là `APPROVED`.
- Nếu `assignment_source = DIRECT`:
  - `registration_id` phải là `NULL`.
- Nhân viên phải `AVAILABLE`.
- Không được trùng thời gian với phân công `ASSIGNED` khác.
- Không vượt `required_staff`.
- Tối đa một `LEADER` cho một ca trong phiên bản cốt lõi.

### Index

- `uk_shift_assignments_shift_employee(shift_id, employee_id)`
- `uk_shift_assignments_registration_id(registration_id)`
- `idx_shift_assignments_shift_status(shift_id, assignment_status)`
- `idx_shift_assignments_employee_status(employee_id, assignment_status)`
- `idx_shift_assignments_shift_role(shift_id, shift_role)`

### Ghi chú

MySQL không dễ tạo unique có điều kiện cho “một LEADER đang hoạt động mỗi ca”. Quy tắc này được kiểm tra ở Service trong transaction.

---

## 3.9 `attendances`

### Mục đích

Lưu chấm công của một phân công.

| Cột | Kiểu | Null | Ràng buộc | Mô tả |
|---|---|:---:|---|---|
| `id` | `BIGINT UNSIGNED` | Không | PK, AUTO_INCREMENT | Mã chấm công |
| `assignment_id` | `BIGINT UNSIGNED` | Không | FK, UNIQUE → `shift_assignments.id` | Phân công |
| `process_status` | `VARCHAR(20)` | Không | DEFAULT `DRAFT` | `DRAFT`, `CONFIRMED` |
| `attendance_result` | `VARCHAR(30)` | Có |  | Kết quả đi làm |
| `check_in_at` | `DATETIME(6)` | Có |  | Giờ vào |
| `check_out_at` | `DATETIME(6)` | Có |  | Giờ ra |
| `late_minutes` | `INT UNSIGNED` | Không | DEFAULT 0 | Số phút trễ |
| `early_leave_minutes` | `INT UNSIGNED` | Không | DEFAULT 0 | Số phút về sớm |
| `note` | `VARCHAR(500)` | Có |  | Ghi chú |
| `recorded_by` | `BIGINT UNSIGNED` | Không | FK → `users.id` | Người ghi nhận |
| `recorded_at` | `DATETIME(6)` | Không | DEFAULT CURRENT_TIMESTAMP(6) | Thời điểm ghi nhận |
| `confirmed_by` | `BIGINT UNSIGNED` | Có | FK → `users.id` | Điều phối viên xác nhận |
| `confirmed_at` | `DATETIME(6)` | Có |  | Thời điểm xác nhận |
| `updated_at` | `DATETIME(6)` | Không | Tự cập nhật | Thời điểm cập nhật |

### Kết quả đi làm

- `PRESENT`
- `LATE`
- `EARLY_LEAVE`
- `LATE_AND_EARLY_LEAVE`
- `ABSENT`

### Ràng buộc

- UNIQUE (`assignment_id`).
- Chỉ tạo chấm công cho phân công `ASSIGNED`.
- Nếu `ABSENT`, `check_in_at` và `check_out_at` được phép `NULL`.
- Nếu có cả hai mốc: `check_in_at < check_out_at`.
- `CONFIRMED` phải có `confirmed_by` và `confirmed_at`.
- Sau `CONFIRMED`, dữ liệu mặc định chỉ đọc.
- Người ghi nhận là điều phối viên hoặc nhân viên có `shift_role = LEADER` trong đúng ca.

### Index

- `uk_attendances_assignment_id(assignment_id)`
- `idx_attendances_process_status(process_status)`
- `idx_attendances_result(attendance_result)`
- `idx_attendances_confirmed_at(confirmed_at)`

---

## 3.10 `payrolls`

### Mục đích

Lưu tiền công của một phân công sau khi chấm công được xác nhận.

| Cột | Kiểu | Null | Ràng buộc | Mô tả |
|---|---|:---:|---|---|
| `id` | `BIGINT UNSIGNED` | Không | PK, AUTO_INCREMENT | Mã tiền công |
| `assignment_id` | `BIGINT UNSIGNED` | Không | FK, UNIQUE → `shift_assignments.id` | Phân công |
| `base_amount` | `DECIMAL(12,2)` | Không |  | Tiền công gốc |
| `adjustment_amount` | `DECIMAL(12,2)` | Không | DEFAULT 0 | Điều chỉnh |
| `adjustment_reason` | `VARCHAR(500)` | Có |  | Lý do điều chỉnh |
| `final_amount` | `DECIMAL(12,2)` | Không |  | Thực nhận |
| `payroll_status` | `VARCHAR(20)` | Không | DEFAULT `CALCULATED` | Trạng thái |
| `calculated_by` | `BIGINT UNSIGNED` | Không | FK → `users.id` | Người tính |
| `calculated_at` | `DATETIME(6)` | Không | DEFAULT CURRENT_TIMESTAMP(6) | Thời điểm tính |
| `confirmed_by` | `BIGINT UNSIGNED` | Có | FK → `users.id` | Người xác nhận |
| `confirmed_at` | `DATETIME(6)` | Có |  | Thời điểm xác nhận |
| `paid_at` | `DATETIME(6)` | Có |  | Thời điểm thanh toán |
| `updated_at` | `DATETIME(6)` | Không | Tự cập nhật | Thời điểm cập nhật |

### Trạng thái

- `CALCULATED`
- `CONFIRMED`
- `PAID`

### Ràng buộc

- UNIQUE (`assignment_id`).
- Chỉ tạo khi chấm công đã `CONFIRMED`.
- `base_amount = shift.pay_amount` tại thời điểm tính.
- `final_amount = base_amount + adjustment_amount`.
- Nếu `adjustment_amount != 0`, bắt buộc có `adjustment_reason`.
- Nếu chấm công `ABSENT`, mặc định `final_amount = 0`.
- Không cho `final_amount < 0`.

### Index

- `uk_payrolls_assignment_id(assignment_id)`
- `idx_payrolls_status(payroll_status)`
- `idx_payrolls_calculated_at(calculated_at)`
- `idx_payrolls_paid_at(paid_at)`

---

## 3.11 `evaluations`

### Mục đích

Lưu một đánh giá sau ca cho mỗi phân công.

| Cột | Kiểu | Null | Ràng buộc | Mô tả |
|---|---|:---:|---|---|
| `id` | `BIGINT UNSIGNED` | Không | PK, AUTO_INCREMENT | Mã đánh giá |
| `assignment_id` | `BIGINT UNSIGNED` | Không | FK, UNIQUE → `shift_assignments.id` | Phân công |
| `score` | `TINYINT UNSIGNED` | Không | CHECK 1–5 | Điểm |
| `comment` | `VARCHAR(1000)` | Có |  | Nhận xét |
| `evaluated_by` | `BIGINT UNSIGNED` | Không | FK → `users.id` | Người đánh giá |
| `evaluated_at` | `DATETIME(6)` | Không | DEFAULT CURRENT_TIMESTAMP(6) | Thời điểm đánh giá |
| `updated_at` | `DATETIME(6)` | Không | Tự cập nhật | Thời điểm cập nhật |

### Ràng buộc

- UNIQUE (`assignment_id`): mỗi phân công có một đánh giá chính thức.
- `score` từ 1 đến 5.
- Người đánh giá phải là:
  - điều phối viên; hoặc
  - nhân viên có `shift_role = LEADER` trong đúng ca.
- Chỉ đánh giá khi ca đã bắt đầu hoặc hoàn thành.
- Không dùng điểm này làm “điểm uy tín” trong Đồ án cơ sở.

### Index

- `uk_evaluations_assignment_id(assignment_id)`
- `idx_evaluations_score(score)`
- `idx_evaluations_evaluated_at(evaluated_at)`

---

# 4. Quan hệ tổng quát

```text
roles 1 ─── N users
users 1 ─── 0..1 employees

venues 1 ─── N events
events 1 ─── N shifts

employees 1 ─── N shift_registrations
shifts 1 ─── N shift_registrations

employees 1 ─── N shift_assignments
shifts 1 ─── N shift_assignments
shift_registrations 1 ─── 0..1 shift_assignments

shift_assignments 1 ─── 0..1 attendances
shift_assignments 1 ─── 0..1 payrolls
shift_assignments 1 ─── 0..1 evaluations
```

---

# 5. Quy tắc không thể chỉ dựa vào khóa ngoại

Các quy tắc sau phải kiểm tra trong Service và transaction:

1. Role của hồ sơ nhân viên phải là `EMPLOYEE`.
2. Ca phải nằm trong thời gian sự kiện.
3. Nhân viên không bị trùng lịch.
4. Số phân công không vượt `required_staff`.
5. Tối đa một trưởng ca còn hiệu lực trong một ca.
6. `registration_id` phải cùng ca và cùng nhân viên với phân công.
7. Trưởng ca chỉ chấm công hoặc đánh giá đúng ca của mình.
8. Chỉ tính công sau khi chấm công được xác nhận.
9. Hủy sự kiện phải cập nhật dữ liệu liên quan trong cùng transaction.
10. Dữ liệu đã xác nhận chỉ được chỉnh sửa bởi quyền phù hợp.

---

# 6. Xử lý đồng thời

## 6.1 Duyệt đăng ký

Trong một transaction:

1. khóa bản ghi ca hoặc truy vấn ca với pessimistic lock;
2. đếm phân công `ASSIGNED`;
3. kiểm tra còn chỉ tiêu;
4. kiểm tra trùng lịch;
5. cập nhật đăng ký `APPROVED`;
6. tạo phân công;
7. commit.

## 6.2 Phân công trực tiếp

Trong một transaction:

1. khóa ca;
2. kiểm tra nhân viên;
3. kiểm tra chỉ tiêu;
4. kiểm tra trùng lịch;
5. tạo phân công `DIRECT`;
6. commit.

## 6.3 Hủy sự kiện

Trong một transaction:

1. khóa sự kiện;
2. chuyển sự kiện `CANCELLED`;
3. chuyển các ca chưa hoàn thành `CANCELLED`;
4. chuyển đăng ký chưa kết thúc `CANCELLED`;
5. chuyển phân công chưa hoàn thành `CANCELLED`;
6. commit.

---

# 7. Checklist đối chiếu với schema và backend hiện tại

Chưa sửa code ngay. Trước tiên kiểm tra:

- [ ] Tên bảng hiện tại có khớp tài liệu không?
- [ ] Enum backend có khớp trạng thái đã chốt không?
- [ ] `shift_assignments.registration_id` đã cho phép `NULL` chưa?
- [ ] Đã có `assignment_source` chưa?
- [ ] Đã có `shift_role` chưa?
- [ ] Chấm công đã tách `process_status` và `attendance_result` chưa?
- [ ] Đã có `registration_deadline` chưa?
- [ ] Đã có các trường hủy và lý do chưa?
- [ ] Đã có bảng `payrolls` và `evaluations` chưa?
- [ ] Unique và index đã đầy đủ chưa?
- [ ] API hiện tại có dùng thuật ngữ thống nhất với SRS không?
- [ ] Service đã kiểm tra trùng lịch bằng công thức chuẩn chưa?
- [ ] Thao tác duyệt và phân công đã dùng transaction chưa?

---

# 8. Trạng thái tài liệu

Đây là **Data Dictionary Draft v0.1**, chưa phải ERD và chưa phải schema cuối cùng.

Chỉ nâng thành phiên bản được khóa sau khi:

1. đối chiếu với `database/01_schema.sql`;
2. đối chiếu với Entity và Enum trong backend;
3. ghi nhận các điểm khác biệt;
4. quyết định sửa tài liệu hay sửa code;
5. chạy lại database và kiểm thử API thành công.
