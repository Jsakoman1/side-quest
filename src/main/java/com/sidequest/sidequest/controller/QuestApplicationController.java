package com.sidequest.sidequest.controller;

import com.sidequest.sidequest.dto.QuestApplicationRequestDTO;
import com.sidequest.sidequest.dto.QuestApplicationResponseDTO;
import com.sidequest.sidequest.model.AppUser;
import com.sidequest.sidequest.service.QuestApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequiredArgsConstructor
public class QuestApplicationController {

    private final QuestApplicationService questApplicationService;

    @PostMapping("/quests/{questId}/applications")
    public QuestApplicationResponseDTO applyForQuest(
            @PathVariable Long questId,
            @Valid @RequestBody QuestApplicationRequestDTO dto,
            Authentication authentication
    ) {
        AppUser currentUser = (AppUser) authentication.getPrincipal();
        return questApplicationService.applyForQuest(questId, dto, currentUser);
    }

    @GetMapping("/quests/{questId}/applications")
    public List<QuestApplicationResponseDTO> getApplicationsForQuest(@PathVariable Long questId, Authentication authentication) {
        AppUser currentUser = (AppUser) authentication.getPrincipal();
        return questApplicationService.getApplicationsForQuest(questId, currentUser);
    }

}