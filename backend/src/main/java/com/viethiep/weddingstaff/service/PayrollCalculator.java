package com.viethiep.weddingstaff.service;

import com.viethiep.weddingstaff.entity.Attendance;
import com.viethiep.weddingstaff.entity.ShiftAssignment;
import com.viethiep.weddingstaff.entity.WorkShift;
import com.viethiep.weddingstaff.enumtype.AttendanceResult;
import com.viethiep.weddingstaff.enumtype.ShiftRole;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;

public final class PayrollCalculator {
    public static final String POLICY_VERSION = "DACN_MULTI_RULE_V1";

    private static final int MONEY_SCALE = 2;
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(MONEY_SCALE);
    private static final BigDecimal LEADER_ALLOWANCE_RATE = new BigDecimal("0.10");
    private static final BigDecimal OVERTIME_MULTIPLIER = new BigDecimal("1.50");

    public Result calculate(
            ShiftAssignment assignment,
            Attendance attendance
    ) {
        WorkShift shift = assignment.getShift();
        BigDecimal basePay = money(shift.getPayAmount());

        long scheduledMinutes = Duration.between(
                shift.getStartAt(),
                shift.getEndAt()
        ).toMinutes();
        if (scheduledMinutes <= 0) {
            throw new IllegalStateException(
                    "Thời lượng ca phải lớn hơn 0 để tính tiền công"
            );
        }

        if (attendance.getAttendanceResult() == AttendanceResult.ABSENT) {
            return new Result(
                    POLICY_VERSION,
                    basePay,
                    ZERO,
                    ZERO,
                    ZERO,
                    0,
                    ZERO,
                    ZERO
            );
        }

        int lateMinutes = nonNegative(attendance.getLateMinutes());
        int earlyLeaveMinutes = nonNegative(attendance.getEarlyLeaveMinutes());
        int overtimeMinutes = attendance.getCheckOutAt() == null
                ? 0
                : positiveMinutes(shift.getEndAt(), attendance.getCheckOutAt());

        BigDecimal leaderAllowance = assignment.getShiftRole() == ShiftRole.LEADER
                ? money(basePay.multiply(LEADER_ALLOWANCE_RATE))
                : ZERO;
        BigDecimal lateDeduction = prorated(
                basePay, lateMinutes, scheduledMinutes, BigDecimal.ONE
        );
        BigDecimal earlyLeaveDeduction = prorated(
                basePay, earlyLeaveMinutes, scheduledMinutes, BigDecimal.ONE
        );
        BigDecimal overtimePay = prorated(
                basePay, overtimeMinutes, scheduledMinutes, OVERTIME_MULTIPLIER
        );

        BigDecimal payable = basePay
                .add(leaderAllowance)
                .add(overtimePay)
                .subtract(lateDeduction)
                .subtract(earlyLeaveDeduction);
        if (payable.signum() < 0) {
            payable = ZERO;
        } else {
            payable = money(payable);
        }

        return new Result(
                POLICY_VERSION,
                basePay,
                leaderAllowance,
                lateDeduction,
                earlyLeaveDeduction,
                overtimeMinutes,
                overtimePay,
                payable
        );
    }

    private BigDecimal prorated(
            BigDecimal basePay,
            int minutes,
            long scheduledMinutes,
            BigDecimal multiplier
    ) {
        if (minutes <= 0 || basePay.signum() == 0) {
            return ZERO;
        }
        return basePay
                .multiply(BigDecimal.valueOf(minutes))
                .multiply(multiplier)
                .divide(
                        BigDecimal.valueOf(scheduledMinutes),
                        MONEY_SCALE,
                        RoundingMode.HALF_UP
                );
    }

    private int positiveMinutes(
            java.time.LocalDateTime expected,
            java.time.LocalDateTime actual
    ) {
        long minutes = Duration.between(expected, actual).toMinutes();
        if (minutes <= 0) {
            return 0;
        }
        return Math.toIntExact(minutes);
    }

    private int nonNegative(Integer value) {
        return value == null ? 0 : Math.max(value, 0);
    }

    private BigDecimal money(BigDecimal value) {
        if (value == null) {
            throw new IllegalStateException("Ca chưa có mức tiền công cơ bản");
        }
        if (value.signum() < 0) {
            throw new IllegalStateException("Tiền công cơ bản không được âm");
        }
        return value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    public record Result(
            String policyVersion,
            BigDecimal basePay,
            BigDecimal leaderAllowance,
            BigDecimal lateDeduction,
            BigDecimal earlyLeaveDeduction,
            Integer overtimeMinutes,
            BigDecimal overtimePay,
            BigDecimal payableAmount
    ) {
    }
}
