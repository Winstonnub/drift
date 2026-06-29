package com.drift.lessons;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface LessonRepository extends MongoRepository<Lesson, String> {
    // builds the MongoDB query with the method name
    // find lessons where trackId matches, ordered by deliveredAt descending
    List<Lesson> findByTrackIdOrderByDeliveredAtDesc(String trackId);
    // supports idempotency
    Optional<Lesson> findByTrackIdAndSyllabusItemIndex(String trackId, Integer syllabusItemIndex);

}