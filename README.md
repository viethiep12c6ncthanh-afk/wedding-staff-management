# Wedding Staff Management System

Hệ thống quản lý ca làm và điều phối nhân sự phục vụ tiệc cưới, sự kiện đa địa điểm.

Repository này chứa phiên bản **Đồ án cơ sở (DACS)**. Các chức năng AI và các cơ chế nâng cao được dành cho giai đoạn **Đồ án chuyên ngành (DACN)**, không thuộc phạm vi bản DACS.

## Công nghệ

- Backend: Java 21, Spring Boot, Spring Security + JWT, Spring Data JPA, Maven
- Frontend: React, Vite, Axios, React Router
- Database: MySQL 8.4
- API: REST/JSON

## Chức năng DACS

- Đăng nhập JWT và phân quyền `ADMIN`, `COORDINATOR`, `EMPLOYEE`
- Quản lý nhân viên và tài khoản
- Quản lý địa điểm, sự kiện và ca làm
- Đăng ký ca; duyệt hoặc từ chối đăng ký
- Phân công từ đăng ký hoặc phân công trực tiếp
- Kiểm tra trùng lịch cơ bản
- Chấm công cơ bản: có mặt, đi trễ, về sớm, trễ và về sớm, vắng mặt
- Chốt tiền công cố định theo ca khi xác nhận chấm công
- Dashboard tổng quan
- Báo cáo tiền công theo khoảng ngày và theo nhân viên
- Xử lý lỗi API thống nhất và automated tests cho các nghiệp vụ backend cốt lõi

## Vai trò nghiệp vụ

- `ADMIN`: quản trị hệ thống
- `COORDINATOR`: điều phối nghiệp vụ
- `EMPLOYEE`: nhân viên đăng ký ca và xem dữ liệu cá nhân

`LEADER` không phải role tài khoản. Đây là `shiftRole` của nhân viên trong từng ca, cùng với `STAFF`.

## Chạy backend local

Backend cần các biến môi trường tối thiểu:

```text
DB_USERNAME
DB_PASSWORD
JWT_SECRET
SPRING_PROFILES_ACTIVE=dev
JPA_DDL_AUTO=update
JWT_EXPIRATION_MS=86400000
SEED_ENABLED=false
```

Sau đó chạy `WeddingStaffApplication` bằng IntelliJ hoặc Maven.

Backend mặc định chạy tại:

```text
http://localhost:8080
```

## Chạy frontend local

```bash
cd frontend
npm install
npm run dev
```

Frontend mặc định gọi API tại:

```text
http://localhost:8080/api
```

Có thể thay đổi bằng biến môi trường Vite:

```text
VITE_API_BASE_URL=http://localhost:8080/api
```

## Kiểm tra trước khi đóng bản DACS

Backend:

```bash
mvn clean test
```

Frontend:

```bash
npm run build
```

Chỉ tạo tag `dacs-v1.0.0` sau khi automated tests, production build và smoke test các luồng chính đều thành công.

## Phạm vi DACN dự kiến

Các chức năng sau được phát triển tiếp ở Đồ án chuyên ngành:

- Chấm công QR / OTP / GPS
- Điểm uy tín nhân viên
- Gợi ý nhân sự và thay thế tự động
- Điều phối khu vực/bàn nâng cao
- Tính công nhiều quy tắc
- Dashboard/báo cáo nâng cao
- AI hỗ trợ gợi ý và điều phối nhân sự
