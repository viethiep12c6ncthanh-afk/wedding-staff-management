package com.viethiep.weddingstaff.dto;

import com.viethiep.weddingstaff.enumtype.AttendanceCheckAction;
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
                null
        );

        Set<ConstraintViolation<DirectAssignmentRequest>> violations = validator.validate(request);

        assertTrue(violations.stream().anyMatch(v ->
                v.getPropertyPath().toString().equals("employeeId")
        ));
    }
    @Test
    void selfAttendanceRejectsOneSidedCoordinates() {
        SelfAttendanceRequest request = new SelfAttendanceRequest(
                1L,
                "qr-token",
                null,
                10.7769,
                null
        );

        Set<ConstraintViolation<SelfAttendanceRequest>> violations =
                validator.validate(request);

        assertTrue(violations.stream().anyMatch(v ->
                v.getPropertyPath().toString().equals("coordinatePairValid")
        ));
    }

    @Test
    void selfAttendanceRejectsBothQrAndOtp() {
        SelfAttendanceRequest request = new SelfAttendanceRequest(
                1L,
                "qr-token",
                "123456",
                null,
                null
        );

        Set<ConstraintViolation<SelfAttendanceRequest>> violations =
                validator.validate(request);

        assertTrue(violations.stream().anyMatch(v ->
                v.getPropertyPath().toString().equals("credentialSelectionValid")
        ));
    }

    @Test
    void selfAttendanceRejectsNonNumericOtp() {
        SelfAttendanceRequest request = new SelfAttendanceRequest(
                1L,
                null,
                "12AB56",
                null,
                null
        );

        Set<ConstraintViolation<SelfAttendanceRequest>> violations =
                validator.validate(request);

        assertTrue(violations.stream().anyMatch(v ->
                v.getPropertyPath().toString().equals("otp")
        ));
    }

    @Test
    void attendanceSessionRejectsRadiusWithoutCoordinates() {
        CreateAttendanceCheckSessionRequest request =
                new CreateAttendanceCheckSessionRequest(
                        1L,
                        AttendanceCheckAction.CHECK_IN,
                        10,
                        null,
                        null,
                        150
                );

        Set<ConstraintViolation<CreateAttendanceCheckSessionRequest>> violations =
                validator.validate(request);

        assertTrue(violations.stream().anyMatch(v ->
                v.getPropertyPath().toString().equals("radiusPolicyValid")
        ));
    }

    @Test
    void replacementRequestRejectsNonPositiveAssignmentId() {
        CreateReplacementRequest request =
                new CreateReplacementRequest(0L, "Có việc đột xuất");

        Set<ConstraintViolation<CreateReplacementRequest>> violations =
                validator.validate(request);

        assertTrue(violations.stream().anyMatch(v ->
                v.getPropertyPath().toString().equals("assignmentId")
        ));
    }

    @Test
    void replacementInvitationRejectsNonPositiveEmployeeId() {
        InviteReplacementRequest request =
                new InviteReplacementRequest(-1L);

        Set<ConstraintViolation<InviteReplacementRequest>> violations =
                validator.validate(request);

        assertTrue(violations.stream().anyMatch(v ->
                v.getPropertyPath().toString().equals("employeeId")
        ));
    }

}
