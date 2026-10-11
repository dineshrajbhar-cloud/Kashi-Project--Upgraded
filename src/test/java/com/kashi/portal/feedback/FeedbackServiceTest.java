package com.kashi.portal.feedback;

import com.kashi.portal.feedback.dto.FeedbackRequest;
import com.kashi.portal.feedback.dto.FeedbackResponse;
import com.kashi.portal.support.TestUsers;
import com.kashi.portal.user.Role;
import com.kashi.portal.user.User;
import com.kashi.portal.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class FeedbackServiceTest {

    @Mock
    private FeedbackRepository feedbackRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private FeedbackService feedbackService;

    private final FeedbackRequest request =
            new FeedbackRequest(" Asha ", " Asha@Example.com ", " Great guide ");

    @Test
    void guestFeedbackIsSavedWithoutAUser() {
        given(feedbackRepository.save(any(Feedback.class))).willAnswer(invocation -> invocation.getArgument(0));

        FeedbackResponse response = feedbackService.submit(request, null);

        ArgumentCaptor<Feedback> saved = ArgumentCaptor.forClass(Feedback.class);
        verify(feedbackRepository).save(saved.capture());
        assertNull(saved.getValue().getUser());
        assertEquals("Asha", saved.getValue().getName());
        assertEquals("asha@example.com", saved.getValue().getEmail());
        assertEquals("Great guide", saved.getValue().getMessage());
        assertNull(response.userId());
        verifyNoInteractions(userRepository);
    }

    @Test
    void feedbackFromALoggedInUserIsLinkedToThatUser() {
        User user = TestUsers.withRole("asha@example.com", Role.USER);
        given(userRepository.findByEmail("asha@example.com")).willReturn(Optional.of(user));
        given(feedbackRepository.save(any(Feedback.class))).willAnswer(invocation -> invocation.getArgument(0));

        FeedbackResponse response = feedbackService.submit(request, "asha@example.com");

        ArgumentCaptor<Feedback> saved = ArgumentCaptor.forClass(Feedback.class);
        verify(feedbackRepository).save(saved.capture());
        assertEquals(user, saved.getValue().getUser());
        assertEquals(user.getId(), response.userId());
    }
}
