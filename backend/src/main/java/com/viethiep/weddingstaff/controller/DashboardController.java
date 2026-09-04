package com.viethiep.weddingstaff.controller;

import com.viethiep.weddingstaff.dto.AiAnalyticsResponse;
import com.viethiep.weddingstaff.dto.AttendanceSummaryResponse;
import com.viethiep.weddingstaff.dto.DashboardSummaryResponse;
import com.viethiep.weddingstaff.dto.OperationsAnalyticsResponse;
import com.viethiep.weddingstaff.dto.WorkforceAnalyticsResponse;
import com.viethiep.weddingstaff.service.AiAnalyticsService;
import com.viethiep.weddingstaff.service.DashboardService;
import com.viethiep.weddingstaff.service.OperationsAnalyticsService;
import com.viethiep.weddingstaff.service.WorkforceAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','COORDINATOR')")
public class DashboardController {
    private final DashboardService dashboardService;
    private final OperationsAnalyticsService operationsAnalyticsService;
    private final WorkforceAnalyticsService workforceAnalyticsService;
    private final AiAnalyticsService aiAnalyticsService;

    @GetMapping("/summary")
    public DashboardSummaryResponse summary() {
        return dashboardService.summary();
    }

    @GetMapping("/attendance-summary")
    public AttendanceSummaryResponse attendanceSummary() {
        return dashboardService.attendanceSummary();
    }

    @GetMapping("/operations")
    public OperationsAnalyticsResponse operations(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate to,
            @RequestParam(required = false)
            Long venueId
    ) {
        return operationsAnalyticsService.analyze(from, to, venueId);
    }

    @GetMapping("/workforce")
    public WorkforceAnalyticsResponse workforce(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate to
    ) {
        return workforceAnalyticsService.analyze(from, to);
    }

    @GetMapping("/ai")
    public AiAnalyticsResponse ai(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate to
    ) {
        return aiAnalyticsService.analyze(from, to);
    }
}
