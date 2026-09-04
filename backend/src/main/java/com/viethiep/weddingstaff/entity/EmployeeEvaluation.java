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
        name = "employee_evaluations",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_employee_evaluation_assignment",
                        columnNames = "assignment_id"
                )
        }
)
public class EmployeeEvaluation extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assignment_id", nullable = false, unique = true)
    private ShiftAssignment assignment;

    @Column(nullable = false)
    private Integer rating;

    @Column(length = 500)
    private String comment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evaluated_by", nullable = false)
    private UserAccount evaluatedBy;

    @Column(name = "evaluated_at", nullable = false)
    private LocalDateTime evaluatedAt;
}
