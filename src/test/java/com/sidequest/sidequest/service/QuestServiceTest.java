package com.sidequest.sidequest.service;

import com.sidequest.sidequest.dto.QuestRequestDTO;
import com.sidequest.sidequest.mapper.QuestMgr;
import com.sidequest.sidequest.model.AppUser;
import com.sidequest.sidequest.model.Quest;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
                .build();
        Quest mappedQuest = new Quest();
        Quest savedQuest = new Quest();
        savedQuest.setId(10L);

        when(questMgr.toEntity(requestDTO, currentUser)).thenReturn(mappedQuest);
        when(questRepository.save(mappedQuest)).thenReturn(savedQuest);

        Quest result = questService.createQuest(requestDTO, currentUser);

        assertEquals(10L, result.getId());
        verify(questMgr).toEntity(requestDTO, currentUser);
        verify(questRepository).save(mappedQuest);
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

    private AppUser createUser(Long id, String username) {
        AppUser appUser = new AppUser();
        appUser.setId(id);
        appUser.setUsername(username);
        appUser.setEmail(username + "@example.com");
        return appUser;
    }
}
