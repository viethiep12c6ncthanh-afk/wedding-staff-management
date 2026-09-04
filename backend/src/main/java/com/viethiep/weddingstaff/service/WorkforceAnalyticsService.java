package com.viethiep.weddingstaff.service;

import com.viethiep.weddingstaff.dto.AttendanceTrendPointResponse;
import com.viethiep.weddingstaff.dto.ReputationBucketResponse;
import com.viethiep.weddingstaff.dto.WorkforceAnalyticsResponse;
import com.viethiep.weddingstaff.dto.WorkloadItemResponse;
import com.viethiep.weddingstaff.entity.Attendance;
import com.viethiep.weddingstaff.entity.Employee;
import com.viethiep.weddingstaff.entity.EmployeeReputation;
import com.viethiep.weddingstaff.enumtype.AttendanceProcessStatus;
import com.viethiep.weddingstaff.enumtype.AttendanceResult;
import com.viethiep.weddingstaff.repository.AttendanceRepository;
import com.viethiep.weddingstaff.repository.EmployeeReputationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class WorkforceAnalyticsService {
    private static final int TOP_WORKLOAD_LIMIT = 10;

    private final AttendanceRepository attendanceRepository;
    private final EmployeeReputationRepository reputationRepository;

    @Transactional(readOnly = true)
    public WorkforceAnalyticsResponse analyze(
            LocalDate from,
            LocalDate to
    ) {
        validateRange(from, to);

        LocalDateTime fromAt = from.atStartOfDay();
        LocalDateTime toExclusive = to.plusDays(1).atStartOfDay();

        List<Attendance> attendances = attendanceRepository.findForPayroll(
                AttendanceProcessStatus.CONFIRMED,
                null,
                fromAt,
                toExclusive
        );

        long present = 0;
        long late = 0;
        long earlyLeave = 0;
        long lateAndEarlyLeave = 0;
        long absent = 0;

        Map<LocalDate, TrendAccumulator> trendByDate =
                initializeTrend(from, to);
        Map<Long, WorkloadAccumulator> workloadByEmployee =
                new LinkedHashMap<>();

        for (Attendance attendance : attendances) {
            AttendanceResult result = attendance.getAttendanceResult();

            if (result == AttendanceResult.PRESENT) {
                present++;
            } else if (result == AttendanceResult.LATE) {
                late++;
            } else if (result == AttendanceResult.EARLY_LEAVE) {
                earlyLeave++;
            } else if (result == AttendanceResult.LATE_AND_EARLY_LEAVE) {
                lateAndEarlyLeave++;
            } else if (result == AttendanceResult.ABSENT) {
                absent++;
            }

            LocalDate shiftDate = attendance
                    .getAssignment()
                    .getShift()
                    .getStartAt()
                    .toLocalDate();

            TrendAccumulator trend = trendByDate.computeIfAbsent(
                    shiftDate,
                    ignored -> new TrendAccumulator()
            );
            trend.confirmed++;
            if (result == AttendanceResult.LATE
                    || result == AttendanceResult.LATE_AND_EARLY_LEAVE) {
                trend.late++;
            }
            if (result == AttendanceResult.ABSENT) {
                trend.absent++;
            }

            Employee employee = attendance.getAssignment().getEmployee();
            WorkloadAccumulator workload = workloadByEmployee.computeIfAbsent(
                    employee.getId(),
                    ignored -> new WorkloadAccumulator(
                            employee.getId(),
                            employee.getEmployeeCode(),
                            employee.getUser().getFullName()
                    )
            );
            workload.confirmedShiftCount++;
            if (result != AttendanceResult.ABSENT) {
                workload.paidShiftCount++;
            }
        }

        long lateForRate = late + lateAndEarlyLeave;

        return new WorkforceAnalyticsResponse(
                from,
                to,
                attendances.size(),
                present,
                late,
                earlyLeave,
                lateAndEarlyLeave,
                absent,
                percentage(lateForRate, attendances.size()),
                percentage(absent, attendances.size()),
                toTrendResponses(trendByDate),
                toTopWorkload(workloadByEmployee),
                buildReputationDistribution()
        );
    }

    private Map<LocalDate, TrendAccumulator> initializeTrend(
            LocalDate from,
            LocalDate to
    ) {
        Map<LocalDate, TrendAccumulator> result = new LinkedHashMap<>();
        LocalDate cursor = from;
        while (!cursor.isAfter(to)) {
            result.put(cursor, new TrendAccumulator());
            cursor = cursor.plusDays(1);
        }
        return result;
    }

    private List<AttendanceTrendPointResponse> toTrendResponses(
            Map<LocalDate, TrendAccumulator> trendByDate
    ) {
        return trendByDate.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new AttendanceTrendPointResponse(
                        entry.getKey(),
                        entry.getValue().confirmed,
                        entry.getValue().late,
                        entry.getValue().absent
                ))
                .toList();
    }

    private List<WorkloadItemResponse> toTopWorkload(
            Map<Long, WorkloadAccumulator> workloadByEmployee
    ) {
        List<WorkloadAccumulator> values =
                new ArrayList<>(workloadByEmployee.values());

        values.sort((left, right) -> {
            int byPaid = Long.compare(
                    right.paidShiftCount,
                    left.paidShiftCount
            );
            if (byPaid != 0) {
                return byPaid;
            }

            int byConfirmed = Long.compare(
                    right.confirmedShiftCount,
                    left.confirmedShiftCount
            );
            if (byConfirmed != 0) {
                return byConfirmed;
            }

            return String.CASE_INSENSITIVE_ORDER.compare(
                    left.employeeCode,
                    right.employeeCode
            );
        });

        return values.stream()
                .limit(TOP_WORKLOAD_LIMIT)
                .map(item -> new WorkloadItemResponse(
                        item.employeeId,
                        item.employeeCode,
                        item.fullName,
                        item.confirmedShiftCount,
                        item.paidShiftCount
                ))
                .toList();
    }

    private List<ReputationBucketResponse> buildReputationDistribution() {
        List<EmployeeReputation> reputations =
                reputationRepository.findAllWithEmployee();

        return List.of(
                bucket(
                        "EXCELLENT",
                        "Xuất sắc",
                        90,
                        100,
                        reputations
                ),
                bucket(
                        "GOOD",
                        "Tốt",
                        80,
                        89,
                        reputations
                ),
                bucket(
                        "WATCH",
                        "Cần theo dõi",
                        60,
                        79,
                        reputations
                ),
                bucket(
                        "RISK",
                        "Rủi ro",
                        0,
                        59,
                        reputations
                )
        );
    }

    private ReputationBucketResponse bucket(
            String code,
            String label,
            int min,
            int max,
            List<EmployeeReputation> reputations
    ) {
        long count = reputations.stream()
                .map(EmployeeReputation::getCurrentScore)
                .filter(score -> score != null)
                .mapToInt(score -> Math.max(0, Math.min(100, score)))
                .filter(score -> score >= min && score <= max)
                .count();

        return new ReputationBucketResponse(
                code,
                label,
                min,
                max,
                count
        );
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

    private static final class TrendAccumulator {
        private long confirmed;
        private long late;
        private long absent;
    }

    private static final class WorkloadAccumulator {
        private final Long employeeId;
        private final String employeeCode;
        private final String fullName;
        private long confirmedShiftCount;
        private long paidShiftCount;

        private WorkloadAccumulator(
                Long employeeId,
                String employeeCode,
                String fullName
        ) {
            this.employeeId = employeeId;
            this.employeeCode = employeeCode;
            this.fullName = fullName;
        }
    }
}
