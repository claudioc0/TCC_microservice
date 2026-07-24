package com.tcc.user.dto;

import com.tcc.user.entity.User;

import java.time.LocalDateTime;

public record UserResponse(Long id, String name, String email, String role, LocalDateTime createdAt) {
    public static UserResponse from(User u) {
        return new UserResponse(u.getId(), u.getName(), u.getEmail(), u.getRole().name(), u.getCreatedAt());
    }
}
