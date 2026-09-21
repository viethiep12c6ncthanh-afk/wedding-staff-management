# QR + AI Quick Demo v1.1

## Cấu hình

```text
ATTENDANCE_DEMO_ENABLED=true
AI_ENABLED=true
AI_PROVIDER=OLLAMA
AI_BASE_URL=http://localhost:11434
AI_MODEL=qwen3:4b-instruct
```

Khởi động lại backend. Đây là cấu hình dành cho demo/dev, không dùng production.

## Test QR không cần chờ giờ

1. Chuẩn bị ca `OPEN`, `CLOSED` hoặc `IN_PROGRESS` có phân công đang hoạt động.
2. ADMIN/COORDINATOR mở Chấm công và kiểm tra banner **Chế độ demo nhanh**.
3. Chọn ca, chọn `CHECK_IN`, tạo phiên 10 phút rồi sao chép QR hoặc OTP.
4. EMPLOYEE được phân công dán QR/OTP và check-in.
5. Gửi lại lần nữa để demo chống thao tác trùng.
6. ADMIN tạo phiên `CHECK_OUT`; EMPLOYEE check-out ngay, không chờ cuối ca.
7. Có thể tạo phiên GPS để demo kiểm tra trong/ngoài bán kính.

Demo mode chỉ bỏ điều kiện giờ ca. Token, OTP băm, hạn phiên, phân công, GPS,
ownership, action và duplicate protection vẫn được kiểm tra.

## Test AI thật bằng Ollama và dữ liệu MySQL thật

1. ADMIN/COORDINATOR mở **Thay thế nhân sự**.
2. Đảm bảo Ollama đang chạy và đã có model `qwen3:4b-instruct`.
3. Ở thẻ **Kiểm tra AI bằng dữ liệu nhân viên thật**, bấm
   **Phân tích dữ liệu thật**.
4. Backend lấy nhân viên `ACTIVE` từ MySQL, tính điểm từ uy tín, độ ổn định và
   lịch sử hoàn thành ca rồi gửi đúng các ID đó tới Ollama. Thao tác không tạo
   yêu cầu thay ca hoặc phân công mới.
5. Khi muốn test luồng theo ca cụ thể, mở một yêu cầu thay ca `OPEN` có ứng viên và bấm
   **Phân tích bằng AI** trong chính yêu cầu đó.

Lệnh chuẩn bị Ollama:

```bash
ollama pull qwen3:4b-instruct
ollama serve
```

Không còn provider AI giả lập. Nếu Ollama lỗi trong luồng nghiệp vụ thật, hệ
thống trả deterministic fallback và không tự ý phân công nhân viên.
