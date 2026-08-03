package com.viethiep.weddingstaff.repository;

import com.viethiep.weddingstaff.entity.WorkShift;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface WorkShiftRepository extends JpaRepository<WorkShift, Long> {
    @Query("""
            select shift
            from WorkShift shift
            join fetch shift.event event
            join fetch event.venue
            order by shift.startAt desc
            """)
    List<WorkShift> findAllWithEventAndVenue();

    List<WorkShift> findAllByEventId(Long eventId);
}
