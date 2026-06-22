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
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class QuestApplicationService {

    private final QuestApplicationRepository questApplicationRepository;
    private final QuestRepository questRepository;
    private final QuestApplicationMgr questApplicationMgr;
    private final QuestNewsService questNewsService;
    private final QuestVisibilityService questVisibilityService;

    @Transactional
    public QuestApplicationResponseDTO applyForQuest(Long questId, QuestApplicationRequestDTO dto, AppUser currentUser) {
        Quest quest = requireVisibleOpenQuest(questId, currentUser);
        validateNotQuestCreator(quest, currentUser);
        validateNoDuplicateApplication(questId, currentUser.getId());
        validateMessage(dto);

        QuestApplication application = questApplicationMgr.toEntity(dto, quest, currentUser);
        QuestApplicationResponseDTO response = saveAndMapApplication(application);
        questNewsService.notifyApplicationCreated(quest, application, currentUser);
        return response;
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

    public List<QuestApplicationResponseDTO> getAllApplicationsForAdmin(AppUser currentUser) {
        validateAdmin(currentUser);

        return questApplicationRepository.findAllDetailed().stream()
                .map(questApplicationMgr::toDto)
                .toList();
    }

    @Transactional
    public QuestApplicationResponseDTO updateMyApplication(Long questId, QuestApplicationRequestDTO dto, AppUser currentUser) {
        QuestApplication application = requirePendingMyApplication(questId, currentUser);
        validateMessage(dto);
        application.setMessage(dto.getMessage() == null ? null : dto.getMessage().trim());
        application.setProposedPrice(dto.getProposedPrice());
        QuestApplicationResponseDTO response = saveAndMapApplication(application);
        questNewsService.notifyApplicationUpdated(application.getQuest(), application, currentUser);
        return response;
    }

    @Transactional
    public QuestApplicationResponseDTO withdrawMyApplication(Long questId, AppUser currentUser) {
        QuestApplication application = requirePendingMyApplication(questId, currentUser);
        application.setStatus(QuestApplicationStatus.WITHDRAWN);
        QuestApplicationResponseDTO response = saveAndMapApplication(application);
        questNewsService.notifyApplicationWithdrawn(application.getQuest(), application, currentUser);
        return response;
    }

    @Transactional
    public QuestApplicationResponseDTO approveApplication(Long questId, Long applicationId, AppUser currentUser) {
        Quest quest = requireOpenQuest(questId);
        validateQuestOwnerOrAdmin(quest, currentUser);

        QuestApplication application = requirePendingApplication(questId, applicationId);
        application.setStatus(QuestApplicationStatus.APPROVED);
        quest.setStatus(QuestStatus.ASSIGNED);

        List<QuestApplication> declinedApplications = declineOtherPendingApplications(questId, applicationId);
        questRepository.save(quest);
        QuestApplicationResponseDTO response = saveAndMapApplication(application);
        for (QuestApplication declinedApplication : declinedApplications) {
            questNewsService.notifyApplicationDeclined(quest, declinedApplication, currentUser);
        }
        questNewsService.notifyApplicationApproved(quest, application, currentUser);
        return response;
    }

    @Transactional
    public QuestApplicationResponseDTO declineApplication(Long questId, Long applicationId, AppUser currentUser) {
        Quest quest = requireOpenQuest(questId);
        validateQuestOwnerOrAdmin(quest, currentUser);

        QuestApplication application = requirePendingApplication(questId, applicationId);
        application.setStatus(QuestApplicationStatus.DECLINED);
        QuestApplicationResponseDTO response = saveAndMapApplication(application);
        questNewsService.notifyApplicationDeclined(quest, application, currentUser);
        return response;
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

    private Quest requireVisibleOpenQuest(Long questId, AppUser currentUser) {
        Quest quest = requireQuest(questId);
        if (!questVisibilityService.canViewQuest(currentUser, quest)) {
            throw ServiceErrors.notFound("Quest not found with id " + questId);
        }

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

    private List<QuestApplication> declineOtherPendingApplications(Long questId, Long approvedApplicationId) {
        List<QuestApplication> pendingApplications = questApplicationRepository.findByQuestIdAndStatus(questId, QuestApplicationStatus.PENDING);
        if (pendingApplications.isEmpty()) {
            return List.of();
        }

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

        return declinedApplications;
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

    private void validateMessage(QuestApplicationRequestDTO dto) {
        if (dto == null || dto.getMessage() == null || dto.getMessage().trim().isEmpty()) {
            throw ServiceErrors.badRequest("Application message is required");
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

    private void validateAdmin(AppUser currentUser) {
        if (!isAdmin(currentUser)) {
            throw ServiceErrors.forbidden("Admin access is required");
        }
    }
}
