package com.viethiep.weddingstaff.entity;

import com.viethiep.weddingstaff.enumtype.AssignmentStatus;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "shift_assignments",
       uniqueConstraints = @UniqueConstraint(name = "uk_assignment_shift_employee", columnNames = {"shift_id", "employee_id"}))
public class ShiftAssignment extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shift_id", nullable = false)
    private WorkShift shift;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(length = 80)
    private String roleInShift;

    @Column(length = 100)
    private String area;

    @Column(length = 300)
    private String task;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AssignmentStatus status;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assigned_by", nullable = false)
    private UserAccount assignedBy;
}
