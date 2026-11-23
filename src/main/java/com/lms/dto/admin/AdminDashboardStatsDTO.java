package com.lms.dto.admin;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Counters for the admin dashboard.
 */
@Data
@NoArgsConstructor
public class AdminDashboardStatsDTO {

    private long totalFaculty;
    private long activeFaculty;
    private long inactiveFaculty;

    private long totalStudents;
    private long activeStudents;
    private long inactiveStudents;

    private long totalCourses;
    private long activeCourses;

    private long totalActiveEnrollments;
    private long totalInactiveEnrollments;
    private long totalCompletedEnrollments;
    private long totalDroppedEnrollments;

    private long totalExamApprovals;
    private long pendingExamApprovals;
    private long approvedExamRequests;
    private long rejectedExamRequests;

    private long totalExtensionRequests;
    private long pendingExtensionRequests;
    private long approvedExtensionRequests;
    private long rejectedExtensionRequests;

    private long pendingCodingSubmissions;

    private long totalResults;
    private long passedResults;
    private long failedResults;
    private double passPercentage;
}
