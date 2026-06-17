package com.sidequest.sidequest.service;

import com.sidequest.sidequest.dto.AppUserRequestDTO;
import com.sidequest.sidequest.mapper.QuestMgr;
import com.sidequest.sidequest.model.AppUser;
import com.sidequest.sidequest.model.AppUserRole;
import com.sidequest.sidequest.model.QuestStatus;
import com.sidequest.sidequest.repository.AppUserRepository;
import com.sidequest.sidequest.repository.QuestRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AppUserService {
    private static final int MAX_PROFILE_AVATAR_DATA_URL_LENGTH = 250_000;

    private final AppUserRepository appUserRepository;
    private final QuestRepository questRepository;
    private final PasswordEncoder passwordEncoder;
    private final QuestMgr questMgr;

    public AppUserService(
            AppUserRepository appUserRepository,
            QuestRepository questRepository,
            QuestMgr questMgr,
            PasswordEncoder passwordEncoder
    ) {
        this.appUserRepository = appUserRepository;
        this.questRepository = questRepository;
        this.questMgr = questMgr;
        this.passwordEncoder = passwordEncoder;
    }

    public AppUser createAppUser(AppUserRequestDTO dto) {
        validatePassword(dto.getPassword());
        AppUser appUser = new AppUser();
        appUser.setEmail(dto.getEmail());
        appUser.setUsername(dto.getUsername());
        applyProfileDetails(appUser, dto, true);
        appUser.setRole(dto.getRole() == null ? AppUserRole.USER : dto.getRole());
        appUser.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        return appUserRepository.save(appUser);
    }

    public List<AppUser> getAllAppUsers() {
        return appUserRepository.findAll();
    }

    public AppUser getAppUser(Long id) {
        return requireAppUser(id);
    }

    public long countQuestsByCreatorId(Long creatorId) {
        return questRepository.countByCreatorIdAndStatus(creatorId, QuestStatus.OPEN);
    }

    public List<com.sidequest.sidequest.dto.QuestResponseDTO> getOpenQuestsByCreatorId(Long creatorId) {
        return questRepository.findByCreatorIdAndStatusOrderByIdDesc(creatorId, QuestStatus.OPEN)
                .stream()
                .limit(6)
                .map(questMgr::toDto)
                .toList();
    }

    public void deleteUser(Long id) {
        appUserRepository.deleteById(id);
    }

    public AppUser updateAppUser(Long id, AppUserRequestDTO dto) {
        AppUser appUser = requireAppUser(id);
        validateUniqueEmail(id, appUser, dto.getEmail());
        updateBasicProfile(appUser, dto);
        applyProfileDetails(appUser, dto, true);
        return appUserRepository.save(appUser);
    }

    public AppUser updateAppUserAsAdmin(Long id, AppUserRequestDTO dto) {
        AppUser appUser = requireAppUser(id);
        validateUniqueEmail(id, appUser, dto.getEmail());
        updateBasicProfile(appUser, dto);
        applyProfileDetails(appUser, dto, false);
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

    private void applyProfileDetails(AppUser appUser, AppUserRequestDTO dto, boolean overwriteExisting) {
        if (overwriteExisting || dto.getProfileDescription() != null) {
            appUser.setProfileDescription(normalizeProfileText(dto.getProfileDescription()));
        }

        if (overwriteExisting || dto.getProfileAvatarDataUrl() != null) {
            appUser.setProfileAvatarDataUrl(normalizeProfileAvatarDataUrl(dto.getProfileAvatarDataUrl()));
        }
    }

    private String normalizeProfileText(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String normalizeProfileAvatarDataUrl(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();
        if (normalized.isEmpty()) {
            return null;
        }

        if (!normalized.startsWith("data:image/")) {
            throw ServiceErrors.badRequest("Profile avatar must be an image data URL");
        }

        if (normalized.length() > MAX_PROFILE_AVATAR_DATA_URL_LENGTH) {
            throw ServiceErrors.badRequest("Profile avatar is too large");
        }

        return normalized;
    }
}
