package com.lms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentProfileDTO {

    private UserDTO user;

    private List<CourseInfo> courses;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CourseInfo {

        private Long enrollmentId;

        private Long courseId;

        private String courseTitle;

        private String courseCode;

        private String description;

        private Integer durationDays;

        private Double totalFees;

        private LocalDateTime enrollmentDate;

        private LocalDateTime dueDate;

        private String status;

        private Double completionPercentage;
    }
}