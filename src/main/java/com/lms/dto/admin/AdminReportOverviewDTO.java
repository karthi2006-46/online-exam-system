package com.lms.dto.admin;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Combined data behind the admin Reports page.
 */
@Data
@NoArgsConstructor
public class AdminReportOverviewDTO {

    private LocalDateTime generatedAt = LocalDateTime.now();

    private long totalCourses;
    private long activeCourses;
    private long totalFaculty;
    private long totalStudents;
    private long totalEnrollments;

    private long activeEnrollments;
    private long inactiveEnrollments;
    private long completedEnrollments;
    private long droppedEnrollments;

    private long totalResults;
    private long passedResults;
    private long failedResults;
    private double passPercentage;

    private long pendingExamApprovals;
    private long pendingExtensionRequests;
    private long pendingCodingSubmissions;

    private List<AdminCourseReportRowDTO> courses = new ArrayList<>();
    private List<AdminFacultyWorkloadDTO> faculty = new ArrayList<>();
}
