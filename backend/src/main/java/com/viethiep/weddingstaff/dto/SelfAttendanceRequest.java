package com.viethiep.weddingstaff.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SelfAttendanceRequest(
        @NotNull @Positive Long shiftId,
        @Size(max = 300) String qrToken,
        @Pattern(regexp = "\\d{6}", message = "OTP phải gồm đúng 6 chữ số") String otp,
        @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
        @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude
) {
    @AssertTrue(message = "Latitude và longitude phải được cung cấp cùng nhau")
    public boolean isCoordinatePairValid() {
        return (latitude == null) == (longitude == null);
    }

    @AssertTrue(message = "Phải cung cấp đúng một phương thức: QR hoặc OTP")
    public boolean isCredentialSelectionValid() {
        boolean hasQr = qrToken != null && !qrToken.isBlank();
        boolean hasOtp = otp != null && !otp.isBlank();
        return hasQr ^ hasOtp;
    }
}
