package com.viethiep.weddingstaff.service.ai;

import java.util.List;

public interface AiRecommendationClient {
    boolean isAvailable();

    String provider();

    String model();

    Result recommend(Prompt prompt);

    record Prompt(String contextJson) {
    }

    record Result(
            String rawJson,
            String summary,
            List<CandidateAnalysis> candidates
    ) {
    }

    record CandidateAnalysis(
            Long employeeId,
            String explanation,
            List<String> strengths,
            List<String> risks
    ) {
    }
}
