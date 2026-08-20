package com.viethiep.weddingstaff.dto;

import com.viethiep.weddingstaff.enumtype.ReplacementRequestStatus;

import java.time.LocalDateTime;
import java.util.List;

public record ReplacementRequestResponse(
        Long id,
        Long originalAssignmentId,
        Long shiftId,
        String shiftName,
        String eventName,
        String venueName,
        LocalDateTime startAt,
        LocalDateTime endAt,
        Long originalEmployeeId,
        String originalEmployeeName,
        String requestedBy,
        String reason,
        ReplacementRequestStatus status,
        String reviewedBy,
        LocalDateTime reviewedAt,
        String reviewNote,
        Long replacementAssignmentId,
        Long replacementEmployeeId,
        String replacementEmployeeName,
        LocalDateTime filledAt,
        LocalDateTime closedAt,
        String closedReason,
        LocalDateTime createdAt,
        List<ReplacementInvitationResponse> invitations
) {
}
