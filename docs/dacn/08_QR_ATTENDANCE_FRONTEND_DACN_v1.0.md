# DACN Commit 8 — QR / OTP / GPS Attendance Frontend

## Mục tiêu

Commit 8 bổ sung UI cho backend Commit 7 mà không thay đổi quy tắc xác nhận Attendance hiện có.

### Manager (ADMIN / COORDINATOR)

- Chọn ca đang nằm trong cửa sổ CHECK_IN hoặc CHECK_OUT.
- Tạo session ngắn hạn 1–30 phút.
- Hiển thị QR chứa payload frontend `WSMATTEND|1|<ACTION>|<SHIFT_ID>|<RAW_TOKEN>`.
- Hiển thị OTP 6 chữ số dự phòng.
- Tùy chọn GPS: vị trí trung tâm + bán kính 20–1000 m.
- Có thể thu hồi session đang hiển thị.
- Tạo session mới cùng ca + cùng action sẽ làm session cũ mất hiệu lực theo backend Commit 7.

### Employee

- Quét QR bằng BarcodeDetector + camera khi trình duyệt hỗ trợ.
- Có fallback dán nội dung QR/token thủ công.
- Có fallback OTP 6 chữ số.
- QR payload frontend tự điền `shiftId`, `action` và `qrToken`; backend vẫn chỉ nhận DTO hiện có.
- GPS được lấy theo thao tác của người dùng và chỉ gửi trong request check-in/check-out hiện tại.
- Sau thành công, tải lại lịch sử Attendance từ endpoint `/attendances/mine`.

## Quy tắc bảo mật / riêng tư

- Không lưu raw QR token hoặc OTP vào localStorage/sessionStorage.
- Raw credential chỉ nằm trong React state của tab hiện tại.
- QR payload chỉ là lớp đóng gói frontend; backend vẫn xác minh token hash như Commit 7.
- GPS không chạy nền, không theo dõi liên tục.
- Frontend luôn gửi cặp latitude/longitude cùng nhau hoặc không gửi, tránh request vị trí nửa vời.
- Attendance `CONFIRMED` vẫn chỉ do ADMIN/COORDINATOR thực hiện qua flow cũ.

## Giới hạn API hiện tại

Backend Commit 7 chưa có endpoint liệt kê session đang active. Vì vậy sau khi refresh trang manager:

- frontend không thể khôi phục raw QR/OTP đã phát hành (cố ý, vì backend không lưu raw credential);
- manager tạo session mới nếu cần; backend tự revoke phiên cũ cùng shift/action;
- nút revoke trực tiếp chỉ áp dụng cho session đang còn trong React state.

## Dependency frontend

Cài package QR renderer:

```bash
npm install qrcode
```

Không thêm thư viện camera scanner. Scanner dùng Web Barcode Detection API khi trình duyệt hỗ trợ và luôn có manual fallback.

## Acceptance checklist

1. `npm run build` PASS.
2. ADMIN/COORDINATOR tạo CHECK_IN session, QR render và OTP hiện đúng 6 số.
3. Session có GPS hiển thị bán kính và center đã lấy từ trình duyệt.
4. Employee quét/paste QR, tự điền shift/action, check-in thành công.
5. GPS-required: không gửi GPS phải bị backend reject; lấy GPS đúng bán kính thì PASS.
6. Duplicate check-in bị reject và hiển thị message backend qua `getApiErrorMessage`.
7. Manager tạo CHECK_OUT session không GPS.
8. Employee dùng OTP check-out thành công.
9. Duplicate check-out bị reject.
10. Bảng lịch sử employee refresh và hiển thị DRAFT/result/timestamps.
11. Manager vẫn sửa/confirm Attendance DRAFT bằng flow cũ.
12. `git diff --check` không có whitespace error thực sự.
