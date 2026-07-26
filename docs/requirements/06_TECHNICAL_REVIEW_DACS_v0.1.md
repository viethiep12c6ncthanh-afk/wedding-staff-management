# RÀ SOÁT KỸ THUẬT BACKEND DACS

**Phiên bản:** v0.1  
**Phạm vi kiểm tra:** Source backend, SQL schema, seed data và cấu hình ứng dụng trong `DACS_TECHNICAL_REVIEW_v0.1.zip`.

---

## 1. Kết luận

Backend hiện tại là một **starter chạy được**, không phải code bỏ đi. Các phần có thể giữ làm nền:

- Spring Boot, Java 21, Maven;
- JWT và Spring Security;
- cấu trúc Controller → Service → Repository;
- Entity nền tảng;
- CRUD địa điểm, sự kiện, ca;
- đăng ký và duyệt ca cơ bản;
- công thức kiểm tra khoảng thời gian giao nhau.

Tuy nhiên, code chưa thể xem là baseline vì schema, Entity, Enum, DTO và nghiệp vụ chưa đồng nhất với SRS mới.

---

## 2. Phát hiện mức nghiêm trọng cao

### SEC-01. Mật khẩu MySQL được ghi trực tiếp trong `application.yml`

```yaml
username: root
password: <mật khẩu thật>
```

Cần chuyển sang biến môi trường. Nếu repository đã được đẩy lên GitHub, phải đổi mật khẩu MySQL vì giá trị cũ vẫn có thể tồn tại trong lịch sử Git.

### SEC-02. JWT secret được commit trực tiếp

JWT secret phải chuyển sang biến môi trường. Nếu repository đã được công khai hoặc chia sẻ, phải thay secret.

### DB-01. `ddl-auto: update` xung đột với schema SQL

Hiện có cả:

- `database/01_schema.sql`;
- Hibernate tự cập nhật cấu trúc bằng `ddl-auto: update`.

Điều này tạo hai nguồn định nghĩa database. Sau khi refactor hoàn tất, chuyển sang `ddl-auto: validate` hoặc dùng migration.

### DATA-01. `registration_open` trùng với `ShiftStatus`

Ca đã có `OPEN` và `CLOSED`, nên boolean `registration_open` tạo ra trạng thái mâu thuẫn, ví dụ:

```text
registration_open = true
status = CLOSED
```

Cần xóa boolean và chỉ dùng `shift_status`.

### DATA-02. Chấm công đang trộn hai loại trạng thái

`Attendance.status` hiện chứa kết quả đi làm, nhưng hệ thống còn cần trạng thái xác nhận.

Cần tách:

- `processStatus`: `DRAFT`, `CONFIRMED`;
- `attendanceResult`: `PRESENT`, `LATE`, `EARLY_LEAVE`, `LATE_AND_EARLY_LEAVE`, `ABSENT`.

---

## 3. Điểm cần sửa trong Data Dictionary v0.1

### DD-01. Không nên chuyển toàn bộ thông tin cá nhân khỏi `users`

`ADMIN` và `COORDINATOR` cũng cần họ tên hiển thị, email và điện thoại. Nếu các trường này chỉ nằm trong `employees`, hai loại tài khoản trên không có hồ sơ tên.

**Quyết định sửa:**

`users` tiếp tục lưu:

- `full_name`;
- `email`;
- `phone`.

`employees` chỉ lưu thông tin chuyên biệt của nhân viên:

- `employee_code`;
- `employment_status`;
- ngày sinh;
- địa chỉ;
- kinh nghiệm;
- ghi chú.

### DD-02. Trạng thái nhân viên

`AVAILABLE` dễ bị hiểu là đang rảnh tại một thời điểm.

Dùng:

- `ACTIVE`;
- `INACTIVE`;
- `SUSPENDED`.

Khả năng rảnh hay trùng lịch được xác định từ ca và phân công, không dùng trạng thái hồ sơ.

### DD-03. Trạng thái sự kiện

Giữ `IN_PROGRESS` vì source hiện có và phù hợp vòng đời thực tế:

- `DRAFT`;
- `CONFIRMED`;
- `IN_PROGRESS`;
- `COMPLETED`;
- `CANCELLED`.

---

## 4. Đánh giá từng lớp nghiệp vụ

### UserAccount

Điểm tốt:

- triển khai `UserDetails`;
- role ánh xạ thành `ROLE_<NAME>`;
- BCrypt được cấu hình.

Cần sửa:

- đổi `password` thành `passwordHash` trong Entity/column;
- thay boolean `enabled` bằng `AccountStatus`;
- `isAccountNonLocked()` hiện luôn trả `true`, nên trạng thái khóa chưa có hiệu lực;
- bổ sung `mustChangePassword`, `lastLoginAt`;
- giữ `fullName`, `email`, `phone`.

### Employee

Cần bổ sung:

- ngày sinh;
- địa chỉ;
- chuẩn hóa `status` thành `employmentStatus`;
- đổi `notes` thành `note`.

Có thể giữ `experienceLevel` như thông tin hồ sơ, nhưng không dùng cho AI trong DACS.

### Venue

Cần sửa:

- `delete()` hiện xóa vật lý;
- phải chuyển thành cập nhật `INACTIVE`;
- bổ sung người liên hệ, ghi chú và người tạo;
- tên trường trạng thái nên thống nhất `venueStatus`.

### Event

Cần sửa:

- không cho client tùy ý truyền mọi trạng thái trong API cập nhật chung;
- bổ sung kiểm tra địa điểm đang hoạt động;
- bổ sung thông tin tạo và hủy;
- cần endpoint/chức năng chuyển trạng thái rõ ràng;
- hủy sự kiện phải cập nhật ca, đăng ký và phân công trong transaction.

### WorkShift

