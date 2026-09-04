package com.viethiep.weddingstaff.service;

import com.viethiep.weddingstaff.dto.CreateEmployeeEvaluationRequest;
import com.viethiep.weddingstaff.entity.*;
import com.viethiep.weddingstaff.enumtype.*;
import com.viethiep.weddingstaff.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReputationServiceTest {
    @Mock
    private EmployeeReputationRepository reputationRepository;
    @Mock
    private ReputationEventRepository eventRepository;
    @Mock
    private EmployeeEvaluationRepository evaluationRepository;
    @Mock
    private ShiftAssignmentRepository assignmentRepository;
    @Mock
    private UserAccountRepository userRepository;

    @InjectMocks
    private ReputationService service;

    @Test
    void initializesEmployeeAtNeutralScoreWithBaselineEvent() {
        Employee employee = employee(7L);
        when(reputationRepository.findByEmployeeIdWithEmployee(7L))
                .thenReturn(Optional.empty());
        when(reputationRepository.save(any(EmployeeReputation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.initializeEmployee(employee);

        ArgumentCaptor<EmployeeReputation> reputationCaptor =
                ArgumentCaptor.forClass(EmployeeReputation.class);
        verify(reputationRepository).save(reputationCaptor.capture());
        assertEquals(80, reputationCaptor.getValue().getCurrentScore());

        ArgumentCaptor<ReputationEvent> eventCaptor =
                ArgumentCaptor.forClass(ReputationEvent.class);
        verify(eventRepository).save(eventCaptor.capture());
        ReputationEvent event = eventCaptor.getValue();
        assertEquals(ReputationSourceType.BASELINE, event.getSourceType());
        assertEquals(ReputationEventType.BASELINE_INITIALIZED, event.getEventType());
        assertEquals(80, event.getScoreBefore());
        assertEquals(0, event.getScoreDelta());
        assertEquals(80, event.getScoreAfter());
    }

    @Test
    void confirmedPresentAddsTwoAndCompletedCount() {
        Employee employee = employee(7L);
        EmployeeReputation reputation = reputation(employee, 80);
        Attendance attendance = attendance(
                21L,
                employee,
                AttendanceResult.PRESENT,
                0,
                0
        );

        when(eventRepository.existsBySourceTypeAndSourceId(
                ReputationSourceType.ATTENDANCE,
                21L
        )).thenReturn(false);
        when(reputationRepository.findByEmployeeIdForUpdate(7L))
                .thenReturn(Optional.of(reputation));

        service.applyConfirmedAttendance(attendance, admin());

        assertEquals(82, reputation.getCurrentScore());
        assertEquals(1, reputation.getCompletedShiftCount());
        assertEquals(0, reputation.getLateCount());
        verify(eventRepository).save(argThat(event ->
                event.getEventType() == ReputationEventType.ATTENDANCE_PRESENT
                        && event.getScoreDelta() == 2
                        && event.getScoreAfter() == 82
        ));
    }

    @Test
    void lateTwentyMinutesSubtractsTwo() {
        Employee employee = employee(7L);
        EmployeeReputation reputation = reputation(employee, 80);
        Attendance attendance = attendance(
                22L,
                employee,
                AttendanceResult.LATE,
                20,
                0
        );

        when(eventRepository.existsBySourceTypeAndSourceId(
                ReputationSourceType.ATTENDANCE,
                22L
        )).thenReturn(false);
        when(reputationRepository.findByEmployeeIdForUpdate(7L))
                .thenReturn(Optional.of(reputation));

        service.applyConfirmedAttendance(attendance, admin());

        assertEquals(78, reputation.getCurrentScore());
        assertEquals(1, reputation.getCompletedShiftCount());
        assertEquals(1, reputation.getLateCount());
    }

    @Test
    void combinedLateAndEarlyLeaveAddsBothPenalties() {
        Employee employee = employee(7L);
        EmployeeReputation reputation = reputation(employee, 80);
        Attendance attendance = attendance(
                23L,
                employee,
                AttendanceResult.LATE_AND_EARLY_LEAVE,
                40,
                10
        );

        when(eventRepository.existsBySourceTypeAndSourceId(
                ReputationSourceType.ATTENDANCE,
                23L
        )).thenReturn(false);
        when(reputationRepository.findByEmployeeIdForUpdate(7L))
                .thenReturn(Optional.of(reputation));

        service.applyConfirmedAttendance(attendance, admin());

        assertEquals(75, reputation.getCurrentScore());
        assertEquals(1, reputation.getLateCount());
        assertEquals(1, reputation.getEarlyLeaveCount());
        assertEquals(1, reputation.getCompletedShiftCount());
    }

    @Test
    void absentSubtractsTenWithoutCompletedCount() {
        Employee employee = employee(7L);
        EmployeeReputation reputation = reputation(employee, 80);
        Attendance attendance = attendance(
                24L,
                employee,
                AttendanceResult.ABSENT,
                0,
                0
        );

        when(eventRepository.existsBySourceTypeAndSourceId(
                ReputationSourceType.ATTENDANCE,
                24L
        )).thenReturn(false);
        when(reputationRepository.findByEmployeeIdForUpdate(7L))
                .thenReturn(Optional.of(reputation));

        service.applyConfirmedAttendance(attendance, admin());

        assertEquals(70, reputation.getCurrentScore());
        assertEquals(1, reputation.getAbsentCount());
        assertEquals(0, reputation.getCompletedShiftCount());
    }

    @Test
    void duplicateAttendanceSourceIsIdempotent() {
        Employee employee = employee(7L);
        Attendance attendance = attendance(
                25L,
                employee,
                AttendanceResult.PRESENT,
                0,
                0
        );
        when(eventRepository.existsBySourceTypeAndSourceId(
                ReputationSourceType.ATTENDANCE,
                25L
        )).thenReturn(true);

        service.applyConfirmedAttendance(attendance, admin());

        verify(reputationRepository, never())
                .findByEmployeeIdForUpdate(anyLong());
        verify(eventRepository, never()).save(any());
    }

    @Test
    void fiveStarEvaluationClampsScoreAtOneHundred() {
        Employee employee = employee(7L);
        ShiftAssignment assignment = completedAssignment(31L, employee);
        EmployeeReputation reputation = reputation(employee, 99);
        UserAccount admin = admin();

        when(assignmentRepository.findByIdForUpdate(31L))
                .thenReturn(Optional.of(assignment));
        when(evaluationRepository.existsByAssignment_Id(31L))
                .thenReturn(false);
        when(userRepository.findByUsername("admin"))
                .thenReturn(Optional.of(admin));
        when(evaluationRepository.save(any(EmployeeEvaluation.class)))
                .thenAnswer(invocation -> {
                    EmployeeEvaluation evaluation = invocation.getArgument(0);
                    evaluation.setId(44L);
                    return evaluation;
                });
        when(reputationRepository.findByEmployeeIdForUpdate(7L))
                .thenReturn(Optional.of(reputation));

        service.createEvaluation(
                new CreateEmployeeEvaluationRequest(31L, 5, "Rất tốt"),
                "admin"
        );

        assertEquals(100, reputation.getCurrentScore());
        assertEquals(1, reputation.getEvaluationCount());
        assertEquals(5, reputation.getRatingSum());

        ArgumentCaptor<ReputationEvent> captor =
                ArgumentCaptor.forClass(ReputationEvent.class);
        verify(eventRepository).save(captor.capture());
        assertEquals(1, captor.getValue().getScoreDelta());
        assertTrue(captor.getValue().getReason().contains("giới hạn 0-100"));
    }

    @Test
    void rejectsEvaluationWhenAssignmentNotCompleted() {
        Employee employee = employee(7L);
        ShiftAssignment assignment = completedAssignment(31L, employee);
        assignment.setStatus(AssignmentStatus.ASSIGNED);
        when(assignmentRepository.findByIdForUpdate(31L))
                .thenReturn(Optional.of(assignment));

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> service.createEvaluation(
                        new CreateEmployeeEvaluationRequest(31L, 5, null),
                        "admin"
                )
        );

        assertEquals(
                "Chỉ được đánh giá phân công đã hoàn thành",
                ex.getMessage()
        );
        verifyNoInteractions(userRepository, reputationRepository);
    }

    @Test
    void rejectsSecondEvaluationForSameAssignment() {
        Employee employee = employee(7L);
        ShiftAssignment assignment = completedAssignment(31L, employee);
        when(assignmentRepository.findByIdForUpdate(31L))
                .thenReturn(Optional.of(assignment));
        when(evaluationRepository.existsByAssignment_Id(31L))
                .thenReturn(true);

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> service.createEvaluation(
                        new CreateEmployeeEvaluationRequest(31L, 4, null),
                        "admin"
                )
        );

        assertEquals("Phân công này đã được đánh giá", ex.getMessage());
        verify(evaluationRepository, never()).save(any());
    }

    private EmployeeReputation reputation(Employee employee, int score) {
        return EmployeeReputation.builder()
                .id(1L)
                .employee(employee)
                .currentScore(score)
                .completedShiftCount(0)
                .lateCount(0)
                .earlyLeaveCount(0)
                .absentCount(0)
                .evaluationCount(0)
                .ratingSum(0)
                .build();
    }

    private Attendance attendance(
            Long id,
            Employee employee,
            AttendanceResult result,
            int lateMinutes,
            int earlyLeaveMinutes
    ) {
        ShiftAssignment assignment = completedAssignment(99L, employee);
        return Attendance.builder()
                .id(id)
                .assignment(assignment)
                .processStatus(AttendanceProcessStatus.CONFIRMED)
                .attendanceResult(result)
                .lateMinutes(lateMinutes)
                .earlyLeaveMinutes(earlyLeaveMinutes)
                .confirmedBy(admin())
                .confirmedAt(LocalDateTime.of(2026, 8, 20, 20, 0))
                .build();
    }

    private ShiftAssignment completedAssignment(Long id, Employee employee) {
        Venue venue = Venue.builder()
                .id(3L)
                .name("Riverside")
                .address("TP.HCM")
                .build();
        Event event = Event.builder()
                .id(4L)
                .name("Tiệc cưới test")
                .venue(venue)
                .build();
        WorkShift shift = WorkShift.builder()
                .id(5L)
                .name("Ca tối")
                .event(event)
                .build();
        return ShiftAssignment.builder()
                .id(id)
                .employee(employee)
                .shift(shift)
                .status(AssignmentStatus.COMPLETED)
                .build();
    }

    private Employee employee(Long id) {
        return Employee.builder()
                .id(id)
                .employeeCode("NV007")
                .employmentStatus(EmployeeStatus.ACTIVE)
                .user(UserAccount.builder()
                        .id(70L)
                        .username("employee7")
                        .fullName("Nhân viên 7")
                        .role(Role.builder().name(RoleName.EMPLOYEE).build())
                        .accountStatus(AccountStatus.ACTIVE)
                        .build())
                .build();
    }

    private UserAccount admin() {
        return UserAccount.builder()
                .id(1L)
                .username("admin")
                .fullName("Quản trị viên")
                .role(Role.builder().name(RoleName.ADMIN).build())
                .accountStatus(AccountStatus.ACTIVE)
                .build();
    }
}
