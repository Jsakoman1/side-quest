package com.sidequest.sidequest.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestApplicationRequestDTO {

    private String message;
    private BigDecimal proposedPrice;
}