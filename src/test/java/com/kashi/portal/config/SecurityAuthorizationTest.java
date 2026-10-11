package com.kashi.portal.config;

import com.kashi.portal.admin.AdminController;
import com.kashi.portal.auth.JwtService;
import com.kashi.portal.common.PageResponse;
import com.kashi.portal.gallery.GalleryController;
import com.kashi.portal.gallery.GalleryService;
import com.kashi.portal.gallery.dto.GalleryImageRequest;
import com.kashi.portal.gallery.dto.GalleryImageResponse;
import com.kashi.portal.support.TestJwt;
import com.kashi.portal.support.TestUsers;
import com.kashi.portal.support.WebTestSetup;
import com.kashi.portal.user.Role;
import com.kashi.portal.user.UserController;
import com.kashi.portal.user.UserRepository;
import com.kashi.portal.user.UserService;
import com.kashi.portal.user.dto.UserResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Exercises the real filter chain: JWT validation, 401 versus 403, role checks and CORS. */
@WebMvcTest({UserController.class, AdminController.class, GalleryController.class})
@WebTestSetup
class SecurityAuthorizationTest {

    private static final String VALID_IMAGE = """
            {"title": "Ganga Aarti", "imageUrl": "https://example.com/aarti.jpg"}
            """;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;
    @MockitoBean
    private UserService userService;
    @MockitoBean
    private GalleryService galleryService;

    @Test
    void protectedEndpointWithoutATokenReturns401InTheStandardJsonShape() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.path").value("/api/users/me"));
    }

    @Test
    void validTokenGivesAccessToTheCurrentUsersProfile() throws Exception {
        given(userRepository.findByEmail("user@kashi.com"))
                .willReturn(Optional.of(TestUsers.withRole("user@kashi.com", Role.USER)));
        given(userService.getByEmail("user@kashi.com")).willReturn(userResponse("user@kashi.com", Role.USER));

        mockMvc.perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, bearer("user@kashi.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("user@kashi.com"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void expiredTokenReturns401() throws Exception {
        given(userRepository.findByEmail("user@kashi.com"))
                .willReturn(Optional.of(TestUsers.withRole("user@kashi.com", Role.USER)));

        mockMvc.perform(get("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TestJwt.expiredToken("user@kashi.com")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenSignedWithAnotherKeyReturns401() throws Exception {
        given(userRepository.findByEmail("admin@kashi.com"))
                .willReturn(Optional.of(TestUsers.withRole("admin@kashi.com", Role.ADMIN)));

        mockMvc.perform(get("/api/admin/users")
                        .header(HttpHeaders.AUTHORIZATION,
                                "Bearer " + TestJwt.tokenSignedWithOtherKey("admin@kashi.com")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenForAUserWhoNoLongerExistsReturns401() throws Exception {
        String token = jwtService.generateToken("deleted@kashi.com");
        given(userRepository.findByEmail("deleted@kashi.com")).willReturn(Optional.empty());

        mockMvc.perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void aNonBearerAuthorizationHeaderReturns401() throws Exception {
        mockMvc.perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, "Basic dXNlcjpwYXNz"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminEndpointsReturn401ForVisitors403ForUsersAnd200ForAdmins() throws Exception {
        given(userRepository.findByEmail("user@kashi.com"))
                .willReturn(Optional.of(TestUsers.withRole("user@kashi.com", Role.USER)));
        given(userRepository.findByEmail("admin@kashi.com"))
                .willReturn(Optional.of(TestUsers.withRole("admin@kashi.com", Role.ADMIN)));
        given(userService.getUsers(anyInt(), anyInt()))
                .willReturn(new PageResponse<>(List.of(userResponse("user@kashi.com", Role.USER)), 0, 10, 1, 1, true, true));

        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/admin/users").header(HttpHeaders.AUTHORIZATION, bearer("user@kashi.com")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));

        mockMvc.perform(get("/api/admin/users").header(HttpHeaders.AUTHORIZATION, bearer("admin@kashi.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].email").value("user@kashi.com"))
                .andExpect(jsonPath("$.content[0].password").doesNotExist());
    }

    @Test
    void theRoleIsTakenFromTheDatabaseNotFromTheToken() throws Exception {
        // The token is valid, but the account has since been changed to a normal USER
        String token = jwtService.generateToken("demoted@kashi.com");
        given(userRepository.findByEmail("demoted@kashi.com"))
                .willReturn(Optional.of(TestUsers.withRole("demoted@kashi.com", Role.USER)));

        mockMvc.perform(get("/api/admin/users").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void galleryListingIsPublic() throws Exception {
        given(galleryService.getImages(anyInt(), anyInt(), anyString()))
                .willReturn(new PageResponse<>(List.of(), 0, 12, 0, 0, true, true));

        mockMvc.perform(get("/api/gallery"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());
    }

    @Test
    void onlyAnAdminCanAddOrDeleteGalleryImages() throws Exception {
        given(userRepository.findByEmail("user@kashi.com"))
                .willReturn(Optional.of(TestUsers.withRole("user@kashi.com", Role.USER)));
        given(userRepository.findByEmail("admin@kashi.com"))
                .willReturn(Optional.of(TestUsers.withRole("admin@kashi.com", Role.ADMIN)));
        given(galleryService.addImage(any(GalleryImageRequest.class)))
                .willReturn(new GalleryImageResponse(1L, "Ganga Aarti", "https://example.com/aarti.jpg", null,
                        LocalDateTime.now()));

        mockMvc.perform(post("/api/gallery").contentType(MediaType.APPLICATION_JSON).content(VALID_IMAGE))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/gallery")
                        .header(HttpHeaders.AUTHORIZATION, bearer("user@kashi.com"))
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_IMAGE))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/gallery/1").header(HttpHeaders.AUTHORIZATION, bearer("user@kashi.com")))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/gallery")
                        .header(HttpHeaders.AUTHORIZATION, bearer("admin@kashi.com"))
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_IMAGE))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Ganga Aarti"));
        mockMvc.perform(delete("/api/gallery/1").header(HttpHeaders.AUTHORIZATION, bearer("admin@kashi.com")))
                .andExpect(status().isNoContent());
    }

    @Test
    void corsAllowsTheConfiguredFrontendOrigin() throws Exception {
        mockMvc.perform(options("/api/gallery")
                        .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"));
    }

    @Test
    void corsRejectsOtherOrigins() throws Exception {
        mockMvc.perform(options("/api/gallery")
                        .header(HttpHeaders.ORIGIN, "http://evil.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isForbidden());
    }

    private String bearer(String email) {
        return "Bearer " + jwtService.generateToken(email);
    }

    private UserResponse userResponse(String email, Role role) {
        return new UserResponse(1L, "Test User", email, null, null, role, LocalDateTime.now());
    }
}
