package com.viethiep.weddingstaff.dto;

import java.time.LocalDate;

public record OperationsAnalyticsResponse(
        LocalDate from,
        LocalDate to,
        Long venueId,
        long totalShifts,
        long understaffedShifts,
        long fullShifts,
        long overstaffedShifts,
        long missingStaffTotal,
        long replacementTotal,
        long replacementPending,
        long replacementOpen,
        long replacementFilled,
        long replacementRejected,
        long replacementCancelled,
        long replacementResolved,
        double replacementFillRate
) {
}
