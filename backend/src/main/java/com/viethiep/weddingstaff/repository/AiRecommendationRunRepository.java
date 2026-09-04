package com.viethiep.weddingstaff.repository;

import com.viethiep.weddingstaff.entity.AiRecommendationRun;
import com.viethiep.weddingstaff.enumtype.AiRecommendationMode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AiRecommendationRunRepository
        extends JpaRepository<AiRecommendationRun, Long> {

    @Query("""
            select count(run)
            from AiRecommendationRun run
            where run.createdAt >= :fromAt
              and run.createdAt < :toExclusive
            """)
    long countForDashboard(
            @Param("fromAt") LocalDateTime fromAt,
            @Param("toExclusive") LocalDateTime toExclusive
    );

    @Query("""
            select count(run)
            from AiRecommendationRun run
            where run.createdAt >= :fromAt
              and run.createdAt < :toExclusive
              and run.mode = :mode
            """)
    long countForDashboardByMode(
            @Param("fromAt") LocalDateTime fromAt,
            @Param("toExclusive") LocalDateTime toExclusive,
            @Param("mode") AiRecommendationMode mode
    );

    @Query("""
            select count(run)
            from AiRecommendationRun run
            where run.createdAt >= :fromAt
              and run.createdAt < :toExclusive
              and run.fallbackUsed = true
            """)
    long countFallbackForDashboard(
            @Param("fromAt") LocalDateTime fromAt,
            @Param("toExclusive") LocalDateTime toExclusive
    );

    @Query("""
            select run.provider as provider,
                   run.model as model,
                   count(run) as runCount
            from AiRecommendationRun run
            where run.createdAt >= :fromAt
              and run.createdAt < :toExclusive
            group by run.provider, run.model
            order by count(run) desc, run.provider asc, run.model asc
            """)
    List<ProviderUsageProjection> summarizeProviderUsage(
            @Param("fromAt") LocalDateTime fromAt,
            @Param("toExclusive") LocalDateTime toExclusive
    );

    @Query("""
            select run.fallbackReason as reason,
                   count(run) as runCount
            from AiRecommendationRun run
            where run.createdAt >= :fromAt
              and run.createdAt < :toExclusive
              and run.fallbackUsed = true
              and run.fallbackReason is not null
            group by run.fallbackReason
            order by count(run) desc, run.fallbackReason asc
            """)
    List<FallbackReasonProjection> summarizeFallbackReasons(
            @Param("fromAt") LocalDateTime fromAt,
            @Param("toExclusive") LocalDateTime toExclusive
    );

    interface ProviderUsageProjection {
        String getProvider();

        String getModel();

        long getRunCount();
    }

    interface FallbackReasonProjection {
        String getReason();

        long getRunCount();
    }
}