Cần sửa:

- bỏ `registrationOpen`;
- thêm `registrationDeadline`;
- kiểm tra ca nằm trong khoảng thời gian sự kiện;
- kiểm tra sự kiện không bị hủy/hoàn thành;
- không cho client thay đổi trạng thái tùy ý qua update CRUD;
- bổ sung thông tin tạo và hủy.

### ShiftRegistration

Điểm tốt:

- có unique `(shift_id, employee_id)`;
- công thức kiểm tra overlap hiện tại đúng;
- có trạng thái duyệt.

Cần sửa:

- kiểm tra nhân viên `ACTIVE`;
- bổ sung hủy đăng ký trước 24 giờ;
- bổ sung người, thời điểm và lý do hủy;
- khi kiểm tra trùng lịch phải kiểm tra **phân công đang hiệu lực**, không chỉ đăng ký `APPROVED`;
- từ chối phải yêu cầu lý do;
- không được duyệt khi ca không còn hợp lệ.

### ShiftAssignment

Cần bổ sung:

- `registration_id` nullable;
- `assignment_source`: `REGISTRATION`, `DIRECT`;
- `shift_role`: `LEADER`, `STAFF`;
- thông tin hủy;
- kiểm tra tối đa một trưởng ca đang hiệu lực;
- kiểm tra trùng lịch;
- transaction và khóa ca khi duyệt/phân công.

Các trạng thái `CONFIRMED` và `ABSENT` trong `AssignmentStatus` nên loại bỏ vì thuộc chấm công.

### Attendance

Cần bổ sung:

- `processStatus`;
- `attendanceResult`;
- `lateMinutes`, `earlyLeaveMinutes`;
- `recordedBy`, `recordedAt`;
- `confirmedAt`;
- kiểm tra quyền trưởng ca đúng ca.

Hiện chưa có Service hoặc Controller cho Attendance.

### Payroll và Evaluation

Chưa tồn tại. Chỉ triển khai sau khi phân công và chấm công ổn định.

---

## 5. Lỗi nghiệp vụ trong RegistrationService hiện tại

### REG-01. Đếm người bằng đăng ký APPROVED

```java
countByShiftIdAndStatus(... APPROVED)
```

Khi hỗ trợ phân công trực tiếp, số người thực tế phải đếm từ `shift_assignments` có trạng thái `ASSIGNED`.

### REG-02. Không khóa ca khi duyệt

Hai điều phối viên có thể duyệt đồng thời và cùng thấy còn chỗ, làm vượt `requiredStaff`.

Cần dùng transaction kết hợp pessimistic lock trên ca.

### REG-03. Kiểm tra trùng lịch chưa bao gồm phân công trực tiếp

Query hiện chỉ tìm `ShiftRegistration.APPROVED`. Phải truy vấn `ShiftAssignment.ASSIGNED`.

### REG-04. Chưa liên kết Assignment với Registration

Khi duyệt, assignment được tạo nhưng không lưu đăng ký nguồn.

### REG-05. `roleInShift` là chuỗi tự do

Cần dùng enum `ShiftRole`.

### REG-06. Không bắt buộc lý do từ chối

Khi `approved = false`, `rejectionReason` phải có nội dung.

---

## 6. Kiến trúc và chất lượng code

### Giữ lại

- record DTO;
- `@Transactional` trong Service;
- `open-in-view: false`;
- phân quyền bằng `@PreAuthorize`;
- BCrypt;
- JWT filter.

### Cần cải thiện

- `EmployeeController` đang gọi Repository trực tiếp và đặt transaction tại Controller;
- thiếu DTO/Service cho Employee;
- thiếu custom exception và mã lỗi;
- chưa có test;
- chưa có Swagger/OpenAPI dependency;
- các `findAll()` chưa phân trang;
- client đang được phép cung cấp trạng thái tạo/cập nhật quá tự do;
- `DataInitializer` chứa mật khẩu mẫu cố định và chạy ở mọi profile.

---

## 7. Thứ tự refactor bắt buộc

### Commit 1 — Bảo mật cấu hình

- chuyển DB username/password và JWT secret sang biến môi trường;
- giới hạn `DataInitializer` ở profile `dev`;
- không thay đổi nghiệp vụ.

### Commit 2 — Chuẩn hóa tài khoản và hồ sơ

- sửa `UserAccount`;
- sửa `Employee`;
- sửa enum trạng thái;
- đồng bộ schema, initializer, login response và employee API.

### Commit 3 — Venue, Event, Shift

- soft deactivate Venue;
- bổ sung audit/hủy Event;
- bỏ `registrationOpen`;
- thêm `registrationDeadline`;
- kiểm tra ca trong thời gian sự kiện.

### Commit 4 — Registration và Assignment

- thêm source/role/registration relation;
- thêm direct assignment;
- overlap từ Assignment;
- pessimistic lock;
- giới hạn chỉ tiêu;
- hủy đăng ký.

### Commit 5 — Attendance

- tách process/result;
- thêm quyền trưởng ca;
- thêm API ghi nhận và xác nhận.

### Commit 6 — Payroll và Evaluation

- thêm bảng, Entity, Service và API.

### Commit 7 — Test và tài liệu API

- unit/integration test;
- Postman;
- Swagger/OpenAPI;
- cập nhật ERD sau khi schema chạy ổn định.

---

## 8. Trạng thái build

Không thể chạy Maven trong môi trường rà soát hiện tại vì không có lệnh `mvn`. Đánh giá này là static review dựa trên toàn bộ source được cung cấp.

Backend đã từng chạy trên máy người dùng, nhưng mọi patch tiếp theo vẫn phải được kiểm tra bằng:

```powershell
mvn clean test
mvn spring-boot:run
```

và Postman sau mỗi commit.
