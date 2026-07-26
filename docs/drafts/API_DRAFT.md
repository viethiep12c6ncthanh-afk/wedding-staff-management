# API baseline v0.1

| Method | Endpoint | Vai trò | Mục đích |
|---|---|---|---|
| POST | `/api/auth/login` | Công khai | Đăng nhập, nhận JWT |
| GET | `/api/venues` | Đã đăng nhập | Danh sách địa điểm |
| POST | `/api/venues` | ADMIN, COORDINATOR | Tạo địa điểm |
| PUT | `/api/venues/{id}` | ADMIN, COORDINATOR | Sửa địa điểm |
| DELETE | `/api/venues/{id}` | ADMIN | Xóa địa điểm |
| GET | `/api/events` | Đã đăng nhập | Danh sách sự kiện |
| POST | `/api/events` | ADMIN, COORDINATOR | Tạo sự kiện |
| PUT | `/api/events/{id}` | ADMIN, COORDINATOR | Sửa sự kiện |
| GET | `/api/shifts` | Đã đăng nhập | Danh sách ca |
| POST | `/api/shifts` | ADMIN, COORDINATOR | Tạo ca |
| PUT | `/api/shifts/{id}` | ADMIN, COORDINATOR | Sửa/mở đăng ký ca |
| POST | `/api/registrations` | EMPLOYEE | Đăng ký ca |
| GET | `/api/registrations` | ADMIN, COORDINATOR | Danh sách đăng ký |
| PUT | `/api/registrations/{id}/review` | ADMIN, COORDINATOR | Duyệt/từ chối và tạo phân công |
| GET | `/api/employees` | ADMIN, COORDINATOR | Danh sách nhân viên |

## Đăng nhập mẫu

```json
{
  "username": "admin",
  "password": "Admin@123"
}
```

## Tạo địa điểm

```json
{
  "name": "Sảnh tiệc Riverside",
  "address": "TP. Hồ Chí Minh",
  "contactPhone": "0900000000",
  "status": "ACTIVE"
}
```
