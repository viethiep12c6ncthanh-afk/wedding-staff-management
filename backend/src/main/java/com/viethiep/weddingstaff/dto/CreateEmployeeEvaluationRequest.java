package com.viethiep.weddingstaff.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateEmployeeEvaluationRequest(
        @NotNull(message = "Phân công là bắt buộc")
        Long assignmentId,

        @NotNull(message = "Điểm đánh giá là bắt buộc")
        @Min(value = 1, message = "Điểm đánh giá phải từ 1 đến 5")
        @Max(value = 5, message = "Điểm đánh giá phải từ 1 đến 5")
        Integer rating,

        @Size(max = 500, message = "Nhận xét tối đa 500 ký tự")
        String comment
) {
}
