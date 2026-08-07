package com.viethiep.weddingstaff.controller;

import com.viethiep.weddingstaff.dto.PayrollReportResponse;
import com.viethiep.weddingstaff.service.PayrollReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/reports/payroll")
@RequiredArgsConstructor
public class ReportController {
    private final PayrollReportService payrollReportService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','COORDINATOR')")
    public PayrollReportResponse payroll(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate to
    ) {
        return payrollReportService.findAll(from, to);
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public PayrollReportResponse myPayroll(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate to,
            Authentication authentication
    ) {
        return payrollReportService.findMine(
                authentication.getName(),
                from,
                to
        );
    }
}
