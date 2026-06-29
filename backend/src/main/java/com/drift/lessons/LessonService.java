package com.drift.lessons;

import com.drift.kafka.LessonGenerationProducer;
import com.drift.tracks.InvalidTrackRequestException;
import com.drift.tracks.SyllabusGenerationStatus;
import com.drift.tracks.Track;
import com.drift.tracks.TrackNotFoundException;
import com.drift.tracks.TrackRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LessonService {

    private final TrackRepository trackRepository;

    private final LessonRepository lessonRepository;

    private final LessonGenerationProducer lessonGenerationProducer;

    public void requestLessonNow(String userId, String trackId) {
        Track track = findOwnedTrack(userId, trackId);

        if (track.getSyllabusGenerationStatus() != SyllabusGenerationStatus.READY) {
            throw new InvalidTrackRequestException("Syllabus is not ready yet");
        }

        lessonGenerationProducer.requestLessonGeneration(track.getId(), true);
    }

    public List<LessonResponse> getLessons(String userId, String trackId) {
        Track track = findOwnedTrack(userId, trackId);

        return lessonRepository.findByTrackIdOrderByDeliveredAtDesc(track.getId()).stream()
            .map(this::toResponse)
            .toList();
    }

    private Track findOwnedTrack(String userId, String trackId) {
        Track track = trackRepository.findById(trackId)
            .orElseThrow(() -> new TrackNotFoundException(trackId));

        if (!track.getUserId().equals(userId)) {
            throw new TrackNotFoundException(trackId);
        }

        return track;
    }

    private LessonResponse toResponse(Lesson lesson) {
        return new LessonResponse(
            lesson.getId(),
            lesson.getTrackId(),
            lesson.getSyllabusItemIndex(),
            lesson.getTitle(),
            lesson.getContentMarkdown(),
            lesson.getEstimatedMinutes(),
            lesson.getDeliveredAt(),
            lesson.getReadAt(),
            lesson.getFeedback()
        );
    }

}
