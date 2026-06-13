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
        return toDto(questService.createQuest(dto, currentUser));
    }

    @GetMapping
    public List<QuestResponseDTO> getAllQuests() {
        return questService.getAllQuests()
                .stream()
                .map(this::toDto)
                .toList();
    }

    @GetMapping("/{id}")
    public QuestResponseDTO getQuestById(@PathVariable long id) {
        return toDto(questService.getQuestById(id));
    }

    @DeleteMapping("/{id}")
    public void deleteQuest(@PathVariable long id, @AuthenticationPrincipal AppUser currentUser) {
        questService.deleteQuest(id, currentUser);
    }

    @PutMapping("/{id}")
    public QuestResponseDTO updateQuest(@PathVariable long id, @Valid @RequestBody QuestRequestDTO dto, @AuthenticationPrincipal AppUser currentUser) {
        return toDto(questService.updateQuest(id, dto, currentUser));
    }

    @PatchMapping("/{id}/start")
    public QuestResponseDTO startQuest(@PathVariable long id, @AuthenticationPrincipal AppUser currentUser) {
        return toDto(questService.startQuest(id, currentUser));
    }

    @PatchMapping("/{id}/complete")
    public QuestResponseDTO completeQuest(@PathVariable long id, @AuthenticationPrincipal AppUser currentUser) {
        return toDto(questService.completeQuest(id, currentUser));
    }

    private QuestResponseDTO toDto(Quest quest) {
        return questMgr.toDto(quest);
    }

}
