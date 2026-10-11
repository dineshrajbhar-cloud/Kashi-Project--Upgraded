package com.kashi.portal.feedback;

import com.kashi.portal.common.PageResponse;
import com.kashi.portal.feedback.dto.FeedbackRequest;
import com.kashi.portal.feedback.dto.FeedbackResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping("/api/feedback")
@RequiredArgsConstructor
@Tag(name = "Feedback")
public class FeedbackController {

    private final FeedbackService feedbackService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Send feedback",
            description = "Public. If a valid Bearer token is sent, the feedback is linked to that account.")
    public FeedbackResponse submitFeedback(@Valid @RequestBody FeedbackRequest request, Principal principal) {
        String userEmail = principal != null ? principal.getName() : null;
        return feedbackService.submit(request, userEmail);
    }

    @GetMapping
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "List all feedback, newest first (ADMIN)")
    public PageResponse<FeedbackResponse> getFeedback(
            @Parameter(description = "Page number, starting at 0")
            @RequestParam(name = "page", defaultValue = "0") int page,
            @Parameter(description = "Items per page (1 to 50)")
            @RequestParam(name = "size", defaultValue = "10") int size) {
        return feedbackService.getFeedback(page, size);
    }
}
