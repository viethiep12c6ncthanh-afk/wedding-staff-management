package com.viethiep.weddingstaff.repository;

import com.viethiep.weddingstaff.entity.OperationalAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OperationalAuditLogRepository extends JpaRepository<OperationalAuditLog, Long> {
    Page<OperationalAuditLog> findByActorUsernameContainingIgnoreCaseAndActionContainingIgnoreCase(
            String actorUsername, String action, Pageable pageable);

    List<OperationalAuditLog> findTop10ByOrderByOccurredAtDesc();

    List<OperationalAuditLog> findTop10ByActorUsernameOrderByOccurredAtDesc(String actorUsername);
}
