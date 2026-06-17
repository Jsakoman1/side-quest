package com.sidequest.sidequest.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

import com.sidequest.sidequest.model.QuestStatus;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestRequestDTO {

    private @NotBlank String title;
    private @NotBlank String description;
    private BigDecimal awardAmount;
    private Instant scheduledAt;
    private Boolean termFixed;
    private Long creatorId;
    private QuestStatus status;
}
