package com.viethiep.weddingstaff.controller;

import com.viethiep.weddingstaff.dto.AttendanceCheckSessionResponse;
import com.viethiep.weddingstaff.dto.AttendanceDemoConfigResponse;
import com.viethiep.weddingstaff.dto.CreateAttendanceCheckSessionRequest;
import com.viethiep.weddingstaff.dto.QrAttendanceResultResponse;
import com.viethiep.weddingstaff.dto.SelfAttendanceRequest;
import com.viethiep.weddingstaff.service.QrAttendanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/qr-attendance")
@RequiredArgsConstructor
public class QrAttendanceController {
    private final QrAttendanceService qrAttendanceService;

    @GetMapping("/demo-config")
    @PreAuthorize("hasAnyRole('ADMIN','COORDINATOR')")
    public AttendanceDemoConfigResponse demoConfig() {
        boolean enabled = qrAttendanceService.isDemoMode();
        return new AttendanceDemoConfigResponse(
                enabled,
                enabled
                        ? "DEMO MODE đang bật: bỏ kiểm tra khung giờ ca, nhưng vẫn kiểm tra phân công, QR/OTP, hạn phiên, GPS và chống chấm công trùng."
                        : "Production rules đang bật: phiên chỉ được tạo và sử dụng trong khung giờ chấm công."
        );
    }

    @PostMapping("/sessions")
    @PreAuthorize("hasAnyRole('ADMIN','COORDINATOR')")
    public AttendanceCheckSessionResponse createSession(
            @Valid @RequestBody CreateAttendanceCheckSessionRequest request,
            Authentication authentication
    ) {
        return qrAttendanceService.createSession(
                request,
                authentication.getName()
        );
    }

    @PostMapping("/sessions/{id}/revoke")
    @PreAuthorize("hasAnyRole('ADMIN','COORDINATOR')")
    public void revokeSession(
            @PathVariable Long id,
            Authentication authentication
    ) {
        qrAttendanceService.revokeSession(
                id,
                authentication.getName()
        );
    }

    @PostMapping("/check-in")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public QrAttendanceResultResponse checkIn(
            @Valid @RequestBody SelfAttendanceRequest request,
            Authentication authentication
    ) {
        return qrAttendanceService.checkIn(
                request,
                authentication.getName()
        );
    }

    @PostMapping("/check-out")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public QrAttendanceResultResponse checkOut(
            @Valid @RequestBody SelfAttendanceRequest request,
            Authentication authentication
    ) {
        return qrAttendanceService.checkOut(
                request,
                authentication.getName()
        );
    }
}
