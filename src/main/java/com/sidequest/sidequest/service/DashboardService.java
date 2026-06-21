package com.sidequest.sidequest.service;

import com.sidequest.sidequest.dto.DashboardSummaryDTO;
import com.sidequest.sidequest.model.AppUser;
import com.sidequest.sidequest.model.AppUserRole;
import com.sidequest.sidequest.model.Quest;
import com.sidequest.sidequest.model.QuestApplication;
import com.sidequest.sidequest.model.QuestApplicationStatus;
import com.sidequest.sidequest.model.QuestStatus;
import com.sidequest.sidequest.repository.AppUserRepository;
import com.sidequest.sidequest.repository.QuestApplicationRepository;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.function.Predicate;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final QuestService questService;
    private final QuestApplicationRepository questApplicationRepository;
    private final QuestNewsService questNewsService;
    private final AppUserRepository appUserRepository;

    public DashboardSummaryDTO getMySummary(AppUser currentUser) {
        if (currentUser == null) {
            return DashboardSummaryDTO.builder().build();
        }

        List<Quest> quests = questService.getAllQuests(currentUser);
        List<QuestApplication> applications = questApplicationRepository.findByApplicantId(currentUser.getId());

        long questCount = quests.size();
        long visibleMyQuestsCount = countMyQuestsByStatus(quests, currentUser.getId(), QuestStatus::isVisibleOwnerWork);
        long activeMyQuestsCount = countActiveMyQuests(quests, currentUser.getId());
        long completedMyQuestsCount = countMyQuestsByStatus(quests, currentUser.getId(), status -> status == QuestStatus.COMPLETED);
        long openQuestCount = countQuestsByStatus(quests, QuestStatus.OPEN);
        long assignedQuestCount = countQuestsByStatus(quests, QuestStatus.ASSIGNED);
        long waitingConfirmationQuestCount = countQuestsByStatus(quests, QuestStatus.WAITING_CONFIRMATION);
        long pendingWorkApplicationsCount = countApplicationsByStatus(applications, QuestApplicationStatus.PENDING);
        long activeWorkApplicationsCount = countActiveWorkApplications(applications);
        long activeWorkCount = activeMyQuestsCount + activeWorkApplicationsCount;

        return DashboardSummaryDTO.builder()
                .questCount(questCount)
                .visibleMyQuestsCount(visibleMyQuestsCount)
                .pendingWorkApplicationsCount(pendingWorkApplicationsCount)
                .activeWorkApplicationsCount(activeWorkApplicationsCount)
                .activeMyQuestsCount(activeMyQuestsCount)
                .activeWorkCount(activeWorkCount)
                .completedMyQuestsCount(completedMyQuestsCount)
                .openQuestCount(openQuestCount)
                .assignedQuestCount(assignedQuestCount)
                .waitingConfirmationQuestCount(waitingConfirmationQuestCount)
                .unreadNewsCount(questNewsService.getUnreadCount(currentUser))
                .totalUserCount(appUserRepository.count())
                .adminUserCount(appUserRepository.countByRole(AppUserRole.ADMIN))
                .build();
    }

    private long countMyQuestsByStatus(
            List<Quest> quests,
            Long currentUserId,
            Predicate<QuestStatus> statusPredicate
    ) {
        return quests.stream()
                .filter(quest -> quest.getCreator() != null && quest.getCreator().getId().equals(currentUserId))
                .filter(quest -> statusPredicate.test(quest.getStatus()))
                .count();
    }

    private long countActiveMyQuests(List<Quest> quests, Long currentUserId) {
        return quests.stream()
                .filter(quest -> quest.getCreator() != null && quest.getCreator().getId().equals(currentUserId))
                .filter(quest -> quest.getStatus().isActiveForOwner())
                .count();
    }

    private long countQuestsByStatus(List<Quest> quests, QuestStatus status) {
        return quests.stream()
                .filter(quest -> quest.getStatus() == status)
                .count();
    }

    private long countApplicationsByStatus(List<QuestApplication> applications, QuestApplicationStatus status) {
        return applications.stream()
                .filter(application -> application.getStatus() == status)
                .count();
    }

    private long countActiveWorkApplications(List<QuestApplication> applications) {
        return applications.stream()
                .filter(application -> application.getStatus() == QuestApplicationStatus.APPROVED)
                .filter(application -> {
                    QuestStatus questStatus = application.getQuest() == null ? null : application.getQuest().getStatus();
                    return questStatus != null && questStatus.isActiveForWorker();
                })
                .count();
    }
}
