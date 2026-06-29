package com.drift.kafka;

public final class KafkaTopics {

    public static final String SYLLABUS_GENERATE = "syllabus.generate";

    public static final String LESSON_GENERATE = "lesson.generate";

    public static final String LESSON_DELIVER_EMAIL = "lesson.deliver.email";

    public static final String FEEDBACK_SUBMITTED = "feedback.submitted";

    public static final String SYLLABUS_GENERATE_DLQ = "syllabus.generate.dlq";

    public static final String LESSON_GENERATE_DLQ = "lesson.generate.dlq";

    public static final String LESSON_DELIVER_EMAIL_DLQ = "lesson.deliver.email.dlq";

    public static final String FEEDBACK_SUBMITTED_DLQ = "feedback.submitted.dlq";

    private KafkaTopics() {
    }

}