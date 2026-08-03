package com.viethiep.weddingstaff.dto;

import com.viethiep.weddingstaff.enumtype.ShiftStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ShiftResponse(
        Long id,
        Long eventId,
        String eventName,
        String venueName,
        String name,
        LocalDateTime startAt,
        LocalDateTime endAt,
        Integer requiredStaff,
        BigDecimal payAmount,
        LocalDateTime registrationDeadline,
        ShiftStatus shiftStatus,
        String description,
        LocalDateTime cancelledAt,
        String cancellationReason
) {
}
