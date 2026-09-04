package com.viethiep.weddingstaff.dto;

public record AiProviderUsageResponse(
        String provider,
        String model,
        long runCount
) {
}
