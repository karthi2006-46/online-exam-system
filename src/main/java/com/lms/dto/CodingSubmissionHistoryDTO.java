package com.lms.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * One coding submission row on the faculty coding history table.
 * Marks are derived from test cases because the entity has no marks column.
 */
@Data
@NoArgsConstructor
public class CodingSubmissionHistoryDTO {

    private Long id;

    private Long studentId;
    private String studentName;
    private String studentEmail;

    private Long courseId;
    private String courseTitle;

    private Long challengeId;
    private String challengeTitle;

    private LocalDateTime submittedAt;

    private Integer testCasesPassed;
    private Integer totalTestCases;
    private Double percentage;

    private String status;
    private String reviewStatus;

    private String facultyFeedback;
    private String reviewedByName;
    private LocalDateTime reviewedAt;
}
