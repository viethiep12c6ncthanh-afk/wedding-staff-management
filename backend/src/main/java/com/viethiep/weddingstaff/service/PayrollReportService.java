package com.viethiep.weddingstaff.service;

import com.viethiep.weddingstaff.exception.ResourceNotFoundException;
import com.viethiep.weddingstaff.dto.PayrollEmployeeSummary;
import com.viethiep.weddingstaff.dto.PayrollReportResponse;
import com.viethiep.weddingstaff.entity.Attendance;
import com.viethiep.weddingstaff.entity.Employee;
import com.viethiep.weddingstaff.enumtype.AttendanceProcessStatus;
import com.viethiep.weddingstaff.enumtype.AttendanceResult;
import com.viethiep.weddingstaff.repository.AttendanceRepository;
import com.viethiep.weddingstaff.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PayrollReportService {
    private static final BigDecimal ZERO =
            BigDecimal.ZERO.setScale(2);

    private final AttendanceRepository attendanceRepository;
    private final EmployeeRepository employeeRepository;

    @Transactional(readOnly = true)
    public PayrollReportResponse findAll(
            LocalDate from,
            LocalDate to
    ) {
        validateRange(from, to);
        return buildReport(null, from, to);
    }

    @Transactional(readOnly = true)
    public PayrollReportResponse findMine(
            String username,
            LocalDate from,
            LocalDate to
    ) {
        validateRange(from, to);

        Employee employee = employeeRepository
                .findByUserUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ nhân viên"));

        return buildReport(employee.getId(), from, to);
    }

    private PayrollReportResponse buildReport(
            Long employeeId,
            LocalDate from,
            LocalDate to
    ) {
        LocalDateTime fromAt =
                from == null ? null : from.atStartOfDay();
        LocalDateTime toExclusive =
                to == null ? null : to.plusDays(1).atStartOfDay();

        List<Attendance> attendances =
                attendanceRepository.findForPayroll(
                        AttendanceProcessStatus.CONFIRMED,
                        employeeId,
                        fromAt,
                        toExclusive
                );

        Map<Long, EmployeeAccumulator> byEmployee =
                new LinkedHashMap<>();

        long paidShiftCount = 0;
        long absentShiftCount = 0;
        BigDecimal totalBasePay = ZERO;
        BigDecimal totalPayable = ZERO;

        for (Attendance attendance : attendances) {
            BigDecimal basePay = zeroIfNull(
                    attendance.getBasePaySnapshot()
            );
            BigDecimal payable = zeroIfNull(
                    attendance.getPayableAmount()
            );

            totalBasePay = totalBasePay.add(basePay);
            totalPayable = totalPayable.add(payable);

            boolean absent = attendance.getAttendanceResult()
                    == AttendanceResult.ABSENT;
            if (absent) {
                absentShiftCount++;
            } else {
                paidShiftCount++;
            }

            Employee employee =
                    attendance.getAssignment().getEmployee();

            EmployeeAccumulator accumulator =
                    byEmployee.computeIfAbsent(
                            employee.getId(),
                            ignored -> new EmployeeAccumulator(
                                    employee.getId(),
                                    employee.getEmployeeCode(),
                                    employee.getUser().getFullName()
                            )
                    );

            accumulator.add(basePay, payable, absent);
        }

        List<PayrollEmployeeSummary> employees =
                byEmployee.values().stream()
                        .map(EmployeeAccumulator::toResponse)
                        .toList();

        return new PayrollReportResponse(
                from,
                to,
                attendances.size(),
                paidShiftCount,
                absentShiftCount,
                totalBasePay,
                totalPayable,
                employees
        );
    }

    private void validateRange(LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException(
                    "Ngày bắt đầu không được sau ngày kết thúc"
            );
        }
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? ZERO : value;
    }

    private static final class EmployeeAccumulator {
        private final Long employeeId;
        private final String employeeCode;
        private final String fullName;

        private long confirmedShiftCount;
        private long paidShiftCount;
        private long absentShiftCount;
        private BigDecimal totalBasePay = ZERO;
        private BigDecimal totalPayable = ZERO;

        private EmployeeAccumulator(
                Long employeeId,
                String employeeCode,
                String fullName
        ) {
            this.employeeId = employeeId;
            this.employeeCode = employeeCode;
            this.fullName = fullName;
        }

        private void add(
                BigDecimal basePay,
                BigDecimal payable,
                boolean absent
        ) {
            confirmedShiftCount++;
            totalBasePay = totalBasePay.add(basePay);
            totalPayable = totalPayable.add(payable);

            if (absent) {
                absentShiftCount++;
            } else {
                paidShiftCount++;
            }
        }

        private PayrollEmployeeSummary toResponse() {
            return new PayrollEmployeeSummary(
                    employeeId,
                    employeeCode,
                    fullName,
                    confirmedShiftCount,
                    paidShiftCount,
                    absentShiftCount,
                    totalBasePay,
                    totalPayable
            );
        }
    }
}
