package com.lms.dto.admin;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * One exam approval request row on the admin Exam Approvals page.
 */
@Data
@NoArgsConstructor
public class AdminApprovalRowDTO {

    private Long id;

    private Long studentId;
    private String studentName;
    private String email;
    private Boolean studentActive;

    private Long courseId;
    private String courseTitle;
    private String courseCode;

    private Long facultyId;
    private String facultyName;

    private String status;
    private LocalDateTime requestedAt;
    private LocalDateTime reviewedAt;
    private String comment;
}
