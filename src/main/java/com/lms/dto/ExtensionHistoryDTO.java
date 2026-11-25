package com.lms.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * One extension request row on the faculty "Request history" table.
 * Read-only projection; it does not replace ExtensionRequestDTO.
 */
@Data
@NoArgsConstructor
public class ExtensionHistoryDTO {

    private Long id;
    private Long enrollmentId;

    private Long studentId;
    private String studentName;
    private String studentEmail;

    private Long courseId;
    private String courseTitle;

    private LocalDateTime originalDueDate;

    private Integer daysRequested;
    private String reason;
    private LocalDateTime requestedAt;

    private String status;

    private String reviewedByName;
    private LocalDateTime reviewedAt;
    private String rejectionReason;
}
