package com.viethiep.weddingstaff.dto;

import java.time.LocalDateTime;

public record EmployeeEvaluationResponse(
        Long id,
        Long assignmentId,
        Long shiftId,
        String shiftName,
        String eventName,
        String venueName,
        Integer rating,
        String comment,
        String evaluatedBy,
        LocalDateTime evaluatedAt
) {
}
