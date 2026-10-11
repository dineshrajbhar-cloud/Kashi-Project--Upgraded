package com.kashi.portal.feedback;

import com.kashi.portal.auth.JwtService;
import com.kashi.portal.common.PageResponse;
import com.kashi.portal.feedback.dto.FeedbackRequest;
import com.kashi.portal.feedback.dto.FeedbackResponse;
import com.kashi.portal.support.TestUsers;
import com.kashi.portal.support.WebTestSetup;
import com.kashi.portal.user.Role;
import com.kashi.portal.user.UserRepository;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FeedbackController.class)
@WebTestSetup
class FeedbackControllerTest {

    private static final String VALID_FEEDBACK = """
            {"name": "Asha", "email": "asha@example.com", "message": "Lovely guide to the ghats."}
            """;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private FeedbackService feedbackService;
    @MockitoBean
    private UserRepository userRepository;

    @Test
    void guestsCanSendFeedback() throws Exception {
        given(feedbackService.submit(any(FeedbackRequest.class), isNull())).willReturn(response(null));

        mockMvc.perform(post("/api/feedback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_FEEDBACK))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Lovely guide to the ghats."))
                .andExpect(jsonPath("$.userId").doesNotExist());

        verify(feedbackService).submit(any(FeedbackRequest.class), isNull());
    }

    @Test
    void feedbackFromALoggedInUserIsPassedToTheServiceWithTheirEmail() throws Exception {
        given(userRepository.findByEmail("user@kashi.com"))
                .willReturn(Optional.of(TestUsers.withRole("user@kashi.com", Role.USER)));
        given(feedbackService.submit(any(FeedbackRequest.class), eq("user@kashi.com"))).willReturn(response(1L));

        mockMvc.perform(post("/api/feedback")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.generateToken("user@kashi.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_FEEDBACK))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(1));
    }

    @Test
    void anInvalidTokenOnTheFeedbackFormIsTreatedAsAGuest() throws Exception {
        given(feedbackService.submit(any(FeedbackRequest.class), isNull())).willReturn(response(null));

        mockMvc.perform(post("/api/feedback")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer garbage.token.value")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_FEEDBACK))
                .andExpect(status().isCreated());

        verify(feedbackService).submit(any(FeedbackRequest.class), isNull());
    }

    @Test
    void invalidFeedbackReturns400WithFieldErrors() throws Exception {
        String invalid = "{\"name\": \"\", \"email\": \"nope\", \"message\": \"" + "x".repeat(2001) + "\"}";

        mockMvc.perform(post("/api/feedback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalid))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.name").value("Name is required"))
                .andExpect(jsonPath("$.fieldErrors.email").value("Enter a valid email address"))
                .andExpect(jsonPath("$.fieldErrors.message").value("Message must be at most 2000 characters"));

        verifyNoInteractions(feedbackService);
    }

    @Test
    void emptyMessageIsRejected() throws Exception {
        mockMvc.perform(post("/api/feedback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Asha\", \"email\": \"asha@example.com\", \"message\": \"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.message").value("Message is required"));
    }

    @Test
    void onlyAnAdminCanReadFeedback() throws Exception {
        given(userRepository.findByEmail("user@kashi.com"))
                .willReturn(Optional.of(TestUsers.withRole("user@kashi.com", Role.USER)));
        given(userRepository.findByEmail("admin@kashi.com"))
                .willReturn(Optional.of(TestUsers.withRole("admin@kashi.com", Role.ADMIN)));
        given(feedbackService.getFeedback(anyInt(), anyInt()))
                .willReturn(new PageResponse<>(List.of(response(null)), 0, 10, 1, 1, true, true));

        mockMvc.perform(get("/api/feedback"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/feedback")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.generateToken("user@kashi.com")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/feedback")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.generateToken("admin@kashi.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].email").value("asha@example.com"));
    }

    private FeedbackResponse response(Long userId) {
        return new FeedbackResponse(1L, "Asha", "asha@example.com", "Lovely guide to the ghats.",
                userId, LocalDateTime.now());
    }
}
