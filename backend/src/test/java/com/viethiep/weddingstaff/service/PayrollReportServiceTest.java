package com.viethiep.weddingstaff.service;

import com.viethiep.weddingstaff.dto.PayrollReportResponse;
import com.viethiep.weddingstaff.entity.Attendance;
import com.viethiep.weddingstaff.entity.Employee;
import com.viethiep.weddingstaff.entity.ShiftAssignment;
import com.viethiep.weddingstaff.entity.UserAccount;
import com.viethiep.weddingstaff.enumtype.AttendanceProcessStatus;
import com.viethiep.weddingstaff.enumtype.AttendanceResult;
import com.viethiep.weddingstaff.repository.AttendanceRepository;
import com.viethiep.weddingstaff.repository.EmployeeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PayrollReportServiceTest {
    @Mock
    private AttendanceRepository attendanceRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @InjectMocks
    private PayrollReportService service;

    @Test
    void rejectsInvalidDateRange() {
        LocalDate from = LocalDate.of(2026, 8, 31);
        LocalDate to = LocalDate.of(2026, 8, 1);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.findAll(from, to)
        );

        assertEquals("Ngày bắt đầu không được sau ngày kết thúc", ex.getMessage());
        verifyNoInteractions(attendanceRepository);
    }

    @Test
    void aggregatesConfirmedPayrollAndKeepsAbsentBasePay() {
        Employee employee = employee(1L, "NV001", "Nhân viên mẫu");
        Attendance present = attendance(
                employee,
                AttendanceResult.PRESENT,
                "100000.00",
                "10000.00",
                "5000.00",
                "0.00",
                "15000.00",
                "120000.00"
        );
        Attendance absent = attendance(
                employee,
                AttendanceResult.ABSENT,
                "100000.00",
                "0.00",
                "0.00",
                "0.00",
                "0.00",
                "0.00"
        );

        when(attendanceRepository.findForPayroll(
                eq(AttendanceProcessStatus.CONFIRMED),
                isNull(),
                isNull(),
                isNull()
        )).thenReturn(List.of(present, absent));

        PayrollReportResponse response = service.findAll(null, null);

        assertEquals(2, response.confirmedAttendanceCount());
        assertEquals(1, response.paidShiftCount());
        assertEquals(1, response.absentShiftCount());
        assertEquals(new BigDecimal("200000.00"), response.totalBasePay());
        assertEquals(new BigDecimal("10000.00"), response.totalLeaderAllowance());
        assertEquals(new BigDecimal("5000.00"), response.totalLateDeduction());
        assertEquals(new BigDecimal("0.00"), response.totalEarlyLeaveDeduction());
        assertEquals(new BigDecimal("15000.00"), response.totalOvertimePay());
        assertEquals(new BigDecimal("120000.00"), response.totalPayable());
        assertEquals(1, response.employees().size());
        assertEquals(2, response.employees().getFirst().confirmedShiftCount());
        assertEquals(1, response.employees().getFirst().absentShiftCount());
        assertEquals(new BigDecimal("10000.00"),
                response.employees().getFirst().totalLeaderAllowance());
        assertEquals(new BigDecimal("15000.00"),
                response.employees().getFirst().totalOvertimePay());
    }

    @Test
    void mineUsesEmployeeResolvedFromAuthenticatedUsername() {
        Employee employee = employee(9L, "NV009", "Nhân viên 9");
        when(employeeRepository.findByUserUsername("employee9"))
                .thenReturn(Optional.of(employee));
        when(attendanceRepository.findForPayroll(
                eq(AttendanceProcessStatus.CONFIRMED),
                eq(9L),
                isNull(),
                isNull()
        )).thenReturn(List.of());

        PayrollReportResponse response = service.findMine(
                "employee9",
                null,
                null
        );

        assertEquals(0, response.confirmedAttendanceCount());
        verify(attendanceRepository).findForPayroll(
                AttendanceProcessStatus.CONFIRMED,
                9L,
                null,
                null
        );
    }

    private Employee employee(Long id, String code, String fullName) {
        UserAccount user = UserAccount.builder()
                .username(code.toLowerCase())
                .fullName(fullName)
                .build();
        return Employee.builder()
                .id(id)
                .employeeCode(code)
                .user(user)
                .build();
    }

    private Attendance attendance(
            Employee employee,
            AttendanceResult result,
            String basePay,
            String leaderAllowance,
            String lateDeduction,
            String earlyLeaveDeduction,
            String overtimePay,
            String payable
    ) {
        ShiftAssignment assignment = ShiftAssignment.builder()
                .employee(employee)
                .build();
        return Attendance.builder()
                .assignment(assignment)
                .processStatus(AttendanceProcessStatus.CONFIRMED)
                .attendanceResult(result)
                .basePaySnapshot(new BigDecimal(basePay))
                .leaderAllowanceSnapshot(new BigDecimal(leaderAllowance))
                .lateDeductionSnapshot(new BigDecimal(lateDeduction))
                .earlyLeaveDeductionSnapshot(new BigDecimal(earlyLeaveDeduction))
                .overtimePaySnapshot(new BigDecimal(overtimePay))
                .payableAmount(new BigDecimal(payable))
                .build();
    }
}
