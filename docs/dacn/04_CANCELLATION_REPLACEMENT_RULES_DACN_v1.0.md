# DACN Commit 4 — Cancellation & Replacement Staffing

## Mục tiêu

Bổ sung workflow xin rút khỏi phân công và tìm nhân sự thay thế mà không phá quy tắc DACS: nhân viên chỉ tự hủy đăng ký khi còn ít nhất 24 giờ trước ca. Yêu cầu thay ca là workflow riêng và không tự động hủy phân công khi nhân viên vừa gửi yêu cầu.

## Vòng đời yêu cầu

`PENDING -> OPEN -> FILLED`

Các nhánh kết thúc khác:

- `PENDING -> REJECTED`: quản lý từ chối, phân công gốc giữ nguyên.
- `PENDING/OPEN -> CANCELLED`: ca/sự kiện bị hủy trước khi tìm được người thay.

Khi quản lý duyệt `PENDING -> OPEN`, phân công gốc mới chuyển sang `CANCELLED`. Nếu phân công gốc sinh từ đăng ký đã duyệt thì đăng ký liên quan cũng chuyển sang `CANCELLED`.

## Lời mời thay ca

Một yêu cầu `OPEN` có thể có nhiều lời mời. Mỗi cặp `(request, employee)` chỉ được mời một lần.

Lời mời có trạng thái:

- `PENDING`
- `ACCEPTED`
- `DECLINED`
- `CANCELLED`

Khi một nhân viên chấp nhận, hệ thống kiểm tra lại ngay tại thời điểm nhận:

1. yêu cầu vẫn `OPEN`;
2. ca chưa bắt đầu và còn ở `OPEN/CLOSED`;
3. hồ sơ nhân viên còn `ACTIVE`;
4. không phải chính nhân viên gốc;
5. chưa có phân công hoạt động trong cùng ca;
6. không trùng lịch theo công thức DACS `newStart < existingEnd AND newEnd > existingStart`;
7. ca vẫn còn chỗ trống.

Nếu hợp lệ, tạo phân công mới với `assignmentSource = REPLACEMENT`, `registrationId = null`, đồng thời sao chép `shiftRole`, `area`, `task` từ phân công gốc. Yêu cầu chuyển `FILLED`; các lời mời `PENDING` khác bị đóng `CANCELLED`.

## Bảo toàn lịch sử

Không xóa vật lý phân công gốc. Chuỗi truy vết được giữ:

`original assignment CANCELLED -> replacement request -> invitation -> replacement assignment`

Điều này cho phép dashboard/recommendation/audit sau này giải thích ai xin rút, ai duyệt, ai được mời và ai nhận thay.

## Phân quyền

- `EMPLOYEE`: tạo yêu cầu cho phân công của chính mình; xem yêu cầu của mình; xem và phản hồi lời mời gửi cho mình.
- `ADMIN/COORDINATOR`: xem toàn bộ yêu cầu; duyệt/từ chối; mời nhân viên thay thế.
- `LEADER` vẫn chỉ là vai trò trong ca, không phải account role.

## Phạm vi chưa làm ở Commit 4

- Không xếp hạng ứng viên trong trang thay ca.
- Không gọi phần xếp hạng rule-based là AI.
- Candidate filtering/scoring thuộc Commit 5.
- LLM-assisted recommendation thuộc Commit 6.
- Không dùng GPS/Maps để ước lượng thời gian di chuyển.
