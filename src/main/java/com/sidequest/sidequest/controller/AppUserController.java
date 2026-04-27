package com.sidequest.sidequest.controller;

import com.sidequest.sidequest.dto.AppUserRequestDTO;
import com.sidequest.sidequest.dto.AppUserResponseDTO;
import com.sidequest.sidequest.mapper.AppUserMgr;
import com.sidequest.sidequest.model.AppUser;
import com.sidequest.sidequest.service.AppUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/app_users")
@RequiredArgsConstructor
public class AppUserController {
    private final AppUserService appUserService;
    private final AppUserMgr appUserMgr;

    @PostMapping
    public AppUserResponseDTO createAppUser(@Valid @RequestBody AppUserRequestDTO dto) {
        AppUser appUser = appUserMgr.toEntity(dto);
        AppUser saved = appUserService.createAppUser(appUser);
        return appUserMgr.toDto(saved);
    }

    @GetMapping
    public List<AppUserResponseDTO> getAllAppUsers() {
        return appUserService.getAllAppUsers()
                .stream()
                .map(appUserMgr::toDto)
                .toList();
    }
}