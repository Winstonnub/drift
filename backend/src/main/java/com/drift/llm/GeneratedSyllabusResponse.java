package com.drift.llm;

import java.util.List;

public record GeneratedSyllabusResponse(

    List<GeneratedSyllabusItem> items

) {
}