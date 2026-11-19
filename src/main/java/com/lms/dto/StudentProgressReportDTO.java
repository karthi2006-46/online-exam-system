package com.lms.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class StudentProgressReportDTO {

    private String reportId;

    private LocalDateTime generatedAt;

    // Student details
    private Long studentId;
    private String studentName;
    private String email;
    private String phone;
    private String address;
    private String accountStatus;
    private LocalDateTime registrationDate;

    // Enrollment details
    private Long enrollmentId;
private LocalDateTime enrollmentDate;
private LocalDateTime dueDate;
    private String enrollmentStatus;
    private Double completionPercentage;
    private Double overallProgress;

    // Course details
    private Long courseId;
    private String courseTitle;
    private String courseCode;
    private String courseDescription;
    private Integer courseDurationDays;
    private Double courseFees;
    private String facultyName;

    // Academic summary
    private Integer totalExamsAttempted = 0;
    private Integer examsPassed = 0;
    private Integer examsFailed = 0;
    private Double averagePercentage = 0.0;

    private List<ExamPerformanceDTO> examPerformances = new ArrayList<>();

    private String verificationCode;

    public static class ExamPerformanceDTO {

        private Long resultId;
        private Long examId;
        private String examTitle;

        private Integer obtainedMarks;
        private Integer totalMarks;

        private Double percentage;
        private String status;

        private Integer correctAnswers;
        private Integer wrongAnswers;
        private Integer skippedQuestions;

        private LocalDateTime submissionDate;

        public Long getResultId() {
            return resultId;
        }

        public void setResultId(Long resultId) {
            this.resultId = resultId;
        }

        public Long getExamId() {
            return examId;
        }

        public void setExamId(Long examId) {
            this.examId = examId;
        }

        public String getExamTitle() {
            return examTitle;
        }

        public void setExamTitle(String examTitle) {
            this.examTitle = examTitle;
        }

        public Integer getObtainedMarks() {
            return obtainedMarks;
        }

        public void setObtainedMarks(Integer obtainedMarks) {
            this.obtainedMarks = obtainedMarks;
        }

        public Integer getTotalMarks() {
            return totalMarks;
        }

        public void setTotalMarks(Integer totalMarks) {
            this.totalMarks = totalMarks;
        }

        public Double getPercentage() {
            return percentage;
        }

        public void setPercentage(Double percentage) {
            this.percentage = percentage;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public Integer getCorrectAnswers() {
            return correctAnswers;
        }

        public void setCorrectAnswers(Integer correctAnswers) {
            this.correctAnswers = correctAnswers;
        }

        public Integer getWrongAnswers() {
            return wrongAnswers;
        }

        public void setWrongAnswers(Integer wrongAnswers) {
            this.wrongAnswers = wrongAnswers;
        }

        public Integer getSkippedQuestions() {
            return skippedQuestions;
        }

        public void setSkippedQuestions(Integer skippedQuestions) {
            this.skippedQuestions = skippedQuestions;
        }

        public LocalDateTime getSubmissionDate() {
            return submissionDate;
        }

        public void setSubmissionDate(LocalDateTime submissionDate) {
            this.submissionDate = submissionDate;
        }
    }

    public String getReportId() {
        return reportId;
    }

    public void setReportId(String reportId) {
        this.reportId = reportId;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getAccountStatus() {
        return accountStatus;
    }

    public void setAccountStatus(String accountStatus) {
        this.accountStatus = accountStatus;
    }

    public LocalDateTime getRegistrationDate() {
        return registrationDate;
    }

    public void setRegistrationDate(LocalDateTime registrationDate) {
        this.registrationDate = registrationDate;
    }

    public Long getEnrollmentId() {
        return enrollmentId;
    }

    public void setEnrollmentId(Long enrollmentId) {
        this.enrollmentId = enrollmentId;
    }

    public LocalDateTime getEnrollmentDate() {
        return enrollmentDate;
    }

    public void setEnrollmentDate(LocalDateTime enrollmentDate) {
        this.enrollmentDate = enrollmentDate;
    }

    public LocalDateTime getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDateTime dueDate) {
        this.dueDate = dueDate;
    }

    public String getEnrollmentStatus() {
        return enrollmentStatus;
    }

    public void setEnrollmentStatus(String enrollmentStatus) {
        this.enrollmentStatus = enrollmentStatus;
    }

    public Double getCompletionPercentage() {
        return completionPercentage;
    }

    public void setCompletionPercentage(Double completionPercentage) {
        this.completionPercentage = completionPercentage;
    }
    public Double getOverallProgress() {
    return overallProgress;
}

public void setOverallProgress(Double overallProgress) {
    this.overallProgress = overallProgress;
}

    public Long getCourseId() {
        return courseId;
    }

    public void setCourseId(Long courseId) {
        this.courseId = courseId;
    }

    public String getCourseTitle() {
        return courseTitle;
    }

    public void setCourseTitle(String courseTitle) {
        this.courseTitle = courseTitle;
    }

    public String getCourseCode() {
        return courseCode;
    }

    public void setCourseCode(String courseCode) {
        this.courseCode = courseCode;
    }

    public String getCourseDescription() {
        return courseDescription;
    }

    public void setCourseDescription(String courseDescription) {
        this.courseDescription = courseDescription;
    }

    public Integer getCourseDurationDays() {
        return courseDurationDays;
    }

    public void setCourseDurationDays(Integer courseDurationDays) {
        this.courseDurationDays = courseDurationDays;
    }

    public Double getCourseFees() {
        return courseFees;
    }

    public void setCourseFees(Double courseFees) {
        this.courseFees = courseFees;
    }

    public String getFacultyName() {
        return facultyName;
    }

    public void setFacultyName(String facultyName) {
        this.facultyName = facultyName;
    }

    public Integer getTotalExamsAttempted() {
        return totalExamsAttempted;
    }

    public void setTotalExamsAttempted(Integer totalExamsAttempted) {
        this.totalExamsAttempted = totalExamsAttempted;
    }

    public Integer getExamsPassed() {
        return examsPassed;
    }

    public void setExamsPassed(Integer examsPassed) {
        this.examsPassed = examsPassed;
    }

    public Integer getExamsFailed() {
        return examsFailed;
    }

    public void setExamsFailed(Integer examsFailed) {
        this.examsFailed = examsFailed;
    }

    public Double getAveragePercentage() {
        return averagePercentage;
    }

    public void setAveragePercentage(Double averagePercentage) {
        this.averagePercentage = averagePercentage;
    }

    public List<ExamPerformanceDTO> getExamPerformances() {
        return examPerformances;
    }

    public void setExamPerformances(
            List<ExamPerformanceDTO> examPerformances) {
        this.examPerformances = examPerformances;
    }

    public String getVerificationCode() {
        return verificationCode;
    }

    public void setVerificationCode(String verificationCode) {
        this.verificationCode = verificationCode;
    }
}