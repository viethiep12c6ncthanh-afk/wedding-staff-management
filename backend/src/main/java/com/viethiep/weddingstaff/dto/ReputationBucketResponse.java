package com.viethiep.weddingstaff.dto;

public record ReputationBucketResponse(
        String code,
        String label,
        int minScore,
        int maxScore,
        long employeeCount
) {
}
