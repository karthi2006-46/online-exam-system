package com.lms.dto;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class MyCourseDTO {

    private Long courseId;
    private String courseTitle;
    private Integer durationDays;
    private Double progress;
    private String status;
    private LocalDateTime dueDate;
}