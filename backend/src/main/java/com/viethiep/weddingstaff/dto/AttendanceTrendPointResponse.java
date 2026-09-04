package com.viethiep.weddingstaff.dto;

import java.time.LocalDate;

public record AttendanceTrendPointResponse(
        LocalDate date,
        long confirmedCount,
        long lateCount,
        long absentCount
) {
}
