package com.sidequest.sidequest.controller;

import com.sidequest.sidequest.dto.QuestRequestDTO;
import com.sidequest.sidequest.dto.QuestResponseDTO;
import com.sidequest.sidequest.mapper.QuestMgr;
import com.sidequest.sidequest.model.AppUser;
import com.sidequest.sidequest.model.Quest;
import com.sidequest.sidequest.service.QuestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/quests")
@RequiredArgsConstructor
public class QuestController {

    private final QuestService questService;
    private final QuestMgr questMgr;

    @PostMapping
    public QuestResponseDTO createQuest(@Valid @RequestBody QuestRequestDTO dto, @AuthenticationPrincipal AppUser currentUser) {
        Quest saved = questService.createQuest(dto, currentUser);
        return questMgr.toDto(saved);
    }

    @GetMapping
    public List<QuestResponseDTO> getAllQuests() {
        return questService.getAllQuests()
                .stream()
                .map(questMgr::toDto)
                .toList();
    }

    @GetMapping("/{id}")
    public QuestResponseDTO getQuestById(@PathVariable long id) {
        Quest quest = questService.getQuestById(id);
        return questMgr.toDto(quest);
    }

    @DeleteMapping("/{id}")
    public void deleteQuest(@PathVariable long id, @AuthenticationPrincipal AppUser currentUser) {
        questService.deleteQuest(id, currentUser);
    }

    @PutMapping("/{id}")
    public QuestResponseDTO updateQuest(@PathVariable long id, @Valid @RequestBody QuestRequestDTO dto, @AuthenticationPrincipal AppUser currentUser) {
        Quest quest = questService.updateQuest(id, dto, currentUser);
        return questMgr.toDto(quest);
    }

    @PatchMapping("/{id}/start")
    public QuestResponseDTO startQuest(@PathVariable long id, @AuthenticationPrincipal AppUser currentUser) {
        Quest quest = questService.startQuest(id, currentUser);
        return questMgr.toDto(quest);
    }

    @PatchMapping("/{id}/complete")
    public QuestResponseDTO completeQuest(@PathVariable long id, @AuthenticationPrincipal AppUser currentUser) {
        Quest quest = questService.completeQuest(id, currentUser);
        return questMgr.toDto(quest);
    }

}
