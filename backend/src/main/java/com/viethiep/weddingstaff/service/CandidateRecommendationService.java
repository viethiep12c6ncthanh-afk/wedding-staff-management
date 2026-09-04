package com.viethiep.weddingstaff.service;

import com.viethiep.weddingstaff.dto.ReplacementCandidateResponse;
import com.viethiep.weddingstaff.entity.Employee;
import com.viethiep.weddingstaff.entity.EmployeeReputation;
import com.viethiep.weddingstaff.entity.ReplacementRequest;
import com.viethiep.weddingstaff.entity.ShiftAssignment;
import com.viethiep.weddingstaff.entity.WorkShift;
import com.viethiep.weddingstaff.enumtype.*;
import com.viethiep.weddingstaff.exception.ResourceNotFoundException;
import com.viethiep.weddingstaff.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CandidateRecommendationService {
    private static final EnumSet<AssignmentStatus> ACTIVE_ASSIGNMENT_STATUSES =
            EnumSet.of(AssignmentStatus.ASSIGNED, AssignmentStatus.CONFIRMED);

    private static final int DEFAULT_REPUTATION_SCORE = 80;
    private static final int DEFAULT_RELIABILITY_PERCENT = 80;

    private final ReplacementRequestRepository requestRepository;
    private final ReplacementInvitationRepository invitationRepository;
    private final EmployeeRepository employeeRepository;
    private final EmployeeReputationRepository reputationRepository;
    private final ShiftAssignmentRepository assignmentRepository;

    @Transactional(readOnly = true)
    public List<ReplacementCandidateResponse> findCandidates(Long requestId) {
        ReplacementRequest request = requestRepository.findByIdWithDetails(requestId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy yêu cầu thay ca"
                ));
        ensureRequestCanRecommend(request);

        ShiftAssignment original = request.getOriginalAssignment();
        WorkShift shift = original.getShift();
        ensureBeforeShiftStart(shift);
        ensureShiftCanUseReplacement(shift);

        Set<Long> invitedEmployeeIds = invitationRepository
                .findAllByRequestIdWithDetails(requestId)
                .stream()
                .map(invitation -> invitation.getEmployee().getId())
                .collect(Collectors.toSet());

        Set<Long> busyEmployeeIds = assignmentRepository
                .findAllForCoordinationWindow(
                        shift.getStartAt(),
                        shift.getEndAt(),
                        ACTIVE_ASSIGNMENT_STATUSES
                )
                .stream()
                .map(assignment -> assignment.getEmployee().getId())
                .collect(Collectors.toSet());

        Map<Long, EmployeeReputation> reputationByEmployeeId = reputationRepository
                .findAllWithEmployee()
                .stream()
                .collect(Collectors.toMap(
                        reputation -> reputation.getEmployee().getId(),
                        Function.identity()
                ));

        List<ScoredCandidate> scored = employeeRepository
                .findCandidatePool(
                        EmployeeStatus.ACTIVE,
                        AccountStatus.ACTIVE,
                        RoleName.EMPLOYEE
                )
                .stream()
                .filter(employee -> !employee.getId().equals(
                        original.getEmployee().getId()
                ))
                .filter(employee -> !invitedEmployeeIds.contains(employee.getId()))
                .filter(employee -> !busyEmployeeIds.contains(employee.getId()))
                .map(employee -> score(
                        employee,
                        reputationByEmployeeId.get(employee.getId()),
                        original.getShiftRole()
                ))
                .sorted(candidateComparator())
                .toList();

        List<ReplacementCandidateResponse> responses = new ArrayList<>();
        for (int index = 0; index < scored.size(); index++) {
            responses.add(toResponse(index + 1, scored.get(index)));
        }
        return responses;
    }

    private ScoredCandidate score(
            Employee employee,
            EmployeeReputation reputation,
            ShiftRole targetRole
    ) {
        int reputationScore = reputation == null
                ? DEFAULT_REPUTATION_SCORE
                : clamp(reputation.getCurrentScore(), 0, 100);
        int attendanceCompletedShiftCount = reputation == null
                ? 0
                : nonNegative(reputation.getCompletedShiftCount());
        int lateCount = reputation == null
                ? 0
                : nonNegative(reputation.getLateCount());
        int earlyLeaveCount = reputation == null
                ? 0
                : nonNegative(reputation.getEarlyLeaveCount());
        int absentCount = reputation == null
                ? 0
                : nonNegative(reputation.getAbsentCount());

        int reliabilityPercent = reliabilityPercent(
                attendanceCompletedShiftCount,
                lateCount,
                earlyLeaveCount,
                absentCount
        );
        int completedShiftCount = Math.toIntExact(
                assignmentRepository.countByEmployeeIdAndStatus(
                        employee.getId(),
                        AssignmentStatus.COMPLETED
                )
        );
        int sameRoleCompletedCount = Math.toIntExact(
                assignmentRepository.countByEmployeeIdAndShiftRoleAndStatus(
                        employee.getId(),
                        targetRole,
                        AssignmentStatus.COMPLETED
                )
        );

        int reputationPoints = Math.round(reputationScore * 0.50f);
        int reliabilityPoints = Math.round(reliabilityPercent * 0.25f);
        int experiencePoints = Math.min(completedShiftCount, 15);
        int sameRolePoints = Math.min(sameRoleCompletedCount * 2, 10);
        int totalScore = reputationPoints
                + reliabilityPoints
                + experiencePoints
                + sameRolePoints;

        List<String> reasons = List.of(
                "Uy tín " + reputationScore + "/100 → "
                        + reputationPoints + "/50 điểm",
                "Độ ổn định " + reliabilityPercent + "/100 → "
                        + reliabilityPoints + "/25 điểm",
                "Đã hoàn thành " + completedShiftCount + " ca → "
                        + experiencePoints + "/15 điểm",
                "Đã hoàn thành " + sameRoleCompletedCount + " ca vai trò "
                        + targetRole + " → " + sameRolePoints + "/10 điểm",
                "Không có phân công hoạt động trùng thời gian với ca cần thay"
        );

        return new ScoredCandidate(
                employee,
                totalScore,
                reputationScore,
                reputationPoints,
                reliabilityPercent,
                reliabilityPoints,
                completedShiftCount,
                experiencePoints,
                sameRoleCompletedCount,
                sameRolePoints,
                reasons
        );
    }

    private int reliabilityPercent(
            int completedShiftCount,
            int lateCount,
            int earlyLeaveCount,
            int absentCount
    ) {
        int observedShiftCount = completedShiftCount + absentCount;
        if (observedShiftCount == 0) {
            return DEFAULT_RELIABILITY_PERCENT;
        }

        double quality = completedShiftCount * 100.0
                - lateCount * 25.0
                - earlyLeaveCount * 25.0;
        return clamp(
                (int) Math.round(quality / observedShiftCount),
                0,
                100
        );
    }

    private Comparator<ScoredCandidate> candidateComparator() {
        return Comparator
                .comparingInt(ScoredCandidate::totalScore).reversed()
                .thenComparing(
                        Comparator.comparingInt(
                                ScoredCandidate::reputationScore
                        ).reversed()
                )
                .thenComparing(
                        Comparator.comparingInt(
                                ScoredCandidate::completedShiftCount
                        ).reversed()
                )
                .thenComparing(candidate -> candidate.employee().getEmployeeCode())
                .thenComparing(candidate -> candidate.employee().getId());
    }

    private ReplacementCandidateResponse toResponse(
            int rank,
            ScoredCandidate candidate
    ) {
        Employee employee = candidate.employee();
        return new ReplacementCandidateResponse(
                rank,
                employee.getId(),
                employee.getEmployeeCode(),
                employee.getUser().getFullName(),
                candidate.totalScore(),
                candidate.reputationScore(),
                candidate.reputationPoints(),
                candidate.reliabilityPercent(),
                candidate.reliabilityPoints(),
                candidate.completedShiftCount(),
                candidate.experiencePoints(),
                candidate.sameRoleCompletedCount(),
                candidate.sameRolePoints(),
                candidate.reasons()
        );
    }

    private void ensureRequestCanRecommend(ReplacementRequest request) {
        if (request.getStatus() != ReplacementRequestStatus.OPEN) {
            throw new IllegalStateException(
                    "Chỉ gợi ý ứng viên khi yêu cầu thay ca đang tìm người thay"
            );
        }
    }

    private void ensureBeforeShiftStart(WorkShift shift) {
        if (!LocalDateTime.now().isBefore(shift.getStartAt())) {
            throw new IllegalStateException(
                    "Không thể gợi ý ứng viên khi ca đã bắt đầu"
            );
        }
    }

    private void ensureShiftCanUseReplacement(WorkShift shift) {
        if (shift.getShiftStatus() != ShiftStatus.OPEN
                && shift.getShiftStatus() != ShiftStatus.CLOSED) {
            throw new IllegalStateException(
                    "Chỉ gợi ý ứng viên khi ca đang mở hoặc đã đóng đăng ký"
            );
        }
    }

    private int nonNegative(Integer value) {
        return value == null ? 0 : Math.max(value, 0);
    }

    private int clamp(Integer value, int min, int max) {
        int safeValue = value == null ? min : value;
        return Math.max(min, Math.min(max, safeValue));
    }

    private record ScoredCandidate(
            Employee employee,
            int totalScore,
            int reputationScore,
            int reputationPoints,
            int reliabilityPercent,
            int reliabilityPoints,
            int completedShiftCount,
            int experiencePoints,
            int sameRoleCompletedCount,
            int sameRolePoints,
            List<String> reasons
    ) {
    }
}
