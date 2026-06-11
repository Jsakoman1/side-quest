package com.sidequest.sidequest.dto;

import com.sidequest.sidequest.model.QuestApplicationStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestApplicationResponseDTO {

    private Long id;

    private Long questId;
    private String questTitle;
    private String questDescription;

    private Long applicantId;
    private String applicantUsername;

    private String message;
    private BigDecimal proposedPrice;

    private QuestApplicationStatus status;
    private Instant createdAt;
}
