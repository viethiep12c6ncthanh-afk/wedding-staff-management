package com.viethiep.weddingstaff.dto;

import com.viethiep.weddingstaff.enumtype.ReplacementInvitationStatus;
import com.viethiep.weddingstaff.enumtype.ReplacementRequestStatus;

import java.time.LocalDateTime;

public record ReplacementInvitationResponse(
        Long id,
        Long requestId,
        Long employeeId,
        String employeeName,
        ReplacementInvitationStatus status,
        String invitedBy,
        LocalDateTime invitedAt,
        LocalDateTime respondedAt,
        String responseNote,
        Long shiftId,
        String shiftName,
        String eventName,
        String venueName,
        LocalDateTime startAt,
        LocalDateTime endAt,
        String originalEmployeeName,
        ReplacementRequestStatus requestStatus,
        Long replacementAssignmentId
) {
}
