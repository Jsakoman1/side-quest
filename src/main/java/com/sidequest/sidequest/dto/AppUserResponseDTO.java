package com.sidequest.sidequest.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppUserResponseDTO {
    private Long id;
    private String email;
    private String username;
    private String role;
}
