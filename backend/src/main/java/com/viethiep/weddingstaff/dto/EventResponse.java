package com.viethiep.weddingstaff.dto;

import com.viethiep.weddingstaff.enumtype.EventStatus;

import java.time.LocalDateTime;

public record EventResponse(
        Long id,
        Long venueId,
        String venueName,
        String name,
        LocalDateTime startAt,
        LocalDateTime endAt,
        EventStatus eventStatus,
        String description,
        LocalDateTime cancelledAt,
        String cancellationReason
) {
}
