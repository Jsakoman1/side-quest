package com.sidequest.sidequest.controller;

import com.sidequest.sidequest.dto.AppUserRequestDTO;
import com.sidequest.sidequest.dto.AppUserResponseDTO;
import com.sidequest.sidequest.mapper.AppUserMgr;
import com.sidequest.sidequest.model.AppUser;
import com.sidequest.sidequest.service.AppUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/app_users")
@RequiredArgsConstructor
public class AppUserController {
    private final AppUserService appUserService;
    private final AppUserMgr appUserMgr;
    private final PasswordEncoder passwordEncoder;

    @PostMapping
    public AppUserResponseDTO createAppUser(@Valid @RequestBody AppUserRequestDTO dto) {
        if (dto.getPassword() == null || dto.getPassword().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password is required");
        }

        AppUser appUser = appUserMgr.toEntity(dto);
        appUser.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
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

    @GetMapping("/me")
    public AppUserResponseDTO getCurrentAppUser(@AuthenticationPrincipal AppUser currentUser) {
        return appUserMgr.toDto(currentUser);
    }

    @DeleteMapping("/{id}")
    public void deleteAppUser(@PathVariable long id) {
        appUserService.deleteUser(id);
    }

    @PutMapping("/{id}")
    public AppUserResponseDTO updateAppUser(@PathVariable long id, @Valid @RequestBody AppUserRequestDTO dto) {
        AppUser appUser = appUserService.updateAppUserAsAdmin(id, dto);
        return appUserMgr.toDto(appUser);
    }

    @PutMapping("/me")
    public AppUserResponseDTO updateCurrentAppUser(@AuthenticationPrincipal AppUser currentUser, @Valid @RequestBody AppUserRequestDTO dto) {
        AppUser updatedAppUser = appUserService.updateAppUser(currentUser.getId(), dto);
        return appUserMgr.toDto(updatedAppUser);
    }
}
