package com.sidequest.sidequest.dto.auth;

public record AuthResponse(Long id, String email, String username, String role, String token) {
}
