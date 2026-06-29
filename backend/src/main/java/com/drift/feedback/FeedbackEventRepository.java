package com.drift.feedback;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface FeedbackEventRepository extends MongoRepository<FeedbackEvent, String> {
}
// gives us a lot of dataabse methods for free
// e.g. save(), findByID(), findAll()