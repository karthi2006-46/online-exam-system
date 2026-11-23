package com.lms.dto.admin;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Create/update payload for the admin course form.
 */
@Data
@NoArgsConstructor
public class AdminCourseRequest {

    private String title;
    private String description;
    private String courseCode;
    private Integer durationDays;
    private Double totalFees;
    private Long facultyId;
    private Boolean active;
}
