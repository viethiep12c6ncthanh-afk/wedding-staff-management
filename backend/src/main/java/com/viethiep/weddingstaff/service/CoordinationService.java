package com.viethiep.weddingstaff.service;

import com.viethiep.weddingstaff.dto.CoordinationIssueResponse;
import com.viethiep.weddingstaff.dto.CoordinationOverviewResponse;
import com.viethiep.weddingstaff.dto.CoordinationShiftResponse;
import com.viethiep.weddingstaff.entity.ShiftAssignment;
import com.viethiep.weddingstaff.entity.WorkShift;
import com.viethiep.weddingstaff.enumtype.AssignmentStatus;
import com.viethiep.weddingstaff.enumtype.CoordinationIssueType;
import com.viethiep.weddingstaff.enumtype.CoordinationStaffingStatus;
import com.viethiep.weddingstaff.enumtype.ShiftStatus;
import com.viethiep.weddingstaff.repository.ShiftAssignmentRepository;
import com.viethiep.weddingstaff.repository.WorkShiftRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CoordinationService {
    private static final int MAX_TRANSITION_BUFFER_MINUTES = 720;

    private static final EnumSet<ShiftStatus> VISIBLE_SHIFT_STATUSES =
            EnumSet.of(
                    ShiftStatus.DRAFT,
                    ShiftStatus.OPEN,
                    ShiftStatus.CLOSED,
                    ShiftStatus.IN_PROGRESS,
                    ShiftStatus.COMPLETED
            );

    private static final EnumSet<AssignmentStatus> SCHEDULE_STATUSES =
            EnumSet.of(
                    AssignmentStatus.ASSIGNED,
                    AssignmentStatus.CONFIRMED,
                    AssignmentStatus.COMPLETED,
                    AssignmentStatus.ABSENT
            );

    private static final EnumSet<AssignmentStatus> EFFECTIVE_STAFF_STATUSES =
            EnumSet.of(
                    AssignmentStatus.ASSIGNED,
                    AssignmentStatus.CONFIRMED,
                    AssignmentStatus.COMPLETED
            );

    private final WorkShiftRepository shiftRepository;
    private final ShiftAssignmentRepository assignmentRepository;

    @Transactional(readOnly = true)
    public CoordinationOverviewResponse overview(
            LocalDate date,
            LocalTime fromTime,
            LocalTime toTime,
            Long venueId,
            int transitionBufferMinutes
    ) {
        if (date == null) {
            throw new IllegalArgumentException("Ngày điều phối là bắt buộc");
        }
        if (transitionBufferMinutes < 0
                || transitionBufferMinutes > MAX_TRANSITION_BUFFER_MINUTES) {
            throw new IllegalArgumentException(
                    "Khoảng đệm di chuyển phải từ 0 đến "
                            + MAX_TRANSITION_BUFFER_MINUTES
                            + " phút"
            );
        }

        LocalDateTime startAt = fromTime == null
                ? date.atStartOfDay()
                : date.atTime(fromTime);
        LocalDateTime endAt = toTime == null
                ? date.plusDays(1).atStartOfDay()
                : date.atTime(toTime);

        if (!endAt.isAfter(startAt)) {
            throw new IllegalArgumentException(
                    "Giờ kết thúc bộ lọc phải sau giờ bắt đầu"
            );
        }

        List<WorkShift> shifts = shiftRepository
                .findForCoordinationWindow(
                        startAt,
                        endAt,
                        venueId,
                        VISIBLE_SHIFT_STATUSES
                );

        List<ShiftAssignment> assignments = assignmentRepository
                .findAllForCoordinationWindow(
                        startAt,
                        endAt,
                        SCHEDULE_STATUSES
                );

        Map<Long, Long> effectiveStaffByShift =
                countEffectiveStaff(assignments);

        List<CoordinationShiftResponse> shiftResponses = shifts.stream()
                .map(shift -> toShiftResponse(
                        shift,
                        effectiveStaffByShift.getOrDefault(
                                shift.getId(),
                                0L
                        )
                ))
                .toList();

        long understaffed = shiftResponses.stream()
                .filter(response -> response.staffingStatus()
                        == CoordinationStaffingStatus.UNDERSTAFFED)
                .count();
        long full = shiftResponses.stream()
                .filter(response -> response.staffingStatus()
                        == CoordinationStaffingStatus.FULL)
                .count();
        long overstaffed = shiftResponses.stream()
                .filter(response -> response.staffingStatus()
                        == CoordinationStaffingStatus.OVERSTAFFED)
                .count();

        List<CoordinationIssueResponse> issues = buildScheduleIssues(
                assignments,
                venueId,
                transitionBufferMinutes
        );

        return new CoordinationOverviewResponse(
                date,
                startAt,
                endAt,
                venueId,
                transitionBufferMinutes,
                shiftResponses.size(),
                understaffed,
                full,
                overstaffed,
                shiftResponses,
                issues
        );
    }

    private Map<Long, Long> countEffectiveStaff(
            List<ShiftAssignment> assignments
    ) {
        Map<Long, Long> counts = new HashMap<>();

        for (ShiftAssignment assignment : assignments) {
            if (!EFFECTIVE_STAFF_STATUSES.contains(
                    assignment.getStatus()
            )) {
                continue;
            }

            counts.merge(
                    assignment.getShift().getId(),
                    1L,
                    Long::sum
            );
        }

        return counts;
    }

    private CoordinationShiftResponse toShiftResponse(
            WorkShift shift,
            long effectiveStaffCount
    ) {
        long required = shift.getRequiredStaff();
        long missing = Math.max(required - effectiveStaffCount, 0L);
        long extra = Math.max(effectiveStaffCount - required, 0L);

        CoordinationStaffingStatus staffingStatus;
        if (effectiveStaffCount < required) {
            staffingStatus = CoordinationStaffingStatus.UNDERSTAFFED;
        } else if (effectiveStaffCount > required) {
            staffingStatus = CoordinationStaffingStatus.OVERSTAFFED;
        } else {
            staffingStatus = CoordinationStaffingStatus.FULL;
        }

        return new CoordinationShiftResponse(
                shift.getId(),
                shift.getEvent().getId(),
                shift.getEvent().getName(),
                shift.getEvent().getVenue().getId(),
                shift.getEvent().getVenue().getName(),
                shift.getEvent().getVenue().getAddress(),
                shift.getName(),
                shift.getStartAt(),
                shift.getEndAt(),
                shift.getShiftStatus(),
                shift.getRequiredStaff(),
                effectiveStaffCount,
                missing,
                extra,
                staffingStatus
        );
    }

    private List<CoordinationIssueResponse> buildScheduleIssues(
            List<ShiftAssignment> assignments,
            Long venueId,
            int transitionBufferMinutes
    ) {
        Map<Long, List<ShiftAssignment>> assignmentsByEmployee =
                new HashMap<>();

        for (ShiftAssignment assignment : assignments) {
            assignmentsByEmployee
                    .computeIfAbsent(
                            assignment.getEmployee().getId(),
                            ignored -> new ArrayList<>()
                    )
                    .add(assignment);
        }

        List<CoordinationIssueResponse> issues = new ArrayList<>();

        for (List<ShiftAssignment> employeeAssignments
                : assignmentsByEmployee.values()) {
            employeeAssignments.sort(
                    Comparator.comparing(
                            assignment -> assignment
                                    .getShift()
                                    .getStartAt()
                    )
            );

            addOverlapIssues(
                    employeeAssignments,
                    venueId,
                    transitionBufferMinutes,
                    issues
            );
            addTransitionIssues(
                    employeeAssignments,
                    venueId,
                    transitionBufferMinutes,
                    issues
            );
        }

        return issues.stream()
                .sorted(Comparator
                        .comparing(CoordinationIssueResponse::employeeName)
                        .thenComparing(
                                CoordinationIssueResponse::secondStartAt
                        ))
                .toList();
    }

    private void addOverlapIssues(
            List<ShiftAssignment> assignments,
            Long venueId,
            int transitionBufferMinutes,
            List<CoordinationIssueResponse> issues
    ) {
        for (int firstIndex = 0;
             firstIndex < assignments.size();
             firstIndex++) {
            ShiftAssignment first = assignments.get(firstIndex);

            for (int secondIndex = firstIndex + 1;
                 secondIndex < assignments.size();
                 secondIndex++) {
                ShiftAssignment second = assignments.get(secondIndex);

                if (!second.getShift().getStartAt()
                        .isBefore(first.getShift().getEndAt())) {
                    break;
                }

                LocalDateTime overlapEnd = first.getShift().getEndAt()
                        .isBefore(second.getShift().getEndAt())
                        ? first.getShift().getEndAt()
                        : second.getShift().getEndAt();

                long overlapMinutes = Duration.between(
                        second.getShift().getStartAt(),
                        overlapEnd
                ).toMinutes();

                if (overlapMinutes > 0
                        && matchesVenueFilter(first, second, venueId)) {
                    issues.add(toIssueResponse(
                            CoordinationIssueType.OVERLAP_CONFLICT,
                            first,
                            second,
                            0L,
                            overlapMinutes,
                            transitionBufferMinutes
                    ));
                }
            }
        }
    }

    private void addTransitionIssues(
            List<ShiftAssignment> assignments,
            Long venueId,
            int transitionBufferMinutes,
            List<CoordinationIssueResponse> issues
    ) {
        if (transitionBufferMinutes == 0) {
            return;
        }

        for (int index = 0; index + 1 < assignments.size(); index++) {
            ShiftAssignment first = assignments.get(index);
            ShiftAssignment second = assignments.get(index + 1);

            if (second.getShift().getStartAt()
                    .isBefore(first.getShift().getEndAt())) {
                continue;
            }

            if (sameVenue(first, second)) {
                continue;
            }

            long gapMinutes = Duration.between(
                    first.getShift().getEndAt(),
                    second.getShift().getStartAt()
            ).toMinutes();

            if (gapMinutes < transitionBufferMinutes
                    && matchesVenueFilter(first, second, venueId)) {
                issues.add(toIssueResponse(
                        CoordinationIssueType
                                .INSUFFICIENT_TRANSITION_TIME,
                        first,
                        second,
                        gapMinutes,
                        0L,
                        transitionBufferMinutes
                ));
            }
        }
    }

    private boolean sameVenue(
            ShiftAssignment first,
            ShiftAssignment second
    ) {
        return first.getShift().getEvent().getVenue().getId()
                .equals(second.getShift().getEvent().getVenue().getId());
    }

    private boolean matchesVenueFilter(
            ShiftAssignment first,
            ShiftAssignment second,
            Long venueId
    ) {
        if (venueId == null) {
            return true;
        }

        return venueId.equals(
                first.getShift().getEvent().getVenue().getId()
        ) || venueId.equals(
                second.getShift().getEvent().getVenue().getId()
        );
    }

    private CoordinationIssueResponse toIssueResponse(
            CoordinationIssueType issueType,
            ShiftAssignment first,
            ShiftAssignment second,
            long gapMinutes,
            long overlapMinutes,
            int requiredBufferMinutes
    ) {
        return new CoordinationIssueResponse(
                issueType,
                first.getEmployee().getId(),
                first.getEmployee().getUser().getFullName(),
                first.getShift().getId(),
                first.getShift().getName(),
                first.getShift().getEvent().getVenue().getId(),
                first.getShift().getEvent().getVenue().getName(),
                first.getShift().getStartAt(),
                first.getShift().getEndAt(),
                second.getShift().getId(),
                second.getShift().getName(),
                second.getShift().getEvent().getVenue().getId(),
                second.getShift().getEvent().getVenue().getName(),
                second.getShift().getStartAt(),
                second.getShift().getEndAt(),
                gapMinutes,
                overlapMinutes,
                requiredBufferMinutes
        );
    }
}
