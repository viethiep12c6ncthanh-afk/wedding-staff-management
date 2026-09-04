package com.viethiep.weddingstaff.dto;

import com.viethiep.weddingstaff.enumtype.CoordinationStaffingStatus;
import com.viethiep.weddingstaff.enumtype.ShiftStatus;

import java.time.LocalDateTime;

public record CoordinationShiftResponse(
        Long shiftId,
        Long eventId,
        String eventName,
        Long venueId,
        String venueName,
        String venueAddress,
        String shiftName,
        LocalDateTime startAt,
        LocalDateTime endAt,
        ShiftStatus shiftStatus,
        Integer requiredStaff,
        long effectiveStaffCount,
        long missingStaffCount,
        long extraStaffCount,
        CoordinationStaffingStatus staffingStatus
) {
}
