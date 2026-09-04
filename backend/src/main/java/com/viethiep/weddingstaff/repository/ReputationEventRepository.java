package com.viethiep.weddingstaff.repository;

import com.viethiep.weddingstaff.entity.ReputationEvent;
import com.viethiep.weddingstaff.enumtype.ReputationSourceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReputationEventRepository
        extends JpaRepository<ReputationEvent, Long> {

    boolean existsBySourceTypeAndSourceId(
            ReputationSourceType sourceType,
            Long sourceId
    );

    @Query("""
            select event
            from ReputationEvent event
            left join fetch event.actorUser
            where event.employee.id = :employeeId
            order by event.occurredAt desc, event.id desc
            """)
    List<ReputationEvent> findAllByEmployeeId(
            @Param("employeeId") Long employeeId
    );
}
