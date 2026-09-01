package com.viethiep.weddingstaff.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.viethiep.weddingstaff.config.AiRecommendationProperties;
import com.viethiep.weddingstaff.dto.AiRecommendationResponse;
import com.viethiep.weddingstaff.dto.AiRecommendedCandidateResponse;
import com.viethiep.weddingstaff.dto.ReplacementCandidateResponse;
import com.viethiep.weddingstaff.entity.*;
import com.viethiep.weddingstaff.enumtype.AiRecommendationMode;
import com.viethiep.weddingstaff.exception.ResourceNotFoundException;
import com.viethiep.weddingstaff.repository.AiRecommendationRunRepository;
import com.viethiep.weddingstaff.repository.EmployeeEvaluationRepository;
import com.viethiep.weddingstaff.repository.ReplacementRequestRepository;
import com.viethiep.weddingstaff.repository.UserAccountRepository;
import com.viethiep.weddingstaff.service.ai.AiRecommendationClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AiRecommendationService {
    private static final int MAX_CANDIDATES = 10;
    private static final int MAX_EVALUATIONS = 5;

    private final CandidateRecommendationService candidateRecommendationService;
    private final ReplacementRequestRepository requestRepository;
    private final EmployeeEvaluationRepository evaluationRepository;
    private final UserAccountRepository userRepository;
    private final AiRecommendationRunRepository runRepository;
    private final AiRecommendationClient aiClient;
    private final AiRecommendationProperties properties;
    private final ObjectMapper objectMapper;

    @Transactional
    public AiRecommendationResponse recommend(
            Long requestId,
            String actorUsername
    ) {
        List<ReplacementCandidateResponse> deterministicAll =
                candidateRecommendationService.findCandidates(requestId);
        ReplacementRequest request = requestRepository.findByIdWithDetails(requestId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy yêu cầu thay ca"
                ));
        UserAccount actor = userRepository.findByUsername(actorUsername)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy tài khoản"
                ));

        int candidateLimit = clamp(properties.getCandidateLimit(), 1, MAX_CANDIDATES);
        List<ReplacementCandidateResponse> deterministic = deterministicAll
                .stream()
                .limit(candidateLimit)
                .toList();
        String deterministicSnapshot = writeJson(deterministic);
        String contextSnapshot = buildContextJson(request, deterministic);

        if (deterministic.isEmpty()) {
            return persistFallback(
                    request,
                    actor,
                    deterministic,
                    deterministicSnapshot,
                    contextSnapshot,
                    "NO_ELIGIBLE_CANDIDATES",
                    "Không có ứng viên hợp lệ để AI phân tích."
            );
        }

        if (!aiClient.isAvailable()) {
            return persistFallback(
                    request,
                    actor,
                    deterministic,
                    deterministicSnapshot,
                    contextSnapshot,
                    "AI_DISABLED_OR_NOT_CONFIGURED",
                    "AI chưa được bật hoặc provider chưa được cấu hình; hệ thống dùng xếp hạng deterministic."
            );
        }

        AiRecommendationClient.Result aiResult;
        try {
            aiResult = aiClient.recommend(
                    new AiRecommendationClient.Prompt(contextSnapshot)
            );
        } catch (RuntimeException ex) {
            return persistFallback(
                    request,
                    actor,
                    deterministic,
                    deterministicSnapshot,
                    contextSnapshot,
                    "AI_PROVIDER_ERROR",
                    "AI không khả dụng tại thời điểm này; hệ thống dùng xếp hạng deterministic."
            );
        }

        if (!isValidAiResult(aiResult, deterministic)) {
            return persistFallback(
                    request,
                    actor,
                    deterministic,
                    deterministicSnapshot,
                    contextSnapshot,
                    "AI_OUTPUT_INVALID",
                    "Kết quả AI không vượt qua kiểm tra an toàn; hệ thống dùng xếp hạng deterministic."
            );
        }

        Map<Long, ReplacementCandidateResponse> deterministicById = deterministic
                .stream()
                .collect(Collectors.toMap(
                        ReplacementCandidateResponse::employeeId,
                        Function.identity()
                ));
        List<AiRecommendedCandidateResponse> candidates = new ArrayList<>();
        for (int index = 0; index < aiResult.candidates().size(); index++) {
            AiRecommendationClient.CandidateAnalysis analysis =
                    aiResult.candidates().get(index);
            ReplacementCandidateResponse source = deterministicById.get(
                    analysis.employeeId()
            );
            candidates.add(new AiRecommendedCandidateResponse(
                    index + 1,
                    source.employeeId(),
                    source.employeeCode(),
                    source.fullName(),
                    source.rank(),
                    source.totalScore(),
                    limitText(analysis.explanation(), 1000),
                    sanitizeList(analysis.strengths()),
                    sanitizeList(analysis.risks())
            ));
        }

        String summary = limitText(aiResult.summary(), 2000);
        AiRecommendationRun run = runRepository.saveAndFlush(
                AiRecommendationRun.builder()
                        .replacementRequest(request)
                        .mode(AiRecommendationMode.AI_ASSISTED)
                        .provider(aiClient.provider())
                        .model(aiClient.model())
                        .fallbackUsed(false)
                        .summary(summary)
                        .deterministicSnapshot(deterministicSnapshot)
                        .contextSnapshot(contextSnapshot)
                        .aiResultJson(aiResult.rawJson())
                        .requestedBy(actor)
                        .build()
        );
        return new AiRecommendationResponse(
                run.getId(),
                requestId,
                AiRecommendationMode.AI_ASSISTED,
                false,
                null,
                aiClient.provider(),
                aiClient.model(),
                summary,
                candidates
        );
    }

    private AiRecommendationResponse persistFallback(
            ReplacementRequest request,
            UserAccount actor,
            List<ReplacementCandidateResponse> deterministic,
            String deterministicSnapshot,
            String contextSnapshot,
            String fallbackReason,
            String summary
    ) {
        List<AiRecommendedCandidateResponse> fallbackCandidates = new ArrayList<>();
        for (int index = 0; index < deterministic.size(); index++) {
            ReplacementCandidateResponse candidate = deterministic.get(index);
            fallbackCandidates.add(new AiRecommendedCandidateResponse(
                    index + 1,
                    candidate.employeeId(),
                    candidate.employeeCode(),
                    candidate.fullName(),
                    candidate.rank(),
                    candidate.totalScore(),
                    "Giữ thứ tự deterministic vì AI không được sử dụng cho kết quả này.",
                    candidate.reasons().stream().limit(2).toList(),
                    List.of()
            ));
        }
        AiRecommendationRun run = runRepository.saveAndFlush(
                AiRecommendationRun.builder()
                        .replacementRequest(request)
                        .mode(AiRecommendationMode.DETERMINISTIC_FALLBACK)
                        .provider(aiClient.provider())
                        .model(aiClient.model())
                        .fallbackUsed(true)
                        .fallbackReason(fallbackReason)
                        .summary(summary)
                        .deterministicSnapshot(deterministicSnapshot)
                        .contextSnapshot(contextSnapshot)
                        .aiResultJson(null)
                        .requestedBy(actor)
                        .build()
        );
        return new AiRecommendationResponse(
                run.getId(),
                request.getId(),
                AiRecommendationMode.DETERMINISTIC_FALLBACK,
                true,
                fallbackReason,
                aiClient.provider(),
                aiClient.model(),
                summary,
                fallbackCandidates
        );
    }

    private String buildContextJson(
            ReplacementRequest request,
            List<ReplacementCandidateResponse> candidates
    ) {
        ShiftAssignment original = request.getOriginalAssignment();
        WorkShift shift = original.getShift();
        Event event = shift.getEvent();
        Venue venue = event.getVenue();

        Map<String, Object> context = new LinkedHashMap<>();
        context.put("requestId", request.getId());
        context.put("decisionPolicy", Map.of(
                "hardConstraintsAlreadyApplied", true,
                "coordinatorMakesFinalDecision", true,
                "aiMayOnlyReorderProvidedEmployeeIds", true
        ));
        context.put("jobContext", Map.ofEntries(
                Map.entry("shiftRole", original.getShiftRole().name()),
                Map.entry(
                        "area",
                        nullable(
                                original.getShiftArea() == null
                                        ? null
                                        : original.getShiftArea().getName()
                        )
                ),
                Map.entry(
                        "tables",
                        original.getTables().stream()
                                .map(ShiftTable::getTableCode)
                                .sorted(String.CASE_INSENSITIVE_ORDER)
                                .toList()
                ),
                Map.entry("task", nullable(original.getTask())),
                Map.entry("shiftName", shift.getName()),
                Map.entry("shiftStartAt", shift.getStartAt().toString()),
                Map.entry("shiftEndAt", shift.getEndAt().toString()),
                Map.entry("shiftDescription", nullable(shift.getDescription())),
                Map.entry("eventName", event.getName()),
                Map.entry("eventDescription", nullable(event.getDescription())),
                Map.entry("venueName", venue.getName()),
                Map.entry("venueAddress", venue.getAddress())
        ));

        List<Map<String, Object>> candidateContexts = new ArrayList<>();
        int evaluationLimit = clamp(
                properties.getRecentEvaluationLimit(),
                0,
                MAX_EVALUATIONS
        );
        for (ReplacementCandidateResponse candidate : candidates) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("employeeId", candidate.employeeId());
            item.put("employeeCode", candidate.employeeCode());
            item.put("deterministicRank", candidate.rank());
            item.put("deterministicScore", candidate.totalScore());
            item.put("reputationScore", candidate.reputationScore());
            item.put("reliabilityPercent", candidate.reliabilityPercent());
            item.put("completedShiftCount", candidate.completedShiftCount());
            item.put("sameRoleCompletedCount", candidate.sameRoleCompletedCount());
            item.put("recentEvaluations", evaluationRepository
                    .findAllByEmployeeId(candidate.employeeId())
                    .stream()
                    .limit(evaluationLimit)
                    .map(this::evaluationContext)
                    .toList());
            candidateContexts.add(item);
        }
        context.put("candidates", candidateContexts);
        return writeJson(context);
    }

    private Map<String, Object> evaluationContext(EmployeeEvaluation evaluation) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("rating", evaluation.getRating());
        result.put("comment", nullable(evaluation.getComment()));
        result.put("shiftName", evaluation.getAssignment().getShift().getName());
        result.put("eventName", evaluation.getAssignment().getShift().getEvent().getName());
        result.put("venueName", evaluation.getAssignment().getShift().getEvent().getVenue().getName());
        result.put("evaluatedAt", evaluation.getEvaluatedAt().toString());
        return result;
    }

    private boolean isValidAiResult(
            AiRecommendationClient.Result result,
            List<ReplacementCandidateResponse> deterministic
    ) {
        if (result == null
                || result.summary() == null
                || result.summary().isBlank()
                || result.candidates() == null
                || result.candidates().size() != deterministic.size()) {
            return false;
        }
        Set<Long> expected = deterministic.stream()
                .map(ReplacementCandidateResponse::employeeId)
                .collect(Collectors.toSet());
        Set<Long> actual = new HashSet<>();
        for (AiRecommendationClient.CandidateAnalysis analysis : result.candidates()) {
            if (analysis == null
                    || analysis.employeeId() == null
                    || analysis.explanation() == null
                    || analysis.explanation().isBlank()
                    || !actual.add(analysis.employeeId())) {
                return false;
            }
        }
        return actual.equals(expected);
    }

    private List<String> sanitizeList(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .filter(Objects::nonNull)
                .map(value -> limitText(value, 300))
                .filter(value -> !value.isBlank())
                .limit(5)
                .toList();
    }

    private String limitText(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        return trimmed.length() <= maxLength
                ? trimmed
                : trimmed.substring(0, maxLength);
    }

    private String nullable(String value) {
        return value == null ? "" : value;
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Không thể tạo snapshot AI", ex);
        }
    }
}
