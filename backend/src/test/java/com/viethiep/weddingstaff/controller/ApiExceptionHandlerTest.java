package com.viethiep.weddingstaff.controller;

import com.viethiep.weddingstaff.dto.ApiErrorResponse;
import com.viethiep.weddingstaff.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.*;

class ApiExceptionHandlerTest {
    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    @Test
    void notFoundUsesStandard404Body() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/employees/999");

        ResponseEntity<ApiErrorResponse> response = handler.handleNotFound(
                new ResourceNotFoundException("Không tìm thấy nhân viên"),
                request
        );

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(404, response.getBody().status());
        assertEquals("NOT_FOUND", response.getBody().error());
        assertEquals("Không tìm thấy nhân viên", response.getBody().message());
        assertEquals("/api/employees/999", response.getBody().path());
    }

    @Test
    void businessRuleUsesStandard400Body() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/assignments/direct");

        ResponseEntity<ApiErrorResponse> response = handler.handleBusiness(
                new IllegalStateException("Ca đã đủ số lượng nhân viên"),
                request
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("BAD_REQUEST", response.getBody().error());
        assertEquals("Ca đã đủ số lượng nhân viên", response.getBody().message());
    }
}
