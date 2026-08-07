package com.viethiep.weddingstaff.dto;

public record AttendanceSummaryResponse(
        long totalAttendances,
        long draftCount,
        long confirmedCount,
        long presentCount,
        long lateCount,
        long earlyLeaveCount,
        long lateAndEarlyLeaveCount,
        long absentCount
) {
}
