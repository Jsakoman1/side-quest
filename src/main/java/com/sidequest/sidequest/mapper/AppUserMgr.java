package com.sidequest.sidequest.mapper;

import com.sidequest.sidequest.dto.AppUserResponseDTO;
import com.sidequest.sidequest.model.AppUser;
import com.sidequest.sidequest.model.AppUserRole;
import org.springframework.stereotype.Component;

@Component
public class AppUserMgr {
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
