package com.sidequest.sidequest.mapper;

import com.sidequest.sidequest.dto.AppUserRequestDTO;
import com.sidequest.sidequest.dto.AppUserResponseDTO;
import com.sidequest.sidequest.model.AppUser;
import com.sidequest.sidequest.model.AppUserRole;
import org.springframework.stereotype.Component;

@Component
public class AppUserMgr {
    public AppUser toEntity(AppUserRequestDTO dto) {
        if (dto == null) {
            return null;
        }

        AppUser appUser = new AppUser();
        appUser.setEmail(dto.getEmail());
        appUser.setUsername(dto.getUsername());
        appUser.setRole(dto.getRole() == null ? AppUserRole.USER : dto.getRole());
        return appUser;
    }

    public AppUserResponseDTO toDto(AppUser appUser) {
        if (appUser == null) {
            return null;
        }

        return AppUserResponseDTO.builder()
                .id(appUser.getId())
                .email(appUser.getEmail())
                .username(appUser.getUsername())
                .role(appUser.getRole() == null ? AppUserRole.USER.name() : appUser.getRole().name())
                .build();
    }
}
