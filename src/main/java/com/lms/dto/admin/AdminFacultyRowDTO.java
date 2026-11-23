package com.lms.dto.admin;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * One row in the admin Faculty Management table.
 */
@Data
@NoArgsConstructor
public class AdminFacultyRowDTO {

    private Long facultyId;
    private String firstName;
    private String lastName;
    private String name;
    private String email;
    private String phone;
    private Boolean active;
    private LocalDateTime createdAt;

    private List<Long> courseIds = new ArrayList<>();
    private List<String> assignedCourses = new ArrayList<>();

    private long totalStudents;
    private long activeStudents;
    private long inactiveStudents;
    private long completedStudents;

    private long pendingExamApprovals;
    private long pendingExtensionRequests;

    private long totalResults;
    private double passPercentage;
}
