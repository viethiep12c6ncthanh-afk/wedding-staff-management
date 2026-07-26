# ĐẶC TẢ YÊU CẦU ĐỒ ÁN CƠ SỞ — BẢN NHÁP v0.1

## 1. Thông tin đề tài

**Tên đề tài:** Xây dựng hệ thống quản lý ca làm và điều phối nhân sự phục vụ tiệc cưới, sự kiện đa địa điểm tích hợp AI.

**Phạm vi tài liệu:** Đồ án cơ sở.

**Lưu ý:** AI, chấm công QR, điểm uy tín, tìm người thay thế tự động và điều phối nâng cao thuộc Đồ án chuyên ngành; không đưa vào phạm vi triển khai của Đồ án cơ sở.

---

## 2. Mô tả bài toán

Các đơn vị tổ chức tiệc cưới, sự kiện thường sử dụng lực lượng nhân viên phục vụ theo ca. Việc quản lý bằng tin nhắn hoặc bảng tính dễ phát sinh các vấn đề:

- khó theo dõi nhân viên nào đã đăng ký và được duyệt;
- dễ phân công trùng giờ;
- khó kiểm soát số người còn thiếu hoặc đã đủ;
- khó quản lý việc đi làm, đi trễ, về sớm hoặc vắng mặt;
- khó tổng hợp số ca và tiền công của từng nhân viên;
- dữ liệu địa điểm, sự kiện, ca làm và nhân sự bị phân tán.

Hệ thống được xây dựng nhằm quản lý tập trung tài khoản, nhân viên, địa điểm, sự kiện, ca làm, đăng ký ca, duyệt đăng ký, phân công, chấm công, tính tiền công cơ bản và thống kê.

---

## 3. Đối tượng sử dụng dự kiến

### 3.1 Quản trị viên

- quản lý tài khoản;
- phân quyền;
- khóa hoặc mở khóa tài khoản;
- quản lý dữ liệu nền;
- xem thống kê tổng quan.

### 3.2 Điều phối viên

- quản lý hồ sơ nhân viên;
- quản lý địa điểm;
- quản lý sự kiện;
- quản lý ca làm;
- mở hoặc đóng đăng ký;
- duyệt hoặc từ chối đăng ký;
- phân công nhân viên;
- ghi nhận hoặc xác nhận chấm công;
- tính tiền công cơ bản;
- đánh giá nhân viên sau ca;
- xem thống kê.

### 3.3 Nhân viên phục vụ

- đăng nhập;
- xem thông tin cá nhân;
- xem danh sách ca đang mở;
- xem chi tiết ca;
- đăng ký ca;
- hủy đăng ký theo điều kiện;
- xem kết quả duyệt;
- xem lịch sử tham gia và tiền công.

### 3.4 Trưởng ca

**Chưa chốt là role đăng nhập riêng hay chỉ là vai trò được gán trong từng ca.**

Phương án đề xuất: trưởng ca vẫn là nhân viên, được gán vai trò `TRƯỞNG_CA` trong một ca cụ thể. Khi đó không cần thêm role hệ thống riêng.

---

## 4. Yêu cầu chức năng

### FR-01. Đăng nhập

Hệ thống cho phép người dùng đăng nhập bằng tên đăng nhập và mật khẩu.

Kết quả:

- tài khoản hợp lệ và đang hoạt động: cấp JWT và chuyển tới giao diện theo quyền;
- sai thông tin hoặc tài khoản bị khóa: từ chối đăng nhập.

### FR-02. Đăng xuất

Người dùng có thể đăng xuất; token tại phía frontend bị xóa.

### FR-03. Quản lý tài khoản

Quản trị viên có thể:

- xem danh sách tài khoản;
- tạo tài khoản;
- cập nhật thông tin tài khoản;
- khóa hoặc mở khóa;
- đặt lại mật khẩu;
- gán quyền phù hợp.

### FR-04. Quản lý hồ sơ nhân viên

Quản trị viên hoặc điều phối viên có thể:

- xem danh sách nhân viên;
- thêm hồ sơ nhân viên;
- cập nhật thông tin;
- tìm kiếm hoặc lọc;
- cập nhật trạng thái làm việc.

### FR-05. Quản lý địa điểm

Quản trị viên hoặc điều phối viên có thể:

- xem danh sách địa điểm;
- thêm địa điểm;
- cập nhật địa điểm;
- ngừng hoạt động địa điểm.

