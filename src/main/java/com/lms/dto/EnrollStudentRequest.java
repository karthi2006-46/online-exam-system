package com.lms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EnrollStudentRequest {
    private Long studentId;
    private Long courseId;
    private Integer extensionDays;
}
