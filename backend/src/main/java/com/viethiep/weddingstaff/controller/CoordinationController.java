package com.viethiep.weddingstaff.controller;

import com.viethiep.weddingstaff.dto.CoordinationOverviewResponse;
import com.viethiep.weddingstaff.service.CoordinationService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalTime;

@RestController
@RequestMapping("/api/coordination")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','COORDINATOR')")
public class CoordinationController {
    private final CoordinationService coordinationService;

    @GetMapping("/overview")
    public CoordinationOverviewResponse overview(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
            LocalTime fromTime,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
            LocalTime toTime,
            @RequestParam(required = false) Long venueId,
            @RequestParam(defaultValue = "60")
            int transitionBufferMinutes
    ) {
        return coordinationService.overview(
                date,
                fromTime,
                toTime,
                venueId,
                transitionBufferMinutes
        );
    }
}
