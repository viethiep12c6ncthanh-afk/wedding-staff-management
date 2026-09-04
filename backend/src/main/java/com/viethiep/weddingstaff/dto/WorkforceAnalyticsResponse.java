package com.viethiep.weddingstaff.dto;

import java.time.LocalDate;
import java.util.List;

public record WorkforceAnalyticsResponse(
        LocalDate from,
        LocalDate to,
        long confirmedAttendances,
        long presentCount,
        long lateCount,
        long earlyLeaveCount,
        long lateAndEarlyLeaveCount,
        long absentCount,
        double lateRate,
        double absenceRate,
        List<AttendanceTrendPointResponse> attendanceTrend,
        List<WorkloadItemResponse> topWorkload,
        List<ReputationBucketResponse> reputationDistribution
) {
}
