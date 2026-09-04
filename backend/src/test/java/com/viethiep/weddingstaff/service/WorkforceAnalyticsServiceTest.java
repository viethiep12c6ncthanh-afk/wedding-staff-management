package com.viethiep.weddingstaff.service;

import com.viethiep.weddingstaff.dto.WorkforceAnalyticsResponse;
import com.viethiep.weddingstaff.entity.Attendance;
import com.viethiep.weddingstaff.entity.Employee;
import com.viethiep.weddingstaff.entity.EmployeeReputation;
import com.viethiep.weddingstaff.entity.Event;
import com.viethiep.weddingstaff.entity.ShiftAssignment;
import com.viethiep.weddingstaff.entity.UserAccount;
import com.viethiep.weddingstaff.entity.Venue;
import com.viethiep.weddingstaff.entity.WorkShift;
import com.viethiep.weddingstaff.enumtype.AttendanceProcessStatus;
import com.viethiep.weddingstaff.enumtype.AttendanceResult;
import com.viethiep.weddingstaff.repository.AttendanceRepository;
import com.viethiep.weddingstaff.repository.EmployeeReputationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkforceAnalyticsServiceTest {
    @Mock
    private AttendanceRepository attendanceRepository;

    @Mock
    private EmployeeReputationRepository reputationRepository;

    @InjectMocks
    private WorkforceAnalyticsService service;

    @Test
    void buildsAttendanceTrendWorkloadAndReputationDistribution() {
        LocalDate from = LocalDate.of(2026, 9, 1);
        LocalDate to = LocalDate.of(2026, 9, 3);

        Employee first = employee(1L, "NV001", "Nguyễn A");
        Employee second = employee(2L, "NV002", "Nguyễn B");

        when(attendanceRepository.findForPayroll(
                eq(AttendanceProcessStatus.CONFIRMED),
                isNull(),
                any(),
                any()
        )).thenReturn(List.of(
                attendance(first, AttendanceResult.PRESENT, 1),
                attendance(first, AttendanceResult.LATE, 2),
                attendance(first, AttendanceResult.ABSENT, 3),
                attendance(
                        second,
                        AttendanceResult.LATE_AND_EARLY_LEAVE,
                        2
                )
        ));

        when(reputationRepository.findAllWithEmployee())
                .thenReturn(List.of(
                        reputation(first, 95),
                        reputation(second, 82),
                        reputation(
                                employee(3L, "NV003", "Nguyễn C"),
                                70
                        ),
                        reputation(
                                employee(4L, "NV004", "Nguyễn D"),
                                45
                        )
                ));

        WorkforceAnalyticsResponse result =
                service.analyze(from, to);

        assertEquals(4, result.confirmedAttendances());
        assertEquals(1, result.presentCount());
        assertEquals(1, result.lateCount());
        assertEquals(1, result.lateAndEarlyLeaveCount());
        assertEquals(1, result.absentCount());
        assertEquals(50.0, result.lateRate());
        assertEquals(25.0, result.absenceRate());

        assertEquals(3, result.attendanceTrend().size());
        assertEquals(
                2,
                result.attendanceTrend().get(1).confirmedCount()
        );
        assertEquals(
                2,
                result.attendanceTrend().get(1).lateCount()
        );

        assertEquals(2, result.topWorkload().size());
        assertEquals("NV001", result.topWorkload().getFirst().employeeCode());
        assertEquals(3, result.topWorkload().getFirst().confirmedShiftCount());
        assertEquals(2, result.topWorkload().getFirst().paidShiftCount());

        assertEquals(4, result.reputationDistribution().size());
        assertEquals(
                1,
                result.reputationDistribution().get(0).employeeCount()
        );
        assertEquals(
                1,
                result.reputationDistribution().get(3).employeeCount()
        );
    }

    private Attendance attendance(
            Employee employee,
            AttendanceResult result,
            int day
    ) {
        Venue venue = Venue.builder()
                .id(1L)
                .name("Venue")
                .address("Address")
                .build();

        Event event = Event.builder()
                .id(10L + day)
                .name("Event")
                .venue(venue)
                .build();

        WorkShift shift = WorkShift.builder()
                .id(20L + day)
                .event(event)
                .name("Shift")
                .startAt(LocalDateTime.of(2026, 9, day, 8, 0))
                .endAt(LocalDateTime.of(2026, 9, day, 12, 0))
                .build();

        ShiftAssignment assignment = ShiftAssignment.builder()
                .id(30L + day)
                .employee(employee)
                .shift(shift)
                .build();

        return Attendance.builder()
                .assignment(assignment)
                .processStatus(AttendanceProcessStatus.CONFIRMED)
                .attendanceResult(result)
                .build();
    }

    private Employee employee(
            Long id,
            String code,
            String fullName
    ) {
        return Employee.builder()
                .id(id)
                .employeeCode(code)
                .user(UserAccount.builder()
                        .id(id)
                        .username(code.toLowerCase())
                        .fullName(fullName)
                        .passwordHash("x")
                        .build())
                .build();
    }

    private EmployeeReputation reputation(
            Employee employee,
            int score
    ) {
        return EmployeeReputation.builder()
                .employee(employee)
                .currentScore(score)
                .build();
    }
}
