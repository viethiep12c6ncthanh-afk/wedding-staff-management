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
        name = "shift_tables",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_shift_table_code",
                        columnNames = {"area_id", "table_code"}
                )
        }
)
public class ShiftTable extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "area_id", nullable = false)
    private ShiftArea area;

    @Column(name = "table_code", nullable = false, length = 30)
    private String tableCode;

    @Column(name = "display_name", length = 100)
    private String displayName;

    @Column(length = 300)
    private String note;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "table_status", nullable = false, length = 20)
    private CommonStatus tableStatus = CommonStatus.ACTIVE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private UserAccount createdBy;
}
