package com.sidequest.sidequest.controller;

import com.sidequest.sidequest.dto.QuestRequestDTO;
import com.sidequest.sidequest.dto.QuestResponseDTO;
import com.sidequest.sidequest.mapper.QuestMgr;
import com.sidequest.sidequest.model.AppUser;
import com.sidequest.sidequest.model.Quest;
import com.sidequest.sidequest.service.QuestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
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
    public QuestResponseDTO createQuest(@Valid @RequestBody QuestRequestDTO dto, Authentication authentication) {
        AppUser currentUser = (AppUser) authentication.getPrincipal();
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

    @DeleteMapping("/{id}")
    public void deleteQuest(@PathVariable long id, Authentication authentication) {
        AppUser currentUser = (AppUser) authentication.getPrincipal();
        questService.deleteQuest(id, currentUser);
    }

    @PutMapping("/{id}")
    public QuestResponseDTO updateQuest(@PathVariable long id, @Valid @RequestBody QuestRequestDTO dto, Authentication authentication) {
        AppUser currentUser = (AppUser) authentication.getPrincipal();
        Quest quest = questService.updateQuest(id, dto, currentUser);
        return questMgr.toDto(quest);
    }
}