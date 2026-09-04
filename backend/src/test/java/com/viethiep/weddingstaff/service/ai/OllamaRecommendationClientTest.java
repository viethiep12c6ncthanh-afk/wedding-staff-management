package com.viethiep.weddingstaff.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.viethiep.weddingstaff.config.AiRecommendationProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OllamaRecommendationClientTest {
    private ObjectMapper objectMapper;
    private AiRecommendationProperties properties;
    private OllamaRecommendationClient client;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        properties = new AiRecommendationProperties();
        properties.setEnabled(true);
        properties.setProvider("OLLAMA");
        properties.setBaseUrl("http://localhost:11434");
        properties.setModel("qwen3:4b-instruct");
        client = new OllamaRecommendationClient(properties, objectMapper);
    }

    @Test
    void buildRequestUsesLocalStructuredOutputContract() throws Exception {
        String body = client.buildRequestBody(
                new AiRecommendationClient.Prompt("{\"candidates\":[{\"employeeId\":4}]}")
        );

        JsonNode root = objectMapper.readTree(body);
        assertEquals("qwen3:4b-instruct", root.path("model").asText());
        assertFalse(root.path("stream").asBoolean(true));
        assertEquals("5m", root.path("keep_alive").asText());
        assertEquals("object", root.path("format").path("type").asText());
        assertEquals(0, root.path("options").path("temperature").asInt());
        assertEquals(768, root.path("options").path("num_predict").asInt());
        assertEquals("system", root.path("messages").get(0).path("role").asText());
        assertEquals("user", root.path("messages").get(1).path("role").asText());
    }

    @Test
    void parseResponseReturnsStructuredRecommendation() throws Exception {
        String structured = """
                {"summary":"Ưu tiên ứng viên 5","candidates":[
                  {"employeeId":5,"explanation":"Nhiều kinh nghiệm hơn","strengths":["Kinh nghiệm"],"risks":[]},
                  {"employeeId":4,"explanation":"Điểm nền tốt","strengths":["Uy tín"],"risks":["Ít kinh nghiệm"]}
                ]}
                """;
        String providerResponse = objectMapper.writeValueAsString(
                java.util.Map.of(
                        "done", true,
                        "done_reason", "stop",
                        "message", java.util.Map.of(
                                "role", "assistant",
                                "content", structured
                        )
                )
        );

        AiRecommendationClient.Result result = client.parseResponse(providerResponse);

        assertEquals("Ưu tiên ứng viên 5", result.summary());
        assertEquals(2, result.candidates().size());
        assertEquals(5L, result.candidates().get(0).employeeId());
        assertEquals("Kinh nghiệm", result.candidates().get(0).strengths().get(0));
        assertEquals(4L, result.candidates().get(1).employeeId());
        assertEquals("Ít kinh nghiệm", result.candidates().get(1).risks().get(0));
    }

    @Test
    void parseResponseRejectsIncompleteProviderResponse() {
        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> client.parseResponse("{\"done\":false}")
        );

        assertEquals("AI response was not completed", ex.getMessage());
    }
}
