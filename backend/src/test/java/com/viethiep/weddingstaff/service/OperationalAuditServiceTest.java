package com.viethiep.weddingstaff.service;

import com.viethiep.weddingstaff.entity.OperationalAuditLog;
import com.viethiep.weddingstaff.repository.OperationalAuditLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OperationalAuditServiceTest {
    @Mock
    private OperationalAuditLogRepository repository;

    @Test
    void recordCapturesActorRequestAndSuccessfulStatus() {
        OperationalAuditService service = new OperationalAuditService(repository);

        service.record("admin", "ROLE_ADMIN", "PATCH", "/api/shifts/5/status", 200, "127.0.0.1");

        ArgumentCaptor<OperationalAuditLog> captor = ArgumentCaptor.forClass(OperationalAuditLog.class);
        verify(repository).save(captor.capture());
        OperationalAuditLog log = captor.getValue();
        assertEquals("admin", log.getActorUsername());
        assertEquals("ROLE_ADMIN", log.getActorRole());
        assertEquals("PATCH", log.getAction());
        assertEquals("/api/shifts/5/status", log.getResourcePath());
        assertEquals(200, log.getHttpStatus());
        assertTrue(log.isSuccess());
        assertNotNull(log.getOccurredAt());
    }

    @Test
    void recordMarksClientErrorsAsFailed() {
        OperationalAuditService service = new OperationalAuditService(repository);

        service.record("employee01", "ROLE_EMPLOYEE", "POST", "/api/replacements", 409, "10.0.0.8");

        ArgumentCaptor<OperationalAuditLog> captor = ArgumentCaptor.forClass(OperationalAuditLog.class);
        verify(repository).save(captor.capture());
        assertFalse(captor.getValue().isSuccess());
        assertEquals(409, captor.getValue().getHttpStatus());
    }
}
