package com.sidequest.sidequest.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestApplicationRequestDTO {

    @NotNull
    private long applicantId;
    private String message;
    private BigDecimal proposedPrice;
}