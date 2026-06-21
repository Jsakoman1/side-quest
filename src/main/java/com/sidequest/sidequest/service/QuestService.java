package com.sidequest.sidequest.service;

import com.sidequest.sidequest.dto.QuestRequestDTO;
import com.sidequest.sidequest.mapper.QuestMgr;
import com.sidequest.sidequest.model.AppUser;
import com.sidequest.sidequest.model.AppUserRole;
import com.sidequest.sidequest.model.QuestAudience;
import com.sidequest.sidequest.model.Quest;
import com.sidequest.sidequest.model.QuestApplication;
import com.sidequest.sidequest.model.QuestStatus;
import com.sidequest.sidequest.model.QuestApplicationStatus;
import com.sidequest.sidequest.model.QuestNewsType;
import com.sidequest.sidequest.repository.AppUserRepository;
import com.sidequest.sidequest.repository.QuestApplicationRepository;
import com.sidequest.sidequest.repository.QuestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class QuestService {

    private final QuestRepository questRepository;
    private final AppUserRepository appUserRepository;
    private final QuestApplicationRepository questApplicationRepository;
    private final QuestNewsService questNewsService;
    private final CircleService circleService;
    private final QuestMgr questMgr;

    public QuestService(
            QuestRepository questRepository,
            AppUserRepository appUserRepository,
            QuestApplicationRepository questApplicationRepository,
            QuestNewsService questNewsService,
            CircleService circleService,
            QuestMgr questMgr
    ) {
        this.questRepository = questRepository;
        this.appUserRepository = appUserRepository;
        this.questApplicationRepository = questApplicationRepository;
        this.questNewsService = questNewsService;
        this.circleService = circleService;
        this.questMgr = questMgr;
    }

    public Quest createQuest(QuestRequestDTO dto, AppUser currentUser) {
        validateQuestCreationTermInput(dto.getScheduledAt(), dto.getTermFixed());
        validateQuestImages(dto.getImages());
        Quest quest = questMgr.toEntity(dto, resolveQuestCreator(dto, currentUser));
        if (quest.getAudience() == null) {
            quest.setAudience(QuestAudience.CIRCLES);
        }
        applyConfirmedQuestTermFields(quest, dto.getScheduledAt(), dto.getTermFixed());
        return questRepository.save(quest);
    }

    public List<Quest> getAllQuests(AppUser currentUser) {
        return questRepository.findAllWithCreator().stream()
                .filter(quest -> canViewQuest(currentUser, quest))
                .toList();
    }

    public Quest getQuestById(Long id, AppUser currentUser) {
        Quest quest = requireQuest(id);
        if (!canViewQuest(currentUser, quest)) {
            throw ServiceErrors.notFound("Quest not found with id " + id);
        }

        return quest;
    }

    @Transactional
    public void deleteQuest(Long id, AppUser currentUser) {
        Quest quest = requireQuestForOwnerActions(id, currentUser);
        notifyQuestDeleted(quest, currentUser);
        questApplicationRepository.deleteByQuestId(id);
        questRepository.deleteById(id);
    }

    @Transactional
    public Quest updateQuest(Long id, QuestRequestDTO dto, AppUser currentUser) {
        Quest quest = requireQuestForOwnerActions(id, currentUser);
        applyQuestUpdates(quest, dto, currentUser);
        return questRepository.save(quest);
    }

    @Transactional
    public Quest startQuest(Long id, AppUser currentUser) {
        Quest quest = requireQuestForExecutionActions(id, currentUser);
        requireQuestStatus(quest, QuestStatus.ASSIGNED, "Quest can only be started after an application is approved");
        quest.setStatus(QuestStatus.IN_PROGRESS);
        Quest savedQuest = questRepository.save(quest);
        notifyApprovedApplicant(savedQuest, currentUser, QuestNewsType.QUEST_STARTED, "Quest started", "The quest \"" + savedQuest.getTitle() + "\" has started.");
        return savedQuest;
    }

    @Transactional
    public Quest completeQuest(Long id, AppUser currentUser) {
        Quest quest = requireQuestForExecutionActions(id, currentUser);
        requireQuestStatus(quest, QuestStatus.IN_PROGRESS, "Quest can only be completed while it is in progress");
        quest.setStatus(QuestStatus.COMPLETED);
        Quest savedQuest = questRepository.save(quest);
        notifyApprovedApplicant(savedQuest, currentUser, QuestNewsType.QUEST_COMPLETED, "Quest completed", "The quest \"" + savedQuest.getTitle() + "\" has been completed.");
        return savedQuest;
    }

    @Transactional
    public Quest confirmQuestTermChange(Long id, AppUser currentUser) {
        Quest quest = requireQuest(id);
        validateQuestTermDecisionAuthority(quest, currentUser);
        applyConfirmedQuestTermChange(quest);
        Quest savedQuest = questRepository.save(quest);
        notifyApprovedApplicant(savedQuest, currentUser, QuestNewsType.QUEST_TERM_CONFIRMED, "Quest time confirmed", "The new time for \"" + savedQuest.getTitle() + "\" was confirmed.");
        notifyQuestCreator(savedQuest, currentUser, QuestNewsType.QUEST_TERM_CONFIRMED, "Quest time confirmed", "The new time for \"" + savedQuest.getTitle() + "\" was confirmed.");
        return savedQuest;
    }

    @Transactional
    public Quest rejectQuestTermChange(Long id, AppUser currentUser) {
        Quest quest = requireQuest(id);
        validateQuestTermDecisionAuthority(quest, currentUser);
        rejectPendingQuestTermChange(quest);
        Quest savedQuest = questRepository.save(quest);
        notifyApprovedApplicant(savedQuest, currentUser, QuestNewsType.QUEST_TERM_REJECTED, "Quest time rejected", "The proposed time change for \"" + savedQuest.getTitle() + "\" was rejected.");
        notifyQuestCreator(savedQuest, currentUser, QuestNewsType.QUEST_TERM_REJECTED, "Quest time rejected", "The proposed time change for \"" + savedQuest.getTitle() + "\" was rejected.");
        return savedQuest;
    }

    private Quest requireQuest(Long id) {
        return questRepository.findByIdWithCreator(id)
                .orElseThrow(() -> ServiceErrors.notFound("Quest not found with id " + id));
    }

    private Quest requireQuestForOwnerActions(Long id, AppUser currentUser) {
        Quest quest = requireQuest(id);
        validateQuestOwnerOrAdmin(quest, currentUser);
        return quest;
    }

    private Quest requireQuestForExecutionActions(Long id, AppUser currentUser) {
        Quest quest = requireQuest(id);
        validateQuestExecutionAuthority(quest, currentUser);
        return quest;
    }

    private void applyQuestUpdates(Quest quest, QuestRequestDTO dto, AppUser currentUser) {
        quest.setTitle(dto.getTitle());
        quest.setDescription(dto.getDescription());
        quest.setAwardAmount(dto.getAwardAmount());
        if (dto.getImages() != null) {
            validateQuestImages(dto.getImages());
            quest.setImages(new ArrayList<>(dto.getImages()));
        }
        if (dto.getAudience() != null) {
            quest.setAudience(dto.getAudience());
        }

        if (!isAdmin(currentUser)) {
            applyQuestTermUpdateForOwner(quest, dto, currentUser);
            return;
        }

        if (dto.getCreatorId() != null) {
            quest.setCreator(requireAppUser(dto.getCreatorId()));
        }

        if (dto.getStatus() != null) {
            applyAdminQuestStatusChange(quest, dto.getStatus(), currentUser);
        }

        if (dto.getScheduledAt() != null || dto.getTermFixed() != null) {
            validateTermInput(dto.getScheduledAt(), dto.getTermFixed());
            applyConfirmedQuestTermFields(quest, dto.getScheduledAt(), dto.getTermFixed());

            if (quest.getStatus() == QuestStatus.WAITING_CONFIRMATION
                    && (dto.getStatus() == null || dto.getStatus() == QuestStatus.WAITING_CONFIRMATION)) {
                restoreQuestStatusAfterTermDecision(quest);
            }

            clearPendingQuestTermChange(quest);
        }
    }

    private void applyQuestTermUpdateForOwner(Quest quest, QuestRequestDTO dto, AppUser currentUser) {
        if (dto.getScheduledAt() == null && dto.getTermFixed() == null) {
            return;
        }

        validateTermInput(dto.getScheduledAt(), dto.getTermFixed());

        if (quest.getStatus() == QuestStatus.OPEN || quest.getStatus() == QuestStatus.CANCELLED) {
            applyConfirmedQuestTermFields(quest, dto.getScheduledAt(), dto.getTermFixed());
            clearPendingQuestTermChange(quest);
            return;
        }

        if (quest.getStatus() == QuestStatus.WAITING_CONFIRMATION) {
            applyPendingQuestTermChange(quest, dto.getScheduledAt(), dto.getTermFixed());
            notifyApprovedApplicant(quest, currentUser, QuestNewsType.QUEST_TERM_CONFIRMATION_REQUESTED, "Term change updated", "The pending time for \"" + quest.getTitle() + "\" was updated.");
            return;
        }

        if (quest.getStatus() == QuestStatus.ASSIGNED || quest.getStatus() == QuestStatus.IN_PROGRESS) {
            queueQuestTermChange(quest, dto.getScheduledAt(), dto.getTermFixed());
            notifyApprovedApplicant(quest, currentUser, QuestNewsType.QUEST_TERM_CONFIRMATION_REQUESTED, "Term confirmation needed", "The owner requested a new time for \"" + quest.getTitle() + "\".");
            return;
        }

        throw ServiceErrors.badRequest("Term can only be changed on an active quest");
    }

    private AppUser requireAppUser(Long userId) {
        return appUserRepository.findById(userId)
                .orElseThrow(() -> ServiceErrors.notFound("Creator not found with id " + userId));
    }

    private void validateQuestCreationTermInput(Instant scheduledAt, Boolean termFixed) {
        if (termFixed == null) {
            throw ServiceErrors.badRequest("Term fixed flag is required");
        }

        if (Boolean.TRUE.equals(termFixed) && scheduledAt == null) {
            throw ServiceErrors.badRequest("Scheduled time is required when the term is fixed");
        }
    }

    private void validateQuestImages(List<String> images) {
        if (images == null) {
          return;
        }

        if (images.size() > 10) {
            throw ServiceErrors.badRequest("A quest can have at most 10 images");
        }

        for (String image : images) {
            if (image == null || image.isBlank()) {
                throw ServiceErrors.badRequest("Quest images must not be empty");
            }
            if (!image.startsWith("data:image/")) {
                throw ServiceErrors.badRequest("Quest images must be image data URLs");
            }
        }
    }

    private void validateTermInput(Instant scheduledAt, Boolean termFixed) {
        if (scheduledAt != null && termFixed == null) {
            throw ServiceErrors.badRequest("Term fixed flag is required when providing a scheduled time");
        }

        if (Boolean.TRUE.equals(termFixed) && scheduledAt == null) {
            throw ServiceErrors.badRequest("Scheduled time is required when the term is fixed");
        }
    }

    private void applyConfirmedQuestTermFields(Quest quest, Instant scheduledAt, Boolean termFixed) {
        quest.setScheduledAt(scheduledAt);
        quest.setTermFixed(Boolean.TRUE.equals(termFixed));
    }

    private void applyPendingQuestTermChange(Quest quest, Instant scheduledAt, Boolean termFixed) {
        quest.setPendingScheduledAt(scheduledAt);
        quest.setPendingTermFixed(termFixed);
    }

    private void applyAdminQuestStatusChange(Quest quest, QuestStatus newStatus, AppUser actor) {
        QuestStatus previousStatus = quest.getStatus();
        quest.setStatus(newStatus);

        if (newStatus != QuestStatus.WAITING_CONFIRMATION) {
            clearPendingQuestTermChange(quest);
        }

        if (previousStatus != QuestStatus.OPEN && newStatus == QuestStatus.OPEN) {
            quest.setReopenedAt(Instant.now());
            reopenQuestApplications(quest);
            notifyQuestApplicants(quest, actor, QuestNewsType.QUEST_REOPENED, "Quest reopened", "The quest \"" + quest.getTitle() + "\" was reopened.");
        }
    }

    private void queueQuestTermChange(Quest quest, Instant scheduledAt, Boolean termFixed) {
        applyPendingQuestTermChange(quest, scheduledAt, termFixed);
        quest.setTermChangePreviousStatus(quest.getStatus());
        quest.setStatus(QuestStatus.WAITING_CONFIRMATION);
    }

    private void clearPendingQuestTermChange(Quest quest) {
        quest.setPendingScheduledAt(null);
        quest.setPendingTermFixed(null);
        quest.setTermChangePreviousStatus(null);
    }

    private void reopenQuestApplications(Quest quest) {
        List<QuestApplication> reopenedApplications = new ArrayList<>();
        List<QuestApplication> applications = questApplicationRepository.findByQuestId(quest.getId());
        if (applications == null || applications.isEmpty()) {
            return;
        }

        for (QuestApplication application : applications) {
            if (application.getStatus() == QuestApplicationStatus.WITHDRAWN) {
                continue;
            }

            application.setStatus(QuestApplicationStatus.PENDING);
            reopenedApplications.add(application);
        }

        if (!reopenedApplications.isEmpty()) {
            questApplicationRepository.saveAll(reopenedApplications);
        }
    }

    private void notifyQuestDeleted(Quest quest, AppUser actor) {
        List<QuestApplication> applications = questApplicationRepository.findByQuestId(quest.getId());
        if (applications == null || applications.isEmpty()) {
            return;
        }

        for (QuestApplication application : applications) {
            if (application.getStatus() == QuestApplicationStatus.WITHDRAWN) {
                continue;
            }

            questNewsService.notifyQuestDeleted(quest, actor, application.getApplicant());
        }
    }

    private void notifyApprovedApplicant(Quest quest, AppUser actor, QuestNewsType type, String title, String message) {
        List<QuestApplication> approvedApplications = questApplicationRepository.findByQuestIdAndStatus(quest.getId(), QuestApplicationStatus.APPROVED);
        if (approvedApplications == null || approvedApplications.isEmpty()) {
            return;
        }

        approvedApplications.stream()
                .findFirst()
                .ifPresent(application -> questNewsService.notifyQuestEvent(application.getApplicant(), actor, quest, null, type, title, message));
    }

    private void notifyQuestCreator(Quest quest, AppUser actor, QuestNewsType type, String title, String message) {
        questNewsService.notifyQuestEvent(quest.getCreator(), actor, quest, null, type, title, message);
    }

    private void notifyQuestApplicants(Quest quest, AppUser actor, QuestNewsType type, String title, String message) {
        List<QuestApplication> applications = questApplicationRepository.findByQuestId(quest.getId());
        if (applications == null || applications.isEmpty()) {
            return;
        }

        for (QuestApplication application : applications) {
            if (application.getStatus() == QuestApplicationStatus.WITHDRAWN) {
                continue;
            }

            questNewsService.notifyQuestEvent(application.getApplicant(), actor, quest, null, type, title, message);
        }
    }

    private void applyConfirmedQuestTermChange(Quest quest) {
        if (quest.getStatus() != QuestStatus.WAITING_CONFIRMATION) {
            throw ServiceErrors.badRequest("Quest term change is not waiting for confirmation");
        }

        if (quest.getPendingTermFixed() == null && quest.getPendingScheduledAt() == null) {
            throw ServiceErrors.badRequest("No pending term change to confirm");
        }

        quest.setScheduledAt(quest.getPendingScheduledAt());
        quest.setTermFixed(Boolean.TRUE.equals(quest.getPendingTermFixed()));
        restoreQuestStatusAfterTermDecision(quest);
        clearPendingQuestTermChange(quest);
    }

    private void rejectPendingQuestTermChange(Quest quest) {
        if (quest.getStatus() != QuestStatus.WAITING_CONFIRMATION) {
            throw ServiceErrors.badRequest("Quest term change is not waiting for confirmation");
        }
        restoreQuestStatusAfterTermDecision(quest);
        clearPendingQuestTermChange(quest);
    }

    private void restoreQuestStatusAfterTermDecision(Quest quest) {
        if (quest.getTermChangePreviousStatus() == null) {
            throw ServiceErrors.badRequest("Missing previous quest status for term change");
        }

        quest.setStatus(quest.getTermChangePreviousStatus());
    }

    private void validateQuestTermDecisionAuthority(Quest quest, AppUser currentUser) {
        if (isAdmin(currentUser)) {
            return;
        }

        if (questApplicationRepository.findByQuestIdAndApplicantIdAndStatus(
                quest.getId(),
                currentUser.getId(),
                QuestApplicationStatus.APPROVED
        ).isEmpty()) {
            throw ServiceErrors.forbidden("You are not allowed to confirm this quest term change");
        }
    }

    private void validateQuestOwner(Quest quest, AppUser currentUser) {
        if (!quest.getCreator().getId().equals(currentUser.getId())) {
            throw ServiceErrors.forbidden("You are not allowed to modify this quest");
        }
    }

    private void validateQuestOwnerOrAdmin(Quest quest, AppUser currentUser) {
        if (isAdmin(currentUser)) {
            return;
        }

        validateQuestOwner(quest, currentUser);
    }

    private void validateQuestExecutionAuthority(Quest quest, AppUser currentUser) {
        if (isAdmin(currentUser)) {
            return;
        }

        if (quest.getCreator().getId().equals(currentUser.getId())) {
            return;
        }

        if (questApplicationRepository.findByQuestIdAndApplicantIdAndStatus(
                quest.getId(),
                currentUser.getId(),
                QuestApplicationStatus.APPROVED
        ).isPresent()) {
            return;
        }

        throw ServiceErrors.forbidden("You are not allowed to manage this quest");
    }

    private void requireQuestStatus(Quest quest, QuestStatus requiredStatus, String message) {
        if (quest.getStatus() != requiredStatus) {
            throw ServiceErrors.badRequest(message);
        }
    }

    private AppUser resolveQuestCreator(QuestRequestDTO dto, AppUser currentUser) {
        if (isAdmin(currentUser) && dto.getCreatorId() != null) {
            return requireAppUser(dto.getCreatorId());
        }

        return currentUser;
    }

    private boolean isAdmin(AppUser user) {
        return user != null && user.getRole() == AppUserRole.ADMIN;
    }

    private boolean canViewQuest(AppUser currentUser, Quest quest) {
        if (quest == null) {
            return false;
        }

        if (isAdmin(currentUser) || quest.getCreator().getId().equals(currentUser != null ? currentUser.getId() : null)) {
            return true;
        }

        if (quest.getAudience() == QuestAudience.EVERYONE) {
            return true;
        }

        return circleService.isCircleBetween(currentUser, quest.getCreator());
    }
}
