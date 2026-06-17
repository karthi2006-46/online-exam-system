package com.lms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExamAttemptDTO {
    private Long id;
    private Long examId;
    private Long studentId;
    private String status;
    private String answers;
}
