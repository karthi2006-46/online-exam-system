package com.lms.dto;

import lombok.Data;

@Data
public class CourseProgressDTO {

    private Long courseId;
    private String courseTitle;

    private Integer courseDurationDays;
    private Long elapsedDays;
    private Double durationProgress;

    private Long completedMaterials;
    private Long totalMaterials;
    private Double materialsProgress;

    private Long completedChallenges;
    private Long totalChallenges;
    private Double challengesProgress;

    private Long completedExams;
    private Long totalExams;
    private Double examsProgress;

    private Double overallProgress;
    private String status;
}