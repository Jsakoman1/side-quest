package com.sidequest.sidequest.controller;

import com.sidequest.sidequest.dto.QuestRequestDTO;
import com.sidequest.sidequest.dto.QuestListResponseDTO;
import com.sidequest.sidequest.dto.QuestResponseDTO;
import com.sidequest.sidequest.mapper.QuestMgr;
import com.sidequest.sidequest.model.AppUser;
import com.sidequest.sidequest.model.QuestAudience;
import com.sidequest.sidequest.model.Quest;
import com.sidequest.sidequest.model.QuestStatus;
import com.sidequest.sidequest.service.QuestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
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
    public List<QuestResponseDTO> getAllQuests(@AuthenticationPrincipal AppUser currentUser) {
        return questService.getAllQuests(currentUser)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @GetMapping("/search")
    public QuestListResponseDTO searchQuests(
            @AuthenticationPrincipal AppUser currentUser,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) QuestStatus status,
            @RequestParam(required = false) QuestAudience audience,
            @RequestParam(required = false) LocalDate dateFrom,
            @RequestParam(required = false) LocalDate dateTo,
            @RequestParam(required = false) Boolean excludeMine,
            @RequestParam(required = false) Boolean withImages,
            @RequestParam(required = false) Boolean scheduledOnly,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        return questService.searchQuests(currentUser, q, status, audience, dateFrom, dateTo, excludeMine, withImages, scheduledOnly, sort, page, size);
    }

    @GetMapping("/{id}")
    public QuestResponseDTO getQuestById(@PathVariable long id, @AuthenticationPrincipal AppUser currentUser) {
        return toDto(questService.getQuestById(id, currentUser));
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

    @PatchMapping("/{id}/term/confirm")
    public QuestResponseDTO confirmQuestTermChange(@PathVariable long id, @AuthenticationPrincipal AppUser currentUser) {
        return toDto(questService.confirmQuestTermChange(id, currentUser));
    }

    @PatchMapping("/{id}/term/reject")
    public QuestResponseDTO rejectQuestTermChange(@PathVariable long id, @AuthenticationPrincipal AppUser currentUser) {
        return toDto(questService.rejectQuestTermChange(id, currentUser));
    }

    private QuestResponseDTO toDto(Quest quest) {
        return questMgr.toDto(quest);
    }

}
