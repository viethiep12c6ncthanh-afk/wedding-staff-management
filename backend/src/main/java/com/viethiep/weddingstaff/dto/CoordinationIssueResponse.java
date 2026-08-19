package com.viethiep.weddingstaff.dto;

import com.viethiep.weddingstaff.enumtype.CoordinationIssueType;

import java.time.LocalDateTime;

public record CoordinationIssueResponse(
        CoordinationIssueType issueType,
        Long employeeId,
        String employeeName,
        Long firstShiftId,
        String firstShiftName,
        Long firstVenueId,
        String firstVenueName,
        LocalDateTime firstStartAt,
        LocalDateTime firstEndAt,
        Long secondShiftId,
        String secondShiftName,
        Long secondVenueId,
        String secondVenueName,
        LocalDateTime secondStartAt,
        LocalDateTime secondEndAt,
        long gapMinutes,
        long overlapMinutes,
        int requiredBufferMinutes
) {
}
