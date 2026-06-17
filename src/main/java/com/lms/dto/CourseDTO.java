package com.lms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CourseDTO {
    private Long id;
    private String title;
    private String description;
    private String courseCode;
    private Integer durationDays;
    private Double totalFees;
    private String facultyName;
    private Boolean active;
    private LocalDateTime createdAt;
}
