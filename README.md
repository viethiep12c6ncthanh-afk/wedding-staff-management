# Wedding Staff Management System — DACN v1.1

Hệ thống quản lý ca làm và điều phối nhân sự phục vụ tiệc cưới, sự kiện đa địa điểm tích hợp AI.

Bản này là kết quả phát triển **Đồ án chuyên ngành (DACN)** trên baseline DACS `dacs-v1.0.0`.
Nhánh nghiệm thu cuối: `main`. Tag `dacn-v1.0.0` được giữ nguyên như mốc release lịch sử; tag hiệu chỉnh cuối chỉ tạo sau khi C10 PASS.

## 1. Chức năng chính

### Nền tảng DACS được bảo toàn

- Đăng nhập JWT và phân quyền `ADMIN`, `COORDINATOR`, `EMPLOYEE`.
- Quản lý nhân viên, địa điểm, sự kiện và ca làm.
- Đăng ký ca, duyệt/từ chối, phân công từ đăng ký hoặc phân công trực tiếp.
- Kiểm tra trùng lịch theo khoảng `[start, end)`.
- Chấm công `DRAFT -> CONFIRMED`.
- Báo cáo tiền công dựa trên snapshot đã xác nhận.

### Mở rộng DACN

- Điều phối nhiều ca / nhiều địa điểm và cảnh báo thiếu nhân sự.
- Uy tín nhân viên + lịch sử đánh giá có thể truy vết.
- Yêu cầu thay ca, lời mời người thay và bảo toàn lịch sử phân công.
- Lọc ứng viên bằng hard constraints + deterministic scoring.
- Hybrid AI recommendation: deterministic ranking -> LLM rerank/explain -> coordinator quyết định.
- Ollama local mặc định; OpenAI là provider tùy chọn.
- QR check-in/check-out, OTP fallback và GPS bán kính tùy chọn.
- QR demo mode cho phép kiểm thử ngay mà không sửa giờ ca.
- Phân khu vực/bàn có cấu trúc cho từng ca.
- Tính công nhiều quy tắc: phụ cấp LEADER, đi trễ, về sớm, tăng ca, vắng mặt.
- Dashboard vận hành, workforce, replacement, AI analytics và payroll.
- Audit log tự động cho mọi API thay đổi dữ liệu, có màn hình tra cứu dành cho ADMIN.
- Trung tâm thông báo vận hành gần thời gian thực (polling 10 giây, phân phạm vi theo vai trò).
- Xuất báo cáo tiền công dạng CSV tương thích Excel và bản in/PDF từ trình duyệt.
- Đóng gói Docker Compose cho MySQL, Spring Boot và React/Nginx.
- Responsive UI, modal/dialog thống nhất và các trạng thái loading/error/disabled.

## 2. Vai trò

| Vai trò | Phạm vi |
|---|---|
| `ADMIN` | Quản trị và toàn bộ nghiệp vụ quản lý |
| `COORDINATOR` | Điều phối ca, nhân sự, replacement, attendance, dashboard/report |
| `EMPLOYEE` | Đăng ký ca, xem phân công cá nhân, replacement cá nhân, QR/OTP attendance |

`LEADER` **không phải account role**. Đây là `ShiftRole` theo từng ca, cùng với `STAFF`.

## 3. Kiến trúc

```text
React / Vite
    |
    | REST JSON + JWT
    v
Spring Boot
    Controller
        -> Service (@Transactional)
            -> Repository (Spring Data JPA)
                -> MySQL

Recommendation:
Hard constraints
    -> deterministic scoring
        -> optional LLM rerank/explanation
            -> Coordinator final decision
```

AI không được tạo employee ID mới, bỏ qua hard constraints, tự gửi invitation hoặc tự tạo assignment.

## 4. Công nghệ

- Java 21
- Spring Boot 3.5.4
- Spring Security + JWT
- Spring Data JPA / Hibernate
- Bean Validation
- MySQL 8.4
- Maven
- React 19
- Vite 8
- Axios
- React Router
- Ollama local hoặc OpenAI cho AI-assisted recommendation

## 5. Database

Các script trong `database/migrations` là **manual one-time migrations**, không phải Flyway runtime migrations.

