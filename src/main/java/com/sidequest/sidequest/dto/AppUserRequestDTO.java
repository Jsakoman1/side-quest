package com.sidequest.sidequest.dto;

import com.sidequest.sidequest.model.AppUserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
    @Size(max = 2000)
    private String profileDescription;
    @Size(max = 250000)
    private String profileAvatarDataUrl;
    private AppUserRole role;
}
