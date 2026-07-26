package com.viethiep.weddingstaff.repository;

import com.viethiep.weddingstaff.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepository extends JpaRepository<Event, Long> {
}
