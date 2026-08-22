package com.viethiep.weddingstaff.dto;

import java.util.List;

public record ReplacementCandidateResponse(
        int rank,
        Long employeeId,
        String employeeCode,
        String fullName,
        int totalScore,
        int reputationScore,
        int reputationPoints,
        int reliabilityPercent,
        int reliabilityPoints,
        int completedShiftCount,
        int experiencePoints,
        int sameRoleCompletedCount,
        int sameRolePoints,
        List<String> reasons
) {
}
