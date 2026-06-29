package com.drift.feedback;

import com.drift.security.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tracks/{trackId}/lessons/{lessonId}/feedback")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public FeedbackResponse submitFeedback(
        @AuthenticationPrincipal CurrentUser currentUser,
        @PathVariable String trackId,
        @PathVariable String lessonId,
        @Valid @RequestBody FeedbackRequest request
    ) {
        return feedbackService.submitFeedback(
            currentUser.id(),
            trackId,
            lessonId,
            request
        );
    }

}