package com.viethiep.weddingstaff.dto;

public record DashboardSummaryResponse(
        long totalEmployees,
        long activeEmployees,
        long totalVenues,
        long activeVenues,
        long totalEvents,
        long confirmedEvents,
        long totalShifts,
        long openShifts,
        long pendingRegistrations,
        long activeAssignments,
        long draftAttendances,
        long confirmedAttendances
) {
}
