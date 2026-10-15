package com.lms.dto;

public class ExamEligibilityDTO {

    private Long studentId;

    private Long courseId;

    private long totalMaterials;

    private long completedMaterials;

    private double materialPercentage;

    private long totalCodingChallenges;

    private long completedCodingChallenges;

    private double codingPercentage;

    private boolean materialsCompleted;

    private boolean codingCompleted;

    private boolean eligibleForExamApproval;


    // =========================
    // GETTERS AND SETTERS
    // =========================

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }

    public Long getCourseId() {
        return courseId;
    }

    public void setCourseId(Long courseId) {
        this.courseId = courseId;
    }

    public long getTotalMaterials() {
        return totalMaterials;
    }

    public void setTotalMaterials(long totalMaterials) {
        this.totalMaterials = totalMaterials;
    }

    public long getCompletedMaterials() {
        return completedMaterials;
    }

    public void setCompletedMaterials(long completedMaterials) {
        this.completedMaterials = completedMaterials;
    }

    public double getMaterialPercentage() {
        return materialPercentage;
    }

    public void setMaterialPercentage(double materialPercentage) {
        this.materialPercentage = materialPercentage;
    }

    public long getTotalCodingChallenges() {
        return totalCodingChallenges;
    }

    public void setTotalCodingChallenges(long totalCodingChallenges) {
        this.totalCodingChallenges = totalCodingChallenges;
    }

    public long getCompletedCodingChallenges() {
        return completedCodingChallenges;
    }

    public void setCompletedCodingChallenges(long completedCodingChallenges) {
        this.completedCodingChallenges = completedCodingChallenges;
    }

    public double getCodingPercentage() {
        return codingPercentage;
    }

    public void setCodingPercentage(double codingPercentage) {
        this.codingPercentage = codingPercentage;
    }

    public boolean isMaterialsCompleted() {
        return materialsCompleted;
    }

    public void setMaterialsCompleted(boolean materialsCompleted) {
        this.materialsCompleted = materialsCompleted;
    }

    public boolean isCodingCompleted() {
        return codingCompleted;
    }

    public void setCodingCompleted(boolean codingCompleted) {
        this.codingCompleted = codingCompleted;
    }

    public boolean isEligibleForExamApproval() {
        return eligibleForExamApproval;
    }

    public void setEligibleForExamApproval(
            boolean eligibleForExamApproval) {

        this.eligibleForExamApproval =
                eligibleForExamApproval;
    }
}