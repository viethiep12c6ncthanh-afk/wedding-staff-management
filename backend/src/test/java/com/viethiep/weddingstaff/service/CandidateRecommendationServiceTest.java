package com.viethiep.weddingstaff.service;

import com.viethiep.weddingstaff.dto.ReplacementCandidateResponse;
import com.viethiep.weddingstaff.entity.*;
import com.viethiep.weddingstaff.enumtype.*;
import com.viethiep.weddingstaff.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CandidateRecommendationServiceTest {
    @Mock
    private ReplacementRequestRepository requestRepository;
    @Mock
    private ReplacementInvitationRepository invitationRepository;
    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private EmployeeReputationRepository reputationRepository;
    @Mock
    private ShiftAssignmentRepository assignmentRepository;

    @InjectMocks
    private CandidateRecommendationService service;

    private WorkShift shift;
    private Employee originalEmployee;
    private ShiftAssignment originalAssignment;
    private ReplacementRequest openRequest;

    @BeforeEach
    void setUp() {
        Venue venue = Venue.builder()
                .id(1L)
                .name("Riverside")
                .address("TP.HCM")
                .build();
        Event event = Event.builder()
                .id(2L)
                .venue(venue)
                .name("Tiệc cưới")
                .startAt(LocalDateTime.now().plusDays(2))
                .endAt(LocalDateTime.now().plusDays(2).plusHours(8))
                .eventStatus(EventStatus.CONFIRMED)
                .build();
        shift = WorkShift.builder()
                .id(3L)
                .event(event)
                .name("Ca tối")
                .startAt(LocalDateTime.now().plusDays(2).plusHours(1))
                .endAt(LocalDateTime.now().plusDays(2).plusHours(5))
                .requiredStaff(3)
                .shiftStatus(ShiftStatus.OPEN)
                .build();
        originalEmployee = candidate(20L, "NV020", "original");
        originalAssignment = ShiftAssignment.builder()
                .id(30L)
                .shift(shift)
                .employee(originalEmployee)
                .assignmentSource(AssignmentSource.DIRECT)
                .shiftRole(ShiftRole.STAFF)
                .status(AssignmentStatus.CANCELLED)
                .build();
        openRequest = ReplacementRequest.builder()
                .id(40L)
                .originalAssignment(originalAssignment)
                .requestedBy(originalEmployee.getUser())
                .reason("Có việc đột xuất")
                .status(ReplacementRequestStatus.OPEN)
                .build();
    }

    @Test
    void ranksCandidatesByTransparentDeterministicScore() {
        Employee first = candidate(21L, "NV021", "first");
        Employee second = candidate(22L, "NV022", "second");
        stubBase(List.of(first, second));
        when(reputationRepository.findAllWithEmployee()).thenReturn(List.of(
                reputation(first, 90, 10, 1, 1, 0),
                reputation(second, 80, 15, 0, 0, 0)
        ));
        when(assignmentRepository.countByEmployeeIdAndStatus(
                21L, AssignmentStatus.COMPLETED
        )).thenReturn(10L);
        when(assignmentRepository.countByEmployeeIdAndStatus(
                22L, AssignmentStatus.COMPLETED
        )).thenReturn(15L);
        when(assignmentRepository.countByEmployeeIdAndShiftRoleAndStatus(
                21L, ShiftRole.STAFF, AssignmentStatus.COMPLETED
        )).thenReturn(3L);
        when(assignmentRepository.countByEmployeeIdAndShiftRoleAndStatus(
                22L, ShiftRole.STAFF, AssignmentStatus.COMPLETED
        )).thenReturn(5L);

        List<ReplacementCandidateResponse> result = service.findCandidates(40L);

        assertEquals(2, result.size());
        assertEquals(22L, result.get(0).employeeId());
        assertEquals(1, result.get(0).rank());
        assertEquals(90, result.get(0).totalScore());
        assertEquals(21L, result.get(1).employeeId());
        assertEquals(85, result.get(1).totalScore());
        assertEquals(95, result.get(1).reliabilityPercent());
        assertFalse(result.get(0).reasons().isEmpty());
    }

    @Test
    void excludesOriginalInvitedAndBusyEmployeesBeforeScoring() {
        Employee invited = candidate(21L, "NV021", "invited");
        Employee busy = candidate(22L, "NV022", "busy");
        Employee eligible = candidate(23L, "NV023", "eligible");
        when(requestRepository.findByIdWithDetails(40L)).thenReturn(Optional.of(openRequest));
        when(invitationRepository.findAllByRequestIdWithDetails(40L)).thenReturn(List.of(
                ReplacementInvitation.builder()
                        .id(50L)
                        .request(openRequest)
                        .employee(invited)
                        .status(ReplacementInvitationStatus.DECLINED)
                        .build()
        ));
        when(assignmentRepository.findAllForCoordinationWindow(
                eq(shift.getStartAt()), eq(shift.getEndAt()), anyCollection()
        )).thenReturn(List.of(
                ShiftAssignment.builder()
                        .id(60L)
                        .shift(shift)
                        .employee(busy)
                        .status(AssignmentStatus.ASSIGNED)
                        .build()
        ));
        when(employeeRepository.findCandidatePool(
                EmployeeStatus.ACTIVE,
                AccountStatus.ACTIVE,
                RoleName.EMPLOYEE
        )).thenReturn(List.of(originalEmployee, invited, busy, eligible));
        when(reputationRepository.findAllWithEmployee()).thenReturn(List.of(
                reputation(eligible, 80, 0, 0, 0, 0)
        ));
        when(assignmentRepository.countByEmployeeIdAndStatus(
                23L, AssignmentStatus.COMPLETED
        )).thenReturn(0L);
        when(assignmentRepository.countByEmployeeIdAndShiftRoleAndStatus(
                23L, ShiftRole.STAFF, AssignmentStatus.COMPLETED
        )).thenReturn(0L);

        List<ReplacementCandidateResponse> result = service.findCandidates(40L);

        assertEquals(1, result.size());
        assertEquals(23L, result.get(0).employeeId());
        verify(assignmentRepository, never())
                .countByEmployeeIdAndShiftRoleAndStatus(
                        eq(21L), any(), any()
                );
        verify(assignmentRepository, never())
                .countByEmployeeIdAndShiftRoleAndStatus(
                        eq(22L), any(), any()
                );
    }

    @Test
    void usesNeutralReliabilityWhenEmployeeHasNoAttendanceHistory() {
        Employee newcomer = candidate(21L, "NV021", "newcomer");
        stubBase(List.of(newcomer));
        when(reputationRepository.findAllWithEmployee()).thenReturn(List.of(
                reputation(newcomer, 80, 0, 0, 0, 0)
        ));
        when(assignmentRepository.countByEmployeeIdAndStatus(
                21L, AssignmentStatus.COMPLETED
        )).thenReturn(0L);
        when(assignmentRepository.countByEmployeeIdAndShiftRoleAndStatus(
                21L, ShiftRole.STAFF, AssignmentStatus.COMPLETED
        )).thenReturn(0L);

        ReplacementCandidateResponse candidate = service.findCandidates(40L).get(0);

        assertEquals(80, candidate.reliabilityPercent());
        assertEquals(20, candidate.reliabilityPoints());
        assertEquals(60, candidate.totalScore());
    }

    @Test
    void fallsBackToBaselineReputationWhenAggregateIsMissing() {
        Employee candidate = candidate(21L, "NV021", "candidate");
        stubBase(List.of(candidate));
        when(reputationRepository.findAllWithEmployee()).thenReturn(List.of());
        when(assignmentRepository.countByEmployeeIdAndStatus(
                21L, AssignmentStatus.COMPLETED
        )).thenReturn(0L);
        when(assignmentRepository.countByEmployeeIdAndShiftRoleAndStatus(
                21L, ShiftRole.STAFF, AssignmentStatus.COMPLETED
        )).thenReturn(0L);

        ReplacementCandidateResponse response = service.findCandidates(40L).get(0);

        assertEquals(80, response.reputationScore());
        assertEquals(40, response.reputationPoints());
        assertEquals(60, response.totalScore());
    }

    @Test
    void stableTieBreakUsesEmployeeCodeThenId() {
        Employee b = candidate(22L, "NVB", "b");
        Employee a2 = candidate(23L, "NVA", "a2");
        Employee a1 = candidate(21L, "NVA", "a1");
        stubBase(List.of(b, a2, a1));
        when(reputationRepository.findAllWithEmployee()).thenReturn(List.of(
                reputation(b, 80, 0, 0, 0, 0),
                reputation(a2, 80, 0, 0, 0, 0),
                reputation(a1, 80, 0, 0, 0, 0)
        ));
        when(assignmentRepository.countByEmployeeIdAndStatus(
                anyLong(), eq(AssignmentStatus.COMPLETED)
        )).thenReturn(0L);
        when(assignmentRepository.countByEmployeeIdAndShiftRoleAndStatus(
                anyLong(), eq(ShiftRole.STAFF), eq(AssignmentStatus.COMPLETED)
        )).thenReturn(0L);

        List<ReplacementCandidateResponse> result = service.findCandidates(40L);

        assertEquals(List.of(21L, 23L, 22L), result.stream()
                .map(ReplacementCandidateResponse::employeeId)
                .toList());
    }

    @Test
    void experienceCountsCompletedAssignmentsIndependentlyFromAttendanceAggregate() {
        Employee worker = candidate(21L, "NV021", "worker");
        stubBase(List.of(worker));
        when(reputationRepository.findAllWithEmployee()).thenReturn(List.of(
                reputation(worker, 80, 0, 0, 0, 0)
        ));
        when(assignmentRepository.countByEmployeeIdAndStatus(
                21L, AssignmentStatus.COMPLETED
        )).thenReturn(1L);
        when(assignmentRepository.countByEmployeeIdAndShiftRoleAndStatus(
                21L, ShiftRole.STAFF, AssignmentStatus.COMPLETED
        )).thenReturn(1L);

        ReplacementCandidateResponse response = service.findCandidates(40L).get(0);

        assertEquals(80, response.reliabilityPercent());
        assertEquals(1, response.completedShiftCount());
        assertEquals(1, response.experiencePoints());
        assertEquals(1, response.sameRoleCompletedCount());
        assertEquals(2, response.sameRolePoints());
        assertEquals(63, response.totalScore());
    }

    @Test
    void rejectsCandidateRecommendationWhenRequestIsNotOpen() {
        openRequest.setStatus(ReplacementRequestStatus.PENDING);
        when(requestRepository.findByIdWithDetails(40L)).thenReturn(Optional.of(openRequest));

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> service.findCandidates(40L)
        );

        assertEquals(
                "Chỉ gợi ý ứng viên khi yêu cầu thay ca đang tìm người thay",
                ex.getMessage()
        );
        verifyNoInteractions(employeeRepository, reputationRepository);
    }

    private void stubBase(List<Employee> candidates) {
        when(requestRepository.findByIdWithDetails(40L)).thenReturn(Optional.of(openRequest));
        when(invitationRepository.findAllByRequestIdWithDetails(40L)).thenReturn(List.of());
        when(assignmentRepository.findAllForCoordinationWindow(
                eq(shift.getStartAt()), eq(shift.getEndAt()), anyCollection()
        )).thenReturn(List.of());
        when(employeeRepository.findCandidatePool(
                EmployeeStatus.ACTIVE,
                AccountStatus.ACTIVE,
                RoleName.EMPLOYEE
        )).thenReturn(candidates);
    }

    private Employee candidate(Long id, String code, String username) {
        Role role = Role.builder()
                .id(1L)
                .name(RoleName.EMPLOYEE)
                .build();
        UserAccount user = UserAccount.builder()
                .id(id + 100)
                .username(username)
                .fullName("Nhân viên " + username)
                .accountStatus(AccountStatus.ACTIVE)
                .role(role)
                .build();
        return Employee.builder()
                .id(id)
                .employeeCode(code)
                .employmentStatus(EmployeeStatus.ACTIVE)
                .user(user)
                .build();
    }

    private EmployeeReputation reputation(
            Employee employee,
            int score,
            int completed,
            int late,
            int early,
            int absent
    ) {
        return EmployeeReputation.builder()
                .id(employee.getId() + 1000)
                .employee(employee)
                .currentScore(score)
                .completedShiftCount(completed)
                .lateCount(late)
                .earlyLeaveCount(early)
                .absentCount(absent)
                .build();
    }
}
