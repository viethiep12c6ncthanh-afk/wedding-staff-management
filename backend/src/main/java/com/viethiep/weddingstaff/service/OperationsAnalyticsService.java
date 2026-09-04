package com.viethiep.weddingstaff.service;

import com.viethiep.weddingstaff.dto.OperationsAnalyticsResponse;
import com.viethiep.weddingstaff.entity.ReplacementRequest;
import com.viethiep.weddingstaff.entity.ShiftAssignment;
import com.viethiep.weddingstaff.entity.WorkShift;
import com.viethiep.weddingstaff.enumtype.AssignmentStatus;
import com.viethiep.weddingstaff.enumtype.ReplacementRequestStatus;
import com.viethiep.weddingstaff.enumtype.ShiftStatus;
import com.viethiep.weddingstaff.repository.ReplacementRequestRepository;
import com.viethiep.weddingstaff.repository.ShiftAssignmentRepository;
import com.viethiep.weddingstaff.repository.WorkShiftRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class OperationsAnalyticsService {
    private static final EnumSet<ShiftStatus> VISIBLE_SHIFT_STATUSES =
            EnumSet.of(
                    ShiftStatus.DRAFT,
                    ShiftStatus.OPEN,
                    ShiftStatus.CLOSED,
                    ShiftStatus.IN_PROGRESS,
                    ShiftStatus.COMPLETED
            );

    private static final EnumSet<AssignmentStatus> EFFECTIVE_STAFF_STATUSES =
            EnumSet.of(
                    AssignmentStatus.ASSIGNED,
                    AssignmentStatus.CONFIRMED,
                    AssignmentStatus.COMPLETED
            );

    private final WorkShiftRepository shiftRepository;
    private final ShiftAssignmentRepository assignmentRepository;
    private final ReplacementRequestRepository replacementRequestRepository;

    @Transactional(readOnly = true)
    public OperationsAnalyticsResponse analyze(
            LocalDate from,
            LocalDate to,
            Long venueId
    ) {
        validateRange(from, to);

        LocalDateTime fromAt = from.atStartOfDay();
        LocalDateTime toExclusive = to.plusDays(1).atStartOfDay();

        List<WorkShift> shifts = shiftRepository.findForDashboardRange(
                fromAt,
                toExclusive,
                venueId,
                VISIBLE_SHIFT_STATUSES
        );

        Set<Long> selectedShiftIds = new HashSet<>();
        for (WorkShift shift : shifts) {
            selectedShiftIds.add(shift.getId());
        }

        Map<Long, Long> effectiveStaffByShift = new HashMap<>();
        if (!selectedShiftIds.isEmpty()) {
            List<ShiftAssignment> assignments =
                    assignmentRepository.findAllForCoordinationWindow(
                            fromAt,
                            toExclusive,
                            EFFECTIVE_STAFF_STATUSES
                    );

            for (ShiftAssignment assignment : assignments) {
                Long shiftId = assignment.getShift().getId();
                if (selectedShiftIds.contains(shiftId)) {
                    effectiveStaffByShift.merge(shiftId, 1L, Long::sum);
                }
            }
        }

        long understaffed = 0;
        long full = 0;
        long overstaffed = 0;
        long missingStaffTotal = 0;

        for (WorkShift shift : shifts) {
            long required = Math.max(
                    shift.getRequiredStaff() == null
                            ? 0
                            : shift.getRequiredStaff(),
                    0
            );
            long effective = effectiveStaffByShift.getOrDefault(
                    shift.getId(),
                    0L
            );

            if (effective < required) {
                understaffed++;
                missingStaffTotal += required - effective;
            } else if (effective > required) {
                overstaffed++;
            } else {
                full++;
            }
        }

        List<ReplacementRequest> replacementRequests =
                replacementRequestRepository.findForDashboard(
                        fromAt,
                        toExclusive,
                        venueId
                );

        long pending = countStatus(
                replacementRequests,
                ReplacementRequestStatus.PENDING
        );
        long open = countStatus(
                replacementRequests,
                ReplacementRequestStatus.OPEN
        );
        long filled = countStatus(
                replacementRequests,
                ReplacementRequestStatus.FILLED
        );
        long rejected = countStatus(
                replacementRequests,
                ReplacementRequestStatus.REJECTED
        );
        long cancelled = countStatus(
                replacementRequests,
                ReplacementRequestStatus.CANCELLED
        );
        long resolved = filled + rejected + cancelled;

        return new OperationsAnalyticsResponse(
                from,
                to,
                venueId,
                shifts.size(),
                understaffed,
                full,
                overstaffed,
                missingStaffTotal,
                replacementRequests.size(),
                pending,
                open,
                filled,
                rejected,
                cancelled,
                resolved,
                percentage(filled, resolved)
        );
    }

    private long countStatus(
            List<ReplacementRequest> requests,
            ReplacementRequestStatus status
    ) {
        return requests.stream()
                .filter(request -> request.getStatus() == status)
                .count();
    }

    private void validateRange(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new IllegalArgumentException(
                    "Khoảng ngày Dashboard là bắt buộc"
            );
        }
        if (from.isAfter(to)) {
            throw new IllegalArgumentException(
                    "Ngày bắt đầu không được sau ngày kết thúc"
            );
        }
    }

    private double percentage(long numerator, long denominator) {
        if (denominator <= 0) {
            return 0.0;
        }
        return Math.round(
                (numerator * 10000.0) / denominator
        ) / 100.0;
    }
}
