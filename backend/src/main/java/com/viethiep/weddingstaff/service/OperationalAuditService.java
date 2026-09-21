package com.viethiep.weddingstaff.service;

import com.viethiep.weddingstaff.dto.AuditLogResponse;
import com.viethiep.weddingstaff.dto.AuditPageResponse;
import com.viethiep.weddingstaff.entity.OperationalAuditLog;
import com.viethiep.weddingstaff.repository.OperationalAuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OperationalAuditService {
    private final OperationalAuditLogRepository repository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String username, String role, String action, String path,
                       int status, String clientIp) {
        repository.save(OperationalAuditLog.builder()
                .actorUsername(username == null ? "anonymous" : username)
                .actorRole(role).action(action)
                .resourcePath(path.length() > 255 ? path.substring(0, 255) : path)
                .httpStatus(status).success(status < 400).clientIp(clientIp)
                .occurredAt(LocalDateTime.now()).build());
    }

    @Transactional(readOnly = true)
    public AuditPageResponse findAll(String actor, String action, int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        Page<OperationalAuditLog> result = repository
                .findByActorUsernameContainingIgnoreCaseAndActionContainingIgnoreCase(
                        actor == null ? "" : actor.trim(), action == null ? "" : action.trim(),
                        PageRequest.of(Math.max(page, 0), safeSize,
                                Sort.by(Sort.Direction.DESC, "occurredAt")));
        return new AuditPageResponse(result.getContent().stream().map(this::toResponse).toList(),
                result.getTotalElements(), result.getTotalPages(), result.getNumber(), result.getSize());
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponse> recent(String username, boolean manager) {
        List<OperationalAuditLog> logs = manager
                ? repository.findTop10ByOrderByOccurredAtDesc()
                : repository.findTop10ByActorUsernameOrderByOccurredAtDesc(username);
        return logs.stream().map(this::toResponse).toList();
    }

    private AuditLogResponse toResponse(OperationalAuditLog log) {
        return new AuditLogResponse(log.getId(), log.getActorUsername(), log.getActorRole(),
                log.getAction(), log.getResourcePath(), log.getHttpStatus(), log.isSuccess(),
                log.getClientIp(), log.getOccurredAt());
    }
}
