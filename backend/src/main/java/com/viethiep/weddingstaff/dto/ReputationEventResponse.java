package com.viethiep.weddingstaff.dto;

import com.viethiep.weddingstaff.enumtype.ReputationEventType;
import com.viethiep.weddingstaff.enumtype.ReputationSourceType;

import java.time.LocalDateTime;

public record ReputationEventResponse(
        Long id,
        ReputationEventType eventType,
        ReputationSourceType sourceType,
        Long sourceId,
        Integer scoreBefore,
        Integer scoreDelta,
        Integer scoreAfter,
        String reason,
        String actorUsername,
        LocalDateTime occurredAt
) {
}
