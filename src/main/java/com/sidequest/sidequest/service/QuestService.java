package com.sidequest.sidequest.service;

import com.sidequest.sidequest.dto.QuestRequestDTO;
import com.sidequest.sidequest.mapper.QuestMgr;
import com.sidequest.sidequest.model.AppUser;
import com.sidequest.sidequest.model.AppUserRole;
import com.sidequest.sidequest.model.Quest;
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
        return questRepository.save(questMgr.toEntity(dto, resolveQuestCreator(dto, currentUser)));
    }

    public List<Quest> getAllQuests() {
        return questRepository.findAllWithCreator();
    }

    public Quest getQuestById(Long id) {
        return requireQuest(id);
    }

    @Transactional
    public void deleteQuest(Long id, AppUser currentUser) {
        Quest quest = requireQuestForOwnerActions(id, currentUser);
        questApplicationRepository.deleteByQuestId(id);
        questRepository.deleteById(id);
    }

    public Quest updateQuest(Long id, QuestRequestDTO dto, AppUser currentUser) {
        Quest quest = requireQuestForOwnerActions(id, currentUser);
        applyQuestUpdates(quest, dto, currentUser);
        return questRepository.save(quest);
    }

    @Transactional
    public Quest startQuest(Long id, AppUser currentUser) {
        Quest quest = requireQuestForOwnerActions(id, currentUser);
        requireQuestStatus(quest, QuestStatus.ASSIGNED, "Quest can only be started after an application is approved");
        quest.setStatus(QuestStatus.IN_PROGRESS);
        return questRepository.save(quest);
    }

    @Transactional
    public Quest completeQuest(Long id, AppUser currentUser) {
        Quest quest = requireQuestForOwnerActions(id, currentUser);
        requireQuestStatus(quest, QuestStatus.IN_PROGRESS, "Quest can only be completed while it is in progress");
        quest.setStatus(QuestStatus.COMPLETED);
        return questRepository.save(quest);
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

    private void applyQuestUpdates(Quest quest, QuestRequestDTO dto, AppUser currentUser) {
        quest.setTitle(dto.getTitle());
        quest.setDescription(dto.getDescription());
        quest.setAwardAmount(dto.getAwardAmount());

        if (!isAdmin(currentUser)) {
            return;
        }

        if (dto.getCreatorId() != null) {
            quest.setCreator(requireAppUser(dto.getCreatorId()));
        }

        if (dto.getStatus() != null) {
            quest.setStatus(dto.getStatus());
        }
    }

    private AppUser requireAppUser(Long userId) {
        return appUserRepository.findById(userId)
                .orElseThrow(() -> ServiceErrors.notFound("Creator not found with id " + userId));
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
}
