package com.sidequest.sidequest.mapper;

import com.sidequest.sidequest.dto.QuestApplicationRequestDTO;
import com.sidequest.sidequest.dto.QuestApplicationResponseDTO;
import com.sidequest.sidequest.model.AppUser;
import com.sidequest.sidequest.model.Quest;
import com.sidequest.sidequest.model.QuestApplication;
import com.sidequest.sidequest.model.QuestApplicationStatus;
import org.springframework.stereotype.Component;

@Component
public class QuestApplicationMgr {

    public QuestApplication toEntity(QuestApplicationRequestDTO dto, Quest quest, AppUser applicant) {
        QuestApplication application = new QuestApplication();
        application.setQuest(quest);
        application.setApplicant(applicant);
        application.setMessage(dto.getMessage());
        application.setProposedPrice(dto.getProposedPrice());
        application.setStatus(QuestApplicationStatus.PENDING);
        return application;
    }

    public QuestApplicationResponseDTO toDto(QuestApplication application) {
        if (application == null) {
            return null;
        }

        return QuestApplicationResponseDTO.builder()
                .id(application.getId())
                .questId(application.getQuest().getId())
                .questTitle(application.getQuest().getTitle())
                .questDescription(application.getQuest().getDescription())
                .applicantId(application.getApplicant().getId())
                .applicantUsername(application.getApplicant().getUsername())
                .message(application.getMessage())
                .proposedPrice(application.getProposedPrice())
                .status(application.getStatus())
                .createdAt(application.getCreatedAt())
                .build();
    }
}
