package com.viethiep.weddingstaff.service;

import com.viethiep.weddingstaff.config.AttendanceDemoProperties;
import com.viethiep.weddingstaff.dto.AttendanceCheckSessionResponse;
import com.viethiep.weddingstaff.dto.AttendanceResponse;
import com.viethiep.weddingstaff.dto.CreateAttendanceCheckSessionRequest;
import com.viethiep.weddingstaff.dto.QrAttendanceResultResponse;
import com.viethiep.weddingstaff.dto.SelfAttendanceRequest;
import com.viethiep.weddingstaff.entity.AttendanceCheckEvent;
import com.viethiep.weddingstaff.entity.AttendanceCheckSession;
import com.viethiep.weddingstaff.entity.ShiftAssignment;
import com.viethiep.weddingstaff.entity.UserAccount;
import com.viethiep.weddingstaff.entity.WorkShift;
import com.viethiep.weddingstaff.enumtype.AssignmentStatus;
import com.viethiep.weddingstaff.enumtype.AttendanceCheckAction;
import com.viethiep.weddingstaff.enumtype.AttendanceCheckMethod;
import com.viethiep.weddingstaff.enumtype.RoleName;
import com.viethiep.weddingstaff.exception.ResourceNotFoundException;
import com.viethiep.weddingstaff.repository.AttendanceCheckEventRepository;
import com.viethiep.weddingstaff.repository.AttendanceCheckSessionRepository;
import com.viethiep.weddingstaff.repository.ShiftAssignmentRepository;
import com.viethiep.weddingstaff.repository.UserAccountRepository;
import com.viethiep.weddingstaff.repository.WorkShiftRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.EnumSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class QrAttendanceService {
    private static final EnumSet<AssignmentStatus> ACTIVE_ASSIGNMENT_STATUSES =
            EnumSet.of(AssignmentStatus.ASSIGNED, AssignmentStatus.CONFIRMED);

    private static final int DEFAULT_SESSION_MINUTES = 10;
    private static final int DEFAULT_RADIUS_METERS = 150;
    private static final int CHECK_IN_EARLY_MINUTES = 120;
    private static final int CHECK_OUT_LATE_MINUTES = 240;
    private static final double EARTH_RADIUS_METERS = 6_371_000.0;

    private final AttendanceCheckSessionRepository sessionRepository;
    private final AttendanceCheckEventRepository eventRepository;
    private final ShiftAssignmentRepository assignmentRepository;
    private final WorkShiftRepository shiftRepository;
    private final UserAccountRepository userRepository;
    private final AttendanceService attendanceService;
    private final PasswordEncoder passwordEncoder;
    private final AttendanceDemoProperties demoProperties;

    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public AttendanceCheckSessionResponse createSession(
            CreateAttendanceCheckSessionRequest request,
            String actorUsername
    ) {
        UserAccount actor = requireUser(actorUsername);
        ensureManager(actor);

        WorkShift shift = shiftRepository.findByIdForUpdate(request.shiftId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ca làm"));

        LocalDateTime now = LocalDateTime.now();
        ensureWithinActionWindow(shift, request.action(), now);

        LocationPolicy location = validateLocationPolicy(
                request.latitude(),
                request.longitude(),
                request.radiusMeters()
        );

        List<AttendanceCheckSession> activeSessions =
                sessionRepository.findActiveForUpdate(
                        shift.getId(),
                        request.action(),
                        now
                );
        for (AttendanceCheckSession active : activeSessions) {
            active.setRevokedBy(actor);
            active.setRevokedAt(now);
        }

        int validMinutes = request.validMinutes() == null
                ? DEFAULT_SESSION_MINUTES
                : request.validMinutes();

        LocalDateTime requestedExpiry = now.plusMinutes(validMinutes);
        LocalDateTime expiresAt;
        if (isDemoMode()) {
            expiresAt = requestedExpiry;
        } else {
            LocalDateTime actionWindowEnd = actionWindowEnd(shift, request.action());
            expiresAt = requestedExpiry.isBefore(actionWindowEnd)
                    ? requestedExpiry
                    : actionWindowEnd;
        }

        if (!expiresAt.isAfter(now)) {
            throw new IllegalStateException(
                    "Không thể tạo phiên chấm công đã hết thời gian sử dụng"
            );
        }

        String qrToken = generateToken();
        String otp = generateOtp();

        AttendanceCheckSession session = AttendanceCheckSession.builder()
                .shift(shift)
                .action(request.action())
                .tokenHash(hashToken(qrToken))
                .otpHash(passwordEncoder.encode(otp))
                .validFrom(now)
                .expiresAt(expiresAt)
                .latitude(toCoordinate(location.latitude()))
                .longitude(toCoordinate(location.longitude()))
                .radiusMeters(location.radiusMeters())
                .createdBy(actor)
                .build();

        session = sessionRepository.save(session);

        return new AttendanceCheckSessionResponse(
                session.getId(),
                shift.getId(),
                shift.getName(),
                session.getAction(),
                session.getValidFrom(),
                session.getExpiresAt(),
                session.getLatitude() != null,
                session.getRadiusMeters(),
                qrToken,
                otp
        );
    }

    @Transactional
    public QrAttendanceResultResponse checkIn(
            SelfAttendanceRequest request,
            String employeeUsername
    ) {
        return perform(
                request,
                employeeUsername,
                AttendanceCheckAction.CHECK_IN
        );
    }

    @Transactional
    public QrAttendanceResultResponse checkOut(
            SelfAttendanceRequest request,
            String employeeUsername
    ) {
        return perform(
                request,
                employeeUsername,
                AttendanceCheckAction.CHECK_OUT
        );
    }

    @Transactional
    public void revokeSession(
            Long sessionId,
            String actorUsername
    ) {
        UserAccount actor = requireUser(actorUsername);
        ensureManager(actor);

        AttendanceCheckSession session = sessionRepository
                .findByIdForUpdate(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy phiên chấm công"
                ));

        if (session.getRevokedAt() != null) {
            return;
        }

        session.setRevokedBy(actor);
        session.setRevokedAt(LocalDateTime.now());
    }

    private QrAttendanceResultResponse perform(
            SelfAttendanceRequest request,
            String employeeUsername,
            AttendanceCheckAction expectedAction
    ) {
        validateCoordinatePair(request.latitude(), request.longitude());
        Credential credential = validateCredential(request);
        LocalDateTime now = LocalDateTime.now();

        AttendanceCheckSession session = resolveSession(
                request,
                expectedAction,
                credential,
                now
        );
        ensureSessionUsable(session, request.shiftId(), expectedAction, now);
        ensureWithinActionWindow(session.getShift(), expectedAction, now);

        Integer distanceMeters = validateGps(
                session,
                request.latitude(),
                request.longitude()
        );

        ShiftAssignment assignment = assignmentRepository
                .findOwnedActiveForUpdate(
                        request.shiftId(),
                        employeeUsername,
                        ACTIVE_ASSIGNMENT_STATUSES
                )
                .orElseThrow(() -> new IllegalStateException(
                        "Bạn không có phân công đang hoạt động cho ca này"
                ));

        UserAccount actor = assignment.getEmployee().getUser();

        AttendanceResponse attendance = expectedAction
                == AttendanceCheckAction.CHECK_IN
                ? attendanceService.selfCheckIn(assignment, actor, now)
                : attendanceService.selfCheckOut(assignment, actor, now);

        if (eventRepository.existsByAttendanceIdAndAction(
                attendance.id(),
                expectedAction
        )) {
            throw new IllegalStateException(
                    "Thao tác chấm công này đã được ghi nhận"
            );
        }

        AttendanceCheckMethod method = credential.qr()
                ? AttendanceCheckMethod.QR
                : AttendanceCheckMethod.OTP;

        boolean gpsRequired = session.getLatitude() != null;

        eventRepository.save(
                AttendanceCheckEvent.builder()
                        .attendanceId(attendance.id())
                        .assignmentId(assignment.getId())
                        .sessionId(session.getId())
                        .action(expectedAction)
                        .method(method)
                        .occurredAt(now)
                        .latitude(gpsRequired
                                ? toCoordinate(request.latitude())
                                : null)
                        .longitude(gpsRequired
                                ? toCoordinate(request.longitude())
                                : null)
                        .distanceMeters(distanceMeters)
                        .build()
        );

        return new QrAttendanceResultResponse(
                attendance,
                expectedAction,
                method,
                session.getLatitude() != null,
                distanceMeters,
                now
        );
    }

    private AttendanceCheckSession resolveSession(
            SelfAttendanceRequest request,
            AttendanceCheckAction expectedAction,
            Credential credential,
            LocalDateTime now
    ) {
        if (credential.qr()) {
            return sessionRepository
                    .findByTokenHashForUpdate(
                            hashToken(request.qrToken().trim())
                    )
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Mã QR chấm công không hợp lệ"
                    ));
        }

        List<AttendanceCheckSession> active =
                sessionRepository.findActiveForUpdate(
                        request.shiftId(),
                        expectedAction,
                        now
                );

        return active.stream()
                .filter(session -> passwordEncoder.matches(
                        request.otp().trim(),
                        session.getOtpHash()
                ))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "OTP chấm công không hợp lệ hoặc đã hết hạn"
                ));
    }

    private void ensureSessionUsable(
            AttendanceCheckSession session,
            Long requestedShiftId,
            AttendanceCheckAction expectedAction,
            LocalDateTime now
    ) {
        if (!session.getShift().getId().equals(requestedShiftId)) {
            throw new IllegalArgumentException(
                    "Phiên chấm công không thuộc ca được yêu cầu"
            );
        }
        if (session.getAction() != expectedAction) {
            throw new IllegalArgumentException(
                    "Phiên chấm công không đúng thao tác"
            );
        }
        if (session.getRevokedAt() != null) {
            throw new IllegalStateException("Phiên chấm công đã bị thu hồi");
        }
        if (now.isBefore(session.getValidFrom())
                || !now.isBefore(session.getExpiresAt())) {
            throw new IllegalStateException("Phiên chấm công đã hết hạn");
        }
    }

    private Credential validateCredential(SelfAttendanceRequest request) {
        boolean qr = request.qrToken() != null
                && !request.qrToken().isBlank();
        boolean otp = request.otp() != null
                && !request.otp().isBlank();

        if (qr == otp) {
            throw new IllegalArgumentException(
                    "Phải cung cấp đúng một phương thức: QR hoặc OTP"
            );
        }
        if (otp && !request.otp().trim().matches("\\d{6}")) {
            throw new IllegalArgumentException("OTP phải gồm đúng 6 chữ số");
        }

        return new Credential(qr);
    }

    private LocationPolicy validateLocationPolicy(
            Double latitude,
            Double longitude,
            Integer radiusMeters
    ) {
        boolean hasLatitude = latitude != null;
        boolean hasLongitude = longitude != null;

        if (hasLatitude != hasLongitude) {
            throw new IllegalArgumentException(
                    "Latitude và longitude phải được cung cấp cùng nhau"
            );
        }

        if (!hasLatitude) {
            if (radiusMeters != null) {
                throw new IllegalArgumentException(
                        "Không được đặt bán kính khi chưa có tọa độ"
                );
            }
            return new LocationPolicy(null, null, null);
        }

        return new LocationPolicy(
                latitude,
                longitude,
                radiusMeters == null
                        ? DEFAULT_RADIUS_METERS
                        : radiusMeters
        );
    }

    private void validateCoordinatePair(
            Double latitude,
            Double longitude
    ) {
        if ((latitude == null) != (longitude == null)) {
            throw new IllegalArgumentException(
                    "Latitude và longitude phải được cung cấp cùng nhau"
            );
        }
    }

    private Integer validateGps(
            AttendanceCheckSession session,
            Double latitude,
            Double longitude
    ) {
        if (session.getLatitude() == null) {
            return null;
        }

        if (latitude == null || longitude == null) {
            throw new IllegalArgumentException(
                    "Phiên này yêu cầu vị trí GPS khi chấm công"
            );
        }

        int distance = (int) Math.round(haversineMeters(
                session.getLatitude().doubleValue(),
                session.getLongitude().doubleValue(),
                latitude,
                longitude
        ));

        if (distance > session.getRadiusMeters()) {
            throw new IllegalStateException(
                    "Vị trí hiện tại nằm ngoài bán kính chấm công cho phép"
            );
        }

        return distance;
    }

    private BigDecimal toCoordinate(Double value) {
        if (value == null) {
            return null;
        }
        return BigDecimal.valueOf(value)
                .setScale(6, RoundingMode.HALF_UP);
    }

    private void ensureWithinActionWindow(
            WorkShift shift,
            AttendanceCheckAction action,
            LocalDateTime now
    ) {
        if (isDemoMode()) {
            return;
        }
        LocalDateTime windowStart = action == AttendanceCheckAction.CHECK_IN
                ? shift.getStartAt().minusMinutes(CHECK_IN_EARLY_MINUTES)
                : shift.getStartAt();

        LocalDateTime windowEnd = actionWindowEnd(shift, action);

        if (now.isBefore(windowStart) || now.isAfter(windowEnd)) {
            throw new IllegalStateException(
                    action == AttendanceCheckAction.CHECK_IN
                            ? "Chưa đến hoặc đã quá thời gian tự check-in"
                            : "Chưa đến hoặc đã quá thời gian tự check-out"
            );
        }
    }

    public boolean isDemoMode() {
        return demoProperties != null && demoProperties.isEnabled();
    }

    private LocalDateTime actionWindowEnd(
            WorkShift shift,
            AttendanceCheckAction action
    ) {
        return action == AttendanceCheckAction.CHECK_IN
                ? shift.getEndAt()
                : shift.getEndAt().plusMinutes(CHECK_OUT_LATE_MINUTES);
    }

    private void ensureManager(UserAccount actor) {
        RoleName role = actor.getRole().getName();
        if (role != RoleName.ADMIN && role != RoleName.COORDINATOR) {
            throw new IllegalStateException(
                    "Chỉ quản trị viên hoặc điều phối viên được quản lý phiên chấm công"
            );
        }
    }

    private UserAccount requireUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy tài khoản"
                ));
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }

    private String generateOtp() {
        return "%06d".formatted(secureRandom.nextInt(1_000_000));
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(
                    rawToken.getBytes(StandardCharsets.UTF_8)
            );
            return java.util.HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 không khả dụng", ex);
        }
    }

    private double haversineMeters(
            double lat1,
            double lon1,
            double lat2,
            double lon2
    ) {
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2)
                * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_METERS * c;
    }

    private record Credential(boolean qr) {
    }

    private record LocationPolicy(
            Double latitude,
            Double longitude,
            Integer radiusMeters
    ) {
    }
}
