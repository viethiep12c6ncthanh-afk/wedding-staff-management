# Commit 14 — Final Release & Demo DACN v1.0

## 1. Mục tiêu

Đóng gói trạng thái DACN thành bản có thể chạy lại, kiểm thử, trình bày và demo mà không thêm nghiệp vụ mới.

Baseline trước Commit 14:

```text
branch: develop/dacn
HEAD: c815a21
checkpoint-after-dacn-integration-security
checkpoint-before-dacn-release
```

Release tag mục tiêu sau final gate:

```text
dacn-v1.0.0
```

## 2. Feature matrix cuối

| Nhóm | Trạng thái |
|---|---|
| Authentication / JWT / roles | DONE |
| Multi-shift / multi-venue coordination | DONE |
| Reputation / evaluation | DONE |
| Cancellation / replacement | DONE |
| Deterministic candidate recommendation | DONE |
| Hybrid AI recommendation + fallback | DONE |
| Ollama local provider | DONE |
| QR / OTP / GPS attendance | DONE |
| Area / table placement | DONE |
| Multi-rule payroll | DONE |
| Advanced dashboard / reports | DONE |
| Responsive UI / dialogs | DONE |
| Security / validation hardening | DONE |

## 3. Dữ liệu tối thiểu để demo

Không commit database dump hoặc mật khẩu thật. Chuẩn bị dữ liệu qua UI/API trên database local:

1. Ít nhất 1 venue `ACTIVE`.
2. 1 event `CONFIRMED`.
3. Ít nhất 2–3 shift trong event, trong đó có 1 shift thiếu nhân sự.
4. Ít nhất 3 employee:
   - employment `ACTIVE`;
   - account `ACTIVE`;
   - role `EMPLOYEE`.
5. Ít nhất 1 employee có lịch sử attendance/reputation để deterministic score có dữ liệu giải thích.
6. Shift demo replacement phải ở `OPEN` hoặc `CLOSED` và chưa bắt đầu.
7. Area/Table có thể tạo trước hoặc trong lúc demo.
8. Chuẩn bị admin/coordinator account và employee account trên máy demo; mật khẩu chỉ giữ local.

## 4. Kịch bản demo 10–15 phút

### Bước A — Dashboard vận hành

Đăng nhập `ADMIN` hoặc `COORDINATOR`.

Mở Dashboard và chỉ ra:

- số ca;
- ca thiếu/đủ/dư người;
- replacement statistics;
- attendance trend;
- workload;
- reputation distribution;
- AI analytics;
- payroll snapshot summary.

Điểm cần nói: dashboard tái sử dụng dữ liệu nghiệp vụ đã xác nhận, không tự tính lại payroll lịch sử.

### Bước B — Điều phối nhiều ca / nhiều venue

Mở trang điều phối:

- lọc khoảng ngày;
- lọc venue;
- chọn ca thiếu người;
- chỉ ra required staff và active assignment;
- nếu có transition warning thì giải thích warning không thay thế hard overlap rule.

### Bước C — Replacement

Đăng nhập employee có assignment hợp lệ:

1. Gửi yêu cầu thay ca.
2. Phân công gốc vẫn giữ trạng thái cho tới khi coordinator duyệt request.

Quay lại manager:

3. Duyệt request.
4. Mở danh sách candidate deterministic.
5. Giải thích hard constraints:
   - employment ACTIVE;
   - account ACTIVE;
   - role EMPLOYEE;
   - không trùng lịch;
   - không phải nhân viên gốc.

### Bước D — AI-assisted recommendation

Bật Ollama nếu dùng AI thật:

```text
AI_ENABLED=true
AI_PROVIDER=OLLAMA
AI_BASE_URL=http://localhost:11434
AI_MODEL=qwen3:4b-instruct
```

Chạy AI recommendation.

Giải thích:

