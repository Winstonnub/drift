package com.drift.tracks;

import com.drift.security.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    public TrackResponse createTrack(
        @AuthenticationPrincipal CurrentUser currentUser,// pulls the authed principal out of Spring Security
        @Valid @RequestBody CreateTrackRequest request
    ) {
        return trackService.createTrack(currentUser.id(), request);
    }

    @GetMapping("/{id}")
    public TrackResponse getTrack(
        @AuthenticationPrincipal CurrentUser currentUser,
        @PathVariable String id
    ) {
        return trackService.getTrack(currentUser.id(), id);
    }
}