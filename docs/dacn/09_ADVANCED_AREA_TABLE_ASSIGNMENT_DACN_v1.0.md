# DACN Commit 9 — Advanced Area / Table Assignment

## 1. Mục tiêu

Commit này thay khu vực dạng chuỗi tự do trên `shift_assignments` bằng mô hình có cấu trúc:

```text
WorkShift
  -> ShiftArea
      -> ShiftTable

ShiftAssignment
  -> tối đa 1 ShiftArea
  -> 0..N ShiftTable
```

Phạm vi không bao gồm floor plan, drag/drop sơ đồ bàn, bản đồ hoặc bố trí 2D/3D.

## 2. Quy tắc nghiệp vụ

1. Khu vực thuộc đúng một ca.
2. Bàn thuộc đúng một khu vực.
3. Một phân công chỉ thuộc tối đa một khu vực.
4. Một phân công có thể phụ trách nhiều bàn.
5. Các bàn được chọn phải thuộc đúng khu vực của phân công.
6. Chỉ khu vực/bàn `ACTIVE` mới được gán mới.
7. Không được ngừng khu vực/bàn nếu còn phân công `ASSIGNED` hoặc `CONFIRMED` đang sử dụng.
8. `requiredStaff` của khu vực là tùy chọn để hỗ trợ capacity; dữ liệu legacy được migrate với giá trị `NULL`.
9. Nếu khu vực có `requiredStaff`, số phân công hoạt động trong khu vực không được vượt giới hạn.
10. Tổng các `requiredStaff` đã cấu hình của khu vực `ACTIVE` không được vượt `WorkShift.requiredStaff`.
11. Ca `COMPLETED` hoặc `CANCELLED` không được chỉnh cấu trúc/placement.
12. Flow replacement giữ nguyên vai trò, khu vực, bàn và nhiệm vụ của phân công gốc.
13. Assignment từ đăng ký hoặc direct assignment có thể được tạo trước rồi phân khu vực/bàn sau.
14. Employee chỉ xem phân công của chính mình qua `/api/assignments/mine`.

## 3. Migration

Script mới:

```text
database/migrations/V011__advanced_area_table_assignment.sql
```

Migration:

- tạo `shift_areas`;
- tạo `shift_tables`;
- thêm `shift_area_id` vào `shift_assignments`;
- backfill các giá trị `area` cũ thành `shift_areas`;
- tạo `shift_assignment_tables`;
- xóa cột chuỗi legacy `shift_assignments.area`.

Migration vẫn là script versioned chạy thủ công, không thay đổi các migration cũ.

## 4. API

### Area / table

```text
GET   /api/placements/areas?shiftId={shiftId}
POST  /api/placements/areas
PUT   /api/placements/areas/{id}
PATCH /api/placements/areas/{id}/status

POST  /api/placements/tables
PUT   /api/placements/tables/{id}
PATCH /api/placements/tables/{id}/status
```

Các endpoint trên: `ADMIN`, `COORDINATOR`.

### Assignment placement

```text
PATCH /api/assignments/{id}/placement
```

Ví dụ:

```json
{
  "areaId": 5,
  "tableIds": [12, 13, 14],
  "task": "Phục vụ bàn và hỗ trợ khách khu A"
}
```

`areaId = null` cùng `tableIds = []` dùng để bỏ phân khu vực/bàn.

### Employee view

```text
GET /api/assignments/mine
```

Chỉ `EMPLOYEE`.

## 5. Tương thích workflow cũ

- Direct assignment vẫn kiểm tra ACTIVE, capacity toàn ca và overlap như trước.
- Registration approval vẫn tạo assignment nhưng không nhập khu vực dạng text.
- Replacement assignment kế thừa structured placement của assignment gốc.
- AI context lấy tên khu vực và mã bàn từ structured placement.
- Attendance và payroll không thay đổi trong commit này.

## 6. Frontend

Manager `/assignments`:

- cấu hình khu vực theo ca;
- cấu hình bàn theo khu vực;
- bật/ngừng khu vực và bàn;
- phân một nhân viên vào một khu vực và nhiều bàn;
- xem khu vực/bàn ngay trong bảng assignment.

Employee:

```text
/my-assignments
```

hiển thị vai trò, khu vực, danh sách bàn, nhiệm vụ và trạng thái.

## 7. Acceptance checklist

Trước khi commit:

1. Apply V011 một lần trên database dev.
2. `DESCRIBE shift_areas`.
3. `DESCRIBE shift_tables`.
4. `DESCRIBE shift_assignments` xác nhận có `shift_area_id` và không còn `area`.
5. `DESCRIBE shift_assignment_tables`.
6. `mvn clean test` backend.
7. Runtime API:
   - create area;
   - create table;
   - placement nhiều bàn;
   - reject table thuộc area khác;
   - reject area capacity full;
   - employee `/assignments/mine`.
8. Replacement smoke: replacement kế thừa area/tables.
9. `npm run build`.
10. UI smoke manager + employee.
11. `git diff --check`.
12. Chỉ commit khi toàn bộ acceptance chính PASS.
