package com.sidequest.sidequest.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertFalse;

class RequestDtoValidationTest {
    private static final ValidatorFactory VALIDATOR_FACTORY = Validation.buildDefaultValidatorFactory();
    private static final Validator VALIDATOR = VALIDATOR_FACTORY.getValidator();

    @AfterAll
    static void tearDown() {
        VALIDATOR_FACTORY.close();
    }

    @Test
    void questRequestRejectsInvalidAmountAndOversizedText() {
        QuestRequestDTO request = QuestRequestDTO.builder()
                .title("x".repeat(256))
                .description("x".repeat(2001))
                .awardAmount(BigDecimal.ZERO)
                .build();

        assertFalse(VALIDATOR.validate(request).isEmpty());
    }

    @Test
    void applicationRequestRejectsMissingPriceAndOversizedMessage() {
        QuestApplicationRequestDTO request = QuestApplicationRequestDTO.builder()
                .message("x".repeat(2001))
                .build();

        assertFalse(VALIDATOR.validate(request).isEmpty());
    }
}
