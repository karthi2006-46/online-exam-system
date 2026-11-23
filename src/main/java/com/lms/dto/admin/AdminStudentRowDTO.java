package com.lms.dto.admin;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * One row in the admin Student Management table.
 */
@Data
@NoArgsConstructor
public class AdminStudentRowDTO {

    private Long studentId;
    private String firstName;
    private String lastName;
    private String name;
    private String email;
    private String phone;
    private Boolean active;
    private LocalDateTime createdAt;

    private long courseCount;
    private long activeCourses;
    private long completedCourses;

    private long totalResults;
    private long passedResults;
    private long failedResults;
}
