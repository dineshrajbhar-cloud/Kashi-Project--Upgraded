package com.kashi.portal.feedback;

import com.kashi.portal.common.PageResponse;
import com.kashi.portal.common.Paging;
import com.kashi.portal.feedback.dto.FeedbackRequest;
import com.kashi.portal.feedback.dto.FeedbackResponse;
import com.kashi.portal.user.User;
import com.kashi.portal.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final UserRepository userRepository;

    /**
     * @param userEmail email of the logged-in user, or null for a guest
     */
    @Transactional
    public FeedbackResponse submit(FeedbackRequest request, String userEmail) {
        User user = userEmail == null ? null : userRepository.findByEmail(userEmail).orElse(null);

        Feedback feedback = Feedback.builder()
                .name(request.name())
                .email(request.email())
                .message(request.message())
                .user(user)
                .build();

        return FeedbackResponse.from(feedbackRepository.save(feedback));
    }

    public PageResponse<FeedbackResponse> getFeedback(int page, int size) {
        Pageable pageable = Paging.of(page, size, "createdAt,desc", Set.of("createdAt"));
        return PageResponse.from(feedbackRepository.findAll(pageable).map(FeedbackResponse::from));
    }
}
