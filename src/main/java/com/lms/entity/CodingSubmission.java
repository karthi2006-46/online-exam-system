package com.lms.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "coding_submissions",
    indexes = {
        @Index(
            name = "idx_submission_student_challenge",
            columnList = "student_id, challenge_id"
        )
    }
)
public class CodingSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Student who submitted the code
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    // Coding challenge reference
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "challenge_id", nullable = false)
    private CodingChallenge challenge;

    // Programming language: Java, Python, C, C++
    @Column(nullable = false, length = 50)
    private String language;

    // Student submitted source code
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String sourceCode;

    // Compiler execution output
    @Column(columnDefinition = "LONGTEXT")
    private String executionOutput;

    // Compilation or runtime error
    @Column(columnDefinition = "LONGTEXT")
    private String executionError;

    // Test case results
    private Integer testCasesPassed = 0;

    private Integer totalTestCases = 0;

    // Execution details
    private Double executionTime;

    private Double memoryUsed;

    // Faculty review status
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CodingSubmissionStatus status =
            CodingSubmissionStatus.PENDING_REVIEW;

    // Faculty feedback
    @Column(columnDefinition = "TEXT")
    private String facultyFeedback;

    // Faculty who reviewed the submission
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    private LocalDateTime submittedAt;

    private LocalDateTime reviewedAt;

    @PrePersist
    protected void onCreate() {
        submittedAt = LocalDateTime.now();

        if (status == null) {
            status = CodingSubmissionStatus.PENDING_REVIEW;
        }
    }

    // GETTERS AND SETTERS

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getStudent() {
        return student;
    }

    public void setStudent(User student) {
        this.student = student;
    }

    public CodingChallenge getChallenge() {
        return challenge;
    }

    public void setChallenge(CodingChallenge challenge) {
        this.challenge = challenge;
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

    public String getExecutionOutput() {
        return executionOutput;
    }

    public void setExecutionOutput(String executionOutput) {
        this.executionOutput = executionOutput;
    }

    public String getExecutionError() {
        return executionError;
    }

    public void setExecutionError(String executionError) {
        this.executionError = executionError;
    }

    public Integer getTestCasesPassed() {
        return testCasesPassed;
    }

    public void setTestCasesPassed(Integer testCasesPassed) {
        this.testCasesPassed = testCasesPassed;
    }

    public Integer getTotalTestCases() {
        return totalTestCases;
    }

    public void setTotalTestCases(Integer totalTestCases) {
        this.totalTestCases = totalTestCases;
    }

    public Double getExecutionTime() {
        return executionTime;
    }

    public void setExecutionTime(Double executionTime) {
        this.executionTime = executionTime;
    }

    public Double getMemoryUsed() {
        return memoryUsed;
    }

    public void setMemoryUsed(Double memoryUsed) {
        this.memoryUsed = memoryUsed;
    }

    public CodingSubmissionStatus getStatus() {
        return status;
    }

    public void setStatus(CodingSubmissionStatus status) {
        this.status = status;
    }

    public String getFacultyFeedback() {
        return facultyFeedback;
    }

    public void setFacultyFeedback(String facultyFeedback) {
        this.facultyFeedback = facultyFeedback;
    }

    public User getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(User reviewedBy) {
        this.reviewedBy = reviewedBy;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(LocalDateTime reviewedAt) {
        this.reviewedAt = reviewedAt;
    }
}