package com.lms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExamDTO {
    private Long id;
    private Long courseId;
    private String title;
    private String description;
    private Integer totalQuestions;
    private Integer totalMarks;
    private Integer passingMarks;
    private Integer durationMinutes;
    private Boolean active;
    private Boolean published;
}
