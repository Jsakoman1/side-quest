package com.sidequest.sidequest.service;

import com.sidequest.sidequest.dto.QuestRequestDTO;
import com.sidequest.sidequest.mapper.QuestMgr;
import com.sidequest.sidequest.model.AppUser;
import com.sidequest.sidequest.model.Quest;
import com.sidequest.sidequest.repository.QuestRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class QuestService {

    private final QuestRepository questRepository;
    private final QuestMgr questMgr;

    public QuestService(QuestRepository questRepository, QuestMgr questMgr) {
        this.questRepository = questRepository;
        this.questMgr = questMgr;
    }

    public Quest createQuest(QuestRequestDTO dto, AppUser currentUser) {
        Quest quest = questMgr.toEntity(dto, currentUser);
        return questRepository.save(quest);
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

    private Quest findQuestOrThrow(Long id) {
        return questRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quest not found with id " + id));
    }

    private void validateQuestOwner(Quest quest, AppUser currentUser) {
        if (!quest.getCreator().getId().equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not allowed to modify this quest");
        }
    }
}