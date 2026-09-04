package com.viethiep.weddingstaff.entity;

import com.viethiep.weddingstaff.enumtype.AiRecommendationMode;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "ai_recommendation_runs")
public class AiRecommendationRun extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "replacement_request_id", nullable = false)
    private ReplacementRequest replacementRequest;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AiRecommendationMode mode;

    @Column(nullable = false, length = 40)
    private String provider;

    @Column(length = 100)
    private String model;

    @Column(name = "fallback_used", nullable = false)
    private boolean fallbackUsed;

    @Column(name = "fallback_reason", length = 120)
    private String fallbackReason;

    @Column(nullable = false, length = 2000)
    private String summary;

    @Lob
    @Column(name = "deterministic_snapshot", nullable = false, columnDefinition = "LONGTEXT")
    private String deterministicSnapshot;

    @Lob
    @Column(name = "context_snapshot", nullable = false, columnDefinition = "LONGTEXT")
    private String contextSnapshot;

    @Lob
    @Column(name = "ai_result_json", columnDefinition = "LONGTEXT")
    private String aiResultJson;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requested_by", nullable = false)
    private UserAccount requestedBy;
}
