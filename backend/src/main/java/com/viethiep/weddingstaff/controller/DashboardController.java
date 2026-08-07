package com.viethiep.weddingstaff.controller;

import com.viethiep.weddingstaff.dto.AttendanceSummaryResponse;
import com.viethiep.weddingstaff.dto.DashboardSummaryResponse;
import com.viethiep.weddingstaff.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','COORDINATOR')")
public class DashboardController {
    private final DashboardService dashboardService;

    @GetMapping("/summary")
    public DashboardSummaryResponse summary() {
        return dashboardService.summary();
    }

    @GetMapping("/attendance-summary")
    public AttendanceSummaryResponse attendanceSummary() {
        return dashboardService.attendanceSummary();
    }
}
