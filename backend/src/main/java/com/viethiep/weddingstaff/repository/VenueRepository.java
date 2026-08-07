package com.viethiep.weddingstaff.repository;

import com.viethiep.weddingstaff.entity.Venue;
import com.viethiep.weddingstaff.enumtype.CommonStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VenueRepository extends JpaRepository<Venue, Long> {
    long countByVenueStatus(CommonStatus venueStatus);

    List<Venue> findAllByOrderByNameAsc();
}
