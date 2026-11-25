package com.lms.dto.admin;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Complete admin report for one student.
 * Built from live repository data and reused by the Excel / PDF exports.
 */
@Data
@NoArgsConstructor
public class AdminStudentReportDTO {

    private Long studentId;
    private String studentName;
    private String email;
    private String phone;
    private String address;
    private String accountStatus;
    private LocalDateTime registeredAt;

    private int enrolledCourses;
    private int activeCourses;
    private int inactiveCourses;
    private int completedCourses;

    private int totalResults;
    private int passedResults;
    private int failedResults;
    private double averagePercentage;

    private LocalDateTime generatedAt;
    private List<CourseSection> courses = new ArrayList<>();

    @Data
    @NoArgsConstructor
    public static class CourseSection {

        private Long courseId;
        private String courseTitle;
        private String courseCode;
        private String facultyName;

        private Long enrollmentId;
        private String enrollmentStatus;
        private LocalDateTime enrollmentDate;
        private LocalDateTime dueDate;
        private Double completionPercentage;
        private Double overallProgress;

        private Integer materialsCompleted = 0;
        private Integer materialsTotal = 0;

        private String examApprovalStatus = "NOT_REQUESTED";

        private Integer codingSubmissions = 0;
        private Integer codingApproved = 0;

        private Integer examsAttempted = 0;
        private Integer examsPassed = 0;
        private Integer examsFailed = 0;
        private Double averagePercentage = 0.0;

        private List<CodingRow> codingHistory = new ArrayList<>();
        private List<ExamRow> examResults = new ArrayList<>();
        private List<ExtensionRow> extensionHistory = new ArrayList<>();
    }

    @Data
    @NoArgsConstructor
    public static class CodingRow {
        private Long submissionId;
        private String challengeTitle;
        private LocalDateTime submittedAt;
        private Integer testCasesPassed;
        private Integer totalTestCases;
        private Double percentage;
        private String reviewStatus;
        private String facultyFeedback;
        private String reviewedByName;
        private LocalDateTime reviewedAt;
    }

    @Data
    @NoArgsConstructor
    public static class ExamRow {
        private Long resultId;
        private String examTitle;
        private Integer obtainedMarks;
        private Integer totalMarks;
        private Double percentage;
        private String status;
        private LocalDateTime submittedAt;
    }

    @Data
    @NoArgsConstructor
    public static class ExtensionRow {
        private Long requestId;
        private LocalDateTime originalDueDate;
        private Integer daysRequested;
        private String reason;
        private LocalDateTime requestedAt;
        private String status;
        private String reviewedByName;
        private LocalDateTime reviewedAt;
        private String rejectionReason;
    }
}
