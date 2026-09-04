# DACN Commit 11 — Advanced Dashboard / Reports v1.0

## Mục tiêu

Mở rộng Dashboard từ các bộ đếm toàn hệ thống thành màn hình phân tích vận hành theo khoảng ngày, nhưng không thay đổi các bất biến nghiệp vụ đã chốt ở các commit trước.

## Phạm vi

### 1. Tổng quan hệ thống

Giữ nguyên hai endpoint cũ:

- `GET /api/dashboard/summary`
- `GET /api/dashboard/attendance-summary`

Các endpoint này tiếp tục phục vụ tương thích ngược và các số liệu tổng thể.

### 2. Operations analytics

Endpoint:

- `GET /api/dashboard/operations?from=YYYY-MM-DD&to=YYYY-MM-DD&venueId=...`

Thống kê:

- tổng ca;
- ca thiếu người;
- ca đủ người;
- ca dư người;
- tổng số vị trí còn thiếu;
- yêu cầu thay thế theo trạng thái;
- tỷ lệ lấp đầy.

Quy tắc staffing giữ nguyên với điều phối:

- ca hiển thị: `DRAFT`, `OPEN`, `CLOSED`, `IN_PROGRESS`, `COMPLETED`;
- nhân sự hiệu lực: assignment `ASSIGNED`, `CONFIRMED`, `COMPLETED`;
- `ABSENT` không được tính là nhân sự hiệu lực.

Tỷ lệ lấp đầy replacement:

`FILLED / (FILLED + REJECTED + CANCELLED) * 100`

`PENDING` và `OPEN` chưa kết thúc nên không nằm trong mẫu số.

### 3. Workforce analytics

Endpoint:

- `GET /api/dashboard/workforce?from=YYYY-MM-DD&to=YYYY-MM-DD`

Chỉ dùng attendance `CONFIRMED`.

Thống kê:

- `PRESENT`;
- `LATE`;
- `EARLY_LEAVE`;
- `LATE_AND_EARLY_LEAVE`;
- `ABSENT`;
- tỷ lệ đi trễ;
- tỷ lệ vắng;
- xu hướng theo ngày;
- top workload;
- phân bố reputation hiện tại.

Tỷ lệ đi trễ tính cả:

- `LATE`;
- `LATE_AND_EARLY_LEAVE`.

Workload trong khoảng dùng số ca attendance đã xác nhận; ca `ABSENT` không được tính là ca có trả công.

Phân bố reputation là snapshot hiện tại:

- `90–100`: Xuất sắc;
- `80–89`: Tốt;
- `60–79`: Cần theo dõi;
- `0–59`: Rủi ro.

### 4. AI analytics

Endpoint:

- `GET /api/dashboard/ai?from=YYYY-MM-DD&to=YYYY-MM-DD`

Nguồn dữ liệu duy nhất là `ai_recommendation_runs`.

Thống kê:

- tổng run;
- `AI_ASSISTED`;
- fallback;
- fallback rate;
- provider/model usage;
- fallback reasons.

Một lần chạy deterministic candidate list thông thường không được tính là AI run nếu không có record trong `ai_recommendation_runs`.

Các query analytics dùng aggregate projection, không load `LONGTEXT` snapshot của run.

### 5. Payroll

Dashboard không tạo payroll engine mới.

Frontend tái sử dụng:

- `GET /api/reports/payroll`

và hiển thị snapshot đã chốt ở Commit 10:

- base pay;
- leader allowance;
- overtime;
- deductions;
- payable.

Không recompute payroll lịch sử.

## Bộ lọc

Advanced analytics yêu cầu:

- `from`;
- `to`.

Khoảng thời gian có semantics:

`[from 00:00, to + 1 day 00:00)`

`venueId` chỉ áp dụng cho operations/replacement.

Frontend mặc định 30 ngày gần nhất.

## Phân quyền

Các endpoint `/api/dashboard/**` tiếp tục chỉ cho:

- `ADMIN`;
- `COORDINATOR`.

Không thay đổi quyền của employee.

## Frontend

Không thêm chart dependency.

Biểu đồ đơn giản dùng CSS bar/progress để:

- giảm dependency;
- dễ demo;
- giữ build nhẹ;
- phù hợp scope đồ án.

Dashboard gồm:

1. system overview;
2. operations;
3. replacement;
4. attendance;
5. attendance trend;
6. workload;
7. reputation distribution;
8. AI analytics;
9. payroll summary.

## Không thuộc phạm vi Commit 11

- realtime WebSocket dashboard;
- BI warehouse;
- export Excel/PDF;
- floorplan/map;
- AI tự ra quyết định;
- payroll recomputation;
- thay đổi chính sách reputation;
- thay đổi logic replacement;
- migration schema mới.
