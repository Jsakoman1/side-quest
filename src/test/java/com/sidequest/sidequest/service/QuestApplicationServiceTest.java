package com.sidequest.sidequest.service;

import com.sidequest.sidequest.dto.QuestApplicationRequestDTO;
import com.sidequest.sidequest.dto.QuestApplicationResponseDTO;
import com.sidequest.sidequest.mapper.QuestApplicationMgr;
import com.sidequest.sidequest.model.AppUser;
import com.sidequest.sidequest.model.Quest;
import com.sidequest.sidequest.model.QuestApplication;
import com.sidequest.sidequest.model.QuestApplicationStatus;
import com.sidequest.sidequest.model.QuestStatus;
import com.sidequest.sidequest.repository.QuestApplicationRepository;
import com.sidequest.sidequest.repository.QuestRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuestApplicationServiceTest {

    @Mock
    private QuestApplicationRepository questApplicationRepository;

    @Mock
    private QuestRepository questRepository;

    @Mock
    private QuestApplicationMgr questApplicationMgr;

    @InjectMocks
    private QuestApplicationService questApplicationService;

    @Test
    void applyForQuestUsesAuthenticatedUserAsApplicant() {
        AppUser creator = createUser(1L, "creator");
        AppUser applicant = createUser(2L, "applicant");
        Quest quest = createQuest(7L, creator, QuestStatus.OPEN);
        QuestApplicationRequestDTO requestDTO = QuestApplicationRequestDTO.builder()
                .message("I can help")
                .proposedPrice(BigDecimal.valueOf(25))
                .build();
        QuestApplication application = new QuestApplication();
        QuestApplication savedApplication = new QuestApplication();
        QuestApplicationResponseDTO responseDTO = QuestApplicationResponseDTO.builder()
                .id(100L)
                .applicantId(applicant.getId())
                .build();

        when(questRepository.findById(7L)).thenReturn(Optional.of(quest));
        when(questApplicationRepository.existsByQuestIdAndApplicantId(7L, 2L)).thenReturn(false);
        when(questApplicationMgr.toEntity(requestDTO, quest, applicant)).thenReturn(application);
        when(questApplicationRepository.save(application)).thenReturn(savedApplication);
        when(questApplicationMgr.toDto(savedApplication)).thenReturn(responseDTO);

        QuestApplicationResponseDTO result = questApplicationService.applyForQuest(7L, requestDTO, applicant);

        assertEquals(100L, result.getId());
        verify(questApplicationMgr).toEntity(requestDTO, quest, applicant);
    }

    @Test
    void applyForQuestThrowsWhenApplicantIsQuestCreator() {
        AppUser creator = createUser(1L, "creator");
        Quest quest = createQuest(7L, creator, QuestStatus.OPEN);
        QuestApplicationRequestDTO requestDTO = QuestApplicationRequestDTO.builder()
                .message("I can help")
                .build();

        when(questRepository.findById(7L)).thenReturn(Optional.of(quest));

        assertThrows(ResponseStatusException.class, () -> questApplicationService.applyForQuest(7L, requestDTO, creator));
    }

    @Test
    void applyForQuestThrowsWhenQuestIsNotOpen() {
        AppUser creator = createUser(1L, "creator");
        AppUser applicant = createUser(2L, "applicant");
        Quest quest = createQuest(7L, creator, QuestStatus.COMPLETED);
        QuestApplicationRequestDTO requestDTO = QuestApplicationRequestDTO.builder()
                .message("I can help")
                .build();

        when(questRepository.findById(7L)).thenReturn(Optional.of(quest));

        assertThrows(ResponseStatusException.class, () -> questApplicationService.applyForQuest(7L, requestDTO, applicant));
    }

    @Test
    void getApplicationsForQuestThrowsWhenAuthenticatedUserIsNotOwner() {
        AppUser creator = createUser(1L, "creator");
        AppUser otherUser = createUser(3L, "other");
        Quest quest = createQuest(7L, creator, QuestStatus.OPEN);

        when(questRepository.findById(7L)).thenReturn(Optional.of(quest));

        assertThrows(ResponseStatusException.class, () -> questApplicationService.getApplicationsForQuest(7L, otherUser));
    }

    @Test
    void getApplicationsForQuestReturnsApplicationsForOwner() {
        AppUser creator = createUser(1L, "creator");
        Quest quest = createQuest(7L, creator, QuestStatus.OPEN);
        QuestApplication application = new QuestApplication();
        QuestApplicationResponseDTO responseDTO = QuestApplicationResponseDTO.builder()
                .id(22L)
                .build();

        when(questRepository.findById(7L)).thenReturn(Optional.of(quest));
        when(questApplicationRepository.findByQuestId(7L)).thenReturn(List.of(application));
        when(questApplicationMgr.toDto(application)).thenReturn(responseDTO);

        List<QuestApplicationResponseDTO> result = questApplicationService.getApplicationsForQuest(7L, creator);

        assertEquals(1, result.size());
        assertEquals(22L, result.getFirst().getId());
    }

    @Test
    void approveApplicationSetsApplicationAndQuestStatus() {
        AppUser creator = createUser(1L, "creator");
        AppUser applicant = createUser(2L, "applicant");
        AppUser otherApplicant = createUser(3L, "other");
        Quest quest = createQuest(7L, creator, QuestStatus.OPEN);
        QuestApplication approvedApplication = createApplication(11L, quest, applicant, QuestApplicationStatus.PENDING);
        QuestApplication otherApplication = createApplication(12L, quest, otherApplicant, QuestApplicationStatus.PENDING);
        QuestApplicationResponseDTO responseDTO = QuestApplicationResponseDTO.builder()
                .id(11L)
                .status(QuestApplicationStatus.APPROVED)
                .build();

        when(questRepository.findById(7L)).thenReturn(Optional.of(quest));
        when(questApplicationRepository.findByIdAndQuestId(11L, 7L)).thenReturn(Optional.of(approvedApplication));
        when(questApplicationRepository.findByQuestIdAndStatus(7L, QuestApplicationStatus.PENDING)).thenReturn(List.of(approvedApplication, otherApplication));
        when(questApplicationRepository.save(approvedApplication)).thenReturn(approvedApplication);
        when(questApplicationMgr.toDto(approvedApplication)).thenReturn(responseDTO);

        QuestApplicationResponseDTO result = questApplicationService.approveApplication(7L, 11L, creator);

        assertEquals(QuestApplicationStatus.APPROVED, result.getStatus());
        assertEquals(QuestStatus.ASSIGNED, quest.getStatus());
        assertEquals(QuestApplicationStatus.APPROVED, approvedApplication.getStatus());
        assertEquals(QuestApplicationStatus.DECLINED, otherApplication.getStatus());
    }

    @Test
    void declineApplicationSetsStatusToDeclined() {
        AppUser creator = createUser(1L, "creator");
        AppUser applicant = createUser(2L, "applicant");
        Quest quest = createQuest(7L, creator, QuestStatus.OPEN);
        QuestApplication application = createApplication(11L, quest, applicant, QuestApplicationStatus.PENDING);
        QuestApplicationResponseDTO responseDTO = QuestApplicationResponseDTO.builder()
                .id(11L)
                .status(QuestApplicationStatus.DECLINED)
                .build();

        when(questRepository.findById(7L)).thenReturn(Optional.of(quest));
        when(questApplicationRepository.findByIdAndQuestId(11L, 7L)).thenReturn(Optional.of(application));
        when(questApplicationRepository.save(application)).thenReturn(application);
        when(questApplicationMgr.toDto(application)).thenReturn(responseDTO);

        QuestApplicationResponseDTO result = questApplicationService.declineApplication(7L, 11L, creator);

        assertEquals(QuestApplicationStatus.DECLINED, result.getStatus());
        assertEquals(QuestApplicationStatus.DECLINED, application.getStatus());
    }

    @Test
    void withdrawMyApplicationSetsStatusToWithdrawn() {
        AppUser creator = createUser(1L, "creator");
        AppUser applicant = createUser(2L, "applicant");
        Quest quest = createQuest(7L, creator, QuestStatus.OPEN);
        QuestApplication application = createApplication(11L, quest, applicant, QuestApplicationStatus.PENDING);
        QuestApplicationResponseDTO responseDTO = QuestApplicationResponseDTO.builder()
                .id(11L)
                .status(QuestApplicationStatus.WITHDRAWN)
                .build();

        when(questApplicationRepository.findByQuestIdAndApplicantId(7L, 2L)).thenReturn(Optional.of(application));
        when(questApplicationRepository.save(application)).thenReturn(application);
        when(questApplicationMgr.toDto(application)).thenReturn(responseDTO);

        QuestApplicationResponseDTO result = questApplicationService.withdrawMyApplication(7L, applicant);

        assertEquals(QuestApplicationStatus.WITHDRAWN, result.getStatus());
        assertEquals(QuestApplicationStatus.WITHDRAWN, application.getStatus());
    }

    @Test
    void withdrawMyApplicationThrowsWhenApplicationIsNotPending() {
        AppUser creator = createUser(1L, "creator");
        AppUser applicant = createUser(2L, "applicant");
        Quest quest = createQuest(7L, creator, QuestStatus.OPEN);
        QuestApplication application = createApplication(11L, quest, applicant, QuestApplicationStatus.APPROVED);

        when(questApplicationRepository.findByQuestIdAndApplicantId(7L, 2L)).thenReturn(Optional.of(application));

        assertThrows(ResponseStatusException.class, () -> questApplicationService.withdrawMyApplication(7L, applicant));
    }

    private AppUser createUser(Long id, String username) {
        AppUser appUser = new AppUser();
        appUser.setId(id);
        appUser.setUsername(username);
        appUser.setEmail(username + "@example.com");
        return appUser;
    }

    private Quest createQuest(Long id, AppUser creator, QuestStatus status) {
        Quest quest = new Quest();
        quest.setId(id);
        quest.setCreator(creator);
        quest.setStatus(status);
        quest.setTitle("Quest title");
        return quest;
    }

    private QuestApplication createApplication(Long id, Quest quest, AppUser applicant, QuestApplicationStatus status) {
        QuestApplication application = new QuestApplication();
        application.setId(id);
        application.setQuest(quest);
        application.setApplicant(applicant);
        application.setStatus(status);
        application.setMessage("Message");
        return application;
    }
}
