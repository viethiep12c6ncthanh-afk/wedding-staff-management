package com.viethiep.weddingstaff.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record CoordinationOverviewResponse(
        LocalDate date,
        LocalDateTime windowStart,
        LocalDateTime windowEnd,
        Long venueId,
        int transitionBufferMinutes,
        long totalShifts,
        long understaffedShifts,
        long fullShifts,
        long overstaffedShifts,
        List<CoordinationShiftResponse> shifts,
        List<CoordinationIssueResponse> scheduleIssues
) {
}
