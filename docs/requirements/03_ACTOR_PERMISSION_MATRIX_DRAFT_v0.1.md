# MA TRẬN ACTOR VÀ QUYỀN — ĐỒ ÁN CƠ SỞ

**Phiên bản:** Draft v0.1

Ký hiệu:

- `✓`: được phép;
- `—`: không được phép;
- `Theo ca`: chỉ được phép trong ca được gán `LEADER`.

| Chức năng | ADMIN | COORDINATOR | EMPLOYEE | LEADER theo ca |
|---|:---:|:---:|:---:|:---:|
| Đăng nhập / đăng xuất | ✓ | ✓ | ✓ | ✓ |
| Xem hồ sơ cá nhân | ✓ | ✓ | ✓ | ✓ |
| Quản lý tài khoản | ✓ | — | — | — |
| Gán role hệ thống | ✓ | — | — | — |
| Quản lý hồ sơ nhân viên | ✓ | ✓ | — | — |
| Quản lý địa điểm | ✓ | ✓ | — | — |
| Quản lý sự kiện | ✓ | ✓ | — | — |
| Quản lý ca làm | ✓ | ✓ | — | — |
| Mở / đóng đăng ký | ✓ | ✓ | — | — |
| Xem ca đang mở | ✓ | ✓ | ✓ | ✓ |
| Đăng ký ca | — | — | ✓ | ✓ |
| Tự hủy đăng ký đúng hạn | — | — | ✓ | ✓ |
| Duyệt / từ chối đăng ký | — | ✓ | — | — |
| Phân công trực tiếp | — | ✓ | — | — |
| Gán `LEADER` / `STAFF` | — | ✓ | — | — |
| Ghi nhận chấm công | — | ✓ | — | Theo ca |
| Chỉnh sửa chấm công nháp | — | ✓ | — | Theo ca |
| Xác nhận chấm công | — | ✓ | — | — |
| Tính / điều chỉnh tiền công | — | ✓ | — | — |
| Đánh giá sau ca | — | ✓ | — | Theo ca |
| Xem tiền công cá nhân | — | — | ✓ | ✓ |
| Xem thống kê tổng quan | ✓ | ✓ | — | — |

`LEADER` không phải role JWT. Khi gọi API chấm công hoặc đánh giá, backend phải kiểm tra người dùng là nhân viên, có phân công trong đúng ca và phân công đó có `shiftRole = LEADER`.
