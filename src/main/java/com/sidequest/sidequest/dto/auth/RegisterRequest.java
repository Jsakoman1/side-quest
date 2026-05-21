package com.sidequest.sidequest.dto.auth;

public record RegisterRequest(String email, String username, String password) {
}