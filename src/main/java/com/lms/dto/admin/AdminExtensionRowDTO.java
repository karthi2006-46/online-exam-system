package com.lms.dto.admin;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * One extension request row on the admin Extension Requests page.
 */
@Data
@NoArgsConstructor
public class AdminExtensionRowDTO {

    private Long id;
    private Long enrollmentId;

    private Long studentId;
    private String studentName;
    private String email;

    private Long courseId;
    private String courseTitle;
    private String courseCode;
    private Long facultyId;
    private String facultyName;

    private String enrollmentStatus;
    private LocalDateTime enrollmentDate;
    private LocalDateTime dueDate;

    private Integer daysRequested;
    private String status;
    private String reason;
    private String rejectionReason;

    private String reviewedByName;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;
}
