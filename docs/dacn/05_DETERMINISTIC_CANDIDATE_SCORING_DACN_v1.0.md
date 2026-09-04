# DACN Commit 5 - Deterministic Candidate Filtering & Scoring v1.0

## Mục tiêu

Bổ sung danh sách gợi ý người thay ca cho yêu cầu thay thế nhân sự đang ở trạng thái `OPEN`. Đây là bước **rule-based/deterministic**, chưa sử dụng LLM hay mô hình AI.

Luồng quyết định giữ nguyên nguyên tắc:

1. Hệ thống loại ứng viên không hợp lệ bằng hard constraints.
2. Hệ thống chấm điểm và xếp hạng các ứng viên còn lại.
3. ADMIN/COORDINATOR là người quyết định gửi lời mời.
4. Khi gửi lời mời và khi nhân viên nhận lời, backend Commit 4 vẫn kiểm tra lại điều kiện nghiệp vụ để chống dữ liệu đã thay đổi.

## API

`GET /api/replacements/requests/{requestId}/candidates`

Quyền: `ADMIN`, `COORDINATOR`.

Chỉ hoạt động khi:

- replacement request ở trạng thái `OPEN`;
- ca chưa bắt đầu;
- shift ở trạng thái `OPEN` hoặc `CLOSED`.

## Hard constraints

Ứng viên chỉ được đưa vào bảng xếp hạng khi đồng thời thỏa:

- `employmentStatus = ACTIVE`;
- `accountStatus = ACTIVE`;
- role tài khoản = `EMPLOYEE`;
- không phải nhân viên gốc đang xin thay ca;
- chưa từng được gửi invitation cho replacement request này;
- không có assignment `ASSIGNED`/`CONFIRMED` trùng thời gian với ca cần thay.

Điều kiện trùng lịch giữ đúng quy tắc dự án:

`candidateShift.startAt < targetEnd AND candidateShift.endAt > targetStart`

Hai ca liền kề không bị xem là overlap.

## Công thức điểm 100

### 1. Uy tín hiện tại - tối đa 50 điểm

`reputationPoints = round(currentScore * 0.50)`

Nếu hồ sơ aggregate uy tín bị thiếu ngoài dự kiến, fallback là baseline 80/100.

### 2. Độ ổn định chấm công - tối đa 25 điểm

Dữ liệu lấy từ aggregate reputation:

- `completedShiftCount`
- `lateCount`
- `earlyLeaveCount`
- `absentCount`

Nếu chưa có lịch sử chấm công: dùng mức trung tính `80/100`.

Nếu đã có dữ liệu:

`observed = completedShiftCount + absentCount`

`quality = completedShiftCount * 100 - lateCount * 25 - earlyLeaveCount * 25`

`reliabilityPercent = clamp(round(quality / observed), 0, 100)`

`reliabilityPoints = round(reliabilityPercent * 0.25)`

Vắng mặt không nằm trong `completedShiftCount`, nên tự làm giảm mạnh tỷ lệ khi tăng mẫu số mà không tạo quality. Đi trễ/về sớm giảm thêm chất lượng của các ca đã hoàn thành.

### 3. Kinh nghiệm hoàn thành ca - tối đa 15 điểm

Dữ liệu kinh nghiệm lấy trực tiếp từ `ShiftAssignment` có `status = COMPLETED`, tách biệt với aggregate chấm công dùng cho reliability. Điều này tránh trường hợp dữ liệu lịch sử có assignment đã hoàn thành nhưng chưa có attendance aggregate tương ứng.

`experiencePoints = min(completedShiftCount, 15)`

Mỗi assignment đã hoàn thành được 1 điểm, tối đa 15.

### 4. Kinh nghiệm đúng vai trò - tối đa 10 điểm

Đếm assignment `COMPLETED` có cùng `ShiftRole` với phân công gốc.

`sameRolePoints = min(sameRoleCompletedCount * 2, 10)`

Mỗi ca đúng vai trò được 2 điểm, tối đa 10.

### Tổng

`totalScore = reputationPoints + reliabilityPoints + experiencePoints + sameRolePoints`

Điểm tối đa = 100.

## Tie-break deterministic

Nếu hai ứng viên bằng `totalScore`, thứ tự được quyết định cố định:

1. `totalScore DESC`
2. `reputationScore DESC`
3. `completedShiftCount DESC`
4. `employeeCode ASC`
5. `employeeId ASC`

Không sử dụng random nên cùng một snapshot dữ liệu luôn cho cùng thứ tự.

## Response giải thích được

Mỗi candidate trả:

- rank;
- employee id/code/name;
- total score;
- reputation score + contribution;
- reliability percent + contribution;
- completed shift count + contribution;
- same-role completed count + contribution;
- danh sách lý do/giải thích điểm.

Frontend hiển thị các thành phần này để coordinator hiểu vì sao một ứng viên đứng trên ứng viên khác.

## Không phải AI

Commit 5 chỉ là deterministic filtering + weighted scoring. Không gọi LLM và không được mô tả là AI trong báo cáo.

Commit 6 sẽ đặt AI phía sau lớp hard constraints và deterministic ranking, với nguyên tắc fallback về kết quả Commit 5 nếu AI lỗi hoặc không khả dụng.
