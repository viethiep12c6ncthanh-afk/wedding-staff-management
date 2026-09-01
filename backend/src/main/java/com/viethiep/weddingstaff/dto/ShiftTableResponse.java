package com.viethiep.weddingstaff.dto;

import com.viethiep.weddingstaff.enumtype.CommonStatus;

public record ShiftTableResponse(
        Long id,
        Long areaId,
        String tableCode,
        String displayName,
        String note,
        CommonStatus status
) {
}
