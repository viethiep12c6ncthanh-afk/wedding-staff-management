# DATA DICTIONARY — ĐỒ ÁN CƠ SỞ

**Phiên bản:** Candidate v0.2  
**Trạng thái:** Baseline nội bộ để refactor  
**Thay thế:** `04_DATA_DICTIONARY_DACS_DRAFT_v0.1.md`

---

## 1. Quyết định mô hình

### 1.1 Thông tin người dùng

Bảng `users` lưu thông tin chung cho mọi tài khoản:

- họ tên;
- email;
- điện thoại;
- thông tin xác thực;
- role;
- trạng thái tài khoản.

Bảng `employees` chỉ mở rộng thông tin nghiệp vụ cho tài khoản nhân viên.

### 1.2 Trưởng ca

`LEADER` không phải role tài khoản. Đây là `shift_role` trong `shift_assignments`.

### 1.3 Trạng thái ca

Không sử dụng `registration_open`. Việc mở/đóng đăng ký được thể hiện bởi `shift_status = OPEN/CLOSED`.

### 1.4 Chấm công

Tách trạng thái quy trình và kết quả đi làm.

---

## 2. Bảng `roles`

| Cột | Kiểu | Null | Ràng buộc |
|---|---|:---:|---|
| `id` | `BIGINT` | Không | PK, AUTO_INCREMENT |
| `name` | `VARCHAR(30)` | Không | UNIQUE |

Dữ liệu: `ADMIN`, `COORDINATOR`, `EMPLOYEE`.

---

## 3. Bảng `users`

| Cột | Kiểu | Null | Ràng buộc/Ghi chú |
|---|---|:---:|---|
| `id` | `BIGINT` | Không | PK, AUTO_INCREMENT |
| `role_id` | `BIGINT` | Không | FK → `roles.id` |
| `username` | `VARCHAR(50)` | Không | UNIQUE |
| `password_hash` | `VARCHAR(255)` | Không | BCrypt |
| `full_name` | `VARCHAR(120)` | Không | Tên hiển thị |
| `email` | `VARCHAR(120)` | Có | UNIQUE |
| `phone` | `VARCHAR(20)` | Có | UNIQUE |
| `account_status` | `VARCHAR(20)` | Không | `ACTIVE`, `LOCKED` |
| `must_change_password` | `BOOLEAN` | Không | DEFAULT TRUE |
| `last_login_at` | `DATETIME(6)` | Có |  |
| `created_by` | `BIGINT` | Có | FK → `users.id` |
| `created_at` | `DATETIME(6)` | Không |  |
| `updated_at` | `DATETIME(6)` | Không |  |

---

## 4. Bảng `employees`

| Cột | Kiểu | Null | Ràng buộc/Ghi chú |
|---|---|:---:|---|
| `id` | `BIGINT` | Không | PK, AUTO_INCREMENT |
| `user_id` | `BIGINT` | Không | FK, UNIQUE → `users.id` |
| `employee_code` | `VARCHAR(30)` | Không | UNIQUE |
| `employment_status` | `VARCHAR(20)` | Không | `ACTIVE`, `INACTIVE`, `SUSPENDED` |
| `date_of_birth` | `DATE` | Có |  |
| `address` | `VARCHAR(255)` | Có |  |
| `experience_level` | `VARCHAR(50)` | Có | Thông tin mô tả, không dùng AI trong DACS |
| `note` | `VARCHAR(500)` | Có |  |
| `created_at` | `DATETIME(6)` | Không |  |
| `updated_at` | `DATETIME(6)` | Không |  |

Quy tắc: tài khoản liên kết phải có role `EMPLOYEE`.

---

## 5. Bảng `venues`

| Cột | Kiểu | Null | Ràng buộc/Ghi chú |
|---|---|:---:|---|
| `id` | `BIGINT` | Không | PK |
| `name` | `VARCHAR(150)` | Không |  |
| `address` | `VARCHAR(300)` | Không |  |
| `contact_name` | `VARCHAR(120)` | Có |  |
| `contact_phone` | `VARCHAR(20)` | Có |  |
| `venue_status` | `VARCHAR(20)` | Không | `ACTIVE`, `INACTIVE` |
| `note` | `VARCHAR(500)` | Có |  |
| `created_by` | `BIGINT` | Có | FK → `users.id` |
| `created_at` | `DATETIME(6)` | Không |  |
| `updated_at` | `DATETIME(6)` | Không |  |

Không xóa vật lý địa điểm đã phát sinh dữ liệu.

---

## 6. Bảng `events`

