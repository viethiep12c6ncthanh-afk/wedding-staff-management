package com.viethiep.weddingstaff.service;

import com.viethiep.weddingstaff.dto.VenueRequest;
import com.viethiep.weddingstaff.dto.VenueResponse;
import com.viethiep.weddingstaff.entity.Venue;
import com.viethiep.weddingstaff.repository.VenueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VenueService {
    private final VenueRepository repository;

    @Transactional(readOnly = true)
    public List<VenueResponse> findAll() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional
    public VenueResponse create(VenueRequest request) {
        Venue venue = Venue.builder()
                .name(request.name())
                .address(request.address())
                .contactPhone(request.contactPhone())
                .status(request.status())
                .build();
        return toResponse(repository.save(venue));
    }

    @Transactional
    public VenueResponse update(Long id, VenueRequest request) {
        Venue venue = repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy địa điểm"));
        venue.setName(request.name());
        venue.setAddress(request.address());
        venue.setContactPhone(request.contactPhone());
        venue.setStatus(request.status());
        return toResponse(venue);
    }

    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) throw new IllegalArgumentException("Không tìm thấy địa điểm");
        repository.deleteById(id);
    }

    private VenueResponse toResponse(Venue v) {
        return new VenueResponse(v.getId(), v.getName(), v.getAddress(), v.getContactPhone(), v.getStatus());
    }
}
