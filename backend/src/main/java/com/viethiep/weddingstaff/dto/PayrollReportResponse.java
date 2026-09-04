package com.viethiep.weddingstaff.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record PayrollReportResponse(
        LocalDate from,
        LocalDate to,
        long confirmedAttendanceCount,
        long paidShiftCount,
        long absentShiftCount,
        BigDecimal totalBasePay,
        BigDecimal totalLeaderAllowance,
        BigDecimal totalLateDeduction,
        BigDecimal totalEarlyLeaveDeduction,
        BigDecimal totalOvertimePay,
        BigDecimal totalPayable,
        List<PayrollEmployeeSummary> employees
) {
}
