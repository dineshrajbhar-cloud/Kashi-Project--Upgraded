package com.kashi.portal.user.dto;

import com.kashi.portal.user.Gender;
import com.kashi.portal.user.Role;
import com.kashi.portal.user.User;

import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String name,
        String email,
        String city,
        Gender gender,
        Role role,
        LocalDateTime createdAt) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getCity(),
                user.getGender(),
                user.getRole(),
                user.getCreatedAt());
    }
}
