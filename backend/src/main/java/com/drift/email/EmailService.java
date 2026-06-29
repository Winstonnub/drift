package com.drift.email;

import com.drift.auth.User;
import com.drift.lessons.Lesson;
import com.drift.tracks.Track;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EmailService {

    @Qualifier("resendRestClient") // Tell Spring which ResstClient to inject (anthropic or Resend)
    private final RestClient resendRestClient;

    private final ResendProperties properties;

    private final LessonEmailRenderer lessonEmailRenderer;

    public String sendLessonEmail(User user, Track track, Lesson lesson) {
        if (properties.apiKey() == null || properties.apiKey().isBlank()) {
            throw new EmailException("RESEND_API_KEY is not configured");
        }

        String lessonUrl = buildLessonUrl(track, lesson);

        ResendSendEmailRequest request = new ResendSendEmailRequest(
            properties.from(),
            List.of(user.getEmail()),
            "Drift: " + lesson.getTitle(),
            lessonEmailRenderer.renderHtml(track, lesson, lessonUrl),
            lessonEmailRenderer.renderText(track, lesson, lessonUrl)
        );

        try {
            ResendSendEmailResponse response = resendRestClient
                .post()
                .uri("/emails")
                .header("Idempotency-Key", "lesson-email-" + lesson.getId())
                .body(request)
                .retrieve()
                .body(ResendSendEmailResponse.class);

            if (response == null || response.id() == null || response.id().isBlank()) {
                throw new EmailException("Resend returned an empty email id");
            }

            return response.id();
        } catch (RestClientResponseException exception) {
            throw new EmailException(
                "Resend API call failed with status " + exception.getStatusCode().value(),
                exception
            );
        } catch (RestClientException exception) {
            throw new EmailException("Resend API call failed", exception);
        }
    }

    private String buildLessonUrl(Track track, Lesson lesson) {
        return properties.appUrl()
            + "/tracks/"
            + track.getId()
            + "/lessons/"
            + lesson.getId();
    }

}