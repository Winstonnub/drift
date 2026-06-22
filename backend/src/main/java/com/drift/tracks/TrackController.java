package com.drift.tracks;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController // JSON HTTP Controller
@RequestMapping("/tracks") // every endpoint in this class starts with /tracks
@RequiredArgsConstructor // Lombok creates constructor for trackSerice
public class TrackController {

    private final TrackService trackService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TrackResponse createTrack(@Valid @RequestBody CreateTrackRequest request) {
        return trackService.createTrack(request);
    }

    @GetMapping("/{id}") // Delegate GET /tracks/{id} to trackService.getTrack(id)
    public TrackResponse getTrack(@PathVariable String id) {
        return trackService.getTrack(id);
    }

}