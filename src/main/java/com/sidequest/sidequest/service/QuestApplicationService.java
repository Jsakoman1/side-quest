package com.sidequest.sidequest.service;

import com.sidequest.sidequest.dto.QuestApplicationRequestDTO;
import com.sidequest.sidequest.dto.QuestApplicationResponseDTO;
import com.sidequest.sidequest.mapper.QuestApplicationMgr;
import com.sidequest.sidequest.model.AppUser;
import com.sidequest.sidequest.model.Quest;
import com.sidequest.sidequest.model.QuestApplication;
import com.sidequest.sidequest.repository.AppUserRepository;
import com.sidequest.sidequest.repository.QuestApplicationRepository;
import com.sidequest.sidequest.repository.QuestRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class QuestApplicationService {

    private final QuestApplicationRepository questApplicationRepository;
    private final QuestRepository questRepository;
    private final AppUserRepository appUserRepository;
    private final QuestApplicationMgr questApplicationMgr;

    public QuestApplicationService(
            QuestApplicationRepository questApplicationRepository,
            QuestRepository questRepository,
            AppUserRepository appUserRepository,
            QuestApplicationMgr questApplicationMgr
    ) {
        this.questApplicationRepository = questApplicationRepository;
        this.questRepository = questRepository;
        this.appUserRepository = appUserRepository;
        this.questApplicationMgr = questApplicationMgr;
    }

    public QuestApplicationResponseDTO applyForQuest(Long questId, QuestApplicationRequestDTO dto) {
        Quest quest = findQuestOrThrow(questId);
        AppUser applicant = findAppUserOrThrow(dto.getApplicantId());

        QuestApplication application = questApplicationMgr.toEntity(dto, quest, applicant);
        QuestApplication savedApplication = questApplicationRepository.save(application);

        return questApplicationMgr.toDto(savedApplication);
    }

    public List<QuestApplicationResponseDTO> getApplicationsForQuest(Long questId) {
        return questApplicationRepository.findByQuestId(questId)
                .stream()
                .map(questApplicationMgr::toDto)
                .toList();
    }

    private Quest findQuestOrThrow(Long questId) {
        return questRepository.findById(questId)
                .orElseThrow(() -> new RuntimeException("Quest not found with id " + questId));
    }

    private AppUser findAppUserOrThrow(Long appUserId) {
        return appUserRepository.findById(appUserId)
                .orElseThrow(() -> new RuntimeException("AppUser not found with id " + appUserId));
    }

}