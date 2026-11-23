package com.lms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payload used by faculty to create or update a coding challenge.
 * The course is referenced by id so the client never has to send
 * a nested Course object.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CodingChallengeRequest {

    private Long courseId;
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
