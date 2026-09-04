# Commit 13 — Integration / Security / Regression

## Mục tiêu

Commit 13 harden các bất biến đã có, không thêm nghiệp vụ mới và không thay đổi schema DB.

## Finding và fix

### 1. QR attendance nhận tọa độ một phía
- `SelfAttendanceRequest` trước đây cho phép latitude hoặc longitude đứng một mình.
- Service chỉ bắt GPS khi phiên yêu cầu GPS; phiên không yêu cầu GPS có thể đi qua với tọa độ không đầy đủ.
- Fix: validation DTO + validation service bắt buộc latitude/longitude cùng có hoặc cùng không.

### 2. Phiên không yêu cầu GPS vẫn có thể lưu tọa độ client gửi lên
- Audit event trước đây ghi latitude/longitude từ request kể cả khi session không cấu hình GPS.
- Fix: chỉ ghi tọa độ vào attendance check event khi session thật sự yêu cầu GPS.
- Nếu session không yêu cầu GPS, cặp tọa độ client gửi lên bị bỏ qua khi persistence.

### 3. Credential self-attendance được validate muộn
- QR và OTP phải đúng một phương thức.
- OTP phải đúng 6 chữ số.
- Fix: thêm Bean Validation ở DTO, đồng thời giữ kiểm tra service để bảo vệ internal calls.

### 4. Direct assignment chưa enforce đầy đủ employee eligibility
Ứng viên phân công trực tiếp phải:
- employment status ACTIVE;
- account status ACTIVE;
- account role EMPLOYEE.

Fix: load employee kèm user và kiểm tra đủ ba điều kiện trước capacity/overlap.

### 5. Replacement candidate chưa enforce đầy đủ account eligibility
Ứng viên thay ca phải:
- không phải original employee;
- employment ACTIVE;
- account ACTIVE;
- role EMPLOYEE;
- không trùng ca / overlap.

### 6. Shift mutation sử dụng pessimistic lock
Update/status transition của shift dùng `findByIdForUpdate` để giảm race giữa các thao tác concurrent trên cùng ca.

### 7. DTO ID validation
- replacement `assignmentId` phải > 0;
- invitation `employeeId` phải > 0;
- attendance session cross-field GPS policy được validate ở DTO.

## Không thay đổi

- Không migration DB.
- Không đổi payroll formula/snapshot.
- Không đổi replacement state machine.
- Không đổi event cancellation behavior.
- Không đổi JWT format.
- Không đổi frontend route/business behavior.

## Source-review limitation

Review đầu vào không chứa file cấu hình runtime thực tế (`application.properties` bị missing) và không chứa controller auth theo path dự kiến. Vì vậy Commit 13 không tuyên bố đã audit hoàn chỉnh phần secret/CORS/auth endpoint surface chỉ từ file review này. Trước khi release cần kiểm tra riêng các file resources thực tế và controller/service auth đang tồn tại.

## Test plan

Targeted:
- `DtoValidationTest`
- `AssignmentServiceTest`
- `QrAttendanceServiceTest`
- `ReplacementServiceTest`
- `AttendanceServiceTest`
- `AttendanceSelfCheckServiceTest`

Sau targeted test:
1. `mvn clean test`
2. `npm run build`
3. Runtime security smoke:
   - unauthenticated protected API -> 401;
   - EMPLOYEE gọi manager endpoint -> 403;
   - ADMIN/COORDINATOR gọi manager endpoint -> success;
   - non-GPS attendance không persist tọa độ;
   - one-sided coordinate -> 400;
   - GPS-required session vẫn verify radius;
   - inactive/wrong-role employee không thể direct assign/invite replacement.
