package com.viethiep.weddingstaff.service;

import com.viethiep.weddingstaff.dto.AiAnalyticsResponse;
import com.viethiep.weddingstaff.enumtype.AiRecommendationMode;
import com.viethiep.weddingstaff.repository.AiRecommendationRunRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiAnalyticsServiceTest {
    @Mock
    private AiRecommendationRunRepository runRepository;

    @InjectMocks
    private AiAnalyticsService service;

    @Test
    void summarizesAiUsageWithoutLoadingRecommendationLobs() {
        when(runRepository.countForDashboard(any(), any()))
                .thenReturn(10L);
        when(runRepository.countForDashboardByMode(
                any(),
                any(),
                eq(AiRecommendationMode.AI_ASSISTED)
        )).thenReturn(7L);
        when(runRepository.countFallbackForDashboard(any(), any()))
                .thenReturn(3L);

        AiRecommendationRunRepository.ProviderUsageProjection provider =
                mock(AiRecommendationRunRepository.ProviderUsageProjection.class);
        when(provider.getProvider()).thenReturn("OLLAMA");
        when(provider.getModel()).thenReturn("qwen3:4b-instruct");
        when(provider.getRunCount()).thenReturn(10L);

        AiRecommendationRunRepository.FallbackReasonProjection reason =
                mock(AiRecommendationRunRepository.FallbackReasonProjection.class);
        when(reason.getReason()).thenReturn("AI_PROVIDER_ERROR");
        when(reason.getRunCount()).thenReturn(3L);

        when(runRepository.summarizeProviderUsage(any(), any()))
                .thenReturn(List.of(provider));
        when(runRepository.summarizeFallbackReasons(any(), any()))
                .thenReturn(List.of(reason));

        AiAnalyticsResponse result = service.analyze(
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 30)
        );

        assertEquals(10, result.totalRuns());
        assertEquals(7, result.aiAssistedRuns());
        assertEquals(3, result.fallbackRuns());
        assertEquals(30.0, result.fallbackRate());
        assertEquals("OLLAMA", result.providerUsage().getFirst().provider());
        assertEquals(
                "AI_PROVIDER_ERROR",
                result.fallbackReasons().getFirst().reason()
        );
    }
}
