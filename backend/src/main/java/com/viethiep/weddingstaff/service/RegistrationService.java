package com.viethiep.weddingstaff.service;

import com.viethiep.weddingstaff.dto.RegistrationResponse;
import com.viethiep.weddingstaff.dto.ReviewRegistrationRequest;
import com.viethiep.weddingstaff.entity.*;
import com.viethiep.weddingstaff.enumtype.AssignmentStatus;
import com.viethiep.weddingstaff.enumtype.RegistrationStatus;
import com.viethiep.weddingstaff.enumtype.ShiftStatus;
import com.viethiep.weddingstaff.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RegistrationService {
    private final ShiftRegistrationRepository registrationRepository;
    private final ShiftAssignmentRepository assignmentRepository;
    private final WorkShiftRepository shiftRepository;
    private final EmployeeRepository employeeRepository;
    private final UserAccountRepository userRepository;

    @Transactional(readOnly = true)
    public List<RegistrationResponse> findAll() {
        return registrationRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public RegistrationResponse register(Long shiftId, String username) {
        Employee employee = employeeRepository.findByUserUsername(username)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Tài khoản chưa có hồ sơ nhân viên"
                        )
                );
        WorkShift shift = shiftRepository.findById(shiftId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Không tìm thấy ca")
                );

        if (shift.getShiftStatus() != ShiftStatus.OPEN) {
            throw new IllegalStateException("Ca chưa mở đăng ký");
        }
        if (shift.getRegistrationDeadline() != null
                && !LocalDateTime.now().isBefore(
                        shift.getRegistrationDeadline()
                )) {
            throw new IllegalStateException("Đã quá hạn đăng ký");
        }
        if (registrationRepository.existsByShiftIdAndEmployeeId(
                shiftId,
                employee.getId()
        )) {
            throw new IllegalStateException("Bạn đã đăng ký ca này");
        }
        if (!registrationRepository.findApprovedOverlaps(
                employee.getId(),
                shift.getStartAt(),
                shift.getEndAt()
        ).isEmpty()) {
            throw new IllegalStateException(
                    "Ca bị trùng với một ca đã được duyệt"
            );
        }

        ShiftRegistration registration = ShiftRegistration.builder()
                .shift(shift)
                .employee(employee)
                .status(RegistrationStatus.PENDING)
                .build();

        return toResponse(registrationRepository.save(registration));
    }

    @Transactional
    public RegistrationResponse review(
            Long registrationId,
            ReviewRegistrationRequest request,
            String reviewerUsername
    ) {
        ShiftRegistration registration = registrationRepository
                .findById(registrationId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Không tìm thấy đăng ký"
                        )
                );

        if (registration.getStatus() != RegistrationStatus.PENDING) {
            throw new IllegalStateException("Đăng ký đã được xử lý");
        }

        UserAccount reviewer = userRepository
                .findByUsername(reviewerUsername)
                .orElseThrow();
        WorkShift shift = registration.getShift();

        if (shift.getShiftStatus() == ShiftStatus.CANCELLED
                || shift.getShiftStatus() == ShiftStatus.COMPLETED) {
            throw new IllegalStateException("Ca không còn nhận xử lý đăng ký");
        }

        if (Boolean.TRUE.equals(request.approved())) {
            long approvedCount = registrationRepository
                    .countByShiftIdAndStatus(
                            shift.getId(),
                            RegistrationStatus.APPROVED
                    );

            if (approvedCount >= shift.getRequiredStaff()) {
                throw new IllegalStateException(
                        "Ca đã đủ số lượng nhân viên"
                );
            }

            if (!registrationRepository.findApprovedOverlaps(
                    registration.getEmployee().getId(),
                    shift.getStartAt(),
                    shift.getEndAt()
            ).isEmpty()) {
                throw new IllegalStateException("Nhân viên bị trùng lịch");
            }

            registration.setStatus(RegistrationStatus.APPROVED);
            assignmentRepository.save(
                    ShiftAssignment.builder()
                            .shift(shift)
                            .employee(registration.getEmployee())
                            .roleInShift(request.roleInShift())
                            .area(request.area())
                            .task(request.task())
                            .status(AssignmentStatus.ASSIGNED)
                            .assignedBy(reviewer)
                            .build()
            );
        } else {
            registration.setStatus(RegistrationStatus.REJECTED);
            registration.setRejectionReason(request.rejectionReason());
        }

        registration.setReviewedBy(reviewer);
        registration.setReviewedAt(LocalDateTime.now());
        return toResponse(registration);
    }

    private RegistrationResponse toResponse(ShiftRegistration registration) {
        return new RegistrationResponse(
                registration.getId(),
                registration.getShift().getId(),
                registration.getShift().getName(),
                registration.getEmployee().getId(),
                registration.getEmployee().getUser().getFullName(),
                registration.getStatus(),
                registration.getCreatedAt(),
                registration.getRejectionReason()
        );
    }
}
