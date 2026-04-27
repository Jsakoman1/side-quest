package com.sidequest.sidequest.repository;

import com.sidequest.sidequest.model.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
}