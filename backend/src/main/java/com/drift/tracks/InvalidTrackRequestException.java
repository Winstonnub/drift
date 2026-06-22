package com.drift.tracks;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST) // if exception is thrown from controller req, return 400
public class InvalidTrackRequestException extends RuntimeException {

    public InvalidTrackRequestException(String message) {
        super(message);
    }

}