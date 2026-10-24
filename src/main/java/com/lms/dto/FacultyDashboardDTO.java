package com.lms.dto;

public class FacultyDashboardDTO {

    private long totalStudents;
    private long activeStudents;
    private long inactiveStudents;
    private long totalCourses;
    private long totalMaterials;
    private long totalExams;
    private long pendingExamApprovals;
    private long pendingExtensionRequests;
    private long totalResults;
    private long passedResults;
    private long failedResults;
    private double averageScore;
    private double passPercentage;

    public FacultyDashboardDTO() {
    }

    public long getTotalStudents() { return totalStudents; }
    public void setTotalStudents(long value) { totalStudents = value; }

    public long getActiveStudents() { return activeStudents; }
    public void setActiveStudents(long value) { activeStudents = value; }

    public long getInactiveStudents() { return inactiveStudents; }
    public void setInactiveStudents(long value) { inactiveStudents = value; }

    public long getTotalCourses() { return totalCourses; }
    public void setTotalCourses(long value) { totalCourses = value; }

    public long getTotalMaterials() { return totalMaterials; }
    public void setTotalMaterials(long value) { totalMaterials = value; }

    public long getTotalExams() { return totalExams; }
    public void setTotalExams(long value) { totalExams = value; }

    public long getPendingExamApprovals() { return pendingExamApprovals; }
    public void setPendingExamApprovals(long value) { pendingExamApprovals = value; }

    public long getPendingExtensionRequests() { return pendingExtensionRequests; }
    public void setPendingExtensionRequests(long value) { pendingExtensionRequests = value; }

    public long getTotalResults() { return totalResults; }
    public void setTotalResults(long value) { totalResults = value; }

    public long getPassedResults() { return passedResults; }
    public void setPassedResults(long value) { passedResults = value; }

    public long getFailedResults() { return failedResults; }
    public void setFailedResults(long value) { failedResults = value; }

    public double getAverageScore() { return averageScore; }
    public void setAverageScore(double value) { averageScore = value; }

    public double getPassPercentage() { return passPercentage; }
    public void setPassPercentage(double value) { passPercentage = value; }
}