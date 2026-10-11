package com.kashi.portal.support;

import com.kashi.portal.user.Role;
import com.kashi.portal.user.User;

public final class TestUsers {

    private TestUsers() {
    }

    public static User withRole(String email, Role role) {
        return User.builder()
                .id(1L)
                .name("Test User")
                .email(email)
                .password("not-a-real-hash")
                .role(role)
                .build();
    }
}
