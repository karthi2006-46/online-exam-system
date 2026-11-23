package com.lms.dto.admin;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * One student/course row on the Student Performance page.
 */
@Data
@NoArgsConstructor
public class AdminPerformanceRowDTO {

    private Long enrollmentId;

    private Long studentId;
    private String studentName;
    private String email;
    private Boolean studentActive;

    private Long courseId;
    private String courseTitle;
    private String courseCode;

    private Long facultyId;
    private String facultyName;

    private String enrollmentStatus;
    private LocalDateTime enrollmentDate;
    private LocalDateTime dueDate;
    private Double completionPercentage;

    private long totalMaterials;
    private long completedMaterials;
    private double materialsProgress;

    private long totalChallenges;
    private long completedChallenges;
    private double challengesProgress;
    private String codingApprovalStatus;

    private String examApprovalStatus;

    private Long latestExamId;
    private String latestExamTitle;
    private Integer latestExamMarks;
    private Integer latestExamTotalMarks;
    private Double latestExamPercentage;
    private String latestExamResult;
    private LocalDateTime latestExamDate;
    private long examAttempts;

    private Double overallProgress;
    private String overallResult;
}
