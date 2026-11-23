package com.lms.dto.admin;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One course line in the admin platform report.
 */
@Data
@NoArgsConstructor
public class AdminCourseReportRowDTO {

    private Long courseId;
    private String title;
    private String courseCode;
    private Boolean active;

    private Long facultyId;
    private String facultyName;

    private long students;
    private long activeStudents;
    private long completedStudents;

    private long exams;
    private long results;
    private long passedResults;
    private long failedResults;
    private double passPercentage;
    private double averagePercentage;
}
