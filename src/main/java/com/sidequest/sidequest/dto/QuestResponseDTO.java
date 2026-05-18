package com.sidequest.sidequest.dto;

import com.sidequest.sidequest.model.QuestStatus;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestResponseDTO {
    private long id;

    private Long creatorId;
    private String creatorUsername;

    private String title;
    private String description;

    private BigDecimal awardAmount;

    private QuestStatus status;

}