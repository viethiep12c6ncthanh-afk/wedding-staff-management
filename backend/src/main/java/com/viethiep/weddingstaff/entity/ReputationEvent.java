package com.viethiep.weddingstaff.entity;

import com.viethiep.weddingstaff.enumtype.ReputationEventType;
import com.viethiep.weddingstaff.enumtype.ReputationSourceType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "reputation_events",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_reputation_event_source",
                        columnNames = {"source_type", "source_id"}
                )
        }
)
public class ReputationEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 40)
    private ReputationEventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 30)
    private ReputationSourceType sourceType;

    @Column(name = "source_id", nullable = false)
    private Long sourceId;

    @Column(name = "score_before", nullable = false)
    private Integer scoreBefore;

    @Column(name = "score_delta", nullable = false)
    private Integer scoreDelta;

    @Column(name = "score_after", nullable = false)
    private Integer scoreAfter;

    @Column(nullable = false, length = 500)
    private String reason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_user_id")
    private UserAccount actorUser;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
