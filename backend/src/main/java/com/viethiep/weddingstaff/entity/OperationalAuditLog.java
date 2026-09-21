package com.viethiep.weddingstaff.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "operational_audit_logs", indexes = {
        @Index(name = "idx_audit_occurred_at", columnList = "occurred_at"),
        @Index(name = "idx_audit_actor_occurred", columnList = "actor_username,occurred_at"),
        @Index(name = "idx_audit_action", columnList = "action")
})
public class OperationalAuditLog extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "actor_username", nullable = false, length = 50)
    private String actorUsername;
    @Column(name = "actor_role", length = 30)
    private String actorRole;
    @Column(nullable = false, length = 20)
    private String action;
    @Column(name = "resource_path", nullable = false, length = 255)
    private String resourcePath;
    @Column(name = "http_status", nullable = false)
    private Integer httpStatus;
    @Column(nullable = false)
    private boolean success;
    @Column(name = "client_ip", length = 64)
    private String clientIp;
    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;
}
