package com.sidequest.sidequest.service;

import com.sidequest.sidequest.dto.QuestRequestDTO;
import com.sidequest.sidequest.mapper.QuestMgr;
import com.sidequest.sidequest.model.AppUser;
import com.sidequest.sidequest.model.AppUserRole;
import com.sidequest.sidequest.model.QuestAudience;
import com.sidequest.sidequest.model.Quest;
import com.sidequest.sidequest.model.QuestApplication;
import com.sidequest.sidequest.model.QuestApplicationStatus;
import com.sidequest.sidequest.model.QuestStatus;
import com.sidequest.sidequest.repository.QuestApplicationRepository;
import com.sidequest.sidequest.repository.QuestRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuestServiceTest {

    @Mock
    private QuestRepository questRepository;

    @Mock
    private QuestApplicationRepository questApplicationRepository;

    @Mock
    private QuestNewsService questNewsService;

    @Mock
    private CircleService circleService;

    @Mock
    private QuestMgr questMgr;

    @InjectMocks
    private QuestService questService;

    @Test
    void createQuestUsesAuthenticatedUserAsCreator() {
        AppUser currentUser = createUser(5L, "creator");
        QuestRequestDTO requestDTO = QuestRequestDTO.builder()
                .title("Fix garden fence")
                .description("Need help with a small repair")
                .awardAmount(BigDecimal.valueOf(45))
                .scheduledAt(Instant.parse("2026-01-10T10:00:00Z"))
                .termFixed(true)
                .build();
        Quest mappedQuest = new Quest();
        Quest savedQuest = new Quest();
        savedQuest.setId(10L);

        when(questMgr.toEntity(requestDTO, currentUser)).thenReturn(mappedQuest);
        when(questRepository.save(mappedQuest)).thenReturn(savedQuest);

        Quest result = questService.createQuest(requestDTO, currentUser);

        assertEquals(10L, result.getId());
        assertEquals(Instant.parse("2026-01-10T10:00:00Z"), mappedQuest.getScheduledAt());
        assertEquals(true, mappedQuest.isTermFixed());
        assertEquals(QuestAudience.CIRCLES, mappedQuest.getAudience());
        verify(questMgr).toEntity(requestDTO, currentUser);
        verify(questRepository).save(mappedQuest);
    }

    @Test
    void getAllQuestsReturnsOnlyVisibleQuestsForNonAdminUsers() {
        AppUser currentUser = createUser(5L, "viewer");
        AppUser creator = createUser(6L, "creator");

        Quest visibleQuest = new Quest();
        visibleQuest.setId(1L);
        visibleQuest.setCreator(currentUser);
        visibleQuest.setAudience(QuestAudience.CIRCLES);

        Quest hiddenQuest = new Quest();
        hiddenQuest.setId(2L);
        hiddenQuest.setCreator(creator);
        hiddenQuest.setAudience(QuestAudience.CIRCLES);

        when(questRepository.findAllWithCreator()).thenReturn(List.of(visibleQuest, hiddenQuest));
        when(circleService.isCircleBetween(currentUser, creator)).thenReturn(false);

        List<Quest> result = questService.getAllQuests(currentUser);

        assertEquals(1, result.size());
        assertEquals(1L, result.getFirst().getId());
    }

    @Test
    void updateQuestThrowsWhenAuthenticatedUserIsNotOwner() {
        AppUser creator = createUser(1L, "creator");
        AppUser otherUser = createUser(2L, "other");
        Quest quest = new Quest();
        quest.setId(9L);
        quest.setCreator(creator);

        QuestRequestDTO requestDTO = QuestRequestDTO.builder()
                .title("Updated title")
                .description("Updated description")
                .awardAmount(BigDecimal.TEN)
                .build();

        when(questRepository.findByIdWithCreator(9L)).thenReturn(Optional.of(quest));

        assertThrows(ResponseStatusException.class, () -> questService.updateQuest(9L, requestDTO, otherUser));
    }

    @Test
    void updateQuestChangesQuestWhenAuthenticatedUserIsOwner() {
        AppUser creator = createUser(1L, "creator");
        Quest quest = new Quest();
        quest.setId(9L);
        quest.setCreator(creator);

        QuestRequestDTO requestDTO = QuestRequestDTO.builder()
                .title("Updated title")
                .description("Updated description")
                .awardAmount(BigDecimal.valueOf(80))
                .build();

        when(questRepository.findByIdWithCreator(9L)).thenReturn(Optional.of(quest));
        when(questRepository.save(quest)).thenReturn(quest);

        questService.updateQuest(9L, requestDTO, creator);

        ArgumentCaptor<Quest> questCaptor = ArgumentCaptor.forClass(Quest.class);
        verify(questRepository).save(questCaptor.capture());
        Quest savedQuest = questCaptor.getValue();

        assertEquals("Updated title", savedQuest.getTitle());
        assertEquals("Updated description", savedQuest.getDescription());
        assertEquals(BigDecimal.valueOf(80), savedQuest.getAwardAmount());
    }

    @Test
    void startQuestMovesQuestToInProgressWhenAssigned() {
        AppUser creator = createUser(1L, "creator");
        Quest quest = new Quest();
        quest.setId(10L);
        quest.setCreator(creator);
        quest.setStatus(QuestStatus.ASSIGNED);

        when(questRepository.findByIdWithCreator(10L)).thenReturn(Optional.of(quest));
        when(questRepository.save(quest)).thenReturn(quest);

        questService.startQuest(10L, creator);

        assertEquals(QuestStatus.IN_PROGRESS, quest.getStatus());
    }

    @Test
    void completeQuestMovesQuestToCompletedWhenInProgress() {
        AppUser creator = createUser(1L, "creator");
        Quest quest = new Quest();
        quest.setId(11L);
        quest.setCreator(creator);
        quest.setStatus(QuestStatus.IN_PROGRESS);

        when(questRepository.findByIdWithCreator(11L)).thenReturn(Optional.of(quest));
        when(questRepository.save(quest)).thenReturn(quest);

        questService.completeQuest(11L, creator);

        assertEquals(QuestStatus.COMPLETED, quest.getStatus());
    }

    @Test
    void startQuestAllowsApprovedApplicantToBeginWork() {
        AppUser creator = createUser(1L, "creator");
        AppUser applicant = createUser(2L, "applicant");
        Quest quest = new Quest();
        quest.setId(21L);
        quest.setCreator(creator);
        quest.setStatus(QuestStatus.ASSIGNED);

        QuestApplication approvedApplication = new QuestApplication();
        approvedApplication.setId(41L);

        when(questRepository.findByIdWithCreator(21L)).thenReturn(Optional.of(quest));
        when(questApplicationRepository.findByQuestIdAndApplicantIdAndStatus(21L, 2L, QuestApplicationStatus.APPROVED))
                .thenReturn(Optional.of(approvedApplication));
        when(questRepository.save(quest)).thenReturn(quest);

        questService.startQuest(21L, applicant);

        assertEquals(QuestStatus.IN_PROGRESS, quest.getStatus());
    }

    @Test
    void completeQuestAllowsApprovedApplicantToFinishWork() {
        AppUser creator = createUser(1L, "creator");
        AppUser applicant = createUser(2L, "applicant");
        Quest quest = new Quest();
        quest.setId(22L);
        quest.setCreator(creator);
        quest.setStatus(QuestStatus.IN_PROGRESS);

        QuestApplication approvedApplication = new QuestApplication();
        approvedApplication.setId(42L);

        when(questRepository.findByIdWithCreator(22L)).thenReturn(Optional.of(quest));
        when(questApplicationRepository.findByQuestIdAndApplicantIdAndStatus(22L, 2L, QuestApplicationStatus.APPROVED))
                .thenReturn(Optional.of(approvedApplication));
        when(questRepository.save(quest)).thenReturn(quest);

        questService.completeQuest(22L, applicant);

        assertEquals(QuestStatus.COMPLETED, quest.getStatus());
    }

    @Test
    void startQuestThrowsWhenQuestIsNotAssigned() {
        AppUser creator = createUser(1L, "creator");
        Quest quest = new Quest();
        quest.setId(12L);
        quest.setCreator(creator);
        quest.setStatus(QuestStatus.OPEN);

        when(questRepository.findByIdWithCreator(12L)).thenReturn(Optional.of(quest));

        assertThrows(ResponseStatusException.class, () -> questService.startQuest(12L, creator));
    }

    @Test
    void updateQuestQueuesTermChangeWhenQuestIsAssigned() {
        AppUser creator = createUser(1L, "creator");
        Quest quest = new Quest();
        quest.setId(15L);
        quest.setCreator(creator);
        quest.setStatus(QuestStatus.ASSIGNED);
        quest.setScheduledAt(Instant.parse("2026-01-10T10:00:00Z"));
        quest.setTermFixed(true);

        QuestRequestDTO requestDTO = QuestRequestDTO.builder()
                .title("Updated title")
                .description("Updated description")
                .awardAmount(BigDecimal.valueOf(80))
                .scheduledAt(Instant.parse("2026-01-12T11:00:00Z"))
                .termFixed(false)
                .build();

        when(questRepository.findByIdWithCreator(15L)).thenReturn(Optional.of(quest));
        when(questRepository.save(quest)).thenReturn(quest);

        questService.updateQuest(15L, requestDTO, creator);

        assertEquals(QuestStatus.WAITING_CONFIRMATION, quest.getStatus());
        assertEquals(Instant.parse("2026-01-12T11:00:00Z"), quest.getPendingScheduledAt());
        assertEquals(Boolean.FALSE, quest.getPendingTermFixed());
        assertEquals(QuestStatus.ASSIGNED, quest.getTermChangePreviousStatus());
    }

    @Test
    void updateQuestRestoresPreviousStatusWhenAdminEditsWaitingConfirmationQuest() {
        AppUser admin = createUser(1L, "admin");
        admin.setRole(AppUserRole.ADMIN);

        Quest quest = new Quest();
        quest.setId(18L);
        quest.setCreator(admin);
        quest.setStatus(QuestStatus.WAITING_CONFIRMATION);
        quest.setScheduledAt(Instant.parse("2026-01-10T10:00:00Z"));
        quest.setTermFixed(true);
        quest.setPendingScheduledAt(Instant.parse("2026-01-12T11:00:00Z"));
        quest.setPendingTermFixed(false);
        quest.setTermChangePreviousStatus(QuestStatus.ASSIGNED);

        QuestRequestDTO requestDTO = QuestRequestDTO.builder()
                .title("Updated title")
                .description("Updated description")
                .awardAmount(BigDecimal.valueOf(80))
                .scheduledAt(Instant.parse("2026-01-15T12:00:00Z"))
                .termFixed(true)
                .status(QuestStatus.WAITING_CONFIRMATION)
                .build();

        when(questRepository.findByIdWithCreator(18L)).thenReturn(Optional.of(quest));
        when(questRepository.save(quest)).thenReturn(quest);

        questService.updateQuest(18L, requestDTO, admin);

        assertEquals(QuestStatus.ASSIGNED, quest.getStatus());
        assertEquals(Instant.parse("2026-01-15T12:00:00Z"), quest.getScheduledAt());
        assertEquals(true, quest.isTermFixed());
        assertEquals(null, quest.getPendingScheduledAt());
        assertEquals(null, quest.getPendingTermFixed());
        assertEquals(null, quest.getTermChangePreviousStatus());
    }

    @Test
    void updateQuestClearsPendingTermStateWhenAdminChangesStatusAwayFromWaitingConfirmation() {
        AppUser admin = createUser(1L, "admin");
        admin.setRole(AppUserRole.ADMIN);

        Quest quest = new Quest();
        quest.setId(19L);
        quest.setCreator(admin);
        quest.setStatus(QuestStatus.WAITING_CONFIRMATION);
        quest.setScheduledAt(Instant.parse("2026-01-10T10:00:00Z"));
        quest.setTermFixed(true);
        quest.setPendingScheduledAt(Instant.parse("2026-01-12T11:00:00Z"));
        quest.setPendingTermFixed(false);
        quest.setTermChangePreviousStatus(QuestStatus.ASSIGNED);

        QuestRequestDTO requestDTO = QuestRequestDTO.builder()
                .title("Updated title")
                .description("Updated description")
                .awardAmount(BigDecimal.valueOf(80))
                .status(QuestStatus.OPEN)
                .build();

        when(questRepository.findByIdWithCreator(19L)).thenReturn(Optional.of(quest));
        when(questRepository.save(quest)).thenReturn(quest);

        questService.updateQuest(19L, requestDTO, admin);

        assertEquals(QuestStatus.OPEN, quest.getStatus());
        assertEquals(null, quest.getPendingScheduledAt());
        assertEquals(null, quest.getPendingTermFixed());
        assertEquals(null, quest.getTermChangePreviousStatus());
    }

    @Test
    void updateQuestResetsQuestApplicationsWhenAdminReopensQuest() {
        AppUser admin = createUser(1L, "admin");
        admin.setRole(AppUserRole.ADMIN);

        Quest quest = new Quest();
        quest.setId(20L);
        quest.setCreator(admin);
        quest.setStatus(QuestStatus.ASSIGNED);

        QuestApplication approvedApplication = new QuestApplication();
        approvedApplication.setId(301L);
        approvedApplication.setStatus(QuestApplicationStatus.APPROVED);

        QuestApplication declinedApplication = new QuestApplication();
        declinedApplication.setId(302L);
        declinedApplication.setStatus(QuestApplicationStatus.DECLINED);

        QuestApplication pendingApplication = new QuestApplication();
        pendingApplication.setId(303L);
        pendingApplication.setStatus(QuestApplicationStatus.PENDING);

        QuestApplication withdrawnApplication = new QuestApplication();
        withdrawnApplication.setId(304L);
        withdrawnApplication.setStatus(QuestApplicationStatus.WITHDRAWN);

        QuestRequestDTO requestDTO = QuestRequestDTO.builder()
                .title("Updated title")
                .description("Updated description")
                .awardAmount(BigDecimal.valueOf(80))
                .status(QuestStatus.OPEN)
                .build();

        when(questRepository.findByIdWithCreator(20L)).thenReturn(Optional.of(quest));
        when(questApplicationRepository.findByQuestId(20L)).thenReturn(List.of(
                approvedApplication,
                declinedApplication,
                pendingApplication,
                withdrawnApplication
        ));
        when(questRepository.save(quest)).thenReturn(quest);
        when(questApplicationRepository.saveAll(List.of(approvedApplication, declinedApplication, pendingApplication)))
                .thenReturn(List.of(approvedApplication, declinedApplication, pendingApplication));

        questService.updateQuest(20L, requestDTO, admin);

        assertEquals(QuestStatus.OPEN, quest.getStatus());
        assertNotNull(quest.getReopenedAt());
        assertEquals(QuestApplicationStatus.PENDING, approvedApplication.getStatus());
        assertEquals(QuestApplicationStatus.PENDING, declinedApplication.getStatus());
        assertEquals(QuestApplicationStatus.PENDING, pendingApplication.getStatus());
        assertEquals(QuestApplicationStatus.WITHDRAWN, withdrawnApplication.getStatus());
    }

    @Test
    void deleteQuestDeletesQuestAndApplicationsWhenAuthenticatedUserIsOwner() {
        AppUser creator = createUser(1L, "creator");
        Quest quest = new Quest();
        quest.setId(14L);
        quest.setCreator(creator);

        when(questRepository.findByIdWithCreator(14L)).thenReturn(Optional.of(quest));

        questService.deleteQuest(14L, creator);

        verify(questApplicationRepository).deleteByQuestId(14L);
        verify(questRepository).deleteById(14L);
    }

    @Test
    void confirmQuestTermChangeAppliesPendingTermForApprovedApplicant() {
        AppUser creator = createUser(1L, "creator");
        AppUser applicant = createUser(2L, "applicant");
        Quest quest = new Quest();
        quest.setId(16L);
        quest.setCreator(creator);
        quest.setStatus(QuestStatus.WAITING_CONFIRMATION);
        quest.setScheduledAt(Instant.parse("2026-01-10T10:00:00Z"));
        quest.setTermFixed(true);
        quest.setPendingScheduledAt(Instant.parse("2026-01-12T11:00:00Z"));
        quest.setPendingTermFixed(false);
        quest.setTermChangePreviousStatus(QuestStatus.ASSIGNED);

        com.sidequest.sidequest.model.QuestApplication approvedApplication = new com.sidequest.sidequest.model.QuestApplication();
        approvedApplication.setId(22L);

        when(questRepository.findByIdWithCreator(16L)).thenReturn(Optional.of(quest));
        when(questApplicationRepository.findByQuestIdAndApplicantIdAndStatus(16L, 2L, com.sidequest.sidequest.model.QuestApplicationStatus.APPROVED))
                .thenReturn(Optional.of(approvedApplication));
        when(questRepository.save(quest)).thenReturn(quest);

        questService.confirmQuestTermChange(16L, applicant);

        assertEquals(Instant.parse("2026-01-12T11:00:00Z"), quest.getScheduledAt());
        assertEquals(false, quest.isTermFixed());
        assertEquals(QuestStatus.ASSIGNED, quest.getStatus());
        assertEquals(null, quest.getPendingScheduledAt());
        assertEquals(null, quest.getPendingTermFixed());
    }

    @Test
    void rejectQuestTermChangeRestoresPreviousStatusForApprovedApplicant() {
        AppUser creator = createUser(1L, "creator");
        AppUser applicant = createUser(2L, "applicant");
        Quest quest = new Quest();
        quest.setId(17L);
        quest.setCreator(creator);
        quest.setStatus(QuestStatus.WAITING_CONFIRMATION);
        quest.setScheduledAt(Instant.parse("2026-01-10T10:00:00Z"));
        quest.setTermFixed(true);
        quest.setPendingScheduledAt(Instant.parse("2026-01-12T11:00:00Z"));
        quest.setPendingTermFixed(false);
        quest.setTermChangePreviousStatus(QuestStatus.IN_PROGRESS);

        com.sidequest.sidequest.model.QuestApplication approvedApplication = new com.sidequest.sidequest.model.QuestApplication();
        approvedApplication.setId(23L);

        when(questRepository.findByIdWithCreator(17L)).thenReturn(Optional.of(quest));
        when(questApplicationRepository.findByQuestIdAndApplicantIdAndStatus(17L, 2L, com.sidequest.sidequest.model.QuestApplicationStatus.APPROVED))
                .thenReturn(Optional.of(approvedApplication));
        when(questRepository.save(quest)).thenReturn(quest);

        questService.rejectQuestTermChange(17L, applicant);

        assertEquals(Instant.parse("2026-01-10T10:00:00Z"), quest.getScheduledAt());
        assertEquals(true, quest.isTermFixed());
        assertEquals(QuestStatus.IN_PROGRESS, quest.getStatus());
        assertEquals(null, quest.getPendingScheduledAt());
        assertEquals(null, quest.getPendingTermFixed());
    }

    private AppUser createUser(Long id, String username) {
        AppUser appUser = new AppUser();
        appUser.setId(id);
        appUser.setUsername(username);
        appUser.setEmail(username + "@example.com");
        return appUser;
    }
}
