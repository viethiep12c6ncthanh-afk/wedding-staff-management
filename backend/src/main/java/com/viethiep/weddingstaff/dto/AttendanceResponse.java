package com.viethiep.weddingstaff.dto;

import com.viethiep.weddingstaff.enumtype.AttendanceProcessStatus;
import com.viethiep.weddingstaff.enumtype.AttendanceResult;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AttendanceResponse(
        Long id,
        Long assignmentId,
        Long shiftId,
        String shiftName,
        LocalDateTime shiftStartAt,
        LocalDateTime shiftEndAt,
        Long employeeId,
        String employeeName,
        AttendanceProcessStatus processStatus,
        AttendanceResult attendanceResult,
        LocalDateTime checkInAt,
        LocalDateTime checkOutAt,
        Integer lateMinutes,
        Integer earlyLeaveMinutes,
        String note,
        String recordedBy,
        LocalDateTime recordedAt,
        String confirmedBy,
        LocalDateTime confirmedAt,
        BigDecimal basePaySnapshot,
        BigDecimal payableAmount
) {
}
