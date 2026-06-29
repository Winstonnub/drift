package com.drift.syllabus;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface SyllabusRepository extends MongoRepository<Syllabus, String> {

    Optional<Syllabus> findByTrackId(String trackId);

}