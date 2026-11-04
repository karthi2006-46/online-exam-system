package com.lms.dto;

import java.time.LocalDateTime;

public class FacultyStudentDTO {

    private Long enrollmentId;
    private Long studentId;
    private String studentName;
    private String email;
    private String courseName;
    private String status;
    private LocalDateTime enrollmentDate;
    private LocalDateTime dueDate;
    private Long courseId;
    private Double overallProgress;


    public FacultyStudentDTO() {
    }

    public FacultyStudentDTO(
            Long enrollmentId,
            Long studentId,
            String studentName,
            String email,
            String courseName,
            Long courseId,
            String status,
            Double overallProgress,
            LocalDateTime enrollmentDate,
            LocalDateTime dueDate) {

        this.enrollmentId = enrollmentId;
        this.studentId = studentId;
        this.studentName = studentName;
        this.email = email;
        this.courseName = courseName;
        this.courseId = courseId;
        this.status = status;
        this.overallProgress = overallProgress;
        this.enrollmentDate = enrollmentDate;
        this.dueDate = dueDate;
    }

    public Long getEnrollmentId() {
        return enrollmentId;
    }

    public void setEnrollmentId(Long enrollmentId) {
        this.enrollmentId = enrollmentId;
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

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

public Double getOverallProgress() {
    return overallProgress;
}

public void setOverallProgress(Double overallProgress) {
    this.overallProgress = overallProgress;
}

    public LocalDateTime getEnrollmentDate() {
        return enrollmentDate;
    }

    public void setEnrollmentDate(LocalDateTime enrollmentDate) {
        this.enrollmentDate = enrollmentDate;
    }
    public Long getCourseId() {
    return courseId;
}

public void setCourseId(Long courseId) {
    this.courseId = courseId;
}

    public LocalDateTime getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDateTime dueDate) {
        this.dueDate = dueDate;
    }
}