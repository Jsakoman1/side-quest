package com.sidequest.sidequest.service;

import com.sidequest.sidequest.dto.QuestRequestDTO;
import com.sidequest.sidequest.mapper.QuestMgr;
import com.sidequest.sidequest.model.AppUser;
import com.sidequest.sidequest.model.Quest;
import com.sidequest.sidequest.model.QuestApplication;
import com.sidequest.sidequest.model.QuestApplicationStatus;
import com.sidequest.sidequest.model.QuestStatus;
import com.sidequest.sidequest.repository.QuestApplicationRepository;
import com.sidequest.sidequest.repository.QuestRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class QuestService {

    private final QuestRepository questRepository;
    private final QuestApplicationRepository questApplicationRepository;
    private final QuestMgr questMgr;

    public QuestService(QuestRepository questRepository, QuestApplicationRepository questApplicationRepository, QuestMgr questMgr) {
        this.questRepository = questRepository;
        this.questApplicationRepository = questApplicationRepository;
        this.questMgr = questMgr;
    }

    public Quest createQuest(QuestRequestDTO dto, AppUser currentUser) {
        Quest quest = questMgr.toEntity(dto, currentUser);
        return questRepository.save(quest);
    }

    public Quest getQuest(Long id) {
        return findQuestOrThrow(id);
    }

    public List<Quest> getAllQuests() {
        return questRepository.findAll();
    }

    public void deleteQuest(Long id, AppUser currentUser) {
        Quest quest = findQuestOrThrow(id);
        validateQuestOwner(quest, currentUser);
        questRepository.deleteById(id);
    }

    public Quest updateQuest(Long id, QuestRequestDTO dto, AppUser currentUser) {
        Quest quest = findQuestOrThrow(id);
        validateQuestOwner(quest, currentUser);
        quest.setTitle(dto.getTitle());
        quest.setDescription(dto.getDescription());
        quest.setAwardAmount(dto.getAwardAmount());
        return questRepository.save(quest);
    }

    public Quest startQuest(Long id, AppUser currentUser) {
        Quest quest = findQuestOrThrow(id);
        validateQuestOwner(quest, currentUser);
        validateQuestStatus(quest, QuestStatus.ASSIGNED, "Quest can only be started after it has been assigned");

        quest.setStatus(QuestStatus.IN_PROGRESS);
        return questRepository.save(quest);
    }

    public Quest completeQuest(Long id, AppUser currentUser) {
        Quest quest = findQuestOrThrow(id);
        validateQuestOwner(quest, currentUser);
        validateQuestStatus(quest, QuestStatus.IN_PROGRESS, "Quest can only be completed after it is in progress");

        quest.setStatus(QuestStatus.COMPLETED);
        return questRepository.save(quest);
    }

    @Transactional
    public Quest cancelQuest(Long id, AppUser currentUser) {
        Quest quest = findQuestOrThrow(id);
        validateQuestOwner(quest, currentUser);
        validateQuestNotTerminal(quest);

        quest.setStatus(QuestStatus.CANCELLED);
        rejectPendingApplications(quest);
        return questRepository.save(quest);
    }

    private Quest findQuestOrThrow(Long id) {
        return questRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quest not found with id " + id));
    }

    private void validateQuestOwner(Quest quest, AppUser currentUser) {
        if (!quest.getCreator().getId().equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not allowed to modify this quest");
        }
    }

    private void validateQuestStatus(Quest quest, QuestStatus expectedStatus, String message) {
        if (quest.getStatus() != expectedStatus) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
    }

    private void validateQuestNotTerminal(Quest quest) {
        if (quest.getStatus() == QuestStatus.COMPLETED || quest.getStatus() == QuestStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quest can no longer be cancelled");
        }
    }

    private void rejectPendingApplications(Quest quest) {
        List<QuestApplication> pendingApplications = questApplicationRepository.findByQuestIdAndStatus(quest.getId(), QuestApplicationStatus.PENDING);
        for (QuestApplication application : pendingApplications) {
            application.setStatus(QuestApplicationStatus.REJECTED);
            questApplicationRepository.save(application);
        }
    }
}
