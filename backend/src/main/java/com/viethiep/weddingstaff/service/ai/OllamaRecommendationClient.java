package com.viethiep.weddingstaff.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.viethiep.weddingstaff.config.AiRecommendationProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "app.ai",
        name = "provider",
        havingValue = "OLLAMA",
        matchIfMissing = true
)
public class OllamaRecommendationClient implements AiRecommendationClient {
    private static final String DEFAULT_BASE_URL = "http://localhost:11434";
    private static final String DEFAULT_MODEL = "qwen3:4b-instruct";
    private static final String INSTRUCTIONS = """
            Bạn là trợ lý hỗ trợ điều phối nhân sự tiệc cưới/sự kiện.
            Chỉ phân tích các ứng viên hợp lệ mà backend đã cung cấp.
            Không tạo thêm employeeId, không bỏ ứng viên, không lặp employeeId.
            Hãy sắp xếp lại toàn bộ ứng viên theo mức phù hợp với ngữ cảnh công việc.
            Dùng dữ liệu deterministic làm nền, còn nhận xét đánh giá gần đây chỉ là tín hiệu mềm.
            Không suy đoán thuộc tính cá nhân không có trong dữ liệu.
            Không sử dụng tuổi, giới tính, địa chỉ nhà hoặc hoàn cảnh cá nhân để xếp hạng.
            Không tự quyết định phân công hay gửi lời mời thay ca.
            Giải thích ngắn, cụ thể, bằng tiếng Việt và nêu cả điểm mạnh lẫn rủi ro nếu có.
            """;

    private final AiRecommendationProperties properties;
    private final ObjectMapper objectMapper;

    @Override
    public boolean isAvailable() {
        return properties.isEnabled();
    }

    @Override
    public String provider() {
        return "OLLAMA";
    }

    @Override
    public String model() {
        String configured = properties.getModel();
        return configured == null || configured.isBlank()
                ? DEFAULT_MODEL
                : configured.trim();
    }

    @Override
    public Result recommend(Prompt prompt) {
        if (!isAvailable()) {
            throw new IllegalStateException("AI provider is disabled");
        }

        try {
            String requestBody = buildRequestBody(prompt);
            int timeoutMs = Math.max(properties.getTimeoutMs(), 1000);
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofMillis(timeoutMs))
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(normalizeBaseUrl() + "/api/chat"))
                    .timeout(Duration.ofMillis(timeoutMs))
                    .header("Content-Type", "application/json; charset=utf-8")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = client.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException(
                        "AI provider returned HTTP " + response.statusCode()
                );
            }

            return parseResponse(response.body());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("AI request was interrupted", ex);
        } catch (Exception ex) {
            if (ex instanceof IllegalStateException illegalStateException) {
                throw illegalStateException;
            }
            throw new IllegalStateException("AI provider request failed", ex);
        }
    }

    String buildRequestBody(Prompt prompt) throws Exception {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", model());
        root.put("stream", false);
        root.put("keep_alive", "5m");

        ArrayNode messages = root.putArray("messages");
        ObjectNode system = messages.addObject();
        system.put("role", "system");
        system.put("content", INSTRUCTIONS);
        ObjectNode user = messages.addObject();
        user.put("role", "user");
        user.put("content", prompt.contextJson());

        root.set("format", buildSchema());
        ObjectNode options = root.putObject("options");
        options.put("temperature", 0);
        options.put("num_predict", 768);
        return objectMapper.writeValueAsString(root);
    }

    Result parseResponse(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);
        if (!root.path("done").asBoolean(false)) {
            throw new IllegalStateException("AI response was not completed");
        }

        String outputText = root.path("message").path("content").asText(null);
        if (outputText == null || outputText.isBlank()) {
            throw new IllegalStateException("AI response did not contain message.content");
        }

        JsonNode structured = objectMapper.readTree(outputText);
        String summary = structured.path("summary").asText(null);
        List<CandidateAnalysis> candidates = new ArrayList<>();
        for (JsonNode item : structured.path("candidates")) {
            candidates.add(new CandidateAnalysis(
                    item.path("employeeId").isIntegralNumber()
                            ? item.path("employeeId").longValue()
                            : null,
                    item.path("explanation").asText(null),
                    toStringList(item.path("strengths")),
                    toStringList(item.path("risks"))
            ));
        }
        return new Result(outputText, summary, candidates);
    }

    private ObjectNode buildSchema() {
        ObjectNode candidate = objectMapper.createObjectNode();
        candidate.put("type", "object");
        ObjectNode candidateProperties = candidate.putObject("properties");
        candidateProperties.putObject("employeeId").put("type", "integer");
        candidateProperties.putObject("explanation").put("type", "string");
        ObjectNode strengths = candidateProperties.putObject("strengths");
        strengths.put("type", "array");
        strengths.putObject("items").put("type", "string");
        ObjectNode risks = candidateProperties.putObject("risks");
        risks.put("type", "array");
        risks.putObject("items").put("type", "string");
        ArrayNode candidateRequired = candidate.putArray("required");
        candidateRequired.add("employeeId");
        candidateRequired.add("explanation");
        candidateRequired.add("strengths");
        candidateRequired.add("risks");
        candidate.put("additionalProperties", false);

        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        ObjectNode propertiesNode = schema.putObject("properties");
        propertiesNode.putObject("summary").put("type", "string");
        ObjectNode candidates = propertiesNode.putObject("candidates");
        candidates.put("type", "array");
        candidates.set("items", candidate);
        ArrayNode required = schema.putArray("required");
        required.add("summary");
        required.add("candidates");
        schema.put("additionalProperties", false);
        return schema;
    }

    private List<String> toStringList(JsonNode node) {
        List<String> result = new ArrayList<>();
        if (!node.isArray()) {
            return result;
        }
        for (JsonNode item : node) {
            if (item.isTextual()) {
                result.add(item.asText());
            }
        }
        return result;
    }

    private String normalizeBaseUrl() {
        String value = properties.getBaseUrl();
        if (value == null || value.isBlank()) {
            return DEFAULT_BASE_URL;
        }
        String trimmed = value.trim();
        return trimmed.endsWith("/")
                ? trimmed.substring(0, trimmed.length() - 1)
                : trimmed;
    }
}
