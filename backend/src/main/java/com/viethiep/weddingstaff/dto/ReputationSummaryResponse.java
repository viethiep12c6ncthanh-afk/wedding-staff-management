package com.viethiep.weddingstaff.dto;

import java.time.LocalDateTime;

public record ReputationSummaryResponse(
        Long employeeId,
        String employeeCode,
        String fullName,
        Integer currentScore,
        Integer completedShiftCount,
        Integer lateCount,
        Integer earlyLeaveCount,
        Integer absentCount,
        Integer evaluationCount,
        Double averageRating,
        LocalDateTime lastEventAt
) {
}