| Cột | Kiểu | Null | Ràng buộc/Ghi chú |
|---|---|:---:|---|
| `id` | `BIGINT` | Không | PK |
| `venue_id` | `BIGINT` | Không | FK → `venues.id` |
| `name` | `VARCHAR(150)` | Không |  |
| `start_at` | `DATETIME(6)` | Không |  |
| `end_at` | `DATETIME(6)` | Không | `start_at < end_at` |
| `event_status` | `VARCHAR(20)` | Không | `DRAFT`, `CONFIRMED`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED` |
| `description` | `VARCHAR(1000)` | Có |  |
| `created_by` | `BIGINT` | Có | FK → `users.id` |
| `cancelled_by` | `BIGINT` | Có | FK → `users.id` |
| `cancelled_at` | `DATETIME(6)` | Có |  |
| `cancellation_reason` | `VARCHAR(500)` | Có | Bắt buộc khi hủy |
| `created_at` | `DATETIME(6)` | Không |  |
| `updated_at` | `DATETIME(6)` | Không |  |

---

## 7. Bảng `shifts`

| Cột | Kiểu | Null | Ràng buộc/Ghi chú |
|---|---|:---:|---|
| `id` | `BIGINT` | Không | PK |
| `event_id` | `BIGINT` | Không | FK → `events.id` |
| `name` | `VARCHAR(120)` | Không |  |
| `start_at` | `DATETIME(6)` | Không |  |
| `end_at` | `DATETIME(6)` | Không | `start_at < end_at` |
| `required_staff` | `SMALLINT` | Không | > 0 |
| `pay_amount` | `DECIMAL(12,2)` | Không | >= 0 |
| `registration_deadline` | `DATETIME(6)` | Có | <= `start_at` |
| `shift_status` | `VARCHAR(20)` | Không | `DRAFT`, `OPEN`, `CLOSED`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED` |
| `description` | `VARCHAR(500)` | Có |  |
| `created_by` | `BIGINT` | Có | FK → `users.id` |
| `cancelled_by` | `BIGINT` | Có | FK → `users.id` |
| `cancelled_at` | `DATETIME(6)` | Có |  |
| `cancellation_reason` | `VARCHAR(500)` | Có |  |
| `created_at` | `DATETIME(6)` | Không |  |
| `updated_at` | `DATETIME(6)` | Không |  |

Ca phải nằm trong khoảng thời gian của sự kiện.

---

## 8. Bảng `shift_registrations`

| Cột | Kiểu | Null | Ràng buộc/Ghi chú |
|---|---|:---:|---|
| `id` | `BIGINT` | Không | PK |
| `shift_id` | `BIGINT` | Không | FK → `shifts.id` |
| `employee_id` | `BIGINT` | Không | FK → `employees.id` |
| `registration_status` | `VARCHAR(20)` | Không | `PENDING`, `APPROVED`, `REJECTED`, `CANCELLED` |
| `registered_at` | `DATETIME(6)` | Không |  |
| `reviewed_by` | `BIGINT` | Có | FK → `users.id` |
| `reviewed_at` | `DATETIME(6)` | Có |  |
| `rejection_reason` | `VARCHAR(500)` | Có | Bắt buộc khi từ chối |
| `cancelled_by` | `BIGINT` | Có | FK → `users.id` |
| `cancelled_at` | `DATETIME(6)` | Có |  |
| `cancellation_reason` | `VARCHAR(500)` | Có |  |
| `updated_at` | `DATETIME(6)` | Không |  |

UNIQUE (`shift_id`, `employee_id`).

---

## 9. Bảng `shift_assignments`

| Cột | Kiểu | Null | Ràng buộc/Ghi chú |
|---|---|:---:|---|
| `id` | `BIGINT` | Không | PK |
| `shift_id` | `BIGINT` | Không | FK → `shifts.id` |
| `employee_id` | `BIGINT` | Không | FK → `employees.id` |
| `registration_id` | `BIGINT` | Có | FK, UNIQUE → `shift_registrations.id` |
| `assignment_source` | `VARCHAR(20)` | Không | `REGISTRATION`, `DIRECT` |
| `shift_role` | `VARCHAR(20)` | Không | `LEADER`, `STAFF` |
| `area` | `VARCHAR(100)` | Có |  |
| `task_description` | `VARCHAR(500)` | Có |  |
| `assignment_status` | `VARCHAR(20)` | Không | `ASSIGNED`, `COMPLETED`, `CANCELLED` |
| `assigned_by` | `BIGINT` | Không | FK → `users.id` |
| `assigned_at` | `DATETIME(6)` | Không |  |
| `cancelled_by` | `BIGINT` | Có | FK → `users.id` |
| `cancelled_at` | `DATETIME(6)` | Có |  |
| `cancellation_reason` | `VARCHAR(500)` | Có |  |
| `updated_at` | `DATETIME(6)` | Không |  |

