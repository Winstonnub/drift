package com.drift.tracks;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class TrackNotFoundException extends RuntimeException {

    public TrackNotFoundException(String id) {
        super("Track not found: " + id); // create useful message containing the missing ID
    }

}