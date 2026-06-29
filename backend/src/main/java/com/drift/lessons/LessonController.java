package com.drift.lessons;

import com.drift.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/tracks/{trackId}/lessons")
@RequiredArgsConstructor
public class LessonController {

    private final LessonService lessonService;

    @PostMapping("/generate-now") // POST 
    @ResponseStatus(HttpStatus.ACCEPTED) // 202
    public void generateLessonNow(
        @AuthenticationPrincipal CurrentUser currentUser,
        @PathVariable String trackId
    ) {
        lessonService.requestLessonNow(currentUser.id(), trackId);
    }

    @GetMapping
    public List<LessonResponse> getLessons(
        @AuthenticationPrincipal CurrentUser currentUser,
        @PathVariable String trackId
    ) {
        return lessonService.getLessons(currentUser.id(), trackId);
    }

}