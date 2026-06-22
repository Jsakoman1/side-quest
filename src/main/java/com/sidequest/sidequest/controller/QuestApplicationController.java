package com.sidequest.sidequest.controller;

import com.sidequest.sidequest.dto.QuestApplicationRequestDTO;
import com.sidequest.sidequest.dto.QuestApplicationResponseDTO;
import com.sidequest.sidequest.model.AppUser;
import com.sidequest.sidequest.service.QuestApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
            @AuthenticationPrincipal AppUser currentUser
    ) {
        return questApplicationService.applyForQuest(questId, dto, currentUser);
    }

    @GetMapping("/quests/{questId}/applications")
    public List<QuestApplicationResponseDTO> getApplicationsForQuest(@PathVariable Long questId, @AuthenticationPrincipal AppUser currentUser) {
        return questApplicationService.getApplicationsForQuest(questId, currentUser);
    }

    @GetMapping("/quests/applications/me")
    public List<QuestApplicationResponseDTO> getMyApplications(@AuthenticationPrincipal AppUser currentUser) {
        return questApplicationService.getApplicationsForApplicant(currentUser);
    }

    @GetMapping("/admin/applications")
    public List<QuestApplicationResponseDTO> getAllApplicationsForAdmin(@AuthenticationPrincipal AppUser currentUser) {
        return questApplicationService.getAllApplicationsForAdmin(currentUser);
    }

    @PutMapping("/quests/{questId}/applications/me")
    public QuestApplicationResponseDTO updateMyApplication(
            @PathVariable Long questId,
            @Valid @RequestBody QuestApplicationRequestDTO dto,
            @AuthenticationPrincipal AppUser currentUser
    ) {
        return questApplicationService.updateMyApplication(questId, dto, currentUser);
    }

    @PatchMapping("/quests/{questId}/applications/me/withdraw")
    public QuestApplicationResponseDTO withdrawMyApplication(@PathVariable Long questId, @AuthenticationPrincipal AppUser currentUser) {
        return questApplicationService.withdrawMyApplication(questId, currentUser);
    }

    @PatchMapping("/quests/{questId}/applications/{applicationId}/approve")
    public QuestApplicationResponseDTO approveApplication(
            @PathVariable Long questId,
            @PathVariable Long applicationId,
            @AuthenticationPrincipal AppUser currentUser
    ) {
        return questApplicationService.approveApplication(questId, applicationId, currentUser);
    }

    @PatchMapping("/quests/{questId}/applications/{applicationId}/decline")
    public QuestApplicationResponseDTO declineApplication(
            @PathVariable Long questId,
            @PathVariable Long applicationId,
            @AuthenticationPrincipal AppUser currentUser
    ) {
        return questApplicationService.declineApplication(questId, applicationId, currentUser);
    }

}
