package com.sidequest.sidequest.mapper;

import com.sidequest.sidequest.dto.QuestRequestDTO;
import com.sidequest.sidequest.dto.QuestResponseDTO;
import com.sidequest.sidequest.dto.CircleSummaryDTO;
import com.sidequest.sidequest.model.AppUser;
import com.sidequest.sidequest.model.Quest;
import com.sidequest.sidequest.model.QuestStatus;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class QuestMgr {

    public QuestResponseDTO toDto(Quest quest) {
        if (quest == null) {
            return null;
        }

        return QuestResponseDTO.builder()
                .id(quest.getId())
                .creatorId(quest.getCreator().getId())
                .creatorUsername(quest.getCreator().getUsername())
                .creatorProfileDescription(quest.getCreator().getProfileDescription())
                .creatorProfileAvatarDataUrl(quest.getCreator().getProfileAvatarDataUrl())
                .title(quest.getTitle())
                .description(quest.getDescription())
                .awardAmount(quest.getAwardAmount())
                .assigneeTarget(quest.getAssigneeTarget())
                .scheduledAt(quest.getScheduledAt())
                .endsAt(quest.getEndsAt())
                .termFixed(quest.isTermFixed())
                .pendingScheduledAt(quest.getPendingScheduledAt())
                .pendingEndsAt(quest.getPendingEndsAt())
                .pendingTermFixed(quest.getPendingTermFixed())
                .reopenedAt(quest.getReopenedAt())
                .audience(quest.getAudience())
                .visibleToCircles(quest.getVisibleToCircles().stream()
                        .map(circle -> CircleSummaryDTO.builder()
                                .id(circle.getId())
                                .name(circle.getName())
                                .build())
                        .toList())
                .images(List.copyOf(quest.getImages()))
                .status(quest.getStatus())
                .build();
    }

    public Quest toEntity(QuestRequestDTO dto, AppUser creator) {
        if (dto == null) {
            return null;
        }

        Quest quest = new Quest();
        quest.setCreator(creator);
        quest.setTitle(dto.getTitle());
        quest.setDescription(dto.getDescription());
        quest.setAwardAmount(dto.getAwardAmount());
        quest.setAssigneeTarget(dto.getAssigneeTarget() == null ? 1 : dto.getAssigneeTarget());
        quest.setScheduledAt(dto.getScheduledAt());
        quest.setEndsAt(dto.getEndsAt());
        quest.setTermFixed(Boolean.TRUE.equals(dto.getTermFixed()));
        quest.setAudience(dto.getAudience() == null ? com.sidequest.sidequest.model.QuestAudience.CIRCLES : dto.getAudience());
        quest.setImages(dto.getImages() == null ? new java.util.ArrayList<>() : new java.util.ArrayList<>(dto.getImages()));
        quest.setStatus(QuestStatus.OPEN);

        return quest;
    }
}
