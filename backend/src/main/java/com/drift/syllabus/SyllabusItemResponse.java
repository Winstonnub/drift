package com.drift.syllabus;

import java.util.List;

public record SyllabusItemResponse(

    Integer index,

    String title,

    String summary,

    List<String> prerequisites,

    SyllabusItemStatus status

) {
}
