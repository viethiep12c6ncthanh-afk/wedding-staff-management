package com.viethiep.weddingstaff.dto;

import com.viethiep.weddingstaff.enumtype.AssignmentStatus;
import com.viethiep.weddingstaff.enumtype.ShiftRole;

import java.time.LocalDateTime;

public record ReplacementEligibleAssignmentResponse(
        Long assignmentId,
        Long shiftId,
        String shiftName,
        String eventName,
        String venueName,
        LocalDateTime startAt,
        LocalDateTime endAt,
        ShiftRole shiftRole,
        String area,
        String task,
        AssignmentStatus assignmentStatus
) {
}
