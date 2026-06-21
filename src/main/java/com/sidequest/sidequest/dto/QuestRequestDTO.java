package com.sidequest.sidequest.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

import com.sidequest.sidequest.model.QuestAudience;
import com.sidequest.sidequest.model.QuestStatus;

import java.util.List;

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
    private QuestAudience audience;
    private Long creatorId;
    private QuestStatus status;
    private List<String> images;
}
