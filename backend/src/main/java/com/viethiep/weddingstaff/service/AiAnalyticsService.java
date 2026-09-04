package com.viethiep.weddingstaff.service;

import com.viethiep.weddingstaff.dto.AiAnalyticsResponse;
import com.viethiep.weddingstaff.dto.AiFallbackReasonResponse;
import com.viethiep.weddingstaff.dto.AiProviderUsageResponse;
import com.viethiep.weddingstaff.enumtype.AiRecommendationMode;
import com.viethiep.weddingstaff.repository.AiRecommendationRunRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AiAnalyticsService {
    private final AiRecommendationRunRepository runRepository;

    @Transactional(readOnly = true)
    public AiAnalyticsResponse analyze(LocalDate from, LocalDate to) {
        validateRange(from, to);

        LocalDateTime fromAt = from.atStartOfDay();
        LocalDateTime toExclusive = to.plusDays(1).atStartOfDay();

        long totalRuns = runRepository.countForDashboard(
                fromAt,
                toExclusive
        );
        long aiAssistedRuns = runRepository.countForDashboardByMode(
                fromAt,
                toExclusive,
                AiRecommendationMode.AI_ASSISTED
        );
        long fallbackRuns = runRepository.countFallbackForDashboard(
                fromAt,
                toExclusive
        );

        List<AiProviderUsageResponse> providerUsage =
                runRepository.summarizeProviderUsage(
                                fromAt,
                                toExclusive
                        )
                        .stream()
                        .map(item -> new AiProviderUsageResponse(
                                normalize(item.getProvider(), "UNKNOWN"),
                                normalize(item.getModel(), "Không xác định"),
                                item.getRunCount()
                        ))
                        .toList();

        List<AiFallbackReasonResponse> fallbackReasons =
                runRepository.summarizeFallbackReasons(
                                fromAt,
                                toExclusive
                        )
                        .stream()
                        .map(item -> new AiFallbackReasonResponse(
                                normalize(item.getReason(), "UNKNOWN"),
                                item.getRunCount()
                        ))
                        .toList();

        return new AiAnalyticsResponse(
                from,
                to,
                totalRuns,
                aiAssistedRuns,
                fallbackRuns,
                percentage(fallbackRuns, totalRuns),
                providerUsage,
                fallbackReasons
        );
    }

    private void validateRange(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new IllegalArgumentException(
                    "Khoảng ngày Dashboard là bắt buộc"
            );
        }
        if (from.isAfter(to)) {
            throw new IllegalArgumentException(
                    "Ngày bắt đầu không được sau ngày kết thúc"
            );
        }
    }

    private String normalize(String value, String fallback) {
        return value == null || value.isBlank()
                ? fallback
                : value;
    }

    private double percentage(long numerator, long denominator) {
        if (denominator <= 0) {
            return 0.0;
        }
        return Math.round(
                (numerator * 10000.0) / denominator
        ) / 100.0;
    }
}
