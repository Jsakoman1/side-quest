package com.sidequest.sidequest.controller;

import com.sidequest.sidequest.dto.auth.LoginRequest;
import com.sidequest.sidequest.dto.auth.RegisterRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.Valid;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthControllerTest {

    private static final ValidatorFactory VALIDATOR_FACTORY = Validation.buildDefaultValidatorFactory();
    private static final Validator VALIDATOR = VALIDATOR_FACTORY.getValidator();

    @AfterAll
    static void tearDown() {
        VALIDATOR_FACTORY.close();
    }

    @Test
    void registerAndLoginEndpointsRequireValidPayloads() throws Exception {
        Method registerMethod = AuthController.class.getDeclaredMethod("register", RegisterRequest.class);
        Method loginMethod = AuthController.class.getDeclaredMethod("login", LoginRequest.class);

        assertTrue(registerMethod.getParameters()[0].isAnnotationPresent(Valid.class));
        assertTrue(loginMethod.getParameters()[0].isAnnotationPresent(Valid.class));
    }

    @Test
    void registerRequestRejectsInvalidValues() {
        RegisterRequest request = new RegisterRequest("not-an-email", "ab", "short");

        Set<ConstraintViolation<RegisterRequest>> violations = VALIDATOR.validate(request);

        assertFalse(violations.isEmpty());
        assertHasViolation(violations, "email");
        assertHasViolation(violations, "username");
        assertHasViolation(violations, "password");
    }

    @Test
    void loginRequestRejectsInvalidValues() {
        LoginRequest request = new LoginRequest("not-an-email", "");

        Set<ConstraintViolation<LoginRequest>> violations = VALIDATOR.validate(request);

        assertFalse(violations.isEmpty());
        assertHasViolation(violations, "email");
        assertHasViolation(violations, "password");
    }

    @Test
    void validAuthRequestsPassValidation() {
        RegisterRequest registerRequest = new RegisterRequest("user@example.com", "new-user", "strongPassword1");
        LoginRequest loginRequest = new LoginRequest("user@example.com", "strongPassword1");

        assertTrue(VALIDATOR.validate(registerRequest).isEmpty());
        assertTrue(VALIDATOR.validate(loginRequest).isEmpty());
    }

    private void assertHasViolation(Set<? extends ConstraintViolation<?>> violations, String propertyPath) {
        assertTrue(
                violations.stream().anyMatch(violation -> violation.getPropertyPath().toString().equals(propertyPath)),
                "Expected violation for property: " + propertyPath
        );
    }
}
