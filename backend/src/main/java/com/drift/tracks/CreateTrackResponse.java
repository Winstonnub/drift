// What we’re doing: Changing track creation to return both track and syllabus.

package com.drift.tracks;

public record CreateTrackResponse(

    TrackResponse track,

    SyllabusStatusResponse syllabusStatus

) {
}