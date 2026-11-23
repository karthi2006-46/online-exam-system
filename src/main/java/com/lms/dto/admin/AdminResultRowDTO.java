package com.lms.dto.admin;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * One exam result row on the admin Examination Results page.
 */
@Data
@NoArgsConstructor
public class AdminResultRowDTO {

    private Long resultId;
    private Long attemptId;

    private Long studentId;
    private String studentName;
    private String email;

    private Long courseId;
    private String courseTitle;

    private Long examId;
    private String examTitle;
    private LocalDateTime examDate;

    private Integer totalMarks;
    private Integer obtainedMarks;
    private Double percentage;
    private String resultStatus;

    private Integer correctAnswers;
    private Integer wrongAnswers;
    private Integer skippedQuestions;

    private long examAttempts;
}
