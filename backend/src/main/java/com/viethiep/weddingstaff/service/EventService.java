package com.viethiep.weddingstaff.service;

import com.viethiep.weddingstaff.exception.ResourceNotFoundException;
import com.viethiep.weddingstaff.dto.EventRequest;
import com.viethiep.weddingstaff.dto.EventResponse;
import com.viethiep.weddingstaff.dto.EventStatusRequest;
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
public class EventService {
    private final EventRepository eventRepository;
    private final VenueRepository venueRepository;
    private final WorkShiftRepository shiftRepository;
    private final ShiftRegistrationRepository registrationRepository;
    private final ShiftAssignmentRepository assignmentRepository;
    private final UserAccountRepository userRepository;
    private final ReplacementService replacementService;

    @Transactional(readOnly = true)
    public List<EventResponse> findAll() {
        return eventRepository.findAllWithVenue().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public EventResponse create(EventRequest request, String username) {
        validateTimes(request.startAt(), request.endAt());
        Venue venue = findActiveVenue(request.venueId());
        UserAccount creator = findUser(username);

        Event event = Event.builder()
                .venue(venue)
                .name(request.name())
                .startAt(request.startAt())
                .endAt(request.endAt())
                .eventStatus(EventStatus.DRAFT)
                .description(request.description())
                .createdBy(creator)
                .build();

        return toResponse(eventRepository.save(event));
    }

    @Transactional
    public EventResponse update(Long id, EventRequest request) {
        validateTimes(request.startAt(), request.endAt());

        Event event = findEvent(id);
        ensureEditable(event);
        Venue venue = findActiveVenue(request.venueId());

        event.setVenue(venue);
        event.setName(request.name());
        event.setStartAt(request.startAt());
        event.setEndAt(request.endAt());
        event.setDescription(request.description());

        validateExistingShiftsInsideEvent(event);
        return toResponse(event);
    }

    @Transactional
    public EventResponse changeStatus(
            Long id,
            EventStatusRequest request,
            String username
    ) {
        Event event = findEvent(id);
        EventStatus target = request.status();

        validateTransition(event.getEventStatus(), target);

        if (target == EventStatus.CANCELLED) {
            requireReason(request.cancellationReason());
            UserAccount actor = findUser(username);
            event.setCancelledBy(actor);
            event.setCancelledAt(LocalDateTime.now());
            event.setCancellationReason(request.cancellationReason().trim());
            cancelRelatedWork(event, actor, request.cancellationReason().trim());
        }

        event.setEventStatus(target);
        return toResponse(event);
    }

    private void cancelRelatedWork(
            Event event,
            UserAccount actor,
            String reason
    ) {
        List<WorkShift> shifts = shiftRepository.findAllByEventId(event.getId());

        for (WorkShift shift : shifts) {
            if (shift.getShiftStatus() == ShiftStatus.COMPLETED
                    || shift.getShiftStatus() == ShiftStatus.CANCELLED) {
                continue;
            }

            shift.setShiftStatus(ShiftStatus.CANCELLED);
            shift.setCancelledBy(actor);
            shift.setCancelledAt(LocalDateTime.now());
            shift.setCancellationReason(reason);
            replacementService.cancelForShift(shift.getId(), reason);

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
    }

    private void validateExistingShiftsInsideEvent(Event event) {
        boolean invalid = shiftRepository.findAllByEventId(event.getId()).stream()
                .filter(shift -> shift.getShiftStatus() != ShiftStatus.CANCELLED)
                .anyMatch(shift ->
                        shift.getStartAt().isBefore(event.getStartAt())
                                || shift.getEndAt().isAfter(event.getEndAt())
                );

        if (invalid) {
            throw new IllegalStateException(
                    "Khoảng thời gian sự kiện không bao phủ toàn bộ ca hiện có"
            );
        }
    }

    private void validateTransition(EventStatus current, EventStatus target) {
        boolean valid = switch (current) {
            case DRAFT -> target == EventStatus.CONFIRMED
                    || target == EventStatus.CANCELLED;
            case CONFIRMED -> target == EventStatus.IN_PROGRESS
                    || target == EventStatus.CANCELLED;
            case IN_PROGRESS -> target == EventStatus.COMPLETED
                    || target == EventStatus.CANCELLED;
            case COMPLETED, CANCELLED -> false;
        };

        if (!valid) {
            throw new IllegalStateException(
                    "Không thể chuyển sự kiện từ " + current + " sang " + target
            );
        }
    }

    private void ensureEditable(Event event) {
        if (event.getEventStatus() == EventStatus.COMPLETED
                || event.getEventStatus() == EventStatus.CANCELLED) {
            throw new IllegalStateException(
                    "Không thể chỉnh sửa sự kiện đã hoàn thành hoặc đã hủy"
            );
        }
    }

    private Venue findActiveVenue(Long venueId) {
        Venue venue = venueRepository.findById(venueId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Không tìm thấy địa điểm")
                );

        if (venue.getVenueStatus() != CommonStatus.ACTIVE) {
            throw new IllegalStateException("Địa điểm đang không hoạt động");
        }
        return venue;
    }

    private Event findEvent(Long id) {
        return eventRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Không tìm thấy sự kiện")
                );
    }

    private UserAccount findUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Không tìm thấy tài khoản")
                );
    }

    private void validateTimes(LocalDateTime startAt, LocalDateTime endAt) {
        if (!endAt.isAfter(startAt)) {
            throw new IllegalArgumentException(
                    "Thời gian kết thúc phải sau thời gian bắt đầu"
            );
        }
    }

    private void requireReason(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException(
                    "Bắt buộc nhập lý do hủy sự kiện"
            );
        }
    }

    private EventResponse toResponse(Event event) {
        return new EventResponse(
                event.getId(),
                event.getVenue().getId(),
                event.getVenue().getName(),
                event.getName(),
                event.getStartAt(),
                event.getEndAt(),
                event.getEventStatus(),
                event.getDescription(),
                event.getCancelledAt(),
                event.getCancellationReason()
        );
    }
}
