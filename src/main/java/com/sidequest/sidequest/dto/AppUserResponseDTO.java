package com.sidequest.sidequest.dto;

import lombok.*;

import java.util.List;

import com.sidequest.sidequest.dto.QuestResponseDTO;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppUserResponseDTO {
    private Long id;
    private String email;
    private String username;
    private String profileDescription;
    private String profileAvatarDataUrl;
    private long openQuestCount;
    private List<QuestResponseDTO> openQuests;
    private String role;
}
