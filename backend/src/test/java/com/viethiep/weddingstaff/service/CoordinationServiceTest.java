package com.viethiep.weddingstaff.service;

import com.viethiep.weddingstaff.dto.CoordinationOverviewResponse;
import com.viethiep.weddingstaff.entity.Employee;
import com.viethiep.weddingstaff.entity.Event;
import com.viethiep.weddingstaff.entity.ShiftAssignment;
import com.viethiep.weddingstaff.entity.UserAccount;
import com.viethiep.weddingstaff.entity.Venue;
import com.viethiep.weddingstaff.entity.WorkShift;
import com.viethiep.weddingstaff.enumtype.AssignmentStatus;
import com.viethiep.weddingstaff.enumtype.CoordinationIssueType;
import com.viethiep.weddingstaff.enumtype.CoordinationStaffingStatus;
import com.viethiep.weddingstaff.enumtype.ShiftStatus;
import com.viethiep.weddingstaff.repository.ShiftAssignmentRepository;
import com.viethiep.weddingstaff.repository.WorkShiftRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CoordinationServiceTest {
    @Mock
    private WorkShiftRepository shiftRepository;

    @Mock
    private ShiftAssignmentRepository assignmentRepository;

    @InjectMocks
    private CoordinationService service;

    private static final LocalDate DATE = LocalDate.of(2026, 8, 20);

    @Test
    void summarizesUnderstaffedFullAndOverstaffedShifts() {
        Venue venue = venue(1L, "Grand Palace");
        WorkShift understaffed = shift(
                11L, venue, "Ca sáng", 8, 10, 12, 0, 3
        );
        WorkShift full = shift(
                12L, venue, "Ca chiều", 13, 0, 15, 0, 2
        );
        WorkShift overstaffed = shift(
                13L, venue, "Ca tối", 17, 0, 20, 0, 1
        );

        when(shiftRepository.findForCoordinationWindow(
                any(), any(), isNull(), anyCollection()
        )).thenReturn(List.of(understaffed, full, overstaffed));

        when(assignmentRepository.findAllForCoordinationWindow(
                any(), any(), anyCollection()
        )).thenReturn(List.of(
                assignment(1L, understaffed, employee(1L, "A")),
                assignment(2L, understaffed, employee(2L, "B")),
                assignment(3L, full, employee(3L, "C")),
                assignment(4L, full, employee(4L, "D")),
                assignment(5L, overstaffed, employee(5L, "E")),
                assignment(6L, overstaffed, employee(6L, "F"))
        ));

        CoordinationOverviewResponse result =
                service.overview(DATE, null, null, null, 60);

        assertEquals(3, result.totalShifts());
        assertEquals(1, result.understaffedShifts());
        assertEquals(1, result.fullShifts());
        assertEquals(1, result.overstaffedShifts());

        assertEquals(
                CoordinationStaffingStatus.UNDERSTAFFED,
                result.shifts().get(0).staffingStatus()
        );
        assertEquals(1, result.shifts().get(0).missingStaffCount());
    }

    @Test
    void detectsOverlapConflict() {
        Employee employee = employee(1L, "Nguyễn A");
        WorkShift first = shift(
                21L, venue(1L, "Venue A"),
                "Ca A", 10, 0, 12, 0, 1
        );
        WorkShift second = shift(
                22L, venue(2L, "Venue B"),
                "Ca B", 11, 0, 13, 0, 1
        );

        stubAssignments(List.of(
                assignment(1L, first, employee),
                assignment(2L, second, employee)
        ));

        CoordinationOverviewResponse result =
                service.overview(DATE, null, null, null, 60);

        assertEquals(1, result.scheduleIssues().size());
        assertEquals(
                CoordinationIssueType.OVERLAP_CONFLICT,
                result.scheduleIssues().get(0).issueType()
        );
        assertEquals(60, result.scheduleIssues().get(0).overlapMinutes());
    }

    @Test
    void detectsInsufficientTransitionTimeBetweenDifferentVenues() {
        Employee employee = employee(1L, "Nguyễn A");
        WorkShift first = shift(
                31L, venue(1L, "Venue A"),
                "Ca A", 10, 0, 12, 0, 1
        );
        WorkShift second = shift(
                32L, venue(2L, "Venue B"),
                "Ca B", 12, 30, 14, 0, 1
        );

        stubAssignments(List.of(
                assignment(1L, first, employee),
                assignment(2L, second, employee)
        ));

        CoordinationOverviewResponse result =
                service.overview(DATE, null, null, null, 60);

        assertEquals(1, result.scheduleIssues().size());
        assertEquals(
                CoordinationIssueType.INSUFFICIENT_TRANSITION_TIME,
                result.scheduleIssues().get(0).issueType()
        );
        assertEquals(30, result.scheduleIssues().get(0).gapMinutes());
        assertEquals(60, result.scheduleIssues().get(0).requiredBufferMinutes());
    }

    @Test
    void doesNotWarnForShortGapAtSameVenue() {
        Employee employee = employee(1L, "Nguyễn A");
        Venue venue = venue(1L, "Venue A");
        WorkShift first = shift(
                41L, venue, "Ca A", 10, 0, 12, 0, 1
        );
        WorkShift second = shift(
                42L, venue, "Ca B", 12, 15, 14, 0, 1
        );

        stubAssignments(List.of(
                assignment(1L, first, employee),
                assignment(2L, second, employee)
        ));

        CoordinationOverviewResponse result =
                service.overview(DATE, null, null, null, 60);

        assertTrue(result.scheduleIssues().isEmpty());
    }

    private void stubAssignments(List<ShiftAssignment> assignments) {
        when(shiftRepository.findForCoordinationWindow(
                any(), any(), isNull(), anyCollection()
        )).thenReturn(List.of());
        when(assignmentRepository.findAllForCoordinationWindow(
                any(), any(), anyCollection()
        )).thenReturn(assignments);
    }

    private Venue venue(Long id, String name) {
        return Venue.builder()
                .id(id)
                .name(name)
                .address(name + " address")
                .build();
    }

    private Employee employee(Long id, String fullName) {
        return Employee.builder()
                .id(id)
                .user(UserAccount.builder()
                        .id(id)
                        .username("user" + id)
                        .fullName(fullName)
                        .passwordHash("x")
                        .build())
                .employeeCode("EMP" + id)
                .build();
    }

    private WorkShift shift(
            Long id,
            Venue venue,
            String name,
            int startHour,
            int startMinute,
            int endHour,
            int endMinute,
            int requiredStaff
    ) {
        Event event = Event.builder()
                .id(id + 100)
                .venue(venue)
                .name("Event " + id)
                .startAt(DATE.atStartOfDay())
                .endAt(DATE.plusDays(1).atStartOfDay())
                .build();

        return WorkShift.builder()
                .id(id)
                .event(event)
                .name(name)
                .startAt(LocalDateTime.of(
                        DATE,
                        java.time.LocalTime.of(startHour, startMinute)
                ))
                .endAt(LocalDateTime.of(
                        DATE,
                        java.time.LocalTime.of(endHour, endMinute)
                ))
                .requiredStaff(requiredStaff)
                .shiftStatus(ShiftStatus.OPEN)
                .build();
    }

    private ShiftAssignment assignment(
            Long id,
            WorkShift shift,
            Employee employee
    ) {
        return ShiftAssignment.builder()
                .id(id)
                .shift(shift)
                .employee(employee)
                .status(AssignmentStatus.ASSIGNED)
                .build();
    }
}
