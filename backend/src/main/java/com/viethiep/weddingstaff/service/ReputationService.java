package com.viethiep.weddingstaff.service;

import com.viethiep.weddingstaff.dto.CreateEmployeeEvaluationRequest;
import com.viethiep.weddingstaff.dto.EmployeeEvaluationResponse;
import com.viethiep.weddingstaff.dto.ReputationDetailResponse;
import com.viethiep.weddingstaff.dto.ReputationEventResponse;
import com.viethiep.weddingstaff.dto.ReputationSummaryResponse;
import com.viethiep.weddingstaff.entity.Attendance;
import com.viethiep.weddingstaff.entity.Employee;
import com.viethiep.weddingstaff.entity.EmployeeEvaluation;
import com.viethiep.weddingstaff.entity.EmployeeReputation;
import com.viethiep.weddingstaff.entity.ReputationEvent;
import com.viethiep.weddingstaff.entity.ShiftAssignment;
import com.viethiep.weddingstaff.entity.UserAccount;
import com.viethiep.weddingstaff.enumtype.AssignmentStatus;
import com.viethiep.weddingstaff.enumtype.AttendanceProcessStatus;
import com.viethiep.weddingstaff.enumtype.AttendanceResult;
import com.viethiep.weddingstaff.enumtype.ReputationEventType;
import com.viethiep.weddingstaff.enumtype.ReputationSourceType;
import com.viethiep.weddingstaff.enumtype.RoleName;
import com.viethiep.weddingstaff.exception.ResourceNotFoundException;
import com.viethiep.weddingstaff.repository.EmployeeEvaluationRepository;
import com.viethiep.weddingstaff.repository.EmployeeReputationRepository;
import com.viethiep.weddingstaff.repository.ReputationEventRepository;
import com.viethiep.weddingstaff.repository.ShiftAssignmentRepository;
import com.viethiep.weddingstaff.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReputationService {
    public static final int INITIAL_SCORE = 80;
    private static final int MIN_SCORE = 0;
    private static final int MAX_SCORE = 100;

    private final EmployeeReputationRepository reputationRepository;
    private final ReputationEventRepository eventRepository;
    private final EmployeeEvaluationRepository evaluationRepository;
    private final ShiftAssignmentRepository assignmentRepository;
    private final UserAccountRepository userRepository;

    @Transactional(readOnly = true)
    public List<ReputationSummaryResponse> findAll() {
        return reputationRepository.findAllWithEmployee().stream()
                .map(this::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public ReputationDetailResponse findByEmployeeId(Long employeeId) {
        EmployeeReputation reputation = reputationRepository
                .findByEmployeeIdWithEmployee(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy hồ sơ uy tín của nhân viên"
                ));
        return toDetail(reputation);
    }

    @Transactional(readOnly = true)
    public ReputationDetailResponse findMine(String username) {
        EmployeeReputation reputation = reputationRepository
                .findByEmployeeUsernameWithEmployee(username)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy hồ sơ uy tín của nhân viên"
                ));
        return toDetail(reputation);
    }

    @Transactional
    public void initializeEmployee(Employee employee) {
        if (employee == null || employee.getId() == null) {
            throw new IllegalArgumentException(
                    "Nhân viên phải được lưu trước khi khởi tạo uy tín"
            );
        }
        if (reputationRepository
                .findByEmployeeIdWithEmployee(employee.getId())
                .isPresent()) {
            return;
        }

        EmployeeReputation reputation = reputationRepository.save(
                EmployeeReputation.builder()
                        .employee(employee)
                        .currentScore(INITIAL_SCORE)
                        .completedShiftCount(0)
                        .lateCount(0)
                        .earlyLeaveCount(0)
                        .absentCount(0)
                        .evaluationCount(0)
                        .ratingSum(0)
                        .build()
        );

        LocalDateTime now = LocalDateTime.now();
        appendEvent(
                reputation,
                ReputationEventType.BASELINE_INITIALIZED,
                ReputationSourceType.BASELINE,
                employee.getId(),
                0,
                "Khởi tạo điểm uy tín ở mức trung tính 80/100",
                null,
                now
        );
    }

    @Transactional
    public void applyConfirmedAttendance(
            Attendance attendance,
            UserAccount actor
    ) {
        if (attendance == null || attendance.getId() == null) {
            throw new IllegalArgumentException(
                    "Chấm công phải được lưu trước khi cập nhật uy tín"
            );
        }
        if (attendance.getProcessStatus()
                != AttendanceProcessStatus.CONFIRMED) {
            throw new IllegalStateException(
                    "Chỉ chấm công đã xác nhận mới ảnh hưởng điểm uy tín"
            );
        }
        if (attendance.getAttendanceResult() == null) {
            throw new IllegalStateException(
                    "Chấm công đã xác nhận phải có kết quả"
            );
        }
        if (eventRepository.existsBySourceTypeAndSourceId(
                ReputationSourceType.ATTENDANCE,
                attendance.getId()
        )) {
            return;
        }

        Employee employee = attendance.getAssignment().getEmployee();
        EmployeeReputation reputation = requireForUpdate(employee);
        AttendanceResult result = attendance.getAttendanceResult();

        if (result != AttendanceResult.ABSENT) {
            reputation.setCompletedShiftCount(
                    reputation.getCompletedShiftCount() + 1
            );
        }
        if (attendance.getLateMinutes() != null
                && attendance.getLateMinutes() > 0) {
            reputation.setLateCount(reputation.getLateCount() + 1);
        }
        if (attendance.getEarlyLeaveMinutes() != null
                && attendance.getEarlyLeaveMinutes() > 0) {
            reputation.setEarlyLeaveCount(
                    reputation.getEarlyLeaveCount() + 1
            );
        }
        if (result == AttendanceResult.ABSENT) {
            reputation.setAbsentCount(reputation.getAbsentCount() + 1);
        }

        int configuredDelta = attendanceDelta(attendance);
        appendEvent(
                reputation,
                eventType(result),
                ReputationSourceType.ATTENDANCE,
                attendance.getId(),
                configuredDelta,
                attendanceReason(attendance, configuredDelta),
                actor,
                attendance.getConfirmedAt() == null
                        ? LocalDateTime.now()
                        : attendance.getConfirmedAt()
        );
    }

    @Transactional
    public EmployeeEvaluationResponse createEvaluation(
            CreateEmployeeEvaluationRequest request,
            String actorUsername
    ) {
        if (request.rating() < 1 || request.rating() > 5) {
            throw new IllegalArgumentException(
                    "Điểm đánh giá phải từ 1 đến 5"
            );
        }

        ShiftAssignment assignment = assignmentRepository
                .findByIdForUpdate(request.assignmentId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy phân công"
                ));
        if (assignment.getStatus() != AssignmentStatus.COMPLETED) {
            throw new IllegalStateException(
                    "Chỉ được đánh giá phân công đã hoàn thành"
            );
        }
        if (evaluationRepository.existsByAssignment_Id(assignment.getId())) {
            throw new IllegalStateException(
                    "Phân công này đã được đánh giá"
            );
        }

        UserAccount actor = requireManagementUser(actorUsername);
        EmployeeEvaluation evaluation = evaluationRepository.save(
                EmployeeEvaluation.builder()
                        .assignment(assignment)
                        .rating(request.rating())
                        .comment(trimToNull(request.comment()))
                        .evaluatedBy(actor)
                        .evaluatedAt(LocalDateTime.now())
                        .build()
        );

        EmployeeReputation reputation = requireForUpdate(
                assignment.getEmployee()
        );
        reputation.setEvaluationCount(reputation.getEvaluationCount() + 1);
        reputation.setRatingSum(
                reputation.getRatingSum() + request.rating()
        );

        int configuredDelta = evaluationDelta(request.rating());
        appendEvent(
                reputation,
                ReputationEventType.EVALUATION_RATING,
                ReputationSourceType.EVALUATION,
                evaluation.getId(),
                configuredDelta,
                "Đánh giá " + request.rating() + "/5; quy tắc "
                        + signed(configuredDelta),
                actor,
                evaluation.getEvaluatedAt()
        );

        return toEvaluationResponse(evaluation);
    }

    private EmployeeReputation requireForUpdate(Employee employee) {
        return reputationRepository
                .findByEmployeeIdForUpdate(employee.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy hồ sơ uy tín của nhân viên"
                ));
    }

    private UserAccount requireManagementUser(String username) {
        UserAccount user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy tài khoản"
                ));
        RoleName role = user.getRole().getName();
        if (role != RoleName.ADMIN && role != RoleName.COORDINATOR) {
            throw new IllegalStateException(
                    "Chỉ quản trị viên hoặc điều phối viên được đánh giá nhân viên"
            );
        }
        return user;
    }

    private void appendEvent(
            EmployeeReputation reputation,
            ReputationEventType eventType,
            ReputationSourceType sourceType,
            Long sourceId,
            int configuredDelta,
            String reason,
            UserAccount actor,
            LocalDateTime occurredAt
    ) {
        int before = reputation.getCurrentScore();
        int after = clamp(before + configuredDelta);
        int actualDelta = after - before;

        reputation.setCurrentScore(after);
        reputation.setLastEventAt(occurredAt);

        eventRepository.save(
                ReputationEvent.builder()
                        .employee(reputation.getEmployee())
                        .eventType(eventType)
                        .sourceType(sourceType)
                        .sourceId(sourceId)
                        .scoreBefore(before)
                        .scoreDelta(actualDelta)
                        .scoreAfter(after)
                        .reason(reasonWithClamp(
                                reason,
                                configuredDelta,
                                actualDelta
                        ))
                        .actorUser(actor)
                        .occurredAt(occurredAt)
                        .build()
        );
    }

    private int attendanceDelta(Attendance attendance) {
        return switch (attendance.getAttendanceResult()) {
            case PRESENT -> 2;
            case LATE -> minutePenalty(attendance.getLateMinutes());
            case EARLY_LEAVE -> minutePenalty(
                    attendance.getEarlyLeaveMinutes()
            );
            case LATE_AND_EARLY_LEAVE ->
                    minutePenalty(attendance.getLateMinutes())
                            + minutePenalty(attendance.getEarlyLeaveMinutes());
            case ABSENT -> -10;
        };
    }

    private int minutePenalty(Integer minutes) {
        int value = minutes == null ? 0 : minutes;
        if (value <= 0) {
            return 0;
        }
        if (value <= 15) {
            return -1;
        }
        if (value <= 30) {
            return -2;
        }
        return -4;
    }

    private int evaluationDelta(int rating) {
        return switch (rating) {
            case 5 -> 5;
            case 4 -> 2;
            case 3 -> 0;
            case 2 -> -3;
            case 1 -> -6;
            default -> throw new IllegalArgumentException(
                    "Điểm đánh giá phải từ 1 đến 5"
            );
        };
    }

    private ReputationEventType eventType(AttendanceResult result) {
        return switch (result) {
            case PRESENT -> ReputationEventType.ATTENDANCE_PRESENT;
            case LATE -> ReputationEventType.ATTENDANCE_LATE;
            case EARLY_LEAVE -> ReputationEventType.ATTENDANCE_EARLY_LEAVE;
            case LATE_AND_EARLY_LEAVE ->
                    ReputationEventType.ATTENDANCE_LATE_AND_EARLY_LEAVE;
            case ABSENT -> ReputationEventType.ATTENDANCE_ABSENT;
        };
    }

    private String attendanceReason(
            Attendance attendance,
            int configuredDelta
    ) {
        return switch (attendance.getAttendanceResult()) {
            case PRESENT -> "Hoàn thành ca đúng giờ; quy tắc +2";
            case LATE -> "Đi trễ " + attendance.getLateMinutes()
                    + " phút; quy tắc " + signed(configuredDelta);
            case EARLY_LEAVE -> "Về sớm "
                    + attendance.getEarlyLeaveMinutes()
                    + " phút; quy tắc " + signed(configuredDelta);
            case LATE_AND_EARLY_LEAVE -> "Đi trễ "
                    + attendance.getLateMinutes() + " phút và về sớm "
                    + attendance.getEarlyLeaveMinutes()
                    + " phút; quy tắc " + signed(configuredDelta);
            case ABSENT -> "Vắng mặt; quy tắc -10";
        };
    }

    private String reasonWithClamp(
            String reason,
            int configuredDelta,
            int actualDelta
    ) {
        if (configuredDelta == actualDelta) {
            return reason;
        }
        return reason + "; điểm thực tế " + signed(actualDelta)
                + " do giới hạn 0-100";
    }

    private int clamp(int value) {
        return Math.max(MIN_SCORE, Math.min(MAX_SCORE, value));
    }

    private String signed(int value) {
        return value > 0 ? "+" + value : String.valueOf(value);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private ReputationDetailResponse toDetail(
            EmployeeReputation reputation
    ) {
        Long employeeId = reputation.getEmployee().getId();
        List<ReputationEventResponse> events = eventRepository
                .findAllByEmployeeId(employeeId)
                .stream()
                .map(this::toEventResponse)
                .toList();
        List<EmployeeEvaluationResponse> evaluations = evaluationRepository
                .findAllByEmployeeId(employeeId)
                .stream()
                .map(this::toEvaluationResponse)
                .toList();
        return new ReputationDetailResponse(
                toSummary(reputation),
                events,
                evaluations
        );
    }

    private ReputationSummaryResponse toSummary(
            EmployeeReputation reputation
    ) {
        Employee employee = reputation.getEmployee();
        return new ReputationSummaryResponse(
                employee.getId(),
                employee.getEmployeeCode(),
                employee.getUser().getFullName(),
                reputation.getCurrentScore(),
                reputation.getCompletedShiftCount(),
                reputation.getLateCount(),
                reputation.getEarlyLeaveCount(),
                reputation.getAbsentCount(),
                reputation.getEvaluationCount(),
                averageRating(reputation),
                reputation.getLastEventAt()
        );
    }

    private Double averageRating(EmployeeReputation reputation) {
        if (reputation.getEvaluationCount() == 0) {
            return null;
        }
        double value = (double) reputation.getRatingSum()
                / reputation.getEvaluationCount();
        return Math.round(value * 100.0) / 100.0;
    }

    private ReputationEventResponse toEventResponse(ReputationEvent event) {
        return new ReputationEventResponse(
                event.getId(),
                event.getEventType(),
                event.getSourceType(),
                event.getSourceId(),
                event.getScoreBefore(),
                event.getScoreDelta(),
                event.getScoreAfter(),
                event.getReason(),
                event.getActorUser() == null
                        ? null
                        : event.getActorUser().getUsername(),
                event.getOccurredAt()
        );
    }

    private EmployeeEvaluationResponse toEvaluationResponse(
            EmployeeEvaluation evaluation
    ) {
        ShiftAssignment assignment = evaluation.getAssignment();
        return new EmployeeEvaluationResponse(
                evaluation.getId(),
                assignment.getId(),
                assignment.getShift().getId(),
                assignment.getShift().getName(),
                assignment.getShift().getEvent().getName(),
                assignment.getShift().getEvent().getVenue().getName(),
                evaluation.getRating(),
                evaluation.getComment(),
                evaluation.getEvaluatedBy().getUsername(),
                evaluation.getEvaluatedAt()
        );
    }
}
