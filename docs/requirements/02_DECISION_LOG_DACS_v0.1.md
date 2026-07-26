# DECISION LOG — ĐỒ ÁN CƠ SỞ

**Phiên bản:** v0.1  
**Trạng thái:** Đề xuất chốt nội bộ, chờ GVHD xác nhận

| ID | Vấn đề | Quyết định đề xuất | Trạng thái |
|---|---|---|---|
| DEC-01 | Trưởng ca | Là `shiftRole = LEADER` trong từng ca, không phải role JWT. | Chờ GVHD |
| DEC-02 | Phân công trực tiếp | Có; tạo `ShiftAssignment` với `assignmentSource = DIRECT`, `registrationId = NULL`. | Chờ GVHD |
| DEC-03 | Hạn tự hủy | Trước giờ bắt đầu ca ít nhất 24 giờ. | Chờ GVHD |
| DEC-04 | Tính công | Cố định theo ca. | Chờ GVHD |
| DEC-05 | Đi trễ/về sớm | Điều chỉnh tiền thủ công và phải có lý do. | Chờ GVHD |
| DEC-06 | Chấm công | Trưởng ca ghi nhận; điều phối viên xác nhận. | Chờ GVHD |
| DEC-07 | Phân công từng bàn | Không; chỉ dùng vai trò, khu vực, mô tả nhiệm vụ. | Chờ GVHD |
| DEC-08 | Đăng ký tài khoản công khai | Không trong ĐACS; quản trị viên tạo tài khoản. | Chờ GVHD |
| DEC-09 | Hủy sự kiện | Ca, đăng ký, phân công chưa hoàn thành chuyển `CANCELLED` trong transaction. | Chờ GVHD |
| DEC-10 | Lịch sử trạng thái | Chưa bắt buộc trong lõi; lưu các mốc thời gian và người thao tác. | Chờ GVHD |
| DEC-11 | Trùng lịch | `newStart < existingEnd AND newEnd > existingStart`. | Chờ GVHD |
| DEC-12 | Hiệu năng | 95% CRUD thông thường không quá 2 giây trong môi trường thử nghiệm. | Chờ GVHD |

Sau khi trao đổi với GVHD, đổi trạng thái thành `Đã duyệt`, `Yêu cầu sửa` hoặc `Loại khỏi phạm vi`, rồi cập nhật lại SRS.
