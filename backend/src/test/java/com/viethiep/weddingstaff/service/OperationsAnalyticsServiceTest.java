package com.viethiep.weddingstaff.service;

import com.viethiep.weddingstaff.dto.OperationsAnalyticsResponse;
import com.viethiep.weddingstaff.entity.Event;
import com.viethiep.weddingstaff.entity.ReplacementRequest;
import com.viethiep.weddingstaff.entity.ShiftAssignment;
import com.viethiep.weddingstaff.entity.Venue;
import com.viethiep.weddingstaff.entity.WorkShift;
import com.viethiep.weddingstaff.enumtype.AssignmentStatus;
import com.viethiep.weddingstaff.enumtype.ReplacementRequestStatus;
import com.viethiep.weddingstaff.enumtype.ShiftStatus;
import com.viethiep.weddingstaff.repository.ReplacementRequestRepository;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OperationsAnalyticsServiceTest {
    @Mock
    private WorkShiftRepository shiftRepository;

    @Mock
    private ShiftAssignmentRepository assignmentRepository;

    @Mock
    private ReplacementRequestRepository replacementRequestRepository;

    @InjectMocks
    private OperationsAnalyticsService service;

    @Test
    void summarizesStaffingAndReplacementWorkflow() {
        LocalDate from = LocalDate.of(2026, 9, 1);
        LocalDate to = LocalDate.of(2026, 9, 3);

        WorkShift understaffed = shift(1L, 3);
        WorkShift full = shift(2L, 2);
        WorkShift overstaffed = shift(3L, 1);

        when(shiftRepository.findForDashboardRange(
                any(),
                any(),
                isNull(),
                anyCollection()
        )).thenReturn(List.of(
                understaffed,
                full,
                overstaffed
        ));

        when(assignmentRepository.findAllForCoordinationWindow(
                any(),
                any(),
                anyCollection()
        )).thenReturn(List.of(
                assignment(1L, understaffed),
                assignment(2L, understaffed),
                assignment(3L, full),
                assignment(4L, full),
                assignment(5L, overstaffed),
                assignment(6L, overstaffed)
        ));

        when(replacementRequestRepository.findForDashboard(
                any(),
                any(),
                isNull()
        )).thenReturn(List.of(
                request(ReplacementRequestStatus.PENDING),
                request(ReplacementRequestStatus.OPEN),
                request(ReplacementRequestStatus.FILLED),
                request(ReplacementRequestStatus.FILLED),
                request(ReplacementRequestStatus.REJECTED),
                request(ReplacementRequestStatus.CANCELLED)
        ));

        OperationsAnalyticsResponse result =
                service.analyze(from, to, null);

        assertEquals(3, result.totalShifts());
        assertEquals(1, result.understaffedShifts());
        assertEquals(1, result.fullShifts());
        assertEquals(1, result.overstaffedShifts());
        assertEquals(1, result.missingStaffTotal());

        assertEquals(6, result.replacementTotal());
        assertEquals(1, result.replacementPending());
        assertEquals(1, result.replacementOpen());
        assertEquals(2, result.replacementFilled());
        assertEquals(1, result.replacementRejected());
        assertEquals(1, result.replacementCancelled());
        assertEquals(4, result.replacementResolved());
        assertEquals(50.0, result.replacementFillRate());
    }

    @Test
    void rejectsInvalidDateRangeBeforeRepositoryCalls() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.analyze(
                        LocalDate.of(2026, 9, 5),
                        LocalDate.of(2026, 9, 1),
                        null
                )
        );

        verifyNoInteractions(
                shiftRepository,
                assignmentRepository,
                replacementRequestRepository
        );
    }

    private WorkShift shift(Long id, int requiredStaff) {
        Venue venue = Venue.builder()
                .id(1L)
                .name("Venue")
                .address("Address")
                .build();

        Event event = Event.builder()
                .id(10L + id)
                .name("Event " + id)
                .venue(venue)
                .build();

        return WorkShift.builder()
                .id(id)
                .event(event)
                .name("Shift " + id)
                .startAt(LocalDateTime.of(
                        2026,
                        9,
                        2,
                        id.intValue() * 2,
                        0
                ))
                .endAt(LocalDateTime.of(
                        2026,
                        9,
                        2,
                        id.intValue() * 2 + 1,
                        0
                ))
                .requiredStaff(requiredStaff)
                .shiftStatus(ShiftStatus.OPEN)
                .build();
    }

    private ShiftAssignment assignment(Long id, WorkShift shift) {
        return ShiftAssignment.builder()
                .id(id)
                .shift(shift)
                .status(AssignmentStatus.ASSIGNED)
                .build();
    }

    private ReplacementRequest request(
            ReplacementRequestStatus status
    ) {
        return ReplacementRequest.builder()
                .status(status)
                .build();
    }
}
