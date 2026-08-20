package com.viethiep.weddingstaff.service;

import com.viethiep.weddingstaff.dto.AttendanceResponse;
import com.viethiep.weddingstaff.dto.CreateAttendanceRequest;
import com.viethiep.weddingstaff.dto.UpdateAttendanceRequest;
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
class AttendanceServiceTest {
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
    void rejectsSecondAttendanceForSameAssignment() {
        ShiftAssignment assignment = assignment(6L, AssignmentStatus.ASSIGNED);
        when(assignmentRepository.findByIdForUpdate(6L))
                .thenReturn(Optional.of(assignment));
        when(attendanceRepository.existsByAssignment_Id(6L))
                .thenReturn(true);

        CreateAttendanceRequest request = new CreateAttendanceRequest(
                6L,
                LocalDateTime.of(2026, 8, 10, 22, 0),
                LocalDateTime.of(2026, 8, 10, 23, 0),
                false,
                null
        );

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> service.create(request, "admin")
        );

        assertEquals("Phân công này đã có bản ghi chấm công", ex.getMessage());
        verify(attendanceRepository, never()).save(any());
    }

    @Test
    void calculatesLateAndEarlyLeaveWhenCreatingDraft() {
        ShiftAssignment assignment = assignment(6L, AssignmentStatus.ASSIGNED);
        UserAccount admin = user("admin", "Quản trị viên", RoleName.ADMIN);

        when(assignmentRepository.findByIdForUpdate(6L))
                .thenReturn(Optional.of(assignment));
        when(attendanceRepository.existsByAssignment_Id(6L))
                .thenReturn(false);
        when(userRepository.findByUsername("admin"))
                .thenReturn(Optional.of(admin));
        when(attendanceRepository.save(any(Attendance.class)))
                .thenAnswer(invocation -> {
                    Attendance attendance = invocation.getArgument(0);
                    attendance.setId(1L);
                    return attendance;
                });

        CreateAttendanceRequest request = new CreateAttendanceRequest(
                6L,
                LocalDateTime.of(2026, 8, 10, 22, 10),
                LocalDateTime.of(2026, 8, 10, 22, 50),
                false,
                "Đi trễ và về sớm"
        );

        AttendanceResponse response = service.create(request, "admin");

        assertEquals(AttendanceProcessStatus.DRAFT, response.processStatus());
        assertEquals(AttendanceResult.LATE_AND_EARLY_LEAVE, response.attendanceResult());
        assertEquals(10, response.lateMinutes());
        assertEquals(10, response.earlyLeaveMinutes());
        assertNull(response.payableAmount());
    }

    @Test
    void confirmedAbsentPaysZeroAndMarksAssignmentAbsent() {
        ShiftAssignment assignment = assignment(1L, AssignmentStatus.ASSIGNED);
        UserAccount admin = user("admin", "Quản trị viên", RoleName.ADMIN);
        Attendance attendance = Attendance.builder()
                .id(2L)
                .assignment(assignment)
                .processStatus(AttendanceProcessStatus.DRAFT)
                .attendanceResult(AttendanceResult.ABSENT)
                .lateMinutes(0)
                .earlyLeaveMinutes(0)
                .recordedBy(admin)
                .recordedAt(LocalDateTime.now())
                .build();

        when(attendanceRepository.findByIdForUpdate(2L))
                .thenReturn(Optional.of(attendance));
        when(assignmentRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(assignment));
        when(userRepository.findByUsername("admin"))
                .thenReturn(Optional.of(admin));

        AttendanceResponse response = service.confirm(2L, "admin");

        assertEquals(AttendanceProcessStatus.CONFIRMED, response.processStatus());
        assertEquals(new BigDecimal("100000.00"), response.basePaySnapshot());
        assertEquals(new BigDecimal("0.00"), response.payableAmount());
        assertEquals(AssignmentStatus.ABSENT, assignment.getStatus());
        verify(reputationService).applyConfirmedAttendance(attendance, admin);
    }

    @Test
    void cannotUpdateConfirmedAttendance() {
        Attendance attendance = Attendance.builder()
                .id(2L)
                .processStatus(AttendanceProcessStatus.CONFIRMED)
                .build();
        when(attendanceRepository.findByIdForUpdate(2L))
                .thenReturn(Optional.of(attendance));

        UpdateAttendanceRequest request = new UpdateAttendanceRequest(
                null,
                null,
                true,
                "thử sửa"
        );

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> service.update(2L, request, "admin")
        );

        assertEquals("Chấm công đã xác nhận nên không thể chỉnh sửa", ex.getMessage());
    }

    private ShiftAssignment assignment(Long id, AssignmentStatus status) {
        UserAccount employeeUser = user("employee", "Nhân viên mẫu", RoleName.EMPLOYEE);
        Employee employee = Employee.builder()
                .id(1L)
                .employeeCode("NV001")
                .employmentStatus(EmployeeStatus.ACTIVE)
                .user(employeeUser)
                .build();
        WorkShift shift = WorkShift.builder()
                .id(3L)
                .name("Ca thu dọn sau tiệc")
                .startAt(LocalDateTime.of(2026, 8, 10, 22, 0))
                .endAt(LocalDateTime.of(2026, 8, 10, 23, 0))
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
                .status(status)
                .assignedBy(user("admin", "Quản trị viên", RoleName.ADMIN))
                .build();
    }

    private UserAccount user(String username, String fullName, RoleName roleName) {
        Role role = Role.builder().name(roleName).build();
        return UserAccount.builder()
                .username(username)
                .fullName(fullName)
                .role(role)
                .accountStatus(AccountStatus.ACTIVE)
                .build();
    }
}
