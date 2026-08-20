package com.viethiep.weddingstaff.entity;

import com.viethiep.weddingstaff.enumtype.ReplacementRequestStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "replacement_requests")
public class ReplacementRequest extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "original_assignment_id", nullable = false)
    private ShiftAssignment originalAssignment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requested_by", nullable = false)
    private UserAccount requestedBy;

    @Column(nullable = false, length = 500)
    private String reason;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReplacementRequestStatus status = ReplacementRequestStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private UserAccount reviewedBy;

    private LocalDateTime reviewedAt;

    @Column(length = 500)
    private String reviewNote;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "replacement_assignment_id", unique = true)
    private ShiftAssignment replacementAssignment;

    private LocalDateTime filledAt;

    private LocalDateTime closedAt;

    @Column(length = 500)
    private String closedReason;
}