Thứ tự hiện tại:

```text
V002__user_employee_refactor.sql
V003__venue_event_shift_refactor.sql
V004__registration_assignment_refactor.sql
V004_1__allow_reassignment_after_cancellation.sql
V005__attendance_basic_pay_refactor.sql
V006__employee_management.sql
V007__employee_reputation_and_evaluation.sql
V008__replacement_staffing_workflow.sql
V009__ai_assisted_recommendation.sql
V010__qr_otp_gps_attendance.sql
V011__advanced_area_table_assignment.sql
V012__multi_rule_payroll.sql
V013__operational_audit.sql
```

### Database mới

Áp dụng từng script theo đúng thứ tự, **mỗi script đúng một lần**.

### Database dev đã migrate

Không chạy lại V011/V012 hoặc các script trước đó. Khi xác minh release, ưu tiên:

```text
JPA_DDL_AUTO=validate
```

`update` chỉ phù hợp cho development có kiểm soát; SQL migration mới là bản ghi version schema của project.

## 6. Cấu hình backend

Các secret không được commit. Backend đọc từ environment variables.

Tối thiểu:

```text
DB_USERNAME=root
DB_PASSWORD=<local-secret>
JWT_SECRET=<strong-random-secret>
SPRING_PROFILES_ACTIVE=dev
JPA_DDL_AUTO=validate
```

Tùy chọn:

```text
DB_URL=jdbc:mysql://localhost:3306/wedding_staff_management?...
SERVER_PORT=8080
JWT_EXPIRATION_MS=86400000

SEED_ENABLED=false
SEED_ADMIN_PASSWORD=
SEED_COORDINATOR_PASSWORD=
SEED_EMPLOYEE_PASSWORD=
```

AI local bằng Ollama:

```text
AI_ENABLED=true
AI_PROVIDER=OLLAMA
AI_BASE_URL=http://localhost:11434
AI_MODEL=qwen3:4b-instruct
AI_TIMEOUT_MS=60000
AI_CANDIDATE_LIMIT=5
AI_RECENT_EVALUATION_LIMIT=3
```

Test nhanh QR và kiểm tra AI thật bằng Ollama:

```text
ATTENDANCE_DEMO_ENABLED=true
AI_ENABLED=true
AI_PROVIDER=OLLAMA
AI_BASE_URL=http://localhost:11434
AI_MODEL=qwen3:4b-instruct
```

Khởi động lại backend sau khi đổi biến môi trường. QR demo chỉ bỏ kiểm tra cửa
sổ giờ; phân công, hạn phiên, QR/OTP, GPS và chống thao tác trùng vẫn được giữ.
Trang Thay thế nhân sự có nút **Test kết nối AI**, dùng Ollama thật với dữ liệu
mẫu nên không cần chuẩn bị yêu cầu thay ca. Luồng nghiệp vụ thật vẫn dùng
deterministic fallback nếu Ollama không khả dụng.

OpenAI là provider tùy chọn:

```text
AI_ENABLED=true
AI_PROVIDER=OPENAI
AI_BASE_URL=https://api.openai.com/v1
AI_API_KEY=<secret>
AI_MODEL=<configured-model>
```

Không commit `DB_PASSWORD`, `JWT_SECRET`, `AI_API_KEY` hoặc mật khẩu seed.

## 7. Chạy backend

```bash
cd backend
mvn spring-boot:run
```

Mặc định:

```text
http://localhost:8080
```

API base:

```text
http://localhost:8080/api
```

## 8. Chạy frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend mặc định gọi:

```text
http://localhost:8080/api
```

Có thể override:

```text
VITE_API_BASE_URL=http://localhost:8080/api
```

Production build:

```bash
npm run build
```

## 9. AI demo local

Cài Ollama và tải model trước khi demo:

```bash
ollama pull qwen3:4b-instruct
```

Nếu AI tắt, Ollama không chạy, provider timeout hoặc output không hợp lệ, workflow vẫn trả deterministic fallback.

## 10. Kiểm thử release

Backend:

```bash
cd backend
mvn clean test
```

Baseline release hiện tại: **94 tests, 0 failures, 0 errors**.

Frontend:

```bash
cd frontend
npm run build
```

