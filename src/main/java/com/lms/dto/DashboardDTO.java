package com.lms.dto;

import lombok.Data;

@Data
public class DashboardDTO {

    private long totalCourses;
    private long completedCourses;
    private long pendingExams;
    private int overallProgress;

}