Không xóa vật lý địa điểm đã phát sinh sự kiện.

### FR-06. Quản lý sự kiện

Quản trị viên hoặc điều phối viên có thể:

- tạo sự kiện;
- liên kết sự kiện với một địa điểm;
- cập nhật thông tin;
- hủy sự kiện;
- xem danh sách và chi tiết.

### FR-07. Quản lý ca làm

Quản trị viên hoặc điều phối viên có thể:

- tạo ca thuộc một sự kiện;
- cập nhật thời gian bắt đầu và kết thúc;
- thiết lập số nhân viên cần;
- thiết lập tiền công cơ bản;
- mở hoặc đóng đăng ký;
- thay đổi trạng thái ca;
- xem số người đã đăng ký và được duyệt.

### FR-08. Nhân viên xem ca

Nhân viên có thể:

- xem danh sách ca đang mở đăng ký;
- lọc theo thời gian, địa điểm hoặc sự kiện;
- xem chi tiết ca.

### FR-09. Nhân viên đăng ký ca

Khi đăng ký, hệ thống phải kiểm tra:

- ca tồn tại;
- ca đang mở đăng ký;
- nhân viên đang hoạt động;
- nhân viên chưa đăng ký ca đó;
- không trùng thời gian với ca đã được duyệt hoặc phân công.

Đăng ký hợp lệ được tạo với trạng thái `PENDING`.

### FR-10. Nhân viên hủy đăng ký

Nhân viên được hủy đăng ký khi thỏa điều kiện nghiệp vụ.

Hệ thống không xóa vật lý bản ghi; trạng thái chuyển sang `CANCELLED`.

**Thời hạn được phép hủy cần hỏi GVHD hoặc người có nghiệp vụ.**

### FR-11. Duyệt hoặc từ chối đăng ký

Điều phối viên có thể:

- xem danh sách đăng ký đang chờ;
- duyệt;
- từ chối và ghi lý do.

Trước khi duyệt, hệ thống phải kiểm tra:

- đăng ký đang ở trạng thái `PENDING`;
- nhân viên không bị trùng lịch;
- số nhân viên đã được duyệt chưa vượt số lượng cần.

### FR-12. Phân công nhân viên

Khi đăng ký được duyệt, hệ thống tạo phân công.

Thông tin phân công có thể gồm:

- vai trò trong ca;
- khu vực;
- nhiệm vụ;
- trạng thái phân công.

**Cần chốt có cho phép điều phối viên phân công trực tiếp người chưa đăng ký hay không.**

### FR-13. Chấm công thủ công

Điều phối viên hoặc người được giao quyền có thể:

- ghi nhận giờ vào;
- ghi nhận giờ ra;
- ghi nhận vắng mặt;
- xác nhận dữ liệu chấm công.

Hệ thống xác định:

- đi trễ;
- về sớm;
- có mặt;
- vắng mặt.

### FR-14. Tính tiền công cơ bản

Hệ thống tính tiền công sau khi dữ liệu chấm công đã được xác nhận.

Phương án tạm:

`tiền công cuối = tiền công cơ bản + điều chỉnh`

**Chưa chốt cách trừ tiền khi đi trễ, về sớm hoặc vắng mặt.**

### FR-15. Đánh giá nhân viên sau ca

Điều phối viên hoặc trưởng ca có thể đánh giá nhân viên sau ca.

Phương án tạm:

- điểm từ 1 đến 5;
- nội dung nhận xét;
- người đánh giá;
- thời điểm đánh giá.

### FR-16. Thống kê cơ bản

Hệ thống cung cấp:

- số ca theo trạng thái;
- số đăng ký chờ duyệt;
- số nhân viên đã được phân công;
- số ca từng nhân viên đã tham gia;
- tình trạng đi làm;
- tổng tiền công cơ bản theo nhân viên hoặc thời gian.

### FR-17. Lưu lịch sử trạng thái

Hệ thống cần lưu lịch sử thay đổi của các trạng thái quan trọng.

**Cần quyết định dùng bảng lịch sử riêng cho từng nghiệp vụ hay một bảng lịch sử dùng chung.**

---

## 5. Quy tắc nghiệp vụ dự kiến

### BR-01

Một địa điểm có thể có nhiều sự kiện; một sự kiện chỉ thuộc một địa điểm.

### BR-02

