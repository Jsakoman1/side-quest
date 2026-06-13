package com.sidequest.sidequest.service;

import com.sidequest.sidequest.dto.AppUserRequestDTO;
import com.sidequest.sidequest.model.AppUser;
import com.sidequest.sidequest.model.AppUserRole;
import com.sidequest.sidequest.repository.AppUserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AppUserService {
    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    public AppUserService(AppUserRepository appUserRepository, PasswordEncoder passwordEncoder) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public AppUser createAppUser(AppUserRequestDTO dto) {
        validatePassword(dto.getPassword());
        AppUser appUser = new AppUser();
        appUser.setEmail(dto.getEmail());
        appUser.setUsername(dto.getUsername());
        appUser.setRole(dto.getRole() == null ? AppUserRole.USER : dto.getRole());
        appUser.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        return appUserRepository.save(appUser);
    }

    public List<AppUser> getAllAppUsers() {
        return appUserRepository.findAll();
    }

    public void deleteUser(Long id) {
        appUserRepository.deleteById(id);
    }

    public AppUser updateAppUser(Long id, AppUserRequestDTO dto) {
        AppUser appUser = requireAppUser(id);
        validateUniqueEmail(id, appUser, dto.getEmail());
        updateBasicProfile(appUser, dto);
        return appUserRepository.save(appUser);
    }

    public AppUser updateAppUserAsAdmin(Long id, AppUserRequestDTO dto) {
        AppUser appUser = requireAppUser(id);
        validateUniqueEmail(id, appUser, dto.getEmail());
        updateBasicProfile(appUser, dto);
        applyAdminOverrides(appUser, dto);
        return appUserRepository.save(appUser);
    }

    private AppUser requireAppUser(Long id) {
        return appUserRepository.findById(id)
                .orElseThrow(() -> ServiceErrors.notFound(String.format("AppUser not found with id %s", id)));
    }

    private void validatePassword(String password) {
        if (password == null || password.isBlank()) {
            throw ServiceErrors.badRequest("Password is required");
        }
    }

    private void validateUniqueEmail(Long id, AppUser appUser, String email) {
        if (email != null && !email.equals(appUser.getEmail()) && appUserRepository.existsByEmailAndIdNot(email, id)) {
            throw ServiceErrors.conflict("Email already exists");
        }
    }

    private void updateBasicProfile(AppUser appUser, AppUserRequestDTO dto) {
        appUser.setUsername(dto.getUsername());
        appUser.setEmail(dto.getEmail());
    }

    private void applyAdminOverrides(AppUser appUser, AppUserRequestDTO dto) {
        if (dto.getRole() != null) {
            appUser.setRole(dto.getRole());
        }

        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            appUser.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        }
    }
}
