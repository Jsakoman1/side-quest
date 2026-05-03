package com.sidequest.sidequest.service;

import com.sidequest.sidequest.dto.AppUserRequestDTO;
import com.sidequest.sidequest.model.AppUser;
import com.sidequest.sidequest.repository.AppUserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AppUserService {
    private final AppUserRepository appUserRepository;

    public AppUserService(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    public AppUser createAppUser(AppUser appUser) {
        return appUserRepository.save(appUser);
    }

    public List<AppUser> getAllAppUsers() {
        return appUserRepository.findAll();
    }

    public void deleteUser(Long id) {
        appUserRepository.deleteById(id);
    }

    public AppUser updateAppUser(Long id, AppUserRequestDTO dto) {
        AppUser appUser = appUserRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(String.format("AppUser not found with id %s", id)));
        appUser.setUsername(dto.getUsername());
        appUser.setEmail(dto.getEmail());
        return appUserRepository.save(appUser);
    }
}