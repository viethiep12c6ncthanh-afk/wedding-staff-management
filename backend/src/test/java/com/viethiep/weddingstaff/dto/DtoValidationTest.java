package com.viethiep.weddingstaff.dto;

import com.viethiep.weddingstaff.enumtype.ShiftRole;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class DtoValidationTest {
    private static jakarta.validation.ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        validatorFactory.close();
    }

    @Test
    void loginRejectsBlankUsername() {
        LoginRequest request = new LoginRequest("   ", "password123");

        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);

        assertTrue(violations.stream().anyMatch(v ->
                v.getPropertyPath().toString().equals("username")
        ));
    }

    @Test
    void registrationRejectsNonPositiveShiftId() {
        RegistrationRequest request = new RegistrationRequest(0L);

        Set<ConstraintViolation<RegistrationRequest>> violations = validator.validate(request);

        assertTrue(violations.stream().anyMatch(v ->
                v.getPropertyPath().toString().equals("shiftId")
        ));
    }

    @Test
    void directAssignmentRejectsNonPositiveEmployeeId() {
        DirectAssignmentRequest request = new DirectAssignmentRequest(
                1L,
                -1L,
                ShiftRole.STAFF,
                null,
                null
        );

        Set<ConstraintViolation<DirectAssignmentRequest>> violations = validator.validate(request);

        assertTrue(violations.stream().anyMatch(v ->
                v.getPropertyPath().toString().equals("employeeId")
        ));
    }
}
