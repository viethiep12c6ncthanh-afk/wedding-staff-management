package com.viethiep.weddingstaff.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ShiftRequest(
        @NotNull @Positive Long eventId,
        @NotBlank @Size(max = 120) String name,
        @NotNull LocalDateTime startAt,
        @NotNull LocalDateTime endAt,
        @NotNull @Min(1) Integer requiredStaff,
        @NotNull @DecimalMin("0.00") BigDecimal payAmount,
        LocalDateTime registrationDeadline,
        @Size(max = 500) String description
) {
}
