package com.lms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CodingChallengeDTO {

    private Long id;
    private Long courseId;
    private String courseTitle;
    private String title;
    private String description;
    private String problemStatement;
    private String starterCode;
    private String expectedOutput;
    private String sampleInput;
    private String sampleOutput;
    private String constraints;
    private String difficulty;
    private Integer marks;
    private Boolean active;
}
