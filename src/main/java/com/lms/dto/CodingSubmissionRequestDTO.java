package com.lms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CodingSubmissionRequestDTO {

    @NotNull(message = "Challenge ID is required")
    private Long challengeId;

    @NotBlank(message = "Programming language is required")
    private String language;

    @NotBlank(message = "Source code cannot be empty")
    @Size(max = 100000, message = "Source code is too large")
    private String sourceCode;

    // GETTERS AND SETTERS

    public Long getChallengeId() {
        return challengeId;
    }

    public void setChallengeId(Long challengeId) {
        this.challengeId = challengeId;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getSourceCode() {
        return sourceCode;
    }

    public void setSourceCode(String sourceCode) {
        this.sourceCode = sourceCode;
    }
}