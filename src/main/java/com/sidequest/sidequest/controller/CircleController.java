package com.sidequest.sidequest.controller;

import com.sidequest.sidequest.dto.CircleRequestCreateDTO;
import com.sidequest.sidequest.dto.CircleBlockCreateDTO;
import com.sidequest.sidequest.dto.CircleRelationDTO;
import com.sidequest.sidequest.dto.CircleOverviewDTO;
import com.sidequest.sidequest.dto.CircleSearchResultDTO;
import com.sidequest.sidequest.dto.CircleRequestResponseDTO;
import com.sidequest.sidequest.dto.AdminCircleOverviewDTO;
import com.sidequest.sidequest.dto.CircleContactDTO;
import com.sidequest.sidequest.dto.ConnectionCircleUpdateDTO;
import com.sidequest.sidequest.dto.CircleGroupRequestDTO;
import com.sidequest.sidequest.dto.CircleGroupResponseDTO;
import com.sidequest.sidequest.model.AppUser;
import com.sidequest.sidequest.service.CircleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/circles")
@RequiredArgsConstructor
public class CircleController {
    private final CircleService circleService;

    @GetMapping("/me/overview")
    public CircleOverviewDTO getOverview(@AuthenticationPrincipal AppUser currentUser) {
        return circleService.getOverview(currentUser);
    }

    @GetMapping("/admin/overview")
    public AdminCircleOverviewDTO getAdminOverview(@AuthenticationPrincipal AppUser currentUser) {
        return circleService.getAdminOverview(currentUser);
    }

    @PostMapping("/groups")
    public CircleGroupResponseDTO createCircle(
            @Valid @RequestBody CircleGroupRequestDTO dto,
            @AuthenticationPrincipal AppUser currentUser
    ) {
        return circleService.createCircle(dto, currentUser);
    }

    @PutMapping("/groups/{id}")
    public CircleGroupResponseDTO updateCircle(
            @PathVariable Long id,
            @Valid @RequestBody CircleGroupRequestDTO dto,
            @AuthenticationPrincipal AppUser currentUser
    ) {
        return circleService.updateCircle(id, dto, currentUser);
    }

    @DeleteMapping("/groups/{id}")
    public void deleteCircle(
            @PathVariable Long id,
            @AuthenticationPrincipal AppUser currentUser
    ) {
        circleService.deleteCircle(id, currentUser);
    }

    @DeleteMapping("/admin/groups/{id}")
    public void deleteCircleAsAdmin(
            @PathVariable Long id,
            @AuthenticationPrincipal AppUser currentUser
    ) {
        circleService.deleteCircleAsAdmin(id, currentUser);
    }

    @PutMapping("/connections/{userId}/circles")
    public CircleContactDTO updateConnectionCircles(
            @PathVariable Long userId,
            @RequestBody ConnectionCircleUpdateDTO dto,
            @AuthenticationPrincipal AppUser currentUser
    ) {
        return circleService.updateConnectionCircles(userId, dto, currentUser);
    }

    @GetMapping
    public List<CircleRequestResponseDTO> getMyCircles(@AuthenticationPrincipal AppUser currentUser) {
        return circleService.getMyCircles(currentUser);
    }

    @GetMapping("/requests/incoming")
    public List<CircleRequestResponseDTO> getIncomingRequests(@AuthenticationPrincipal AppUser currentUser) {
        return circleService.getIncomingRequests(currentUser);
    }

    @GetMapping("/requests/outgoing")
    public List<CircleRequestResponseDTO> getOutgoingRequests(@AuthenticationPrincipal AppUser currentUser) {
        return circleService.getOutgoingRequests(currentUser);
    }

    @GetMapping("/candidates")
    public List<CircleSearchResultDTO> getInviteCandidates(@AuthenticationPrincipal AppUser currentUser) {
        return circleService.getInviteCandidates(currentUser);
    }

    @GetMapping("/relations/{userId}")
    public CircleRelationDTO getRelationWithUser(
            @PathVariable Long userId,
            @AuthenticationPrincipal AppUser currentUser
    ) {
        return circleService.getRelationWithUser(currentUser, userId);
    }

    @GetMapping("/search")
    public List<CircleSearchResultDTO> searchCircleUsers(
            @RequestParam(value = "q", required = false) String query,
            @AuthenticationPrincipal AppUser currentUser
    ) {
        return circleService.searchCircleUsers(currentUser, query);
    }

    @PostMapping("/requests")
    public CircleRequestResponseDTO createCircleRequest(
            @Valid @RequestBody CircleRequestCreateDTO dto,
            @AuthenticationPrincipal AppUser currentUser
    ) {
        return circleService.createCircleRequest(dto, currentUser);
    }

    @PatchMapping("/requests/{id}/accept")
    public CircleRequestResponseDTO acceptCircleRequest(
            @PathVariable Long id,
            @AuthenticationPrincipal AppUser currentUser
    ) {
        return circleService.acceptCircleRequest(id, currentUser);
    }

    @DeleteMapping("/requests/{id}")
    public void deleteCircleRequest(
            @PathVariable Long id,
            @AuthenticationPrincipal AppUser currentUser
    ) {
        circleService.deleteCircleRequest(id, currentUser);
    }

    @PostMapping("/blocks")
    public CircleRequestResponseDTO blockCircleUser(
            @Valid @RequestBody CircleBlockCreateDTO dto,
            @AuthenticationPrincipal AppUser currentUser
    ) {
        return circleService.blockCircleUser(dto, currentUser);
    }

    @DeleteMapping("/blocks/{userId}")
    public void unblockCircleUser(
            @PathVariable Long userId,
            @AuthenticationPrincipal AppUser currentUser
    ) {
        circleService.unblockCircleUser(userId, currentUser);
    }
}
