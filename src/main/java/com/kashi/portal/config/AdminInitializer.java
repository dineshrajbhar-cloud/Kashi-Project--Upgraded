package com.kashi.portal.config;

import com.kashi.portal.user.Role;
import com.kashi.portal.user.User;
import com.kashi.portal.user.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Creates the first ADMIN account at startup from ADMIN_EMAIL and ADMIN_PASSWORD.
 * Nothing happens if an admin already exists. The password is hashed and never logged.
 */
@Slf4j
@Component
public class AdminInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminEmail;
    private final String adminPassword;

    public AdminInitializer(UserRepository userRepository,
                            PasswordEncoder passwordEncoder,
                            @Value("${app.admin.email:}") String adminEmail,
                            @Value("${app.admin.password:}") String adminPassword) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.existsByRole(Role.ADMIN)) {
            return;
        }
        if (adminEmail.isBlank() || adminPassword.isBlank()) {
            log.warn("No ADMIN account exists. Set ADMIN_EMAIL and ADMIN_PASSWORD to create one at startup.");
            return;
        }
        if (adminPassword.startsWith("replace_with")) {
            log.warn("ADMIN_PASSWORD is still the placeholder from .env.example. No admin was created.");
            return;
        }
        int passwordBytes = adminPassword.getBytes(StandardCharsets.UTF_8).length;
        if (passwordBytes < 8 || passwordBytes > 72) {
            log.warn("ADMIN_PASSWORD must be between 8 and 72 bytes. No admin was created.");
            return;
        }

        String email = adminEmail.trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmail(email)) {
            log.warn("ADMIN_EMAIL is already registered as a normal user. No admin was created.");
            return;
        }

        User admin = User.builder()
                .name("Administrator")
                .email(email)
                .password(passwordEncoder.encode(adminPassword))
                .role(Role.ADMIN)
                .build();
        userRepository.save(admin);
        log.info("Initial ADMIN account created for {}", email);
    }
}
