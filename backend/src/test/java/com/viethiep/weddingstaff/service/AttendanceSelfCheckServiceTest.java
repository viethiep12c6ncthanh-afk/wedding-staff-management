package com.viethiep.weddingstaff.service;

import com.viethiep.weddingstaff.dto.AttendanceResponse;
import com.viethiep.weddingstaff.entity.*;
import com.viethiep.weddingstaff.enumtype.*;
import com.viethiep.weddingstaff.repository.AttendanceRepository;
import com.viethiep.weddingstaff.repository.ShiftAssignmentRepository;
import com.viethiep.weddingstaff.repository.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AttendanceSelfCheckServiceTest {
    @Mock
    private AttendanceRepository attendanceRepository;
    @Mock
    private ShiftAssignmentRepository assignmentRepository;
    @Mock
    private UserAccountRepository userRepository;
    @Mock
    private ReputationService reputationService;

    @InjectMocks
    private AttendanceService service;

    @Test
    void selfCheckInCreatesDraftWithoutConfirmingAttendance() {
        LocalDateTime shiftStart = LocalDateTime.of(2026, 8, 30, 18, 0);
        ShiftAssignment assignment = assignment(9L, shiftStart);
        UserAccount employee = assignment.getEmployee().getUser();
        LocalDateTime checkInAt = shiftStart.plusMinutes(7);

        when(attendanceRepository.findByAssignmentIdForUpdate(9L))
                .thenReturn(Optional.empty());
        when(attendanceRepository.save(any(Attendance.class)))
                .thenAnswer(invocation -> {
                    Attendance attendance = invocation.getArgument(0);
                    attendance.setId(41L);
                    return attendance;
                });

        AttendanceResponse response = service.selfCheckIn(
                assignment,
                employee,
                checkInAt
        );

        assertEquals(AttendanceProcessStatus.DRAFT, response.processStatus());
        assertEquals(checkInAt, response.checkInAt());
        assertNull(response.checkOutAt());
        assertNull(response.attendanceResult());
        assertEquals(7, response.lateMinutes());
        assertNull(response.confirmedAt());
        assertNull(response.payableAmount());
    }

    @Test
    void selfCheckOutCompletesDraftTimesAndCalculatesResult() {
        LocalDateTime shiftStart = LocalDateTime.of(2026, 8, 30, 18, 0);
        ShiftAssignment assignment = assignment(9L, shiftStart);
        UserAccount employee = assignment.getEmployee().getUser();

        Attendance attendance = Attendance.builder()
                .id(41L)
                .assignment(assignment)
                .processStatus(AttendanceProcessStatus.DRAFT)
                .checkInAt(shiftStart.plusMinutes(7))
                .lateMinutes(7)
                .earlyLeaveMinutes(0)
                .recordedBy(employee)
                .recordedAt(shiftStart.plusMinutes(7))
                .build();

        when(attendanceRepository.findByAssignmentIdForUpdate(9L))
                .thenReturn(Optional.of(attendance));
        when(attendanceRepository.save(attendance))
                .thenReturn(attendance);

        AttendanceResponse response = service.selfCheckOut(
                assignment,
                employee,
                shiftStart.plusMinutes(50)
        );

        assertEquals(AttendanceProcessStatus.DRAFT, response.processStatus());
        assertEquals(AttendanceResult.LATE_AND_EARLY_LEAVE, response.attendanceResult());
        assertEquals(7, response.lateMinutes());
        assertEquals(10, response.earlyLeaveMinutes());
        assertNull(response.payableAmount());
    }

    @Test
    void selfCheckInRejectsDuplicate() {
        LocalDateTime shiftStart = LocalDateTime.of(2026, 8, 30, 18, 0);
        ShiftAssignment assignment = assignment(9L, shiftStart);
        UserAccount employee = assignment.getEmployee().getUser();

        Attendance attendance = Attendance.builder()
                .id(41L)
                .assignment(assignment)
                .processStatus(AttendanceProcessStatus.DRAFT)
                .checkInAt(shiftStart.plusMinutes(2))
                .lateMinutes(2)
                .earlyLeaveMinutes(0)
                .recordedBy(employee)
                .recordedAt(shiftStart.plusMinutes(2))
                .build();

        when(attendanceRepository.findByAssignmentIdForUpdate(9L))
                .thenReturn(Optional.of(attendance));

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> service.selfCheckIn(
                        assignment,
                        employee,
                        shiftStart.plusMinutes(5)
                )
        );

        assertEquals("Nhân viên đã check-in cho ca này", ex.getMessage());
        verify(attendanceRepository, never()).save(any());
    }

    private ShiftAssignment assignment(
            Long id,
            LocalDateTime shiftStart
    ) {
        UserAccount employeeUser = user("employee", RoleName.EMPLOYEE);
        Employee employee = Employee.builder()
                .id(7L)
                .employeeCode("NV007")
                .employmentStatus(EmployeeStatus.ACTIVE)
                .user(employeeUser)
                .build();

        WorkShift shift = WorkShift.builder()
                .id(3L)
                .name("Ca QR")
                .startAt(shiftStart)
                .endAt(shiftStart.plusHours(1))
                .requiredStaff(2)
                .payAmount(new BigDecimal("100000.00"))
                .shiftStatus(ShiftStatus.OPEN)
                .build();

        return ShiftAssignment.builder()
                .id(id)
                .shift(shift)
                .employee(employee)
                .assignmentSource(AssignmentSource.DIRECT)
                .shiftRole(ShiftRole.STAFF)
                .status(AssignmentStatus.ASSIGNED)
                .assignedBy(user("admin", RoleName.ADMIN))
                .build();
    }

    private UserAccount user(String username, RoleName roleName) {
        return UserAccount.builder()
                .id(roleName == RoleName.EMPLOYEE ? 7L : 1L)
                .username(username)
                .fullName(username)
                .role(Role.builder().name(roleName).build())
                .accountStatus(AccountStatus.ACTIVE)
                .build();
    }
}
