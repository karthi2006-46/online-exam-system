package com.lms.service;

import com.lms.dto.admin.AdminDashboardStatsDTO;
import com.lms.entity.CodingSubmissionStatus;
import com.lms.entity.ExamApprovalRequest;
import com.lms.repository.CodingSubmissionRepository;
import com.lms.repository.CourseRepository;
import com.lms.repository.EnrollmentRepository;
import com.lms.repository.ExamApprovalRequestRepository;
import com.lms.repository.ExtensionRequestRepository;
import com.lms.repository.ResultRepository;
import com.lms.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Aggregated counters for the admin dashboard.
 * Every value comes from an existing repository count method.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminDashboardService {

    private static final String FACULTY = "FACULTY";
    private static final String STUDENT = "STUDENT";

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final ExamApprovalRequestRepository approvalRepository;
    private final ExtensionRequestRepository extensionRepository;
    private final CodingSubmissionRepository codingSubmissionRepository;
    private final ResultRepository resultRepository;

    public AdminDashboardStatsDTO getStats() {

        AdminDashboardStatsDTO stats = new AdminDashboardStatsDTO();

        // Faculty
        stats.setTotalFaculty(
                userRepository.countByRole_Name(FACULTY));
        stats.setActiveFaculty(
                userRepository.countByRole_NameAndActive(FACULTY, true));
        stats.setInactiveFaculty(
                Math.max(0, stats.getTotalFaculty() - stats.getActiveFaculty()));

        // Students
        stats.setTotalStudents(
                userRepository.countByRole_Name(STUDENT));
        stats.setActiveStudents(
                userRepository.countByRole_NameAndActive(STUDENT, true));
        stats.setInactiveStudents(
                Math.max(0, stats.getTotalStudents() - stats.getActiveStudents()));

        // Courses
        stats.setTotalCourses(courseRepository.count());
        stats.setActiveCourses(courseRepository.countByActiveTrue());

        // Enrollments
        stats.setTotalActiveEnrollments(
                enrollmentRepository.countByStatusIgnoreCase("ACTIVE"));
        stats.setTotalInactiveEnrollments(
                enrollmentRepository.countByStatusIgnoreCase("INACTIVE"));
        stats.setTotalCompletedEnrollments(
                enrollmentRepository.countByStatusIgnoreCase("COMPLETED"));
        stats.setTotalDroppedEnrollments(
                enrollmentRepository.countByStatusIgnoreCase("DROPPED"));

        // Exam approval requests
        stats.setPendingExamApprovals(
                approvalRepository.countByStatus(
                        ExamApprovalRequest.Status.PENDING));
        stats.setApprovedExamRequests(
                approvalRepository.countByStatus(
                        ExamApprovalRequest.Status.APPROVED));
        stats.setRejectedExamRequests(
                approvalRepository.countByStatus(
                        ExamApprovalRequest.Status.REJECTED));
        stats.setTotalExamApprovals(
                stats.getPendingExamApprovals()
                        + stats.getApprovedExamRequests()
                        + stats.getRejectedExamRequests());

        // Extension requests
        stats.setPendingExtensionRequests(
                extensionRepository.countByStatusIgnoreCase("PENDING"));
        stats.setApprovedExtensionRequests(
                extensionRepository.countByStatusIgnoreCase("APPROVED"));
        stats.setRejectedExtensionRequests(
                extensionRepository.countByStatusIgnoreCase("REJECTED"));
        stats.setTotalExtensionRequests(
                stats.getPendingExtensionRequests()
                        + stats.getApprovedExtensionRequests()
                        + stats.getRejectedExtensionRequests());

        // Coding
        stats.setPendingCodingSubmissions(
                codingSubmissionRepository.countByStatus(
                        CodingSubmissionStatus.PENDING_REVIEW));

        // Results
        long passed = resultRepository.countByStatusIgnoreCase("PASS");
        long failed = resultRepository.countByStatusIgnoreCase("FAIL");
        long total = resultRepository.count();

        stats.setTotalResults(total);
        stats.setPassedResults(passed);
        stats.setFailedResults(failed);
        stats.setPassPercentage(
                total == 0 ? 0.0 : round(passed * 100.0 / total));

        return stats;
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
