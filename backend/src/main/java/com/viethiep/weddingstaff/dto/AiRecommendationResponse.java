package com.viethiep.weddingstaff.dto;

import com.viethiep.weddingstaff.enumtype.AiRecommendationMode;

import java.util.List;

public record AiRecommendationResponse(
        Long runId,
        Long requestId,
        AiRecommendationMode mode,
        boolean fallbackUsed,
        String fallbackReason,
        String provider,
        String model,
        String summary,
        List<AiRecommendedCandidateResponse> candidates
) {
}
