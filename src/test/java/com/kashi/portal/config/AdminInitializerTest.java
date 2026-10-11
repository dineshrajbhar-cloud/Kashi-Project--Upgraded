package com.kashi.portal.config;

import com.kashi.portal.user.Role;
import com.kashi.portal.user.User;
import com.kashi.portal.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AdminInitializerTest {

    @Mock
    private UserRepository userRepository;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);

    @Test
    void createsAnAdminWithAHashedPasswordWhenNoneExists() {
        given(userRepository.existsByRole(Role.ADMIN)).willReturn(false);
        given(userRepository.existsByEmail("admin@kashi.com")).willReturn(false);
        AdminInitializer initializer =
                new AdminInitializer(userRepository, passwordEncoder, " Admin@Kashi.com ", "StrongAdminPass1");

        initializer.run(null);

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        User admin = saved.getValue();
        assertEquals("admin@kashi.com", admin.getEmail());
        assertEquals(Role.ADMIN, admin.getRole());
        assertNotEquals("StrongAdminPass1", admin.getPassword());
        assertTrue(passwordEncoder.matches("StrongAdminPass1", admin.getPassword()));
    }

    @Test
    void doesNothingWhenAnAdminAlreadyExists() {
        given(userRepository.existsByRole(Role.ADMIN)).willReturn(true);
        AdminInitializer initializer =
                new AdminInitializer(userRepository, passwordEncoder, "admin@kashi.com", "StrongAdminPass1");

        initializer.run(null);

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void doesNothingWhenCredentialsAreNotConfigured() {
        given(userRepository.existsByRole(Role.ADMIN)).willReturn(false);
        AdminInitializer initializer = new AdminInitializer(userRepository, passwordEncoder, "", "");

        initializer.run(null);

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void refusesThePlaceholderPasswordFromTheExampleFile() {
        given(userRepository.existsByRole(Role.ADMIN)).willReturn(false);
        AdminInitializer initializer = new AdminInitializer(
                userRepository, passwordEncoder, "admin@kashi.com", "replace_with_a_strong_password");

        initializer.run(null);

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void refusesAPasswordThatIsTooShort() {
        given(userRepository.existsByRole(Role.ADMIN)).willReturn(false);
        AdminInitializer initializer =
                new AdminInitializer(userRepository, passwordEncoder, "admin@kashi.com", "short");

        initializer.run(null);

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void doesNotTakeOverAnEmailThatIsAlreadyARegularUser() {
        given(userRepository.existsByRole(Role.ADMIN)).willReturn(false);
        given(userRepository.existsByEmail("admin@kashi.com")).willReturn(true);
        AdminInitializer initializer =
                new AdminInitializer(userRepository, passwordEncoder, "admin@kashi.com", "StrongAdminPass1");

        initializer.run(null);

        verify(userRepository, never()).save(any(User.class));
    }
}
