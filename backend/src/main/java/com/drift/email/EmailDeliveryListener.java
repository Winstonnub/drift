package com.drift.email;

import com.drift.auth.User;
import com.drift.auth.UserRepository;
import com.drift.kafka.KafkaTopics;
import com.drift.kafka.events.EmailDeliveryEvent;
import com.drift.lessons.Lesson;
import com.drift.lessons.LessonRepository;
import com.drift.tracks.Track;
import com.drift.tracks.TrackRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailDeliveryListener {

    private static final Logger log = LoggerFactory.getLogger(EmailDeliveryListener.class);

    private final LessonRepository lessonRepository;

    private final TrackRepository trackRepository;

    private final UserRepository userRepository;

    private final EmailService emailService;

    @KafkaListener(
        topics = KafkaTopics.LESSON_DELIVER_EMAIL,
        groupId = "drift-email-delivery"
    )
    public void deliverLessonEmail(EmailDeliveryEvent event) {
        Lesson lesson = lessonRepository.findById(event.lessonId())
            .orElseThrow(() -> new IllegalStateException("Lesson not found: " + event.lessonId()));

        Track track = trackRepository.findById(lesson.getTrackId())
            .orElseThrow(() -> new IllegalStateException("Track not found: " + lesson.getTrackId()));

        User user = userRepository.findById(lesson.getUserId())
            .orElseThrow(() -> new IllegalStateException("User not found: " + lesson.getUserId()));

        String resendEmailId = emailService.sendLessonEmail(user, track, lesson);

        log.info("Delivered lesson {} by email via Resend id {}", lesson.getId(), resendEmailId);
    }

    @KafkaListener(
        topics = KafkaTopics.LESSON_DELIVER_EMAIL_DLQ,
        groupId = "drift-email-dlq"
    )
    public void handleEmailDeliveryFailure(EmailDeliveryEvent event) {
        log.warn("Email delivery failed and moved to DLQ for lesson {}", event.lessonId());
    }

}