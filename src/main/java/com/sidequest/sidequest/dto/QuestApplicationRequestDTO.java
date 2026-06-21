package com.sidequest.sidequest.dto;

import lombok.*;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestApplicationRequestDTO {

    private @NotBlank String message;
    private BigDecimal proposedPrice;
}
