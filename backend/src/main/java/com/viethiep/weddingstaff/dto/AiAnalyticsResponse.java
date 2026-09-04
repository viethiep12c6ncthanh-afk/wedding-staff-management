package com.viethiep.weddingstaff.dto;

import java.time.LocalDate;
import java.util.List;

public record AiAnalyticsResponse(
        LocalDate from,
        LocalDate to,
        long totalRuns,
        long aiAssistedRuns,
        long fallbackRuns,
        double fallbackRate,
        List<AiProviderUsageResponse> providerUsage,
        List<AiFallbackReasonResponse> fallbackReasons
) {
}
