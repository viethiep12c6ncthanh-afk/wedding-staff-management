package com.viethiep.weddingstaff.entity;

import com.viethiep.weddingstaff.enumtype.CommonStatus;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "shift_areas",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_shift_area_name",
                        columnNames = {"shift_id", "name"}
                )
        }
)
public class ShiftArea extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shift_id", nullable = false)
    private WorkShift shift;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "required_staff")
    private Integer requiredStaff;

    @Column(length = 500)
    private String description;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "area_status", nullable = false, length = 20)
    private CommonStatus areaStatus = CommonStatus.ACTIVE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private UserAccount createdBy;
}
