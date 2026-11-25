package com.lms.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * One exam approval row on the faculty exam approval history table.
 */
@Data
@NoArgsConstructor
public class ExamApprovalHistoryDTO {

    private Long id;

    private Long studentId;
    private String studentName;
    private String studentEmail;

    private Long courseId;
    private String courseTitle;
    private String courseCode;

    private LocalDateTime requestedAt;

    private String status;

    private String reviewedByName;
    private LocalDateTime reviewedAt;
    private String comment;
}
