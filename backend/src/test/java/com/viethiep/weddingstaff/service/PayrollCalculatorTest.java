package com.viethiep.weddingstaff.service;

import com.viethiep.weddingstaff.entity.Attendance;
import com.viethiep.weddingstaff.entity.ShiftAssignment;
import com.viethiep.weddingstaff.entity.WorkShift;
import com.viethiep.weddingstaff.enumtype.AttendanceResult;
import com.viethiep.weddingstaff.enumtype.ShiftRole;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PayrollCalculatorTest {
    private final PayrollCalculator calculator = new PayrollCalculator();

    @Test
    void staffPresentReceivesBasePayOnly() {
        PayrollCalculator.Result result = calculate(
                ShiftRole.STAFF, AttendanceResult.PRESENT, 0, 0, 60
        );

        assertMoney("100000.00", result.basePay());
        assertMoney("0.00", result.leaderAllowance());
        assertMoney("0.00", result.lateDeduction());
        assertMoney("0.00", result.earlyLeaveDeduction());
        assertEquals(0, result.overtimeMinutes());
        assertMoney("0.00", result.overtimePay());
        assertMoney("100000.00", result.payableAmount());
    }

    @Test
    void leaderReceivesTenPercentAllowance() {
        PayrollCalculator.Result result = calculate(
                ShiftRole.LEADER, AttendanceResult.PRESENT, 0, 0, 60
        );

        assertMoney("10000.00", result.leaderAllowance());
        assertMoney("110000.00", result.payableAmount());
    }

    @Test
    void lateDeductionIsProratedByScheduledMinutes() {
        PayrollCalculator.Result result = calculate(
                ShiftRole.STAFF, AttendanceResult.LATE, 15, 0, 60
        );

        assertMoney("25000.00", result.lateDeduction());
        assertMoney("75000.00", result.payableAmount());
    }

    @Test
    void earlyLeaveDeductionIsProratedByScheduledMinutes() {
        PayrollCalculator.Result result = calculate(
                ShiftRole.STAFF, AttendanceResult.EARLY_LEAVE, 0, 30, 60
        );

        assertMoney("50000.00", result.earlyLeaveDeduction());
        assertMoney("50000.00", result.payableAmount());
    }

    @Test
    void overtimeUsesOnePointFiveMultiplier() {
        PayrollCalculator.Result result = calculate(
                ShiftRole.STAFF, AttendanceResult.PRESENT, 0, 0, 90
        );

        assertEquals(30, result.overtimeMinutes());
        assertMoney("75000.00", result.overtimePay());
        assertMoney("175000.00", result.payableAmount());
    }

    @Test
    void combinesLeaderLateAndOvertimeAdjustments() {
        PayrollCalculator.Result result = calculate(
                ShiftRole.LEADER, AttendanceResult.LATE, 10, 0, 90
        );

        assertMoney("10000.00", result.leaderAllowance());
        assertMoney("16666.67", result.lateDeduction());
        assertMoney("75000.00", result.overtimePay());
        assertMoney("168333.33", result.payableAmount());
    }

    @Test
    void absentAlwaysPaysZeroAndHasNoAdjustments() {
        PayrollCalculator.Result result = calculate(
                ShiftRole.LEADER, AttendanceResult.ABSENT, 0, 0, null
        );

        assertMoney("100000.00", result.basePay());
        assertMoney("0.00", result.leaderAllowance());
        assertMoney("0.00", result.lateDeduction());
        assertMoney("0.00", result.earlyLeaveDeduction());
        assertEquals(0, result.overtimeMinutes());
        assertMoney("0.00", result.overtimePay());
        assertMoney("0.00", result.payableAmount());
    }

    @Test
    void deductionsNeverMakePayableNegative() {
        PayrollCalculator.Result result = calculate(
                ShiftRole.STAFF, AttendanceResult.LATE_AND_EARLY_LEAVE, 50, 50, 60
        );

        assertMoney("83333.33", result.lateDeduction());
        assertMoney("83333.33", result.earlyLeaveDeduction());
        assertMoney("0.00", result.payableAmount());
    }

    private PayrollCalculator.Result calculate(
            ShiftRole role,
            AttendanceResult attendanceResult,
            int lateMinutes,
            int earlyLeaveMinutes,
            Integer checkOutMinute
    ) {
        LocalDateTime start = LocalDateTime.of(2026, 9, 1, 18, 0);
        LocalDateTime end = start.plusMinutes(60);
        WorkShift shift = WorkShift.builder()
                .startAt(start)
                .endAt(end)
                .payAmount(new BigDecimal("100000.00"))
                .build();
        ShiftAssignment assignment = ShiftAssignment.builder()
                .shift(shift)
                .shiftRole(role)
                .build();
        Attendance attendance = Attendance.builder()
                .assignment(assignment)
                .attendanceResult(attendanceResult)
                .lateMinutes(lateMinutes)
                .earlyLeaveMinutes(earlyLeaveMinutes)
                .checkOutAt(checkOutMinute == null
                        ? null
                        : start.plusMinutes(checkOutMinute))
                .build();

        return calculator.calculate(assignment, attendance);
    }

    private void assertMoney(String expected, BigDecimal actual) {
        assertEquals(new BigDecimal(expected), actual);
    }
}
