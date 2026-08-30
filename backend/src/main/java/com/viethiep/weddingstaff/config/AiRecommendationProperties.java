package com.viethiep.weddingstaff.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.ai")
public class AiRecommendationProperties {
    private boolean enabled = false;
    private String provider = "OLLAMA";
    private String baseUrl = "";
    private String apiKey = "";
    private String model = "";
    private int timeoutMs = 60000;
    private int candidateLimit = 5;
    private int recentEvaluationLimit = 3;
}
