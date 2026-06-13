package com.sidequest.sidequest.dto;

import com.sidequest.sidequest.model.AppUserRole;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppUserRequestDTO {
    private @NotBlank String email;
    private @NotBlank String username;
    private String password;
    private AppUserRole role;
}
