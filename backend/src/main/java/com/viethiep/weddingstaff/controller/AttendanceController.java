package com.viethiep.weddingstaff.controller;

import com.viethiep.weddingstaff.dto.AttendanceResponse;
import com.viethiep.weddingstaff.dto.CreateAttendanceRequest;
import com.viethiep.weddingstaff.dto.UpdateAttendanceRequest;
import com.viethiep.weddingstaff.service.AttendanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/attendances")
@RequiredArgsConstructor
public class AttendanceController {
    private final AttendanceService attendanceService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','COORDINATOR')")
    public List<AttendanceResponse> findAll() {
        return attendanceService.findAll();
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public List<AttendanceResponse> findMine(Authentication authentication) {
        return attendanceService.findMine(authentication.getName());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','COORDINATOR','EMPLOYEE')")
    public AttendanceResponse create(
            @Valid @RequestBody CreateAttendanceRequest request,
            Authentication authentication
    ) {
        return attendanceService.create(request, authentication.getName());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','COORDINATOR','EMPLOYEE')")
    public AttendanceResponse update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateAttendanceRequest request,
            Authentication authentication
    ) {
        return attendanceService.update(id, request, authentication.getName());
    }

    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasAnyRole('ADMIN','COORDINATOR')")
    public AttendanceResponse confirm(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return attendanceService.confirm(id, authentication.getName());
    }
}
