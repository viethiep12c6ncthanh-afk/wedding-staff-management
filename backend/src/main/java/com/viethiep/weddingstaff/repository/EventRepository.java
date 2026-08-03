package com.viethiep.weddingstaff.repository;

import com.viethiep.weddingstaff.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {
    @Query("""
            select event
            from Event event
            join fetch event.venue
            order by event.startAt desc
            """)
    List<Event> findAllWithVenue();
}
