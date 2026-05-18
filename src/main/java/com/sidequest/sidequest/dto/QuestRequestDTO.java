package com.sidequest.sidequest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestRequestDTO {

    private @NotNull Long creatorId;
    private @NotBlank String title;
    private @NotBlank String description;
    private BigDecimal awardAmount;
}