package com.viethiep.weddingstaff.dto;

import com.viethiep.weddingstaff.enumtype.ShiftStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ShiftRequest(
        @NotNull Long eventId,
        @NotBlank String name,
        @NotNull LocalDateTime startAt,
        @NotNull LocalDateTime endAt,
        @NotNull @Min(1) Integer requiredStaff,
        @NotNull @DecimalMin("0") BigDecimal payAmount,
        boolean registrationOpen,
        @NotNull ShiftStatus status,
        String note
) {}
