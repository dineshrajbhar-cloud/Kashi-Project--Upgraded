package com.kashi.portal.auth;

import com.kashi.portal.auth.dto.AuthResponse;
import com.kashi.portal.auth.dto.LoginRequest;
import com.kashi.portal.auth.dto.RegisterRequest;
import com.kashi.portal.common.EmailAlreadyExistsException;
import com.kashi.portal.support.WebTestSetup;
import com.kashi.portal.user.Role;
import com.kashi.portal.user.UserRepository;
import com.kashi.portal.user.dto.UserResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@WebTestSetup
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;
    @MockitoBean
    private UserRepository userRepository;

    private static final String VALID_REGISTRATION = """
            {"name": "Asha Verma", "email": "asha@example.com", "password": "StrongPass123",
             "city": "Varanasi", "gender": "FEMALE"}
            """;

    @Test
    void registrationReturns201WithTokenAndSafeUserDetails() throws Exception {
        given(authService.register(any(RegisterRequest.class))).willReturn(authResponse());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REGISTRATION))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.email").value("asha@example.com"))
                .andExpect(jsonPath("$.user.role").value("USER"))
                .andExpect(jsonPath("$.user.password").doesNotExist());
    }

    @Test
    void registrationWithInvalidFieldsReturns400WithFieldErrors() throws Exception {
        String invalid = """
                {"name": "A", "email": "not-an-email", "password": "short"}
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalid))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.path").value("/api/auth/register"))
                .andExpect(jsonPath("$.fieldErrors.name").value("Name must be between 2 and 100 characters"))
                .andExpect(jsonPath("$.fieldErrors.email").value("Enter a valid email address"))
                .andExpect(jsonPath("$.fieldErrors.password").value("Password must be between 8 and 72 characters"));

        verifyNoInteractions(authService);
    }

    @Test
    void registrationWithAnUnknownGenderReturns400() throws Exception {
        String body = """
                {"name": "Asha Verma", "email": "asha@example.com", "password": "StrongPass123", "gender": "UNKNOWN"}
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request body is missing or not valid JSON"));
    }

    @Test
    void registrationWithADuplicateEmailReturns409() throws Exception {
        given(authService.register(any(RegisterRequest.class)))
                .willThrow(new EmailAlreadyExistsException("An account with this email already exists"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REGISTRATION))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("An account with this email already exists"));
    }

    @Test
    void malformedJsonReturns400() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ this is not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request body is missing or not valid JSON"));
    }

    @Test
    void loginReturns200WithToken() throws Exception {
        given(authService.login(any(LoginRequest.class))).willReturn(authResponse());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "asha@example.com", "password": "StrongPass123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"));
    }

    @Test
    void loginWithWrongCredentialsReturns401WithAGenericMessage() throws Exception {
        given(authService.login(any(LoginRequest.class))).willThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "asha@example.com", "password": "wrong-password"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void loginWithMissingPasswordReturns400() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "asha@example.com"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.password").value("Password is required"));

        verifyNoInteractions(authService);
    }

    private AuthResponse authResponse() {
        UserResponse user = new UserResponse(
                1L, "Asha Verma", "asha@example.com", "Varanasi", null, Role.USER, LocalDateTime.now());
        return new AuthResponse("jwt-token", "Bearer", 3600L, user);
    }
}
