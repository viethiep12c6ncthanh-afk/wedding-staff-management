package com.viethiep.weddingstaff.service;

import com.viethiep.weddingstaff.exception.ResourceNotFoundException;
import com.viethiep.weddingstaff.dto.AttendanceResponse;
import com.viethiep.weddingstaff.dto.CreateAttendanceRequest;
import com.viethiep.weddingstaff.dto.UpdateAttendanceRequest;
import com.viethiep.weddingstaff.entity.Attendance;
import com.viethiep.weddingstaff.entity.ShiftAssignment;
import com.viethiep.weddingstaff.entity.UserAccount;
import com.viethiep.weddingstaff.entity.WorkShift;
import com.viethiep.weddingstaff.enumtype.AssignmentStatus;
import com.viethiep.weddingstaff.enumtype.AttendanceProcessStatus;
import com.viethiep.weddingstaff.enumtype.AttendanceResult;
import com.viethiep.weddingstaff.enumtype.RoleName;
import com.viethiep.weddingstaff.enumtype.ShiftRole;
import com.viethiep.weddingstaff.repository.AttendanceRepository;
import com.viethiep.weddingstaff.repository.ShiftAssignmentRepository;
import com.viethiep.weddingstaff.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AttendanceService {
    private static final EnumSet<AssignmentStatus> ATTENDABLE_STATUSES =
            EnumSet.of(AssignmentStatus.ASSIGNED, AssignmentStatus.CONFIRMED);

    private final AttendanceRepository attendanceRepository;
    private final ShiftAssignmentRepository assignmentRepository;
    private final UserAccountRepository userRepository;

    @Transactional(readOnly = true)
    public List<AttendanceResponse> findAll() {
        return attendanceRepository.findAllWithDetails().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AttendanceResponse> findMine(String username) {
        return attendanceRepository
                .findAllByEmployeeUsername(username)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public AttendanceResponse create(
            CreateAttendanceRequest request,
            String actorUsername
    ) {
        ShiftAssignment assignment = assignmentRepository
                .findByIdForUpdate(request.assignmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phân công"));
        ensureAssignmentCanBeAttended(assignment);

        if (attendanceRepository.existsByAssignment_Id(assignment.getId())) {
            throw new IllegalStateException(
                    "Phân công này đã có bản ghi chấm công"
            );
        }

        UserAccount actor = requireUser(actorUsername);
        ensureCanRecord(actor, assignment.getShift().getId());

        Attendance attendance = Attendance.builder()
                .assignment(assignment)
                .processStatus(AttendanceProcessStatus.DRAFT)
                .recordedBy(actor)
                .recordedAt(LocalDateTime.now())
                .build();

        applyDraftData(
                attendance,
                request.checkInAt(),
                request.checkOutAt(),
                request.absent(),
                request.note()
        );

        return toResponse(attendanceRepository.save(attendance));
    }

    @Transactional
    public AttendanceResponse update(
            Long attendanceId,
            UpdateAttendanceRequest request,
            String actorUsername
    ) {
        Attendance attendance = attendanceRepository
                .findByIdForUpdate(attendanceId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chấm công"));
        ensureDraft(attendance);
        ensureAssignmentCanBeAttended(attendance.getAssignment());

        UserAccount actor = requireUser(actorUsername);
        ensureCanRecord(actor, attendance.getAssignment().getShift().getId());

        applyDraftData(
                attendance,
                request.checkInAt(),
                request.checkOutAt(),
                request.absent(),
                request.note()
        );
        attendance.setRecordedBy(actor);
        attendance.setRecordedAt(LocalDateTime.now());

        return toResponse(attendance);
    }

    @Transactional
    public AttendanceResponse confirm(
            Long attendanceId,
            String confirmerUsername
    ) {
        Attendance attendance = attendanceRepository
                .findByIdForUpdate(attendanceId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chấm công"));
        ensureDraft(attendance);

        ShiftAssignment assignment = assignmentRepository
                .findByIdForUpdate(attendance.getAssignment().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phân công"));
        attendance.setAssignment(assignment);
        ensureAssignmentCanBeAttended(assignment);
        ensureReadyToConfirm(attendance);

        UserAccount confirmer = requireUser(confirmerUsername);
        if (confirmer.getRole().getName() != RoleName.ADMIN
                && confirmer.getRole().getName() != RoleName.COORDINATOR) {
            throw new IllegalStateException(
                    "Chỉ quản trị viên hoặc điều phối viên được xác nhận chấm công"
            );
        }

        LocalDateTime now = LocalDateTime.now();
        BigDecimal basePay = assignment.getShift().getPayAmount();
        BigDecimal payable = attendance.getAttendanceResult()
                == AttendanceResult.ABSENT
                ? BigDecimal.ZERO.setScale(basePay.scale())
                : basePay;

        attendance.setProcessStatus(AttendanceProcessStatus.CONFIRMED);
        attendance.setConfirmedBy(confirmer);
        attendance.setConfirmedAt(now);
        attendance.setBasePaySnapshot(basePay);
        attendance.setPayableAmount(payable);

        assignment.setStatus(
                attendance.getAttendanceResult() == AttendanceResult.ABSENT
                        ? AssignmentStatus.ABSENT
                        : AssignmentStatus.COMPLETED
        );

        return toResponse(attendance);
    }

    private void applyDraftData(
            Attendance attendance,
            LocalDateTime checkInAt,
            LocalDateTime checkOutAt,
            boolean absent,
            String note
    ) {
        if (absent) {
            if (checkInAt != null || checkOutAt != null) {
                throw new IllegalArgumentException(
                        "Chấm công vắng mặt không được có giờ vào hoặc giờ ra"
                );
            }
            attendance.setCheckInAt(null);
            attendance.setCheckOutAt(null);
            attendance.setLateMinutes(0);
            attendance.setEarlyLeaveMinutes(0);
            attendance.setAttendanceResult(AttendanceResult.ABSENT);
        } else {
            validateTimeOrder(checkInAt, checkOutAt);
            attendance.setCheckInAt(checkInAt);
            attendance.setCheckOutAt(checkOutAt);
            calculateAttendanceResult(attendance);
        }
        attendance.setNote(trimToNull(note));
    }

    private void calculateAttendanceResult(Attendance attendance) {
        WorkShift shift = attendance.getAssignment().getShift();
        LocalDateTime checkInAt = attendance.getCheckInAt();
        LocalDateTime checkOutAt = attendance.getCheckOutAt();

        int lateMinutes = checkInAt == null
                ? 0
                : positiveMinutes(shift.getStartAt(), checkInAt);
        int earlyLeaveMinutes = checkOutAt == null
                ? 0
                : positiveMinutes(checkOutAt, shift.getEndAt());

        attendance.setLateMinutes(lateMinutes);
        attendance.setEarlyLeaveMinutes(earlyLeaveMinutes);

        if (checkInAt == null || checkOutAt == null) {
            attendance.setAttendanceResult(null);
            return;
        }

        if (lateMinutes > 0 && earlyLeaveMinutes > 0) {
            attendance.setAttendanceResult(
                    AttendanceResult.LATE_AND_EARLY_LEAVE
            );
        } else if (lateMinutes > 0) {
            attendance.setAttendanceResult(AttendanceResult.LATE);
        } else if (earlyLeaveMinutes > 0) {
            attendance.setAttendanceResult(
                    AttendanceResult.EARLY_LEAVE
            );
        } else {
            attendance.setAttendanceResult(AttendanceResult.PRESENT);
        }
    }

    private int positiveMinutes(
            LocalDateTime expected,
            LocalDateTime actual
    ) {
        long minutes = Duration.between(expected, actual).toMinutes();
        if (minutes <= 0) {
            return 0;
        }
        return Math.toIntExact(minutes);
    }

    private void validateTimeOrder(
            LocalDateTime checkInAt,
            LocalDateTime checkOutAt
    ) {
        if (checkInAt != null
                && checkOutAt != null
                && !checkInAt.isBefore(checkOutAt)) {
            throw new IllegalArgumentException(
                    "Giờ vào phải trước giờ ra"
            );
        }
    }

    private void ensureReadyToConfirm(Attendance attendance) {
        if (attendance.getAttendanceResult() == AttendanceResult.ABSENT) {
            return;
        }
        if (attendance.getCheckInAt() == null
                || attendance.getCheckOutAt() == null) {
            throw new IllegalStateException(
                    "Phải có đủ giờ vào và giờ ra trước khi xác nhận"
            );
        }
        calculateAttendanceResult(attendance);
    }

    private void ensureDraft(Attendance attendance) {
        if (attendance.getProcessStatus()
                != AttendanceProcessStatus.DRAFT) {
            throw new IllegalStateException(
                    "Chấm công đã xác nhận nên không thể chỉnh sửa"
            );
        }
    }

    private void ensureAssignmentCanBeAttended(
            ShiftAssignment assignment
    ) {
        if (!ATTENDABLE_STATUSES.contains(assignment.getStatus())) {
            throw new IllegalStateException(
                    "Chỉ chấm công cho phân công đang hoạt động"
            );
        }
    }

    private void ensureCanRecord(
            UserAccount actor,
            Long shiftId
    ) {
        RoleName roleName = actor.getRole().getName();
        if (roleName == RoleName.ADMIN
                || roleName == RoleName.COORDINATOR) {
            return;
        }

        boolean leaderOfShift = roleName == RoleName.EMPLOYEE
                && assignmentRepository
                .countLeaderAssignments(
                        shiftId,
                        actor.getUsername(),
                        ShiftRole.LEADER,
                        ATTENDABLE_STATUSES
                ) > 0;
        if (!leaderOfShift) {
            throw new IllegalStateException(
                    "Chỉ điều phối viên hoặc trưởng ca của ca này được ghi nhận chấm công"
            );
        }
    }

    private UserAccount requireUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản"));
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private AttendanceResponse toResponse(Attendance attendance) {
        ShiftAssignment assignment = attendance.getAssignment();
        return new AttendanceResponse(
                attendance.getId(),
                assignment.getId(),
                assignment.getShift().getId(),
                assignment.getShift().getName(),
                assignment.getShift().getStartAt(),
                assignment.getShift().getEndAt(),
                assignment.getEmployee().getId(),
                assignment.getEmployee().getUser().getFullName(),
                attendance.getProcessStatus(),
                attendance.getAttendanceResult(),
                attendance.getCheckInAt(),
                attendance.getCheckOutAt(),
                attendance.getLateMinutes(),
                attendance.getEarlyLeaveMinutes(),
                attendance.getNote(),
                attendance.getRecordedBy().getUsername(),
                attendance.getRecordedAt(),
                attendance.getConfirmedBy() == null
                        ? null
                        : attendance.getConfirmedBy().getUsername(),
                attendance.getConfirmedAt(),
                attendance.getBasePaySnapshot(),
                attendance.getPayableAmount()
        );
    }
}
