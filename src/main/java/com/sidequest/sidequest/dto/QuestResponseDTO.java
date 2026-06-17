package com.sidequest.sidequest.dto;

import com.sidequest.sidequest.model.QuestStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

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
    private Instant scheduledAt;
    private boolean termFixed;
    private Instant pendingScheduledAt;
    private Boolean pendingTermFixed;
    private Instant reopenedAt;

    private QuestStatus status;

}
