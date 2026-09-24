# DACN v1.0 — Test & Verification Report

## 1. Final automated backend regression

Final hardening baseline trước Commit 14:

```text
mvn clean test

Tests run: 97
Failures: 0
Errors: 0
Skipped: 0
BUILD SUCCESS
```

Các test bao phủ service/business rules cho:

- authentication-adjacent validation/error behavior;
- assignment;
- attendance;
- self attendance;
- QR/OTP/GPS;
- replacement;
- deterministic recommendation;
- AI recommendation/fallback;
- reputation;
- coordination;
- payroll;
- dashboard analytics;
- area/table placement.

## 2. Targeted security regression

Command:

```text
mvn "-Dtest=DtoValidationTest,AssignmentServiceTest,QrAttendanceServiceTest,ReplacementServiceTest,AttendanceServiceTest,AttendanceSelfCheckServiceTest" clean test
```

Kết quả:

```text
Tests run: 40
Failures: 0
Errors: 0
Skipped: 0
BUILD SUCCESS
```

## 3. Frontend production build

```text
npm run build

vite v8.2.1
164 modules transformed
BUILD PASS
```

Không có frontend dependency mới trong Commit 13/14.

## 4. Runtime security smoke

Kết quả final hardening:

| Test | Expected | Actual |
|---|---:|---:|
| Protected API không JWT | 401 | 401 |
| JWT không hợp lệ | 401 | 401 |
| ADMIN gọi dashboard manager API | 200 | 200 |
| ADMIN gọi EMPLOYEE-only `/assignments/mine` | 403 | 403 |
| Attendance request chỉ có một phía GPS | 400 | 400 |

## 5. CORS smoke

Allowed development origin:

```text
Origin: http://localhost:5173
HTTP 200
Access-Control-Allow-Origin: http://localhost:5173
```

Untrusted origin:

```text
Origin: https://evil.example
HTTP 403
```

## 6. Milestone runtime verification đã thực hiện

### QR / OTP / GPS

Đã smoke:

- QR check-in;
- OTP check-out fallback;
- GPS radius validation;
- employee ownership;
- Attendance vẫn DRAFT trước manager confirmation.

### Area / Table

Đã smoke:

- create area/table;
- structured placement;
- replacement kế thừa placement;
- employee xem placement của mình.

### Multi-rule payroll

Đã smoke các trường hợp:

- STAFF present;
- LEADER allowance;
- late deduction;
- early-leave deduction;
- ABSENT;
- overtime;
- manager payroll report.

### Advanced dashboard

Đã smoke:

- operations analytics;
- workforce analytics;
- AI analytics;
- payroll summary;
- UI filter/render.

### UI/UX

Đã smoke:

- responsive sidebar;
- modal/dialog;
- Area/Table edit modal;
- cancellation reason dialog;
- attendance confirmation dialog;
- QR revoke confirmation;
- Escape close;
- mobile viewport.

## 7. Security/hardening assertions

Release giữ các invariant:

- confirmed attendance immutable;
- payroll historical snapshots không recompute;
- direct/replacement candidate phải employment ACTIVE + account ACTIVE + role EMPLOYEE;
- one-sided GPS bị reject;
- non-GPS session không cần lưu vị trí request;
- shift mutations critical dùng pessimistic lock;
- replacement vẫn re-check candidate/capacity/overlap lúc nhận lời;
- invalid JWT không authenticate;
- role endpoint protection vẫn áp dụng.

## 8. Giới hạn kiểm thử

Project hiện **không tuyên bố có**:

- browser E2E suite bằng Playwright/Cypress;
- Testcontainers/full database integration suite;
- load/performance benchmark;
- penetration test chuyên nghiệp.

Do đó release evidence phải được mô tả đúng là:

```text
backend automated tests
+ frontend production build
+ manual runtime API/security/UI smoke
```

không gọi toàn bộ 97 tests là “full end-to-end integration tests”.

## 9. Final release gate

Trước `dacn-v1.0.0` chạy lại:

```text
git status --short
git diff --check

cd backend
mvn clean test

cd ../frontend
npm run build
```

Sau đó runtime smoke tối thiểu:

```text
login
dashboard
replacement
AI deterministic/fallback
attendance
payroll
401/403
CORS
```
