# DACN Commit 6 - Hybrid AI-assisted Recommendation v1.0

## Mục tiêu

Bổ sung lớp AI hỗ trợ phân tích ứng viên thay ca sau lớp hard constraints và deterministic ranking của Commit 5.

Đây là kiến trúc **Hybrid Rule-based + LLM-assisted Recommendation**:

1. Backend Commit 5 loại ứng viên không hợp lệ.
2. Backend chấm điểm deterministic và tạo thứ tự nền.
3. Chỉ top N ứng viên hợp lệ được đưa sang LLM để phân tích ngữ cảnh mềm.
4. LLM chỉ được phép sắp xếp lại các `employeeId` đã được backend cung cấp và giải thích quyết định.
5. Backend kiểm tra lại toàn bộ output AI.
6. Nếu AI lỗi, timeout, chưa cấu hình hoặc output không hợp lệ, hệ thống fallback về deterministic ranking.
7. ADMIN/COORDINATOR vẫn là người quyết định gửi invitation.

## API

`POST /api/replacements/requests/{requestId}/ai-recommendation`

Quyền: `ADMIN`, `COORDINATOR`.

Endpoint rule-based cũ vẫn giữ nguyên:

`GET /api/replacements/requests/{requestId}/candidates`

AI không thay đổi ý nghĩa endpoint này.

## Dữ liệu đưa vào AI

AI chỉ nhận dữ liệu liên quan trực tiếp đến công việc:

- vai trò ca;
- khu vực và nhiệm vụ của assignment gốc;
- tên/thời gian/mô tả ca;
- tên/mô tả sự kiện;
- tên/địa chỉ venue;
- employee id/code;
- deterministic rank/score;
- reputation score;
- reliability percent;
- completed shift count;
- same-role completed count;
- một số đánh giá gần nhất: rating, comment, shift/event/venue, evaluatedAt.

Không gửi vào AI:

- ngày sinh;
- địa chỉ nhà nhân viên;
- email/điện thoại;
- ghi chú hồ sơ nhân viên;
- lý do cá nhân của người xin thay ca;
- mật khẩu, JWT hoặc API key.

## Output AI có cấu trúc

Hệ thống dùng abstraction `AiRecommendationClient` và hỗ trợ hai provider:

- `OLLAMA` - mặc định cho demo/local, chạy LLM trên máy và không cần API key;
- `OPENAI` - tùy chọn, dùng Responses API khi có API key/credit.

Với Ollama, backend gọi `POST /api/chat`, `stream=false`, truyền JSON Schema qua `format`, `temperature=0` và giới hạn output. Với OpenAI, backend dùng Responses API Structured Outputs (`json_schema`, strict mode).

Output gồm:

- `summary`;
- danh sách candidate theo thứ tự AI;
- `employeeId`;
- `explanation`;
- `strengths`;
- `risks`.

Backend không tin trực tiếp dữ liệu AI. Trước khi dùng output phải kiểm tra:

- số candidate bằng số candidate đã gửi;
- mọi `employeeId` đều thuộc tập hợp deterministic hợp lệ;
- không duplicate id;
- không thiếu candidate;
- explanation không rỗng.

Nếu bất kỳ kiểm tra nào thất bại, dùng deterministic fallback.

## Fallback

Các mã fallback:

- `NO_ELIGIBLE_CANDIDATES`;
- `AI_DISABLED_OR_NOT_CONFIGURED`;
- `AI_PROVIDER_ERROR`;
- `AI_OUTPUT_INVALID`.

Fallback không làm hỏng workflow thay ca và không tự tạo assignment.

## Lịch sử recommendation

Bảng `ai_recommendation_runs` lưu:

- replacement request;
- mode;
- provider/model;
- trạng thái fallback;
- deterministic snapshot;
- AI context snapshot;
- raw structured AI result nếu có;
- summary;
- người yêu cầu phân tích;
- timestamp.

Mục đích là truy vết, demo, báo cáo và phục vụ dashboard sau này. API key không được lưu.

## Cấu hình

Biến môi trường chung:

- `AI_ENABLED=false` mặc định;
- `AI_PROVIDER=OLLAMA` mặc định;
- `AI_BASE_URL`;
- `AI_MODEL`;
- `AI_TIMEOUT_MS=60000`;
- `AI_CANDIDATE_LIMIT=5`;
- `AI_RECENT_EVALUATION_LIMIT=3`.

Cấu hình demo/local khuyến nghị:

- `AI_PROVIDER=OLLAMA`;
- `AI_BASE_URL=http://localhost:11434`;
- `AI_MODEL=qwen3:4b-instruct`;
- không cần `AI_API_KEY`.

Chuẩn bị máy demo một lần bằng:

```text
ollama pull qwen3:4b-instruct
```

Nếu muốn dùng OpenAI thay thế:

- `AI_PROVIDER=OPENAI`;
- `AI_BASE_URL=https://api.openai.com/v1`;
- `AI_API_KEY=<secret>`;
- `AI_MODEL=gpt-5.4-mini` hoặc model được cấu hình.

Nếu AI bị tắt, Ollama không chạy, provider lỗi/timeout hoặc output không hợp lệ, endpoint vẫn trả deterministic fallback và workflow thay ca không bị gián đoạn.

## Quyền quyết định

AI không được:

- bypass hard constraints;
- đưa ứng viên mới vào danh sách;
- tự gửi invitation;
- tự tạo assignment;
- tự thay đổi điểm reputation/deterministic score.

Coordinator là người quyết định cuối cùng.

## Provider local cho demo

Bản demo DACN ưu tiên Ollama local để tránh phụ thuộc billing/quota và giảm rủi ro mạng khi bảo vệ. Model không được commit lên Git; repository chỉ lưu source/config mẫu. Máy chạy demo tự cài Ollama và tải model.

OpenAI client vẫn được giữ như provider tùy chọn để chứng minh kiến trúc không khóa vào một LLM cụ thể.
