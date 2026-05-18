package com.sidequest.sidequest.service;

import com.sidequest.sidequest.dto.QuestRequestDTO;
import com.sidequest.sidequest.mapper.QuestMgr;
import com.sidequest.sidequest.model.AppUser;
import com.sidequest.sidequest.model.Quest;
import com.sidequest.sidequest.repository.AppUserRepository;
import com.sidequest.sidequest.repository.QuestRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class QuestService {

    private final QuestRepository questRepository;
    private final AppUserRepository appUserRepository;
    private final QuestMgr questMgr;

    public QuestService(QuestRepository questRepository, AppUserRepository appUserRepository, QuestMgr questMgr) {
        this.questRepository = questRepository;
        this.appUserRepository = appUserRepository;
        this.questMgr = questMgr;
    }

    public Quest createQuest(QuestRequestDTO dto) {
        AppUser creator = appUserRepository.findById(dto.getCreatorId())
                .orElseThrow(() -> new RuntimeException(String.format("AppUser not found with id %s", dto.getCreatorId())));
        Quest quest = questMgr.toEntity(dto, creator);
        return questRepository.save(quest);
    }

    public List<Quest> getAllQuests() {
        return questRepository.findAll();
    }

    public void deleteQuest(Long id) {
        questRepository.deleteById(id);
    }

    public Quest updateQuest(Long id, QuestRequestDTO dto) {
        Quest quest = questRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(String.format("Quest not found with id %s", id)));
        AppUser creator = appUserRepository.findById(dto.getCreatorId())
                .orElseThrow(() -> new RuntimeException(String.format("AppUser not found with id %s", dto.getCreatorId())));
        quest.setCreator(creator);
        quest.setTitle(dto.getTitle());
        quest.setDescription(dto.getDescription());
        quest.setAwardAmount(dto.getAwardAmount());
        return questRepository.save(quest);
    }
}