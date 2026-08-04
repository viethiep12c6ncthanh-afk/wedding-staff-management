package com.viethiep.weddingstaff.dto;

import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record UpdateAttendanceRequest(
        LocalDateTime checkInAt,
        LocalDateTime checkOutAt,
        boolean absent,
        @Size(max = 500) String note
) {
}
