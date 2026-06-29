package com.drift.syllabus;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class SyllabusNotFoundException extends RuntimeException {

    public SyllabusNotFoundException(String trackId) {
        super("Syllabus not found for track: " + trackId);
    }

}