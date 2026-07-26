package com.viethiep.weddingstaff.service;

import com.viethiep.weddingstaff.dto.ShiftRequest;
import com.viethiep.weddingstaff.dto.ShiftResponse;
import com.viethiep.weddingstaff.entity.Event;
import com.viethiep.weddingstaff.entity.WorkShift;
import com.viethiep.weddingstaff.repository.EventRepository;
import com.viethiep.weddingstaff.repository.WorkShiftRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ShiftService {
    private final WorkShiftRepository shiftRepository;
    private final EventRepository eventRepository;

    @Transactional(readOnly = true)
    public List<ShiftResponse> findAll() {
        return shiftRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional
    public ShiftResponse create(ShiftRequest request) {
        validate(request);
        Event event = eventRepository.findById(request.eventId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sự kiện"));
        WorkShift shift = WorkShift.builder()
                .event(event).name(request.name()).startAt(request.startAt()).endAt(request.endAt())
                .requiredStaff(request.requiredStaff()).payAmount(request.payAmount())
                .registrationOpen(request.registrationOpen()).status(request.status()).note(request.note()).build();
        return toResponse(shiftRepository.save(shift));
    }

    @Transactional
    public ShiftResponse update(Long id, ShiftRequest request) {
        validate(request);
        WorkShift shift = shiftRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy ca"));
        Event event = eventRepository.findById(request.eventId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sự kiện"));
        shift.setEvent(event);
        shift.setName(request.name());
        shift.setStartAt(request.startAt());
        shift.setEndAt(request.endAt());
        shift.setRequiredStaff(request.requiredStaff());
        shift.setPayAmount(request.payAmount());
        shift.setRegistrationOpen(request.registrationOpen());
        shift.setStatus(request.status());
        shift.setNote(request.note());
        return toResponse(shift);
    }

    private void validate(ShiftRequest request) {
        if (!request.endAt().isAfter(request.startAt())) {
            throw new IllegalArgumentException("Thời gian kết thúc ca phải sau thời gian bắt đầu");
        }
    }

    private ShiftResponse toResponse(WorkShift s) {
        return new ShiftResponse(s.getId(), s.getEvent().getId(), s.getEvent().getName(),
                s.getEvent().getVenue().getName(), s.getName(), s.getStartAt(), s.getEndAt(),
                s.getRequiredStaff(), s.getPayAmount(), s.isRegistrationOpen(), s.getStatus(), s.getNote());
    }
}