UNIQUE (`shift_id`, `employee_id`).

Quy tắc:

- `REGISTRATION` → `registration_id` bắt buộc;
- `DIRECT` → `registration_id` phải `NULL`;
- tối đa một `LEADER` đang hiệu lực trong một ca;
- không vượt `required_staff`;
- không trùng lịch.

---

## 10. Bảng `attendances`

| Cột | Kiểu | Null | Ràng buộc/Ghi chú |
|---|---|:---:|---|
| `id` | `BIGINT` | Không | PK |
| `assignment_id` | `BIGINT` | Không | FK, UNIQUE |
| `process_status` | `VARCHAR(20)` | Không | `DRAFT`, `CONFIRMED` |
| `attendance_result` | `VARCHAR(30)` | Có | `PRESENT`, `LATE`, `EARLY_LEAVE`, `LATE_AND_EARLY_LEAVE`, `ABSENT` |
| `check_in_at` | `DATETIME(6)` | Có |  |
| `check_out_at` | `DATETIME(6)` | Có |  |
| `late_minutes` | `INT` | Không | DEFAULT 0 |
| `early_leave_minutes` | `INT` | Không | DEFAULT 0 |
| `note` | `VARCHAR(500)` | Có |  |
| `recorded_by` | `BIGINT` | Không | FK → `users.id` |
| `recorded_at` | `DATETIME(6)` | Không |  |
| `confirmed_by` | `BIGINT` | Có | FK → `users.id` |
| `confirmed_at` | `DATETIME(6)` | Có |  |
| `updated_at` | `DATETIME(6)` | Không |  |

---

## 11. Bảng `payrolls`

| Cột | Kiểu | Null | Ràng buộc/Ghi chú |
|---|---|:---:|---|
| `id` | `BIGINT` | Không | PK |
| `assignment_id` | `BIGINT` | Không | FK, UNIQUE |
| `base_amount` | `DECIMAL(12,2)` | Không |  |
| `adjustment_amount` | `DECIMAL(12,2)` | Không | DEFAULT 0 |
| `adjustment_reason` | `VARCHAR(500)` | Có | Bắt buộc khi điều chỉnh khác 0 |
| `final_amount` | `DECIMAL(12,2)` | Không | >= 0 |
| `payroll_status` | `VARCHAR(20)` | Không | `CALCULATED`, `CONFIRMED`, `PAID` |
| `calculated_by` | `BIGINT` | Không | FK → `users.id` |
| `calculated_at` | `DATETIME(6)` | Không |  |
| `confirmed_by` | `BIGINT` | Có | FK → `users.id` |
| `confirmed_at` | `DATETIME(6)` | Có |  |
| `paid_at` | `DATETIME(6)` | Có |  |
| `updated_at` | `DATETIME(6)` | Không |  |

---

## 12. Bảng `evaluations`

| Cột | Kiểu | Null | Ràng buộc/Ghi chú |
|---|---|:---:|---|
| `id` | `BIGINT` | Không | PK |
| `assignment_id` | `BIGINT` | Không | FK, UNIQUE |
| `score` | `TINYINT` | Không | 1–5 |
| `comment` | `VARCHAR(1000)` | Có |  |
| `evaluated_by` | `BIGINT` | Không | FK → `users.id` |
| `evaluated_at` | `DATETIME(6)` | Không |  |
| `updated_at` | `DATETIME(6)` | Không |  |

---

## 13. Index nghiệp vụ tối thiểu

- `events(venue_id, start_at)`
- `shifts(event_id, start_at)`
- `shifts(shift_status, start_at)`
- `shift_registrations(employee_id, registration_status)`
- `shift_registrations(shift_id, registration_status)`
- `shift_assignments(employee_id, assignment_status)`
- `shift_assignments(shift_id, assignment_status)`
- `attendances(process_status)`
- `payrolls(payroll_status)`

---

## 14. Quy tắc xử lý trong Service/Transaction

Database không tự bảo đảm hoàn toàn các quy tắc sau:

1. tài khoản nhân viên phải có role `EMPLOYEE`;
2. ca nằm trong thời gian sự kiện;
3. không trùng lịch;
4. không vượt số người cần;
5. tối đa một trưởng ca;
6. registration và assignment cùng nhân viên/cùng ca;
7. quyền trưởng ca đúng ca;
8. chỉ tính công sau khi chấm công xác nhận;
9. hủy sự kiện cập nhật toàn bộ dữ liệu liên quan.