Runtime security gate đã kiểm tra:

```text
No JWT              -> 401
Invalid JWT         -> 401
ADMIN manager API   -> 200
ADMIN employee-only -> 403
One-sided GPS       -> 400
Allowed CORS origin -> 200
Untrusted origin    -> 403
```

Chi tiết: `docs/dacn/14_TEST_REPORT_DACN_v1.0.md`.

## 11. Demo end-to-end

Luồng demo chính:

```text
Dashboard nhiều địa điểm
    -> phát hiện ca thiếu người
    -> employee gửi yêu cầu thay ca
    -> coordinator duyệt
    -> deterministic candidate ranking
    -> AI hỗ trợ rerank/giải thích
    -> coordinator mời ứng viên
    -> employee nhận thay
    -> phân khu vực/bàn
    -> QR/OTP check-in + check-out
    -> manager CONFIRM attendance
    -> reputation + payroll snapshot cập nhật
    -> dashboard/report phản ánh kết quả
```

Kịch bản chi tiết và dữ liệu cần chuẩn bị:
`docs/dacn/14_RELEASE_DEMO_DACN_v1.0.md`.

## 12. Tài liệu

- `docs/dacn/01_ARCHITECTURE_REVIEW_DACN_v1.0.md`
- `docs/dacn/02_ROADMAP_DACN_v1.0.md`
- `docs/dacn/03_REPUTATION_RULES_DACN_v1.0.md`
- `docs/dacn/04_CANCELLATION_REPLACEMENT_RULES_DACN_v1.0.md`
- `docs/dacn/05_DETERMINISTIC_CANDIDATE_SCORING_DACN_v1.0.md`
- `docs/dacn/06_HYBRID_AI_ASSISTED_RECOMMENDATION_DACN_v1.0.md`
- `docs/dacn/07_QR_ATTENDANCE_BACKEND_DACN_v1.0.md`
- `docs/dacn/08_QR_ATTENDANCE_FRONTEND_DACN_v1.0.md`
- `docs/dacn/09_ADVANCED_AREA_TABLE_ASSIGNMENT_DACN_v1.0.md`
- `docs/dacn/10_MULTI_RULE_PAYROLL_DACN_v1.0.md`
- `docs/dacn/11_ADVANCED_DASHBOARD_REPORTS_DACN_v1.0.md`
- `docs/dacn/12_UI_UX_POLISH_DACN_v1.0.md`
- `docs/dacn/13_INTEGRATION_SECURITY_REGRESSION_DACN_v1.0.md`
- `docs/dacn/14_DIAGRAMS_DACN_v1.0.md`
- `docs/dacn/14_TEST_REPORT_DACN_v1.0.md`
- `docs/dacn/14_RELEASE_DEMO_DACN_v1.0.md`
- `docs/dacn/14_RELEASE_CHECKLIST_DACN_v1.0.md`
- `docs/dacn/15_FINAL_ACCEPTANCE_DACN_v1.0.md`
- `docs/dacn/16_STRETCH_GOALS_DACN_v1.1.md`

Postman:
`docs/postman/Wedding_Staff_Management_DACN_v1.postman_collection.json`.

## 13. Giới hạn có chủ đích

- Thông báo dùng polling 10 giây, chưa dùng WebSocket/SSE.
- Không shift swap.
- Không floorplan/drag-drop 2D/3D.
- Không Maps routing.
- Excel đang xuất CSV UTF-8 tương thích Excel; PDF dùng bản in tối ưu của trình duyệt.
- Không Kubernetes/deployment platform.
- Không để AI tự động ra quyết định nhân sự.
- Chưa có browser E2E automation hoặc database-container integration suite; release dựa trên backend automated tests + production build + runtime/API/UI smoke.

## 14. Release

Trạng thái nghiệm thu cuối được tổng hợp tại:

`docs/dacn/15_FINAL_ACCEPTANCE_DACN_v1.0.md`

Tag `dacn-v1.0.0` là mốc release lịch sử và không được di chuyển.

Sau khi C10 final gate PASS:

```bash
git status --short
git log -1 --oneline
git tag dacn-v1.0.1
```

Chỉ tạo tag mới khi backend regression, frontend production build và repository hygiene đều PASS.
