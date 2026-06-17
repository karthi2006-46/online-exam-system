package com.lms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResultDTO {
    private Long id;
    private Long examId;
    private String examTitle;
    private Integer obtainedMarks;
    private Integer totalMarks;
    private Double percentage;
    private String status;
    private Integer correctAnswers;
    private Integer wrongAnswers;
    private Integer skippedQuestions;
}
