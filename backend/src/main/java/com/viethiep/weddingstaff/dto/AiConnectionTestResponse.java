package com.viethiep.weddingstaff.dto;

import java.util.List;

public record AiConnectionTestResponse(String provider, String model, String dataSource,
                                       int inputCandidateCount, String summary,
                                       List<AiRecommendedCandidateResponse> candidates) {
}
