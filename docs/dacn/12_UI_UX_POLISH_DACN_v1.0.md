# Commit 12 — UI/UX Polish DACN v1.0

## Mục tiêu

Commit 12 chỉ cải thiện trải nghiệm người dùng của frontend hiện có. Không thay đổi nghiệp vụ backend, database, công thức payroll, quy tắc chấm công, replacement, recommendation hay phân quyền.

## Phạm vi

### 1. Responsive application shell

- Sidebar desktop giữ dạng cố định.
- Màn hình <= 900px chuyển sidebar thành drawer.
- Topbar có nút mở menu trên màn hình nhỏ.
- Có overlay và nút đóng drawer.
- Menu quản lý được chia nhóm: Tổng quan, Nhân sự, Vận hành, Báo cáo.
- Menu nhân viên được chia nhóm: Công việc, Cá nhân.

### 2. Modal accessibility

Modal dùng chung được bổ sung:

- `aria-labelledby` liên kết tiêu đề dialog.
- focus vào dialog khi mở.
- trả focus về phần tử trước đó khi đóng.
- đóng bằng phím Escape.
- khóa scroll của `body` khi modal mở.
- backdrop chỉ đóng modal khi click trực tiếp lên backdrop.

### 3. Action dialog dùng chung

Thêm `ActionDialog` để thay browser `window.prompt()` / `window.confirm()` cho các hành động nghiệp vụ quan trọng:

- hủy phân công;
- hủy sự kiện;
- hủy đăng ký ca;
- hủy ca làm;
- ngừng hoạt động địa điểm;
- xác nhận chấm công và chốt payroll snapshot;
- duyệt/từ chối replacement request;
- nhận/từ chối replacement invitation;
- thu hồi phiên QR/OTP.

Các thao tác yêu cầu lý do tiếp tục bắt buộc lý do ở UI; backend vẫn là nguồn kiểm tra cuối cùng.

### 4. Area/Table editing

Loại bỏ chuỗi browser prompt khi sửa khu vực và bàn. Thay bằng modal form có:

- validation HTML cơ bản;
- trạng thái saving;
- nút hủy/cập nhật thống nhất;
- giữ nguyên API và cấu trúc dữ liệu Commit 9.

### 5. Shared visual polish

- focus-visible thống nhất cho keyboard navigation;
- disabled state thống nhất;
- mobile modal dạng bottom-aligned card;
- toolbar co về một cột trên mobile;
- spacing/content width tốt hơn;
- không thêm UI framework hoặc chart dependency.

## Ngoài phạm vi

- Không thêm nghiệp vụ mới.
- Không migration database.
- Không sửa API contract.
- Không thay đổi authentication/authorization.
- Không thay đổi AI recommendation logic.
- Không thay đổi QR/OTP/GPS rule.
- Không thay đổi payroll calculation.
- Không thay đổi reputation scoring.

## Kiểm thử đề xuất

1. `npm run build`.
2. Desktop smoke: sidebar, modal CRUD, dashboard, report, attendance, replacement, assignment placement.
3. Mobile responsive smoke ở khoảng 390–430px: mở/đóng sidebar, scroll table, modal, form.
4. Keyboard smoke: Tab, Shift+Tab cơ bản và Escape để đóng modal.
5. Kiểm tra không còn `window.prompt` hoặc `window.confirm` trong `frontend/src`.
6. Full backend regression không bắt buộc cho thay đổi frontend-only, nhưng Commit 13 sẽ chạy toàn bộ integration/security regression.
