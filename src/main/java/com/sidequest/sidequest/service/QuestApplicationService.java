package com.sidequest.sidequest.service;

import com.sidequest.sidequest.dto.QuestApplicationRequestDTO;
import com.sidequest.sidequest.dto.QuestApplicationResponseDTO;
import com.sidequest.sidequest.mapper.QuestApplicationMgr;
import com.sidequest.sidequest.model.AppUser;
import com.sidequest.sidequest.model.AppUserRole;
import com.sidequest.sidequest.model.Quest;
import com.sidequest.sidequest.model.QuestApplication;
import com.sidequest.sidequest.model.QuestApplicationStatus;
import com.sidequest.sidequest.model.QuestStatus;
import com.sidequest.sidequest.repository.QuestApplicationRepository;
import com.sidequest.sidequest.repository.QuestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
public class QuestApplicationService {

    private final QuestApplicationRepository questApplicationRepository;
    private final QuestRepository questRepository;
    private final QuestApplicationMgr questApplicationMgr;

    public QuestApplicationService(
            QuestApplicationRepository questApplicationRepository,
            QuestRepository questRepository,
            QuestApplicationMgr questApplicationMgr
    ) {
        this.questApplicationRepository = questApplicationRepository;
        this.questRepository = questRepository;
        this.questApplicationMgr = questApplicationMgr;
    }

    public QuestApplicationResponseDTO applyForQuest(Long questId, QuestApplicationRequestDTO dto, AppUser currentUser) {
        Quest quest = requireOpenQuest(questId);
        validateNotQuestCreator(quest, currentUser);
        validateNoDuplicateApplication(questId, currentUser.getId());

        return saveAndMapApplication(questApplicationMgr.toEntity(dto, quest, currentUser));
    }

    public List<QuestApplicationResponseDTO> getApplicationsForQuest(Long questId, AppUser currentUser) {
        validateQuestOwnerOrAdmin(requireQuest(questId), currentUser);

        return questApplicationRepository.findByQuestId(questId)
                .stream()
                .map(questApplicationMgr::toDto)
                .toList();
    }

    public List<QuestApplicationResponseDTO> getApplicationsForApplicant(AppUser currentUser) {
        return questApplicationRepository.findByApplicantId(currentUser.getId())
                .stream()
                .map(questApplicationMgr::toDto)
                .toList();
    }

    @Transactional
    public QuestApplicationResponseDTO updateMyApplication(Long questId, QuestApplicationRequestDTO dto, AppUser currentUser) {
        QuestApplication application = requirePendingMyApplication(questId, currentUser);
        application.setMessage(dto.getMessage());
        application.setProposedPrice(dto.getProposedPrice());
        return saveAndMapApplication(application);
    }

    @Transactional
    public QuestApplicationResponseDTO withdrawMyApplication(Long questId, AppUser currentUser) {
        QuestApplication application = requirePendingMyApplication(questId, currentUser);
        application.setStatus(QuestApplicationStatus.WITHDRAWN);
        return saveAndMapApplication(application);
    }

    @Transactional
    public QuestApplicationResponseDTO approveApplication(Long questId, Long applicationId, AppUser currentUser) {
        Quest quest = requireOpenQuest(questId);
        validateQuestOwnerOrAdmin(quest, currentUser);

        QuestApplication application = requirePendingApplication(questId, applicationId);
        application.setStatus(QuestApplicationStatus.APPROVED);
        quest.setStatus(QuestStatus.ASSIGNED);

        declineOtherPendingApplications(questId, applicationId);
        questRepository.save(quest);
        return saveAndMapApplication(application);
    }

    @Transactional
    public QuestApplicationResponseDTO declineApplication(Long questId, Long applicationId, AppUser currentUser) {
        Quest quest = requireOpenQuest(questId);
        validateQuestOwnerOrAdmin(quest, currentUser);

        QuestApplication application = requirePendingApplication(questId, applicationId);
        application.setStatus(QuestApplicationStatus.DECLINED);
        return saveAndMapApplication(application);
    }

    private Quest requireQuest(Long questId) {
        return questRepository.findByIdWithCreator(questId)
                .orElseThrow(() -> ServiceErrors.notFound("Quest not found with id " + questId));
    }

    private Quest requireOpenQuest(Long questId) {
        Quest quest = requireQuest(questId);
        validateQuestIsOpen(quest);
        return quest;
    }

    private QuestApplication requirePendingApplication(Long questId, Long applicationId) {
        QuestApplication application = questApplicationRepository.findByIdAndQuestId(applicationId, questId)
                .orElseThrow(() -> ServiceErrors.notFound("Quest application not found with id " + applicationId));

        if (application.getStatus() != QuestApplicationStatus.PENDING) {
            throw ServiceErrors.badRequest("Only pending applications can be modified");
        }

        return application;
    }

    private QuestApplication requirePendingMyApplication(Long questId, AppUser currentUser) {
        QuestApplication application = questApplicationRepository.findByQuestIdAndApplicantId(questId, currentUser.getId())
                .orElseThrow(() -> ServiceErrors.notFound("Quest application not found for current user"));

        if (application.getStatus() != QuestApplicationStatus.PENDING) {
            throw ServiceErrors.badRequest("Only pending applications can be modified");
        }

        return application;
    }

    private QuestApplicationResponseDTO saveAndMapApplication(QuestApplication application) {
        return questApplicationMgr.toDto(questApplicationRepository.save(application));
    }

    private void declineOtherPendingApplications(Long questId, Long approvedApplicationId) {
        List<QuestApplication> pendingApplications = questApplicationRepository.findByQuestIdAndStatus(questId, QuestApplicationStatus.PENDING);
        List<QuestApplication> declinedApplications = new java.util.ArrayList<>();
        for (QuestApplication application : pendingApplications) {
            if (!Objects.equals(application.getId(), approvedApplicationId)) {
                application.setStatus(QuestApplicationStatus.DECLINED);
                declinedApplications.add(application);
            }
        }

        if (!declinedApplications.isEmpty()) {
            questApplicationRepository.saveAll(declinedApplications);
        }
    }

    private void validateQuestIsOpen(Quest quest) {
        if (quest.getStatus() != QuestStatus.OPEN) {
            throw ServiceErrors.badRequest("Applications are only allowed for open quests");
        }
    }

    private void validateNotQuestCreator(Quest quest, AppUser currentUser) {
        if (quest.getCreator().getId().equals(currentUser.getId())) {
            throw ServiceErrors.badRequest("Quest creator cannot apply to their own quest");
        }
    }

    private void validateNoDuplicateApplication(Long questId, Long applicantId) {
        if (questApplicationRepository.existsByQuestIdAndApplicantId(questId, applicantId)) {
            throw ServiceErrors.conflict("You have already applied for this quest");
        }
    }

    private void validateQuestOwnerOrAdmin(Quest quest, AppUser currentUser) {
        if (isAdmin(currentUser)) {
            return;
        }

        if (!quest.getCreator().getId().equals(currentUser.getId())) {
            throw ServiceErrors.forbidden("You are not allowed to view these applications");
        }
    }

    private boolean isAdmin(AppUser user) {
        return user != null && user.getRole() == AppUserRole.ADMIN;
    }
}
