package com.drift.syllabus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyllabusItem {

    private Integer index;

    private String title;

    private String summary;

    private List<String> prerequisites;

    private SyllabusItemStatus status;

}