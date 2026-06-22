package com.drift.tracks;

import org.springframework.data.mongodb.repository.MongoRepository;
// import Spring Data MongoDB repo interface
import java.util.List;

public interface TrackRepository extends MongoRepository<Track, String> {
// Create a Spring Data MongoDB repository for Track documents, with String as the type of the _id field
    List<Track> findByUserId(String userId);
    // find all Track doc where userId field equals given value
}