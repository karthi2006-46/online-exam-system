package com.lms.dto.admin;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One faculty line in the admin workload report.
 */
@Data
@NoArgsConstructor
public class AdminFacultyWorkloadDTO {

    private Long facultyId;
    private String name;
    private String email;
    private Boolean active;

    private long courses;
    private long students;
    private long exams;

    private long pendingExamApprovals;
    private long pendingExtensions;
    private long pendingCodingReviews;
}
