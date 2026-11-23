package com.lms.dto.admin;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * One course row on the admin Course Management page.
 */
@Data
@NoArgsConstructor
public class AdminCourseRowDTO {

    private Long id;
    private String title;
    private String description;
    private String courseCode;
    private Integer durationDays;
    private Double totalFees;
    private Boolean active;
    private LocalDateTime createdAt;

    private Long facultyId;
    private String facultyName;
    private String facultyEmail;

    private long totalStudents;
    private long activeStudents;
    private long inactiveStudents;
    private long completedStudents;

    private long totalExams;
    private long totalMaterials;
    private long totalChallenges;

    private long totalResults;
    private long passedResults;
    private double passPercentage;
    private double averagePercentage;
}
