package com.viethiep.weddingstaff.dto;

import com.viethiep.weddingstaff.enumtype.AttendanceCheckAction;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateAttendanceCheckSessionRequest(
        @NotNull @Positive Long shiftId,
        @NotNull AttendanceCheckAction action,
        @Min(1) @Max(30) Integer validMinutes,
        @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
        @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude,
        @Min(20) @Max(1000) Integer radiusMeters
) {
    @AssertTrue(message = "Latitude và longitude phải được cung cấp cùng nhau")
    public boolean isCoordinatePairValid() {
        return (latitude == null) == (longitude == null);
    }

    @AssertTrue(message = "Không được đặt bán kính khi chưa có tọa độ")
    public boolean isRadiusPolicyValid() {
        return radiusMeters == null || (latitude != null && longitude != null);
    }
}
