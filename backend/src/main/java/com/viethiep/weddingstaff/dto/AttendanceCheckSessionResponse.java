package com.viethiep.weddingstaff.dto;

import com.viethiep.weddingstaff.enumtype.AttendanceCheckAction;

import java.time.LocalDateTime;

public record AttendanceCheckSessionResponse(
        Long sessionId,
        Long shiftId,
        String shiftName,
        AttendanceCheckAction action,
        LocalDateTime validFrom,
        LocalDateTime expiresAt,
        boolean gpsRequired,
        Integer radiusMeters,
        String qrToken,
        String otp
) {
}
