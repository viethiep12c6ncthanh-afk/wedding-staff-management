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
    private String provider = "OPENAI";
    private String baseUrl = "https://api.openai.com/v1";
    private String apiKey = "";
    private String model = "gpt-5.4-mini";
    private int timeoutMs = 20000;
    private int candidateLimit = 5;
    private int recentEvaluationLimit = 3;
}
