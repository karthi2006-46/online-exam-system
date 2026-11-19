package com.lms.dto;

import com.lms.entity.CodingSubmission;
import java.time.LocalDateTime;

public class CodingSubmissionResponseDTO {

    private Long id;
    private StudentInfo student;
    private ChallengeInfo challenge;

    private String language;
    private String sourceCode;
    private String executionOutput;
    private String executionError;

    private Integer testCasesPassed;
    private Integer totalTestCases;

    private Double executionTime;
    private Double memoryUsed;

    private String status;
    private String facultyFeedback;

    private LocalDateTime submittedAt;
    private LocalDateTime reviewedAt;

    public static CodingSubmissionResponseDTO fromEntity(
            CodingSubmission submission) {

        CodingSubmissionResponseDTO dto =
                new CodingSubmissionResponseDTO();

        dto.setId(submission.getId());
        dto.setLanguage(submission.getLanguage());
        dto.setSourceCode(submission.getSourceCode());
        dto.setExecutionOutput(submission.getExecutionOutput());
        dto.setExecutionError(submission.getExecutionError());
        dto.setTestCasesPassed(submission.getTestCasesPassed());
        dto.setTotalTestCases(submission.getTotalTestCases());
        dto.setExecutionTime(submission.getExecutionTime());
        dto.setMemoryUsed(submission.getMemoryUsed());

        dto.setStatus(
                submission.getStatus() != null
                        ? submission.getStatus().name()
                        : null
        );

        dto.setFacultyFeedback(submission.getFacultyFeedback());
        dto.setSubmittedAt(submission.getSubmittedAt());
        dto.setReviewedAt(submission.getReviewedAt());

        if (submission.getStudent() != null) {
            StudentInfo student = new StudentInfo();

            student.setId(submission.getStudent().getId());
            student.setFirstName(submission.getStudent().getFirstName());
            student.setLastName(submission.getStudent().getLastName());
            student.setEmail(submission.getStudent().getEmail());

            dto.setStudent(student);
        }

        if (submission.getChallenge() != null) {
            ChallengeInfo challenge = new ChallengeInfo();

            challenge.setId(submission.getChallenge().getId());
            challenge.setTitle(submission.getChallenge().getTitle());

            if (submission.getChallenge().getCourse() != null) {
                challenge.setCourseId(
                        submission.getChallenge()
                                .getCourse().getId()
                );
            }

            dto.setChallenge(challenge);
        }

        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public StudentInfo getStudent() { return student; }
    public void setStudent(StudentInfo student) { this.student = student; }

    public ChallengeInfo getChallenge() { return challenge; }
    public void setChallenge(ChallengeInfo challenge) { this.challenge = challenge; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public String getSourceCode() { return sourceCode; }
    public void setSourceCode(String sourceCode) { this.sourceCode = sourceCode; }

    public String getExecutionOutput() { return executionOutput; }
    public void setExecutionOutput(String executionOutput) { this.executionOutput = executionOutput; }

    public String getExecutionError() { return executionError; }
    public void setExecutionError(String executionError) { this.executionError = executionError; }

    public Integer getTestCasesPassed() { return testCasesPassed; }
    public void setTestCasesPassed(Integer testCasesPassed) { this.testCasesPassed = testCasesPassed; }

    public Integer getTotalTestCases() { return totalTestCases; }
    public void setTotalTestCases(Integer totalTestCases) { this.totalTestCases = totalTestCases; }

    public Double getExecutionTime() { return executionTime; }
    public void setExecutionTime(Double executionTime) { this.executionTime = executionTime; }

    public Double getMemoryUsed() { return memoryUsed; }
    public void setMemoryUsed(Double memoryUsed) { this.memoryUsed = memoryUsed; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getFacultyFeedback() { return facultyFeedback; }
    public void setFacultyFeedback(String facultyFeedback) { this.facultyFeedback = facultyFeedback; }

    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }

    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }

    public static class StudentInfo {
        private Long id;
        private String firstName;
        private String lastName;
        private String email;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }

        public String getLastName() { return lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }

    public static class ChallengeInfo {
        private Long id;
        private String title;
        private Long courseId;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public Long getCourseId() { return courseId; }
        public void setCourseId(Long courseId) { this.courseId = courseId; }
    }
}