package com.viethiep.weddingstaff.dto;

import com.viethiep.weddingstaff.enumtype.EventStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record EventRequest(
        @NotNull Long venueId,
        @NotBlank String name,
        @NotNull LocalDateTime startAt,
        @NotNull LocalDateTime endAt,
        @NotNull EventStatus status,
        String description
) {}
