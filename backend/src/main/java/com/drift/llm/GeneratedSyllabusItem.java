package com.drift.llm;

import java.util.List;

public record GeneratedSyllabusItem(

    Integer index,

    String title,

    String summary,

    List<String> prerequisites

) {
}