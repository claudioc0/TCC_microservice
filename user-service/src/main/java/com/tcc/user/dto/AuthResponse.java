package com.tcc.user.dto;

public record AuthResponse(String token, String type, Long userId, String name, String email, String role) {
    public static AuthResponse of(String token, Long userId, String name, String email, String role) {
        return new AuthResponse(token, "Bearer", userId, name, email, role);
    }
}
