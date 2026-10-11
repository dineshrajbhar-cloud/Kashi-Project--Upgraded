package com.kashi.portal.common;

import com.kashi.portal.auth.JwtService;
import com.kashi.portal.place.PlaceController;
import com.kashi.portal.place.PlaceService;
import com.kashi.portal.place.dto.PlaceRequest;
import com.kashi.portal.support.TestUsers;
import com.kashi.portal.support.WebTestSetup;
import com.kashi.portal.user.Role;
import com.kashi.portal.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PlaceController.class)
@WebTestSetup
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private PlaceService placeService;
    @MockitoBean
    private UserRepository userRepository;

    @Test
    void unexpectedErrorsReturn500WithoutLeakingInternalDetails() throws Exception {
        given(placeService.getPlace(1L))
                .willThrow(new RuntimeException("jdbc:mysql://secret-host:3306 password=hunter2"));

        mockMvc.perform(get("/api/places/1"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value("Something went wrong on the server"))
                .andExpect(content().string(not(containsString("hunter2"))))
                .andExpect(content().string(not(containsString("secret-host"))))
                .andExpect(jsonPath("$.stackTrace").doesNotExist());
    }

    @Test
    void databaseConstraintViolationsReturn409WithoutExposingSql() throws Exception {
        given(placeService.createPlace(any(PlaceRequest.class)))
                .willThrow(new DataIntegrityViolationException("Duplicate entry 'x' for key 'uk_something'"));

        mockMvc.perform(post("/api/places")
                        .header(HttpHeaders.AUTHORIZATION, bearerFor("admin@kashi.com", Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Assi Ghat", "type": "GHAT", "shortDescription": "Southern ghat"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("The request conflicts with existing data"))
                .andExpect(content().string(not(containsString("Duplicate entry"))));
    }

    @Test
    void unknownUrlsReturn404InTheStandardShape() throws Exception {
        mockMvc.perform(get("/api/does-not-exist")
                        .header(HttpHeaders.AUTHORIZATION, bearerFor("user@kashi.com", Role.USER)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Resource not found"));
    }

    @Test
    void unsupportedHttpMethodsReturn405() throws Exception {
        mockMvc.perform(patch("/api/places/1")
                        .header(HttpHeaders.AUTHORIZATION, bearerFor("user@kashi.com", Role.USER)))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405))
                .andExpect(jsonPath("$.message").value("Method PATCH is not supported for this endpoint"));
    }

    @Test
    void unsupportedContentTypesReturn415() throws Exception {
        mockMvc.perform(post("/api/places")
                        .header(HttpHeaders.AUTHORIZATION, bearerFor("admin@kashi.com", Role.ADMIN))
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("name=Assi"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.status").value(415));
    }

    private String bearerFor(String email, Role role) {
        given(userRepository.findByEmail(email)).willReturn(Optional.of(TestUsers.withRole(email, role)));
        return "Bearer " + jwtService.generateToken(email);
    }
}
