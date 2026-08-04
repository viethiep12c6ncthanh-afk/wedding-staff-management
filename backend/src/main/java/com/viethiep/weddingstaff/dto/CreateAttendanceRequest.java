package com.viethiep.weddingstaff.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record CreateAttendanceRequest(
        @NotNull Long assignmentId,
        LocalDateTime checkInAt,
        LocalDateTime checkOutAt,
        boolean absent,
        @Size(max = 500) String note
) {
}
