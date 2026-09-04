package com.viethiep.weddingstaff.service;

import com.viethiep.weddingstaff.dto.AttendanceCheckSessionResponse;
import com.viethiep.weddingstaff.dto.AttendanceResponse;
import com.viethiep.weddingstaff.dto.CreateAttendanceCheckSessionRequest;
import com.viethiep.weddingstaff.dto.QrAttendanceResultResponse;
import com.viethiep.weddingstaff.dto.SelfAttendanceRequest;
import com.viethiep.weddingstaff.entity.*;
import com.viethiep.weddingstaff.enumtype.*;
import com.viethiep.weddingstaff.repository.AttendanceCheckEventRepository;
import com.viethiep.weddingstaff.repository.AttendanceCheckSessionRepository;
import com.viethiep.weddingstaff.repository.ShiftAssignmentRepository;
import com.viethiep.weddingstaff.repository.UserAccountRepository;
import com.viethiep.weddingstaff.repository.WorkShiftRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QrAttendanceServiceTest {
    @Mock
    private AttendanceCheckSessionRepository sessionRepository;
    @Mock
    private AttendanceCheckEventRepository eventRepository;
    @Mock
    private ShiftAssignmentRepository assignmentRepository;
    @Mock
    private WorkShiftRepository shiftRepository;
    @Mock
    private UserAccountRepository userRepository;
    @Mock
    private AttendanceService attendanceService;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private QrAttendanceService service;

    @Test
    void createsShortLivedCheckInSessionAndDoesNotStoreRawOtp() {
        LocalDateTime now = LocalDateTime.now();
        WorkShift shift = shift(now.minusMinutes(10), now.plusHours(2));
        UserAccount admin = user("admin", RoleName.ADMIN);

        when(userRepository.findByUsername("admin"))
                .thenReturn(Optional.of(admin));
        when(shiftRepository.findByIdForUpdate(3L))
                .thenReturn(Optional.of(shift));
        when(sessionRepository.findActiveForUpdate(
                eq(3L),
                eq(AttendanceCheckAction.CHECK_IN),
                any(LocalDateTime.class)
        )).thenReturn(List.of());
        when(passwordEncoder.encode(anyString()))
                .thenReturn("$2a$10$encodedOtp");
        when(sessionRepository.save(any(AttendanceCheckSession.class)))
                .thenAnswer(invocation -> {
                    AttendanceCheckSession session = invocation.getArgument(0);
                    session.setId(9L);
                    return session;
                });

        AttendanceCheckSessionResponse response = service.createSession(
                new CreateAttendanceCheckSessionRequest(
                        3L,
                        AttendanceCheckAction.CHECK_IN,
                        10,
                        null,
                        null,
                        null
                ),
                "admin"
        );

        assertEquals(9L, response.sessionId());
        assertEquals(AttendanceCheckAction.CHECK_IN, response.action());
        assertFalse(response.gpsRequired());
        assertNotNull(response.qrToken());
        assertEquals(43, response.qrToken().length());
        assertTrue(response.otp().matches("\\d{6}"));

        verify(sessionRepository).save(argThat(session ->
                !session.getTokenHash().equals(response.qrToken())
                        && session.getTokenHash().length() == 64
                        && "$2a$10$encodedOtp".equals(session.getOtpHash())
        ));
    }

    @Test
    void qrCheckInAcceptsOwnedActiveAssignmentAndWritesAuditEvent() {
        LocalDateTime now = LocalDateTime.now();
        WorkShift shift = shift(now.minusMinutes(5), now.plusHours(2));
        String rawToken = "local-test-qr-token";

        AttendanceCheckSession session = session(
                20L,
                shift,
                AttendanceCheckAction.CHECK_IN,
                sha256(rawToken),
                now.minusMinutes(1),
                now.plusMinutes(9)
        );
        ShiftAssignment assignment = assignment(11L, shift, "employee");

        when(sessionRepository.findByTokenHashForUpdate(sha256(rawToken)))
                .thenReturn(Optional.of(session));
        when(assignmentRepository.findOwnedActiveForUpdate(
                eq(3L),
                eq("employee"),
                anyCollection()
        )).thenReturn(Optional.of(assignment));
        when(attendanceService.selfCheckIn(
                eq(assignment),
                eq(assignment.getEmployee().getUser()),
                any(LocalDateTime.class)
        )).thenReturn(attendanceResponse(31L));
        when(eventRepository.existsByAttendanceIdAndAction(
                31L,
                AttendanceCheckAction.CHECK_IN
        )).thenReturn(false);

        QrAttendanceResultResponse response = service.checkIn(
                new SelfAttendanceRequest(
                        3L,
                        rawToken,
                        null,
                        null,
                        null
                ),
                "employee"
        );

        assertEquals(AttendanceCheckMethod.QR, response.method());
        assertEquals(AttendanceCheckAction.CHECK_IN, response.action());
        assertFalse(response.gpsVerified());

        verify(eventRepository).save(argThat(event ->
                event.getAttendanceId().equals(31L)
                        && event.getAssignmentId().equals(11L)
                        && event.getSessionId().equals(20L)
                        && event.getMethod() == AttendanceCheckMethod.QR
        ));
    }

    @Test
    void otpFallbackCanCheckOut() {
        LocalDateTime now = LocalDateTime.now();
        WorkShift shift = shift(now.minusHours(1), now.plusMinutes(30));
        AttendanceCheckSession session = session(
                21L,
                shift,
                AttendanceCheckAction.CHECK_OUT,
                "unused",
                now.minusMinutes(1),
                now.plusMinutes(9)
        );
        session.setOtpHash("encoded");

        ShiftAssignment assignment = assignment(12L, shift, "employee");

        when(sessionRepository.findActiveForUpdate(
                eq(3L),
                eq(AttendanceCheckAction.CHECK_OUT),
                any(LocalDateTime.class)
        )).thenReturn(List.of(session));
        when(passwordEncoder.matches("123456", "encoded"))
                .thenReturn(true);
        when(assignmentRepository.findOwnedActiveForUpdate(
                eq(3L),
                eq("employee"),
                anyCollection()
        )).thenReturn(Optional.of(assignment));
        when(attendanceService.selfCheckOut(
                eq(assignment),
                eq(assignment.getEmployee().getUser()),
                any(LocalDateTime.class)
        )).thenReturn(attendanceResponse(32L));
        when(eventRepository.existsByAttendanceIdAndAction(
                32L,
                AttendanceCheckAction.CHECK_OUT
        )).thenReturn(false);

        QrAttendanceResultResponse response = service.checkOut(
                new SelfAttendanceRequest(
                        3L,
                        null,
                        "123456",
                        null,
                        null
                ),
                "employee"
        );

        assertEquals(AttendanceCheckMethod.OTP, response.method());
        assertEquals(AttendanceCheckAction.CHECK_OUT, response.action());
    }

    @Test
    void rejectsExpiredQrSession() {
        LocalDateTime now = LocalDateTime.now();
        WorkShift shift = shift(now.minusMinutes(20), now.plusHours(1));
        String rawToken = "expired-token";

        AttendanceCheckSession session = session(
                22L,
                shift,
                AttendanceCheckAction.CHECK_IN,
                sha256(rawToken),
                now.minusMinutes(20),
                now.minusMinutes(1)
        );

        when(sessionRepository.findByTokenHashForUpdate(sha256(rawToken)))
                .thenReturn(Optional.of(session));

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> service.checkIn(
                        new SelfAttendanceRequest(
                                3L,
                                rawToken,
                                null,
                                null,
                                null
                        ),
                        "employee"
                )
        );

        assertEquals("Phiên chấm công đã hết hạn", ex.getMessage());
        verifyNoInteractions(attendanceService);
    }

    @Test
    void rejectsGpsOutsideConfiguredRadius() {
        LocalDateTime now = LocalDateTime.now();
        WorkShift shift = shift(now.minusMinutes(5), now.plusHours(1));
        String rawToken = "gps-token";

        AttendanceCheckSession session = session(
                23L,
                shift,
                AttendanceCheckAction.CHECK_IN,
                sha256(rawToken),
                now.minusMinutes(1),
                now.plusMinutes(9)
        );
        session.setLatitude(new BigDecimal("10.776900"));
        session.setLongitude(new BigDecimal("106.700900"));
        session.setRadiusMeters(100);

        when(sessionRepository.findByTokenHashForUpdate(sha256(rawToken)))
                .thenReturn(Optional.of(session));

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> service.checkIn(
                        new SelfAttendanceRequest(
                                3L,
                                rawToken,
                                null,
                                10.790000,
                                106.700900
                        ),
                        "employee"
                )
        );

        assertEquals(
                "Vị trí hiện tại nằm ngoài bán kính chấm công cho phép",
                ex.getMessage()
        );
        verifyNoInteractions(attendanceService);
    }

    @Test
    void rejectsEmployeeWithoutOwnedActiveAssignment() {
        LocalDateTime now = LocalDateTime.now();
        WorkShift shift = shift(now.minusMinutes(5), now.plusHours(1));
        String rawToken = "owned-token";

        AttendanceCheckSession session = session(
                24L,
                shift,
                AttendanceCheckAction.CHECK_IN,
                sha256(rawToken),
                now.minusMinutes(1),
                now.plusMinutes(9)
        );

        when(sessionRepository.findByTokenHashForUpdate(sha256(rawToken)))
                .thenReturn(Optional.of(session));
        when(assignmentRepository.findOwnedActiveForUpdate(
                eq(3L),
                eq("otherEmployee"),
                anyCollection()
        )).thenReturn(Optional.empty());

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> service.checkIn(
                        new SelfAttendanceRequest(
                                3L,
                                rawToken,
                                null,
                                null,
                                null
                        ),
                        "otherEmployee"
                )
        );

        assertEquals(
                "Bạn không có phân công đang hoạt động cho ca này",
                ex.getMessage()
        );
        verifyNoInteractions(attendanceService);
    }


    @Test
    void rejectsOneSidedCoordinatesEvenWhenSessionDoesNotRequireGps() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.checkIn(
                        new SelfAttendanceRequest(
                                3L,
                                "qr-token",
                                null,
                                10.7769,
                                null
                        ),
                        "employee"
                )
        );

        assertEquals(
                "Latitude và longitude phải được cung cấp cùng nhau",
                ex.getMessage()
        );
        verifyNoInteractions(
                sessionRepository,
                eventRepository,
                assignmentRepository,
                attendanceService
        );
    }

    @Test
    void nonGpsSessionDoesNotPersistOptionalCoordinates() {
        LocalDateTime now = LocalDateTime.now();
        WorkShift shift = shift(now.minusMinutes(5), now.plusHours(2));
        String rawToken = "privacy-test-token";

        AttendanceCheckSession session = session(
                25L,
                shift,
                AttendanceCheckAction.CHECK_IN,
                sha256(rawToken),
                now.minusMinutes(1),
                now.plusMinutes(9)
        );
        ShiftAssignment assignment = assignment(13L, shift, "employee");

        when(sessionRepository.findByTokenHashForUpdate(sha256(rawToken)))
                .thenReturn(Optional.of(session));
        when(assignmentRepository.findOwnedActiveForUpdate(
                eq(3L),
                eq("employee"),
                anyCollection()
        )).thenReturn(Optional.of(assignment));
        when(attendanceService.selfCheckIn(
                eq(assignment),
                eq(assignment.getEmployee().getUser()),
                any(LocalDateTime.class)
        )).thenReturn(attendanceResponse(33L));
        when(eventRepository.existsByAttendanceIdAndAction(
                33L,
                AttendanceCheckAction.CHECK_IN
        )).thenReturn(false);

        QrAttendanceResultResponse response = service.checkIn(
                new SelfAttendanceRequest(
                        3L,
                        rawToken,
                        null,
                        10.7769,
                        106.7009
                ),
                "employee"
        );

        assertFalse(response.gpsVerified());
        assertNull(response.distanceMeters());

        verify(eventRepository).save(argThat(event ->
                event.getLatitude() == null
                        && event.getLongitude() == null
                        && event.getDistanceMeters() == null
        ));
    }

    @Test
    void gpsSessionPersistsVerifiedCoordinates() {
        LocalDateTime now = LocalDateTime.now();
        WorkShift shift = shift(now.minusMinutes(5), now.plusHours(2));
        String rawToken = "gps-inside-token";

        AttendanceCheckSession session = session(
                26L,
                shift,
                AttendanceCheckAction.CHECK_IN,
                sha256(rawToken),
                now.minusMinutes(1),
                now.plusMinutes(9)
        );
        session.setLatitude(new BigDecimal("10.776900"));
        session.setLongitude(new BigDecimal("106.700900"));
        session.setRadiusMeters(150);

        ShiftAssignment assignment = assignment(14L, shift, "employee");

        when(sessionRepository.findByTokenHashForUpdate(sha256(rawToken)))
                .thenReturn(Optional.of(session));
        when(assignmentRepository.findOwnedActiveForUpdate(
                eq(3L),
                eq("employee"),
                anyCollection()
        )).thenReturn(Optional.of(assignment));
        when(attendanceService.selfCheckIn(
                eq(assignment),
                eq(assignment.getEmployee().getUser()),
                any(LocalDateTime.class)
        )).thenReturn(attendanceResponse(34L));
        when(eventRepository.existsByAttendanceIdAndAction(
                34L,
                AttendanceCheckAction.CHECK_IN
        )).thenReturn(false);

        QrAttendanceResultResponse response = service.checkIn(
                new SelfAttendanceRequest(
                        3L,
                        rawToken,
                        null,
                        10.7769,
                        106.7009
                ),
                "employee"
        );

        assertTrue(response.gpsVerified());
        assertEquals(0, response.distanceMeters());

        verify(eventRepository).save(argThat(event ->
                new BigDecimal("10.776900").equals(event.getLatitude())
                        && new BigDecimal("106.700900").equals(event.getLongitude())
                        && Integer.valueOf(0).equals(event.getDistanceMeters())
        ));
    }

    private WorkShift shift(
            LocalDateTime startAt,
            LocalDateTime endAt
    ) {
        return WorkShift.builder()
                .id(3L)
                .name("Ca QR")
                .startAt(startAt)
                .endAt(endAt)
                .requiredStaff(2)
                .payAmount(new BigDecimal("200000.00"))
                .shiftStatus(ShiftStatus.OPEN)
                .build();
    }

    private AttendanceCheckSession session(
            Long id,
            WorkShift shift,
            AttendanceCheckAction action,
            String tokenHash,
            LocalDateTime validFrom,
            LocalDateTime expiresAt
    ) {
        return AttendanceCheckSession.builder()
                .id(id)
                .shift(shift)
                .action(action)
                .tokenHash(tokenHash)
                .otpHash("encoded")
                .validFrom(validFrom)
                .expiresAt(expiresAt)
                .createdBy(user("admin", RoleName.ADMIN))
                .build();
    }

    private ShiftAssignment assignment(
            Long id,
            WorkShift shift,
            String username
    ) {
        UserAccount employeeUser = user(username, RoleName.EMPLOYEE);
        Employee employee = Employee.builder()
                .id(7L)
                .employeeCode("NV007")
                .employmentStatus(EmployeeStatus.ACTIVE)
                .user(employeeUser)
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

    private AttendanceResponse attendanceResponse(Long id) {
        return new AttendanceResponse(
                id,
                11L,
                3L,
                "Ca QR",
                LocalDateTime.now().minusMinutes(5),
                LocalDateTime.now().plusHours(1),
                7L,
                "employee",
                AttendanceProcessStatus.DRAFT,
                null,
                LocalDateTime.now(),
                null,
                0,
                0,
                null,
                "employee",
                LocalDateTime.now(),
                null,
                null,
                null,
                null
        );
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256")
                            .digest(value.getBytes(StandardCharsets.UTF_8))
            );
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
