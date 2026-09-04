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
@Table(
        name = "employee_reputations",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_employee_reputation_employee",
                        columnNames = "employee_id"
                )
        }
)
public class EmployeeReputation extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false, unique = true)
    private Employee employee;

    @Builder.Default
    @Column(name = "current_score", nullable = false)
    private Integer currentScore = 80;

    @Builder.Default
    @Column(name = "completed_shift_count", nullable = false)
    private Integer completedShiftCount = 0;

    @Builder.Default
    @Column(name = "late_count", nullable = false)
    private Integer lateCount = 0;

    @Builder.Default
    @Column(name = "early_leave_count", nullable = false)
    private Integer earlyLeaveCount = 0;

    @Builder.Default
    @Column(name = "absent_count", nullable = false)
    private Integer absentCount = 0;

    @Builder.Default
    @Column(name = "evaluation_count", nullable = false)
    private Integer evaluationCount = 0;

    @Builder.Default
    @Column(name = "rating_sum", nullable = false)
    private Integer ratingSum = 0;

    @Column(name = "last_event_at")
    private LocalDateTime lastEventAt;
}
