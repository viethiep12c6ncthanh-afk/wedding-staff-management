package com.viethiep.weddingstaff.dto;

import java.util.List;

public record AiRecommendedCandidateResponse(
        int aiRank,
        Long employeeId,
        String employeeCode,
        String fullName,
        int deterministicRank,
        int deterministicScore,
        String explanation,
        List<String> strengths,
        List<String> risks
) {
}
