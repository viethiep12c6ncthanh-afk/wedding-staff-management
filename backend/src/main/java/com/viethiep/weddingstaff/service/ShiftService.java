package com.viethiep.weddingstaff.service;

import com.viethiep.weddingstaff.exception.ResourceNotFoundException;
import com.viethiep.weddingstaff.dto.ShiftRequest;
import com.viethiep.weddingstaff.dto.ShiftResponse;
import com.viethiep.weddingstaff.dto.ShiftStatusRequest;
import com.viethiep.weddingstaff.entity.*;
import com.viethiep.weddingstaff.enumtype.*;
import com.viethiep.weddingstaff.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ShiftService {
    private final WorkShiftRepository shiftRepository;
    private final EventRepository eventRepository;
    private final ShiftRegistrationRepository registrationRepository;
    private final ShiftAssignmentRepository assignmentRepository;
    private final UserAccountRepository userRepository;
    private final ReplacementService replacementService;

    @Transactional(readOnly = true)
    public List<ShiftResponse> findAll() {
        return shiftRepository.findAllWithEventAndVenue().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ShiftResponse create(ShiftRequest request, String username) {
        validateTimes(request);
        Event event = findUsableEvent(request.eventId());
        validateInsideEvent(request, event);
        validateRegistrationDeadline(request);
        UserAccount creator = findUser(username);

        WorkShift shift = WorkShift.builder()
                .event(event)
                .name(request.name())
                .startAt(request.startAt())
                .endAt(request.endAt())
                .requiredStaff(request.requiredStaff())
                .payAmount(request.payAmount())
                .registrationDeadline(request.registrationDeadline())
                .shiftStatus(ShiftStatus.DRAFT)
                .description(request.description())
                .createdBy(creator)
                .build();

        return toResponse(shiftRepository.save(shift));
    }

    @Transactional
    public ShiftResponse update(Long id, ShiftRequest request) {
        validateTimes(request);
        validateRegistrationDeadline(request);

        WorkShift shift = findShift(id);
        ensureEditable(shift);
        Event event = findUsableEvent(request.eventId());
        validateInsideEvent(request, event);

        shift.setEvent(event);
        shift.setName(request.name());
        shift.setStartAt(request.startAt());
        shift.setEndAt(request.endAt());
        shift.setRequiredStaff(request.requiredStaff());
        shift.setPayAmount(request.payAmount());
        shift.setRegistrationDeadline(request.registrationDeadline());
        shift.setDescription(request.description());

        return toResponse(shift);
    }

    @Transactional
    public ShiftResponse changeStatus(
            Long id,
            ShiftStatusRequest request,
            String username
    ) {
        WorkShift shift = findShift(id);
        ShiftStatus target = request.status();

        validateTransition(shift.getShiftStatus(), target);

        if (target == ShiftStatus.OPEN) {
            validateCanOpen(shift);
        }

        if (target == ShiftStatus.CANCELLED) {
            requireReason(request.cancellationReason());
            UserAccount actor = findUser(username);
            String reason = request.cancellationReason().trim();

            shift.setCancelledBy(actor);
            shift.setCancelledAt(LocalDateTime.now());
            shift.setCancellationReason(reason);
            cancelRegistrationsAndAssignments(shift, actor, reason);
            replacementService.cancelForShift(shift.getId(), reason);
        }

        shift.setShiftStatus(target);
        return toResponse(shift);
    }

    private void cancelRegistrationsAndAssignments(
            WorkShift shift,
            UserAccount actor,
            String reason
    ) {
        registrationRepository.findAllByShiftIdAndStatusIn(
                shift.getId(),
                EnumSet.of(
                        RegistrationStatus.PENDING,
                        RegistrationStatus.APPROVED
                )
        ).forEach(registration -> {
            registration.setStatus(RegistrationStatus.CANCELLED);
            registration.setCancelledBy(actor);
            registration.setCancelledAt(LocalDateTime.now());
            registration.setCancellationReason(reason);
        });

        assignmentRepository.findAllByShiftIdAndStatusIn(
                shift.getId(),
                EnumSet.of(
                        AssignmentStatus.ASSIGNED,
                        AssignmentStatus.CONFIRMED
                )
        ).forEach(assignment -> {
            assignment.setStatus(AssignmentStatus.CANCELLED);
            assignment.setCancelledBy(actor);
            assignment.setCancelledAt(LocalDateTime.now());
            assignment.setCancellationReason(reason);
        });
    }

    private void validateTransition(ShiftStatus current, ShiftStatus target) {
        boolean valid = switch (current) {
            case DRAFT -> target == ShiftStatus.OPEN
                    || target == ShiftStatus.CANCELLED;
            case OPEN -> target == ShiftStatus.CLOSED
                    || target == ShiftStatus.CANCELLED;
            case CLOSED -> target == ShiftStatus.IN_PROGRESS
                    || target == ShiftStatus.CANCELLED;
            case IN_PROGRESS -> target == ShiftStatus.COMPLETED
                    || target == ShiftStatus.CANCELLED;
            case COMPLETED, CANCELLED -> false;
        };

        if (!valid) {
            throw new IllegalStateException(
                    "Không thể chuyển ca từ " + current + " sang " + target
            );
        }
    }

    private void validateCanOpen(WorkShift shift) {
        if (shift.getEvent().getEventStatus() != EventStatus.CONFIRMED) {
            throw new IllegalStateException(
                    "Chỉ mở đăng ký khi sự kiện đã được xác nhận"
            );
        }

        if (shift.getRegistrationDeadline() != null
                && !shift.getRegistrationDeadline().isAfter(LocalDateTime.now())) {
            throw new IllegalStateException("Hạn đăng ký đã qua");
        }
    }

    private void ensureEditable(WorkShift shift) {
        if (shift.getShiftStatus() == ShiftStatus.IN_PROGRESS
                || shift.getShiftStatus() == ShiftStatus.COMPLETED
                || shift.getShiftStatus() == ShiftStatus.CANCELLED) {
            throw new IllegalStateException(
                    "Không thể chỉnh sửa ca đang diễn ra, đã hoàn thành hoặc đã hủy"
            );
        }
    }

    private Event findUsableEvent(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Không tìm thấy sự kiện")
                );

        if (event.getEventStatus() == EventStatus.CANCELLED
                || event.getEventStatus() == EventStatus.COMPLETED) {
            throw new IllegalStateException(
                    "Không thể tạo hoặc cập nhật ca cho sự kiện này"
            );
        }
        return event;
    }

    private WorkShift findShift(Long id) {
        return shiftRepository.findByIdForUpdate(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Không tìm thấy ca")
                );
    }

    private UserAccount findUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Không tìm thấy tài khoản")
                );
    }

    private void validateTimes(ShiftRequest request) {
        if (!request.endAt().isAfter(request.startAt())) {
            throw new IllegalArgumentException(
                    "Thời gian kết thúc ca phải sau thời gian bắt đầu"
            );
        }
    }

    private void validateInsideEvent(ShiftRequest request, Event event) {
        if (request.startAt().isBefore(event.getStartAt())
                || request.endAt().isAfter(event.getEndAt())) {
            throw new IllegalArgumentException(
                    "Ca phải nằm trong khoảng thời gian của sự kiện"
            );
        }
    }

    private void validateRegistrationDeadline(ShiftRequest request) {
        if (request.registrationDeadline() != null
                && request.registrationDeadline().isAfter(request.startAt())) {
            throw new IllegalArgumentException(
                    "Hạn đăng ký không được sau giờ bắt đầu ca"
            );
        }
    }

    private void requireReason(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Bắt buộc nhập lý do hủy ca");
        }
    }

    private ShiftResponse toResponse(WorkShift shift) {
        return new ShiftResponse(
                shift.getId(),
                shift.getEvent().getId(),
                shift.getEvent().getName(),
                shift.getEvent().getVenue().getName(),
                shift.getName(),
                shift.getStartAt(),
                shift.getEndAt(),
                shift.getRequiredStaff(),
                shift.getPayAmount(),
                shift.getRegistrationDeadline(),
                shift.getShiftStatus(),
                shift.getDescription(),
                shift.getCancelledAt(),
                shift.getCancellationReason()
        );
    }
}
