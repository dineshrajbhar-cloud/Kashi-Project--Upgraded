package com.kashi.portal.auth;

import com.kashi.portal.auth.dto.AuthResponse;
import com.kashi.portal.auth.dto.LoginRequest;
import com.kashi.portal.auth.dto.RegisterRequest;
import com.kashi.portal.common.BadRequestException;
import com.kashi.portal.common.EmailAlreadyExistsException;
import com.kashi.portal.support.TestUsers;
import com.kashi.portal.user.Gender;
import com.kashi.portal.user.Role;
import com.kashi.portal.user.User;
import com.kashi.portal.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtService jwtService;

    // A real encoder (lowest cost, so the tests stay fast) lets us check the stored hash for real
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, authenticationManager, jwtService);
    }

    @Test
    void registerSavesUserWithHashedPasswordAndUserRole() {
        RegisterRequest request = new RegisterRequest(
                "  Asha Verma ", " Asha@Example.COM ", "StrongPass123", "Varanasi", Gender.FEMALE);
        given(userRepository.existsByEmail("asha@example.com")).willReturn(false);
        given(userRepository.save(any(User.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(jwtService.generateToken("asha@example.com")).willReturn("jwt-token");
        given(jwtService.getExpirationSeconds()).willReturn(3600L);

        AuthResponse response = authService.register(request);

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        User user = saved.getValue();
        assertEquals("Asha Verma", user.getName());
        assertEquals("asha@example.com", user.getEmail());
        assertEquals(Role.USER, user.getRole());
        assertNotEquals("StrongPass123", user.getPassword());
        assertTrue(passwordEncoder.matches("StrongPass123", user.getPassword()));

        assertEquals("jwt-token", response.token());
        assertEquals("Bearer", response.tokenType());
        assertEquals(3600L, response.expiresInSeconds());
        assertEquals("asha@example.com", response.user().email());
        assertEquals(Role.USER, response.user().role());
    }

    @Test
    void registerRejectsDuplicateEmailWithoutSaving() {
        RegisterRequest request = new RegisterRequest("Asha", "asha@example.com", "StrongPass123", null, null);
        given(userRepository.existsByEmail("asha@example.com")).willReturn(true);

        assertThrows(EmailAlreadyExistsException.class, () -> authService.register(request));

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void registerRejectsPasswordLongerThan72Bytes() {
        RegisterRequest request = new RegisterRequest("Asha", "asha@example.com", "a".repeat(73), null, null);

        assertThrows(BadRequestException.class, () -> authService.register(request));

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void registrationRequestCannotCarryARole() {
        boolean hasRoleField = Arrays.stream(RegisterRequest.class.getRecordComponents())
                .anyMatch(component -> component.getName().equalsIgnoreCase("role"));

        assertFalse(hasRoleField, "Public registration must never accept a role from the client");
    }

    @Test
    void loginReturnsTokenForValidCredentials() {
        LoginRequest request = new LoginRequest(" Asha@Example.com ", "StrongPass123");
        User user = TestUsers.withRole("asha@example.com", Role.USER);
        given(authenticationManager.authenticate(any(Authentication.class)))
                .willReturn(new UsernamePasswordAuthenticationToken("asha@example.com", null));
        given(userRepository.findByEmail("asha@example.com")).willReturn(Optional.of(user));
        given(jwtService.generateToken("asha@example.com")).willReturn("jwt-token");
        given(jwtService.getExpirationSeconds()).willReturn(3600L);

        AuthResponse response = authService.login(request);

        ArgumentCaptor<Authentication> attempt = ArgumentCaptor.forClass(Authentication.class);
        verify(authenticationManager).authenticate(attempt.capture());
        assertEquals("asha@example.com", attempt.getValue().getName());
        assertEquals("jwt-token", response.token());
        assertEquals("asha@example.com", response.user().email());
    }

    @Test
    void loginFailsAndIssuesNoTokenForInvalidCredentials() {
        LoginRequest request = new LoginRequest("asha@example.com", "wrong-password");
        given(authenticationManager.authenticate(any(Authentication.class)))
                .willThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(BadCredentialsException.class, () -> authService.login(request));

        verify(jwtService, never()).generateToken(any());
    }
}
