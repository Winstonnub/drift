package com.drift.tracks;
import java.util.List;


import com.drift.security.CurrentUser;
import com.drift.syllabus.SyllabusResponse;
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

@RestController
@RequestMapping("/tracks")
@RequiredArgsConstructor
public class TrackController {

    private final TrackService trackService;

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public CreateTrackResponse createTrack(
        @AuthenticationPrincipal CurrentUser currentUser,
        @Valid @RequestBody CreateTrackRequest request
    ) {
        return trackService.createTrack(currentUser.id(), request);
    }


    @GetMapping
    public List<TrackResponse> getTracks(
        @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return trackService.getTracks(currentUser.id());
    }

    @GetMapping("/{id}")
    public TrackResponse getTrack(
        @AuthenticationPrincipal CurrentUser currentUser,
        @PathVariable String id
    ) {
        return trackService.getTrack(currentUser.id(), id);
    }



    @GetMapping("/{id}/syllabus")
    public SyllabusResponse getSyllabus(
        @AuthenticationPrincipal CurrentUser currentUser,
        @PathVariable String id
    ) {
        return trackService.getSyllabus(currentUser.id(), id);
    }

    @GetMapping("/{id}/syllabus-status")
    public SyllabusStatusResponse getSyllabusStatus(
        @AuthenticationPrincipal CurrentUser currentUser,
        @PathVariable String id
    ) {
        return trackService.getSyllabusStatus(currentUser.id(), id);
    }

}