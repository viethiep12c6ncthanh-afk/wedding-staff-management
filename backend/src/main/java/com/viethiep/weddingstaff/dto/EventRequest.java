package com.viethiep.weddingstaff.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record EventRequest(
        @NotNull @Positive Long venueId,
        @NotBlank @Size(max = 150) String name,
        @NotNull LocalDateTime startAt,
        @NotNull LocalDateTime endAt,
        @Size(max = 1000) String description
) {
}
