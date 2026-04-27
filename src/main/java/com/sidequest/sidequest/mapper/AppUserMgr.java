package com.sidequest.sidequest.mapper;

import com.sidequest.sidequest.dto.AppUserRequestDTO;
import com.sidequest.sidequest.dto.AppUserResponseDTO;
import com.sidequest.sidequest.model.AppUser;
import org.springframework.stereotype.Component;

@Component
public class AppUserMgr {
    public AppUser toEntity(AppUserRequestDTO dto) {
        if (dto == null) {
            return null;
        } else {
            AppUser appUser = new AppUser();
            appUser.setEmail(dto.getEmail());
            appUser.setUsername(dto.getUsername());
            return appUser;
        }
    }

    public AppUserResponseDTO toDto(AppUser appUser) {
        if (appUser == null) {
            return null;
        } else {
            return AppUserResponseDTO.builder()
                    .id(appUser.getId())
                    .email(appUser.getEmail())
                    .username(appUser.getUsername())
                    .build();
        }
    }
}