Một sự kiện có thể có nhiều ca; một ca chỉ thuộc một sự kiện.

### BR-03

Một nhân viên chỉ được có một đăng ký cho cùng một ca.

### BR-04

Đăng ký mới luôn có trạng thái `PENDING`.

### BR-05

Chỉ đăng ký được duyệt mới tạo phân công.

### BR-06

Số phân công đang hoạt động không được vượt `requiredStaff`.

### BR-07

Chấm công phải gắn với phân công, không gắn trực tiếp với đăng ký.

### BR-08

Tiền công chỉ được tính hoặc xác nhận khi chấm công hợp lệ.

### BR-09

Dữ liệu nghiệp vụ đã phát sinh không xóa vật lý; ưu tiên đổi trạng thái.

### BR-10

Mọi thời điểm phải thỏa `startAt < endAt`.

### BR-11

Ca làm phải thuộc khoảng thời gian hợp lý của sự kiện hoặc được GVHD cho phép khác.

### BR-12

AI không tham gia vào bất kỳ quyết định nào trong Đồ án cơ sở.

---

## 6. Trạng thái dự kiến

### Tài khoản

- `ACTIVE`
- `LOCKED`

### Nhân viên

- `AVAILABLE`
- `INACTIVE`

### Sự kiện

- `DRAFT`
- `CONFIRMED`
- `CANCELLED`
- `COMPLETED`

### Ca làm

- `DRAFT`
- `OPEN`
- `CLOSED`
- `IN_PROGRESS`
- `COMPLETED`
- `CANCELLED`

### Đăng ký ca

- `PENDING`
- `APPROVED`
- `REJECTED`
- `CANCELLED`

### Phân công

- `ASSIGNED`
- `CANCELLED`
- `COMPLETED`

### Chấm công

- `PRESENT`
- `LATE`
- `EARLY_LEAVE`
- `ABSENT`

### Tiền công

- `CALCULATED`
- `CONFIRMED`
- `PAID`

---

## 7. Yêu cầu phi chức năng dự kiến

### NFR-01. Bảo mật

- mật khẩu phải được mã hóa;
- API bảo vệ bằng JWT;
- kiểm tra quyền ở backend;
- không trả password hash cho frontend.

### NFR-02. Toàn vẹn dữ liệu

- sử dụng khóa chính, khóa ngoại và unique constraint;
- dùng transaction cho thao tác duyệt và tạo phân công;
- kiểm tra dữ liệu đầu vào.

### NFR-03. Khả năng sử dụng

- giao diện tiếng Việt;
- thông báo lỗi rõ ràng;
- bảng dữ liệu có tìm kiếm và phân trang khi cần.

### NFR-04. Khả năng bảo trì

- kiến trúc Controller → Service → Repository;
- DTO tách khỏi Entity;
- tên API, class và bảng thống nhất với tài liệu;
- mã nguồn quản lý bằng Git.

### NFR-05. Hiệu năng

Trong phạm vi đồ án, các thao tác thông thường nên phản hồi trong khoảng vài giây với dữ liệu thử nghiệm.

---

## 8. Các câu hỏi bắt buộc phải chốt

1. Trưởng ca là role hệ thống hay vai trò trong từng ca?
2. Điều phối viên có thể phân công trực tiếp người chưa đăng ký không?
3. Nhân viên được hủy đăng ký trước giờ bắt đầu bao lâu?
4. Tính công theo ca cố định hay theo giờ thực tế?
5. Đi trễ, về sớm có làm giảm tiền công không?
6. Ai có quyền ghi nhận và ai có quyền xác nhận chấm công?
7. Có cần phân công cụ thể đến từng bàn trong Đồ án cơ sở không?
8. Có cho phép nhân viên tự đăng ký tài khoản không?
9. Khi sự kiện bị hủy, các ca và đăng ký liên quan chuyển trạng thái thế nào?
10. Có cần lưu lịch sử trạng thái trong Đồ án cơ sở không?

---

## 9. Trạng thái tài liệu

Đây là **bản nháp để rà soát**, chưa phải baseline chính thức.

Chỉ chuyển thành baseline 1.0 sau khi:

- phạm vi được xác nhận;
- các câu hỏi nghiệp vụ được trả lời;
- yêu cầu chức năng không còn mâu thuẫn;
- GVHD hoặc người có nghiệp vụ chấp nhận.
