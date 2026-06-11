package com.sidequest.sidequest.service;

import com.sidequest.sidequest.dto.QuestRequestDTO;
import com.sidequest.sidequest.mapper.QuestMgr;
import com.sidequest.sidequest.model.AppUser;
import com.sidequest.sidequest.model.AppUserRole;
import com.sidequest.sidequest.model.Quest;
import com.sidequest.sidequest.model.QuestApplication;
import com.sidequest.sidequest.model.QuestApplicationStatus;
import com.sidequest.sidequest.model.QuestStatus;
import com.sidequest.sidequest.repository.AppUserRepository;
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
    private final AppUserRepository appUserRepository;
    private final QuestApplicationRepository questApplicationRepository;
    private final QuestMgr questMgr;

    public QuestService(
            QuestRepository questRepository,
            AppUserRepository appUserRepository,
            QuestApplicationRepository questApplicationRepository,
            QuestMgr questMgr
    ) {
        this.questRepository = questRepository;
        this.appUserRepository = appUserRepository;
        this.questApplicationRepository = questApplicationRepository;
        this.questMgr = questMgr;
    }

    public Quest createQuest(QuestRequestDTO dto, AppUser currentUser) {
        AppUser creator = resolveQuestCreator(dto, currentUser);
        Quest quest = questMgr.toEntity(dto, creator);
        return questRepository.save(quest);
    }

    public List<Quest> getAllQuests() {
        return questRepository.findAll();
    }

    public Quest getQuestById(Long id) {
        return findQuestOrThrow(id);
    }

    public void deleteQuest(Long id, AppUser currentUser) {
        Quest quest = findQuestOrThrow(id);
        validateQuestOwnerOrAdmin(quest, currentUser);
        questRepository.deleteById(id);
    }

    public Quest updateQuest(Long id, QuestRequestDTO dto, AppUser currentUser) {
        Quest quest = findQuestOrThrow(id);
        validateQuestOwnerOrAdmin(quest, currentUser);
        quest.setTitle(dto.getTitle());
        quest.setDescription(dto.getDescription());
        quest.setAwardAmount(dto.getAwardAmount());
        if (isAdmin(currentUser) && dto.getCreatorId() != null) {
            AppUser creator = appUserRepository.findById(dto.getCreatorId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Creator not found with id " + dto.getCreatorId()));
            quest.setCreator(creator);
        }
        if (isAdmin(currentUser) && dto.getStatus() != null) {
            quest.setStatus(dto.getStatus());
        }
        return questRepository.save(quest);
    }

    @Transactional
    public Quest startQuest(Long id, AppUser currentUser) {
        Quest quest = findQuestOrThrow(id);
        validateQuestOwnerOrAdmin(quest, currentUser);
        validateQuestStatus(quest, QuestStatus.ASSIGNED, "Quest can only be started after an application is accepted");
        quest.setStatus(QuestStatus.IN_PROGRESS);
        return questRepository.save(quest);
    }

    @Transactional
    public Quest completeQuest(Long id, AppUser currentUser) {
        Quest quest = findQuestOrThrow(id);
        validateQuestOwnerOrAdmin(quest, currentUser);
        validateQuestStatus(quest, QuestStatus.IN_PROGRESS, "Quest can only be completed while it is in progress");
        quest.setStatus(QuestStatus.COMPLETED);
        return questRepository.save(quest);
    }

    @Transactional
    public Quest cancelQuest(Long id, AppUser currentUser) {
        Quest quest = findQuestOrThrow(id);
        validateQuestOwnerOrAdmin(quest, currentUser);
        if (quest.getStatus() == QuestStatus.COMPLETED || quest.getStatus() == QuestStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quest is already finished");
        }

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

    private void validateQuestOwnerOrAdmin(Quest quest, AppUser currentUser) {
        if (isAdmin(currentUser)) {
            return;
        }

        validateQuestOwner(quest, currentUser);
    }

    private void validateQuestStatus(Quest quest, QuestStatus requiredStatus, String message) {
        if (quest.getStatus() != requiredStatus) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
    }

    private void rejectPendingApplications(Quest quest) {
        List<QuestApplication> pendingApplications = questApplicationRepository.findByQuestIdAndStatus(
                quest.getId(),
                QuestApplicationStatus.PENDING
        );

        for (QuestApplication application : pendingApplications) {
            application.setStatus(QuestApplicationStatus.REJECTED);
            questApplicationRepository.save(application);
        }
    }

    private AppUser resolveQuestCreator(QuestRequestDTO dto, AppUser currentUser) {
        if (isAdmin(currentUser) && dto.getCreatorId() != null) {
            return appUserRepository.findById(dto.getCreatorId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Creator not found with id " + dto.getCreatorId()));
        }

        return currentUser;
    }

    private boolean isAdmin(AppUser user) {
        return user != null && user.getRole() == AppUserRole.ADMIN;
    }
}
