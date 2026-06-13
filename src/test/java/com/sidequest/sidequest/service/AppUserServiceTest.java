package com.sidequest.sidequest.service;

import com.sidequest.sidequest.dto.AppUserRequestDTO;
import com.sidequest.sidequest.model.AppUser;
import com.sidequest.sidequest.model.AppUserRole;
import com.sidequest.sidequest.repository.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppUserServiceTest {

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AppUserService appUserService;

    @Test
    void updateAppUserThrowsConflictWhenEmailAlreadyExistsOnAnotherUser() {
        AppUser existingUser = new AppUser();
        existingUser.setId(1L);
        existingUser.setEmail("old@example.com");
        existingUser.setUsername("old");

        AppUserRequestDTO dto = new AppUserRequestDTO();
        dto.setEmail("taken@example.com");
        dto.setUsername("new-name");

        when(appUserRepository.findById(1L)).thenReturn(Optional.of(existingUser));
        when(appUserRepository.existsByEmailAndIdNot("taken@example.com", 1L)).thenReturn(true);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> appUserService.updateAppUser(1L, dto));
        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    }

    @Test
    void adminUpdateAppUserCanChangeRoleAndPassword() {
        AppUser existingUser = new AppUser();
        existingUser.setId(1L);
        existingUser.setEmail("old@example.com");
        existingUser.setUsername("old");
        existingUser.setRole(AppUserRole.USER);

        AppUserRequestDTO dto = new AppUserRequestDTO();
        dto.setEmail("new@example.com");
        dto.setUsername("new-name");
        dto.setRole(AppUserRole.ADMIN);
        dto.setPassword("new-password");

        when(appUserRepository.findById(1L)).thenReturn(Optional.of(existingUser));
        when(appUserRepository.save(existingUser)).thenReturn(existingUser);
        when(passwordEncoder.encode("new-password")).thenReturn("encoded");

        AppUser updated = appUserService.updateAppUserAsAdmin(1L, dto);

        assertEquals(AppUserRole.ADMIN, updated.getRole());
        assertEquals("encoded", updated.getPasswordHash());
    }
}
