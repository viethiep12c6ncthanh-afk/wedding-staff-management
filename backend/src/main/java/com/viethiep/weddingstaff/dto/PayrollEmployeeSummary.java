package com.viethiep.weddingstaff.dto;

import java.math.BigDecimal;

public record PayrollEmployeeSummary(
        Long employeeId,
        String employeeCode,
        String fullName,
        long confirmedShiftCount,
        long paidShiftCount,
        long absentShiftCount,
        BigDecimal totalBasePay,
        BigDecimal totalLeaderAllowance,
        BigDecimal totalLateDeduction,
        BigDecimal totalEarlyLeaveDeduction,
        BigDecimal totalOvertimePay,
        BigDecimal totalPayable
) {
}
