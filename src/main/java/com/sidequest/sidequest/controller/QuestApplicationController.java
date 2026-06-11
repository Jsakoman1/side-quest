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

    @GetMapping("/quests/applications/me")
    public List<QuestApplicationResponseDTO> getMyApplications(Authentication authentication) {
        AppUser currentUser = (AppUser) authentication.getPrincipal();
        return questApplicationService.getApplicationsForApplicant(currentUser);
    }

    @PutMapping("/quests/{questId}/applications/me")
    public QuestApplicationResponseDTO updateMyApplication(
            @PathVariable Long questId,
            @Valid @RequestBody QuestApplicationRequestDTO dto,
            Authentication authentication
    ) {
        AppUser currentUser = (AppUser) authentication.getPrincipal();
        return questApplicationService.updateMyApplication(questId, dto, currentUser);
    }

    @PatchMapping("/quests/{questId}/applications/{applicationId}/accept")
    public QuestApplicationResponseDTO acceptApplication(
            @PathVariable Long questId,
            @PathVariable Long applicationId,
            Authentication authentication
    ) {
        AppUser currentUser = (AppUser) authentication.getPrincipal();
        return questApplicationService.acceptApplication(questId, applicationId, currentUser);
    }

    @PatchMapping("/quests/{questId}/applications/{applicationId}/reject")
    public QuestApplicationResponseDTO rejectApplication(
            @PathVariable Long questId,
            @PathVariable Long applicationId,
            Authentication authentication
    ) {
        AppUser currentUser = (AppUser) authentication.getPrincipal();
        return questApplicationService.rejectApplication(questId, applicationId, currentUser);
    }

}
