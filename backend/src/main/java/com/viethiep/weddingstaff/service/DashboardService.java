package com.viethiep.weddingstaff.service;

import com.viethiep.weddingstaff.dto.AttendanceSummaryResponse;
import com.viethiep.weddingstaff.dto.DashboardSummaryResponse;
import com.viethiep.weddingstaff.enumtype.AssignmentStatus;
import com.viethiep.weddingstaff.enumtype.AttendanceProcessStatus;
import com.viethiep.weddingstaff.enumtype.AttendanceResult;
import com.viethiep.weddingstaff.enumtype.CommonStatus;
import com.viethiep.weddingstaff.enumtype.EmployeeStatus;
import com.viethiep.weddingstaff.enumtype.EventStatus;
import com.viethiep.weddingstaff.enumtype.RegistrationStatus;
import com.viethiep.weddingstaff.enumtype.ShiftStatus;
import com.viethiep.weddingstaff.repository.AttendanceRepository;
import com.viethiep.weddingstaff.repository.EmployeeRepository;
import com.viethiep.weddingstaff.repository.EventRepository;
import com.viethiep.weddingstaff.repository.ShiftAssignmentRepository;
import com.viethiep.weddingstaff.repository.ShiftRegistrationRepository;
import com.viethiep.weddingstaff.repository.VenueRepository;
import com.viethiep.weddingstaff.repository.WorkShiftRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;

@Service
@RequiredArgsConstructor
public class DashboardService {
    private final EmployeeRepository employeeRepository;
    private final VenueRepository venueRepository;
    private final EventRepository eventRepository;
    private final WorkShiftRepository shiftRepository;
    private final ShiftRegistrationRepository registrationRepository;
    private final ShiftAssignmentRepository assignmentRepository;
    private final AttendanceRepository attendanceRepository;

    @Transactional(readOnly = true)
    public DashboardSummaryResponse summary() {
        return new DashboardSummaryResponse(
                employeeRepository.count(),
                employeeRepository.countByEmploymentStatus(
                        EmployeeStatus.ACTIVE
                ),
                venueRepository.count(),
                venueRepository.countByVenueStatus(CommonStatus.ACTIVE),
                eventRepository.count(),
                eventRepository.countByEventStatus(EventStatus.CONFIRMED),
                shiftRepository.count(),
                shiftRepository.countByShiftStatus(ShiftStatus.OPEN),
                registrationRepository.countByStatus(
                        RegistrationStatus.PENDING
                ),
                assignmentRepository.countByStatusIn(
                        EnumSet.of(
                                AssignmentStatus.ASSIGNED,
                                AssignmentStatus.CONFIRMED
                        )
                ),
                attendanceRepository.countByProcessStatus(
                        AttendanceProcessStatus.DRAFT
                ),
                attendanceRepository.countByProcessStatus(
                        AttendanceProcessStatus.CONFIRMED
                )
        );
    }

    @Transactional(readOnly = true)
    public AttendanceSummaryResponse attendanceSummary() {
        AttendanceProcessStatus confirmed =
                AttendanceProcessStatus.CONFIRMED;

        return new AttendanceSummaryResponse(
                attendanceRepository.count(),
                attendanceRepository.countByProcessStatus(
                        AttendanceProcessStatus.DRAFT
                ),
                attendanceRepository.countByProcessStatus(confirmed),
                attendanceRepository
                        .countByProcessStatusAndAttendanceResult(
                                confirmed,
                                AttendanceResult.PRESENT
                        ),
                attendanceRepository
                        .countByProcessStatusAndAttendanceResult(
                                confirmed,
                                AttendanceResult.LATE
                        ),
                attendanceRepository
                        .countByProcessStatusAndAttendanceResult(
                                confirmed,
                                AttendanceResult.EARLY_LEAVE
                        ),
                attendanceRepository
                        .countByProcessStatusAndAttendanceResult(
                                confirmed,
                                AttendanceResult.LATE_AND_EARLY_LEAVE
                        ),
                attendanceRepository
                        .countByProcessStatusAndAttendanceResult(
                                confirmed,
                                AttendanceResult.ABSENT
                        )
        );
    }
}
