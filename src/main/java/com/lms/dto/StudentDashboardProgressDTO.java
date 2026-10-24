package com.lms.dto;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class StudentDashboardProgressDTO {

    private long totalCourses;
    private long completedCourses;
    private long pendingExams;

    private Double overallProgress;

    private List<CourseProgressDTO> courses = new ArrayList<>();
}