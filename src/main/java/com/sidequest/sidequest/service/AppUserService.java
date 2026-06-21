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
import lombok.RequiredArgsConstructor;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AppUserService {
    private final AppUserRepository appUserRepository;
    private final QuestRepository questRepository;
    private final PasswordEncoder passwordEncoder;
    private final QuestMgr questMgr;

    public AppUser createAppUser(AppUserRequestDTO dto) {
        validatePassword(dto.getPassword());
        String email = UserInputNormalizer.normalizeEmail(dto.getEmail());
        validateUniqueEmail(null, email);
        AppUser appUser = new AppUser();
        appUser.setEmail(email);
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

    public void deleteUser(Long id, AppUser currentUser) {
        AppUser targetUser = requireAppUser(id);
        if (currentUser != null && id.equals(currentUser.getId())) {
            throw ServiceErrors.badRequest("You cannot delete your own account");
        }
        if (targetUser.getRole() == AppUserRole.ADMIN && appUserRepository.countByRole(AppUserRole.ADMIN) <= 1) {
            throw ServiceErrors.conflict("The last administrator cannot be deleted");
        }
        appUserRepository.delete(targetUser);
    }

    public AppUser updateAppUser(Long id, AppUserRequestDTO dto) {
        AppUser appUser = requireAppUser(id);
        validateUniqueEmail(id, dto.getEmail());
        updateBasicProfile(appUser, dto);
        applyProfileDetails(appUser, dto, true);
        return appUserRepository.save(appUser);
    }

    public AppUser updateAppUserAsAdmin(Long id, AppUserRequestDTO dto) {
        AppUser appUser = requireAppUser(id);
        validateUniqueEmail(id, dto.getEmail());
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

    private void validateUniqueEmail(Long id, String email) {
        String normalizedEmail = UserInputNormalizer.normalizeEmail(email);
        boolean exists = id == null
                ? appUserRepository.existsByEmail(normalizedEmail)
                : appUserRepository.existsByEmailAndIdNot(normalizedEmail, id);
        if (exists) {
            throw ServiceErrors.conflict("Email already exists");
        }
    }

    private void updateBasicProfile(AppUser appUser, AppUserRequestDTO dto) {
        appUser.setUsername(dto.getUsername());
        appUser.setEmail(UserInputNormalizer.normalizeEmail(dto.getEmail()));
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
            appUser.setProfileDescription(ProfileValueNormalizer.normalizeText(dto.getProfileDescription()));
        }

        if (overwriteExisting || dto.getProfileAvatarDataUrl() != null) {
            appUser.setProfileAvatarDataUrl(ProfileValueNormalizer.normalizeAvatarDataUrl(dto.getProfileAvatarDataUrl()));
        }
    }
}