```text
hard constraints
-> deterministic ranking
-> LLM chỉ rerank/explain candidate hợp lệ
-> backend validate AI output
-> coordinator quyết định
```

Nếu Ollama lỗi/tắt, cố ý demo deterministic fallback: workflow vẫn hoạt động và không tự tạo assignment.

### Bước E — Nhận thay + Area/Table

Manager gửi invitation cho candidate.

Đăng nhập candidate:

- nhận lời mời;
- request chuyển `FILLED`;
- replacement assignment được tạo với source `REPLACEMENT`.

Manager gán:

- area;
- một hoặc nhiều table;
- task.

Chỉ ra rằng replacement giữ được role/placement/task từ assignment gốc khi workflow yêu cầu.

### Bước F — QR / OTP / GPS attendance

Manager tạo `CHECK_IN` session:

- QR token ngắn hạn;
- OTP fallback;
- GPS tùy chọn.

Employee:

- quét/paste QR hoặc dùng OTP;
- check-in;
- sau đó check-out với CHECK_OUT session.

Điểm cần nói:

- raw QR token không lưu DB;
- OTP lưu BCrypt hash;
- GPS không tracking nền;
- self-attendance chỉ tạo/cập nhật `DRAFT`;
- employee không tự confirm payroll.

### Bước G — Confirm attendance

Manager mở attendance DRAFT và `CONFIRM`.

Sau confirm:

- attendance immutable;
- assignment `COMPLETED` hoặc `ABSENT`;
- reputation update;
- payroll snapshot được ghi.

### Bước H — Payroll và dashboard sau workflow

Mở report:

- base;
- leader allowance;
- overtime;
- late deduction;
- early-leave deduction;
- payable.

Quay lại Dashboard để cho thấy dữ liệu vận hành/workforce/payroll thay đổi theo snapshot mới.

## 5. Công thức payroll cần nhớ khi bảo vệ

```text
scheduledMinutes = shift.endAt - shift.startAt
overtimeMinutes  = max(0, checkOutAt - shift.endAt)

leaderAllowance     = LEADER ? base * 10% : 0
lateDeduction       = base * lateMinutes / scheduledMinutes
earlyLeaveDeduction = base * earlyLeaveMinutes / scheduledMinutes
overtimePay         = base * overtimeMinutes / scheduledMinutes * 1.5

payable = max(
  0,
  base + leaderAllowance + overtimePay
       - lateDeduction - earlyLeaveDeduction
)
```

`ABSENT` có payable = 0.

## 6. Checklist demo trước khi vào phòng

- Backend chạy.
- Frontend chạy.
- MySQL chạy.
- Database đã apply migration đúng một lần.
- `JPA_DDL_AUTO=validate` cho final verification.
- Admin/coordinator/employee login được.
- Browser camera/location permission đã kiểm tra nếu demo QR/GPS.
- Ollama đã tải model nếu muốn demo AI thật.
- Có sẵn một ca chưa bắt đầu cho replacement.
- Có ít nhất hai candidate hợp lệ.
- Không dùng API key hoặc password thật trên slide/screen chia sẻ.

## 7. Fallback khi demo

### AI không chạy

Không sửa dữ liệu. Chuyển sang deterministic fallback và giải thích đây là behavior thiết kế.

### Camera không hỗ trợ

Dùng paste QR token/payload hoặc OTP fallback.

### GPS không cấp quyền

Dùng session không GPS để demo QR/OTP; sau đó giải thích GPS là policy tùy chọn theo session.

### Dữ liệu ca đã bắt đầu

Tạo/chọn một future shift khác; không sửa timestamp lịch sử đã xác nhận.

## 8. Không làm trong demo

- Không chạy lại V011/V012 trên database đã migrate.
- Không restore backup trừ khi thật sự cần recovery.
- Không xóa confirmed attendance để “làm lại”.
- Không dùng AI để tự assign candidate.
- Không commit `.env`, DB password, JWT secret hoặc AI key.
