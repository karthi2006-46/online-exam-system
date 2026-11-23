package com.lms.dto.admin;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Course summary shown on admin faculty and course views.
 */
@Data
@NoArgsConstructor
public class AdminCourseSummaryDTO {

    private Long id;
    private String title;
    private String courseCode;
    private Boolean active;

    private Long facultyId;
    private String facultyName;

    private long totalStudents;
    private long activeStudents;
    private long inactiveStudents;
    private long completedStudents;

    private long totalExams;
    private long totalMaterials;
    private long totalChallenges;

    private double averagePercentage;
}
