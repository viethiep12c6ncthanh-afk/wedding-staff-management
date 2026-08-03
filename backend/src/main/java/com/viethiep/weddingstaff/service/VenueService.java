package com.viethiep.weddingstaff.service;

import com.viethiep.weddingstaff.dto.VenueRequest;
import com.viethiep.weddingstaff.dto.VenueResponse;
import com.viethiep.weddingstaff.dto.VenueStatusRequest;
import com.viethiep.weddingstaff.entity.UserAccount;
import com.viethiep.weddingstaff.entity.Venue;
import com.viethiep.weddingstaff.enumtype.CommonStatus;
import com.viethiep.weddingstaff.repository.UserAccountRepository;
import com.viethiep.weddingstaff.repository.VenueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VenueService {
    private final VenueRepository venueRepository;
    private final UserAccountRepository userRepository;

    @Transactional(readOnly = true)
    public List<VenueResponse> findAll() {
        return venueRepository.findAllByOrderByNameAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public VenueResponse create(VenueRequest request, String username) {
        UserAccount creator = findUser(username);

        Venue venue = Venue.builder()
                .name(request.name())
                .address(request.address())
                .contactName(request.contactName())
                .contactPhone(request.contactPhone())
                .venueStatus(CommonStatus.ACTIVE)
                .note(request.note())
                .createdBy(creator)
                .build();

        return toResponse(venueRepository.save(venue));
    }

    @Transactional
    public VenueResponse update(Long id, VenueRequest request) {
        Venue venue = findVenue(id);
        venue.setName(request.name());
        venue.setAddress(request.address());
        venue.setContactName(request.contactName());
        venue.setContactPhone(request.contactPhone());
        venue.setNote(request.note());
        return toResponse(venue);
    }

    @Transactional
    public VenueResponse changeStatus(Long id, VenueStatusRequest request) {
        Venue venue = findVenue(id);
        venue.setVenueStatus(request.status());
        return toResponse(venue);
    }

    @Transactional
    public void deactivate(Long id) {
        Venue venue = findVenue(id);
        venue.setVenueStatus(CommonStatus.INACTIVE);
    }

    private Venue findVenue(Long id) {
        return venueRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy địa điểm"));
    }

    private UserAccount findUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản"));
    }

    private VenueResponse toResponse(Venue venue) {
        return new VenueResponse(
                venue.getId(),
                venue.getName(),
                venue.getAddress(),
                venue.getContactName(),
                venue.getContactPhone(),
                venue.getVenueStatus(),
                venue.getNote()
        );
    }
}
