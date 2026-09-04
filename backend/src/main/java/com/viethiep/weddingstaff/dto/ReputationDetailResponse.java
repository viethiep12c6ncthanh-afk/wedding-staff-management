package com.viethiep.weddingstaff.dto;

import java.util.List;

public record ReputationDetailResponse(
        ReputationSummaryResponse summary,
        List<ReputationEventResponse> events,
        List<EmployeeEvaluationResponse> evaluations
) {
}
