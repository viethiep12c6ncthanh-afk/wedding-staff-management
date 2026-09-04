package com.viethiep.weddingstaff.service;

import com.viethiep.weddingstaff.dto.*;
import com.viethiep.weddingstaff.entity.*;
import com.viethiep.weddingstaff.enumtype.*;
import com.viethiep.weddingstaff.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReplacementServiceTest {
    @Mock
    private ReplacementRequestRepository requestRepository;
    @Mock
    private ReplacementInvitationRepository invitationRepository;
    @Mock
    private ShiftAssignmentRepository assignmentRepository;
    @Mock
    private WorkShiftRepository shiftRepository;
    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private UserAccountRepository userRepository;

    @InjectMocks
    private ReplacementService service;

    private UserAccount employeeUser;
    private UserAccount managerUser;
    private Employee originalEmployee;
    private WorkShift shift;
    private ShiftArea originalArea;
    private ShiftTable originalTable;
    private ShiftAssignment originalAssignment;

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
                .shiftStatus(ShiftStatus.CLOSED)
                .build();
        employeeUser = UserAccount.builder()
                .id(10L)
                .username("employee")
                .fullName("Nhân viên gốc")
                .build();
        managerUser = UserAccount.builder()
                .id(11L)
                .username("admin")
                .fullName("Quản trị viên")
                .build();
        originalEmployee = Employee.builder()
                .id(20L)
                .user(employeeUser)
                .employmentStatus(EmployeeStatus.ACTIVE)
                .employeeCode("NV020")
                .build();
        originalArea = ShiftArea.builder()
                .id(80L)
                .shift(shift)
                .name("Sảnh A")
                .requiredStaff(2)
                .areaStatus(CommonStatus.ACTIVE)
                .build();
        originalTable = ShiftTable.builder()
                .id(81L)
                .area(originalArea)
                .tableCode("A01")
                .tableStatus(CommonStatus.ACTIVE)
                .build();
        originalAssignment = ShiftAssignment.builder()
                .id(30L)
                .shift(shift)
                .employee(originalEmployee)
                .assignmentSource(AssignmentSource.DIRECT)
                .shiftRole(ShiftRole.LEADER)
                .shiftArea(originalArea)
                .tables(new LinkedHashSet<>(List.of(originalTable)))
                .task("Điều phối bàn")
                .status(AssignmentStatus.ASSIGNED)
                .assignedBy(managerUser)
                .build();
    }

    @Test
    void employeeCreatesPendingReplacementRequestWithoutCancellingAssignment() {
        when(assignmentRepository.findByIdForUpdate(30L))
                .thenReturn(Optional.of(originalAssignment));
        when(employeeRepository.findByUserUsername("employee"))
                .thenReturn(Optional.of(originalEmployee));
        when(requestRepository.existsByOriginalAssignmentIdAndStatusIn(eq(30L), anyCollection()))
                .thenReturn(false);
        when(userRepository.findByUsername("employee"))
                .thenReturn(Optional.of(employeeUser));
        when(requestRepository.save(any(ReplacementRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ReplacementRequestResponse response = service.createRequest(
                new CreateReplacementRequest(30L, "Có việc gia đình"),
                "employee"
        );

        assertEquals(ReplacementRequestStatus.PENDING, response.status());
        assertEquals(AssignmentStatus.ASSIGNED, originalAssignment.getStatus());
        verify(assignmentRepository, never()).save(any());
    }

    @Test
    void employeeCannotCreateReplacementRequestForAnotherEmployeeAssignment() {
        Employee otherEmployee = Employee.builder()
                .id(99L)
                .user(UserAccount.builder().username("other").build())
                .employmentStatus(EmployeeStatus.ACTIVE)
                .build();
        when(assignmentRepository.findByIdForUpdate(30L))
                .thenReturn(Optional.of(originalAssignment));
        when(employeeRepository.findByUserUsername("other"))
                .thenReturn(Optional.of(otherEmployee));

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> service.createRequest(
                        new CreateReplacementRequest(30L, "Xin thay ca"),
                        "other"
                )
        );

        assertEquals(
                "Chỉ được gửi yêu cầu thay ca cho phân công của chính bạn",
                ex.getMessage()
        );
    }

    @Test
    void managerApprovalCancelsOriginalAssignmentAndApprovedRegistration() {
        ShiftRegistration registration = ShiftRegistration.builder()
                .id(40L)
                .shift(shift)
                .employee(originalEmployee)
                .status(RegistrationStatus.APPROVED)
                .build();
        originalAssignment.setRegistration(registration);
        originalAssignment.setAssignmentSource(AssignmentSource.REGISTRATION);
        ReplacementRequest request = openableRequest(50L, ReplacementRequestStatus.PENDING);

        when(requestRepository.findByIdForUpdate(50L)).thenReturn(Optional.of(request));
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(managerUser));
        when(assignmentRepository.findByIdForUpdate(30L))
                .thenReturn(Optional.of(originalAssignment));
        when(invitationRepository.findAllByRequestIdWithDetails(50L)).thenReturn(List.of());

        ReplacementRequestResponse response = service.reviewRequest(
                50L,
                new ReviewReplacementRequest(true, "Đồng ý cho thay ca"),
                "admin"
        );

        assertEquals(ReplacementRequestStatus.OPEN, response.status());
        assertEquals(AssignmentStatus.CANCELLED, originalAssignment.getStatus());
        assertEquals(RegistrationStatus.CANCELLED, registration.getStatus());
        assertEquals("admin", originalAssignment.getCancelledBy().getUsername());
        assertTrue(originalAssignment.getCancellationReason().contains("Yêu cầu thay ca #50"));
    }

    @Test
    void managerRejectionKeepsOriginalAssignmentActive() {
        ReplacementRequest request = openableRequest(51L, ReplacementRequestStatus.PENDING);
        when(requestRepository.findByIdForUpdate(51L)).thenReturn(Optional.of(request));
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(managerUser));
        when(invitationRepository.findAllByRequestIdWithDetails(51L)).thenReturn(List.of());

        ReplacementRequestResponse response = service.reviewRequest(
                51L,
                new ReviewReplacementRequest(false, "Không đủ căn cứ"),
                "admin"
        );

        assertEquals(ReplacementRequestStatus.REJECTED, response.status());
        assertEquals(AssignmentStatus.ASSIGNED, originalAssignment.getStatus());
        assertEquals("Không đủ căn cứ", response.closedReason());
        verify(assignmentRepository, never()).findByIdForUpdate(30L);
    }

    @Test
    void managerCannotInviteOriginalEmployee() {
        ReplacementRequest request = openableRequest(52L, ReplacementRequestStatus.OPEN);
        when(requestRepository.findByIdForUpdate(52L)).thenReturn(Optional.of(request));
        when(shiftRepository.findByIdForUpdate(3L)).thenReturn(Optional.of(shift));
        when(employeeRepository.findByIdWithUser(20L)).thenReturn(Optional.of(originalEmployee));

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> service.inviteEmployee(
                        52L,
                        new InviteReplacementRequest(20L),
                        "admin"
                )
        );

        assertEquals("Không thể mời chính nhân viên đang xin thay ca", ex.getMessage());
    }

    @Test
    void managerCannotInviteEmployeeWithOverlappingAssignment() {
        ReplacementRequest request = openableRequest(53L, ReplacementRequestStatus.OPEN);
        Employee candidate = candidate(21L, "candidate");
        when(requestRepository.findByIdForUpdate(53L)).thenReturn(Optional.of(request));
        when(shiftRepository.findByIdForUpdate(3L)).thenReturn(Optional.of(shift));
        when(employeeRepository.findByIdWithUser(21L)).thenReturn(Optional.of(candidate));
        when(assignmentRepository.existsByShiftIdAndEmployeeIdAndStatusIn(eq(3L), eq(21L), anyCollection()))
                .thenReturn(false);
        when(assignmentRepository.findOverlaps(eq(21L), any(), any(), anyCollection()))
                .thenReturn(List.of(ShiftAssignment.builder().id(99L).build()));

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> service.inviteEmployee(
                        53L,
                        new InviteReplacementRequest(21L),
                        "admin"
                )
        );

        assertEquals("Nhân viên bị trùng với ca đã được phân công", ex.getMessage());
        verify(invitationRepository, never()).save(any());
    }

    @Test
    void employeeDeclinesInvitationAndRequestRemainsOpen() {
        Employee candidate = candidate(21L, "candidate");
        ReplacementRequest request = openableRequest(54L, ReplacementRequestStatus.OPEN);
        ReplacementInvitation invitation = invitation(60L, request, candidate);
        when(invitationRepository.findByIdForUpdate(60L)).thenReturn(Optional.of(invitation));
        when(requestRepository.findByIdForUpdate(54L)).thenReturn(Optional.of(request));

        ReplacementInvitationResponse response = service.respondInvitation(
                60L,
                new RespondReplacementInvitationRequest(false, "Không sắp xếp được"),
                "candidate"
        );

        assertEquals(ReplacementInvitationStatus.DECLINED, response.status());
        assertEquals(ReplacementRequestStatus.OPEN, request.getStatus());
        assertNull(request.getReplacementAssignment());
    }

    @Test
    void employeeAcceptsInvitationCreatesReplacementAssignmentAndFillsRequest() {
        Employee candidate = candidate(21L, "candidate");
        ReplacementRequest request = openableRequest(55L, ReplacementRequestStatus.OPEN);
        ReplacementInvitation accepted = invitation(61L, request, candidate);
        Employee other = candidate(22L, "other");
        ReplacementInvitation otherPending = invitation(62L, request, other);

        when(invitationRepository.findByIdForUpdate(61L)).thenReturn(Optional.of(accepted));
        when(requestRepository.findByIdForUpdate(55L)).thenReturn(Optional.of(request));
        when(shiftRepository.findByIdForUpdate(3L)).thenReturn(Optional.of(shift));
        when(assignmentRepository.existsByShiftIdAndEmployeeIdAndStatusIn(eq(3L), eq(21L), anyCollection()))
                .thenReturn(false);
        when(assignmentRepository.findOverlaps(eq(21L), any(), any(), anyCollection()))
                .thenReturn(List.of());
        when(assignmentRepository.countByShiftIdAndStatusIn(eq(3L), anyCollection()))
                .thenReturn(1L);
        when(assignmentRepository.save(any(ShiftAssignment.class)))
                .thenAnswer(invocation -> {
                    ShiftAssignment saved = invocation.getArgument(0);
                    saved.setId(70L);
                    return saved;
                });
        when(invitationRepository.findAllByRequestIdAndStatusIn(eq(55L), anyCollection()))
                .thenReturn(List.of(accepted, otherPending));

        ReplacementInvitationResponse response = service.respondInvitation(
                61L,
                new RespondReplacementInvitationRequest(true, "Tôi nhận ca"),
                "candidate"
        );

        ArgumentCaptor<ShiftAssignment> assignmentCaptor =
                ArgumentCaptor.forClass(ShiftAssignment.class);
        verify(assignmentRepository).save(assignmentCaptor.capture());
        ShiftAssignment replacement = assignmentCaptor.getValue();

        assertEquals(AssignmentSource.REPLACEMENT, replacement.getAssignmentSource());
        assertEquals(ShiftRole.LEADER, replacement.getShiftRole());
        assertEquals(originalArea, replacement.getShiftArea());
        assertEquals(List.of(originalTable), replacement.getTables().stream().toList());
        assertEquals("Điều phối bàn", replacement.getTask());
        assertEquals(candidate, replacement.getEmployee());
        assertEquals(ReplacementInvitationStatus.ACCEPTED, response.status());
        assertEquals(ReplacementRequestStatus.FILLED, request.getStatus());
        assertEquals(70L, request.getReplacementAssignment().getId());
        assertEquals(ReplacementInvitationStatus.CANCELLED, otherPending.getStatus());
    }


    @Test
    void managerCannotInviteEmployeeWithInactiveAccount() {
        ReplacementRequest request =
                openableRequest(58L, ReplacementRequestStatus.OPEN);
        Employee candidate = candidate(
                21L,
                "candidate",
                null,
                RoleName.EMPLOYEE
        );

        when(requestRepository.findByIdForUpdate(58L))
                .thenReturn(Optional.of(request));
        when(shiftRepository.findByIdForUpdate(3L))
                .thenReturn(Optional.of(shift));
        when(employeeRepository.findByIdWithUser(21L))
                .thenReturn(Optional.of(candidate));

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> service.inviteEmployee(
                        58L,
                        new InviteReplacementRequest(21L),
                        "admin"
                )
        );

        assertEquals(
                "Tài khoản nhân viên đang không hoạt động",
                ex.getMessage()
        );
        verify(invitationRepository, never()).save(any());
    }

    @Test
    void managerCannotInviteAccountWithoutEmployeeRole() {
        ReplacementRequest request =
                openableRequest(59L, ReplacementRequestStatus.OPEN);
        Employee candidate = candidate(
                21L,
                "candidate",
                AccountStatus.ACTIVE,
                RoleName.COORDINATOR
        );

        when(requestRepository.findByIdForUpdate(59L))
                .thenReturn(Optional.of(request));
        when(shiftRepository.findByIdForUpdate(3L))
                .thenReturn(Optional.of(shift));
        when(employeeRepository.findByIdWithUser(21L))
                .thenReturn(Optional.of(candidate));

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> service.inviteEmployee(
                        59L,
                        new InviteReplacementRequest(21L),
                        "admin"
                )
        );

        assertEquals(
                "Tài khoản nhân viên thay ca phải có vai trò EMPLOYEE",
                ex.getMessage()
        );
        verify(invitationRepository, never()).save(any());
    }

    @Test
    void shiftCancellationClosesPendingAndOpenReplacementRequests() {
        ReplacementRequest pending = openableRequest(56L, ReplacementRequestStatus.PENDING);
        ReplacementRequest open = openableRequest(57L, ReplacementRequestStatus.OPEN);
        Employee candidate = candidate(21L, "candidate");
        ReplacementInvitation invitation = invitation(63L, open, candidate);

        when(requestRepository.findAllByOriginalAssignmentShiftIdAndStatusIn(eq(3L), anyCollection()))
                .thenReturn(List.of(pending, open));
        when(invitationRepository.findAllByRequestIdAndStatusIn(eq(56L), anyCollection()))
                .thenReturn(List.of());
        when(invitationRepository.findAllByRequestIdAndStatusIn(eq(57L), anyCollection()))
                .thenReturn(List.of(invitation));

        service.cancelForShift(3L, "Ca bị hủy");

        assertEquals(ReplacementRequestStatus.CANCELLED, pending.getStatus());
        assertEquals(ReplacementRequestStatus.CANCELLED, open.getStatus());
        assertEquals("Ca bị hủy", open.getClosedReason());
        assertEquals(ReplacementInvitationStatus.CANCELLED, invitation.getStatus());
    }

    private ReplacementRequest openableRequest(
            Long id,
            ReplacementRequestStatus status
    ) {
        return ReplacementRequest.builder()
                .id(id)
                .originalAssignment(originalAssignment)
                .requestedBy(employeeUser)
                .reason("Có việc đột xuất")
                .status(status)
                .build();
    }

    private Employee candidate(Long id, String username) {
        return candidate(
                id,
                username,
                AccountStatus.ACTIVE,
                RoleName.EMPLOYEE
        );
    }

    private Employee candidate(
            Long id,
            String username,
            AccountStatus accountStatus,
            RoleName roleName
    ) {
        return Employee.builder()
                .id(id)
                .employeeCode("NV" + id)
                .employmentStatus(EmployeeStatus.ACTIVE)
                .user(UserAccount.builder()
                        .id(id + 100)
                        .username(username)
                        .fullName("Nhân viên " + username)
                        .accountStatus(accountStatus)
                        .role(Role.builder().name(roleName).build())
                        .build())
                .build();
    }

    private ReplacementInvitation invitation(
            Long id,
            ReplacementRequest request,
            Employee employee
    ) {
        return ReplacementInvitation.builder()
                .id(id)
                .request(request)
                .employee(employee)
                .status(ReplacementInvitationStatus.PENDING)
                .invitedBy(managerUser)
                .invitedAt(LocalDateTime.now())
                .build();
    }
}
