package com.viethiep.weddingstaff.dto;

import com.viethiep.weddingstaff.enumtype.AttendanceCheckAction;
import com.viethiep.weddingstaff.enumtype.AttendanceCheckMethod;

import java.time.LocalDateTime;

public record QrAttendanceResultResponse(
        AttendanceResponse attendance,
        AttendanceCheckAction action,
        AttendanceCheckMethod method,
        boolean gpsVerified,
        Integer distanceMeters,
        LocalDateTime occurredAt
) {
}
