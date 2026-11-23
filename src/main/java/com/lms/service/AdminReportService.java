package com.lms.service;

import com.lms.dto.admin.AdminCourseReportRowDTO;
import com.lms.dto.admin.AdminFacultyWorkloadDTO;
import com.lms.dto.admin.AdminReportOverviewDTO;
import com.lms.entity.CodingSubmissionStatus;
import com.lms.entity.Course;
import com.lms.entity.ExamApprovalRequest;
import com.lms.entity.User;
import com.lms.repository.CodingSubmissionRepository;
import com.lms.repository.CourseRepository;
import com.lms.repository.EnrollmentRepository;
import com.lms.repository.ExamApprovalRequestRepository;
import com.lms.repository.ExamRepository;
import com.lms.repository.ExtensionRequestRepository;
import com.lms.repository.ResultRepository;
import com.lms.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Platform-wide reports for the admin console: one overview, one row per
 * course and one row per faculty member.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminReportService {

    private static final String FACULTY = "FACULTY";
    private static final String STUDENT = "STUDENT";

    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final ResultRepository resultRepository;
    private final ExamRepository examRepository;
    private final ExamApprovalRequestRepository approvalRepository;
    private final ExtensionRequestRepository extensionRepository;
    private final CodingSubmissionRepository codingSubmissionRepository;

    public AdminReportOverviewDTO overview() {

        AdminReportOverviewDTO report = new AdminReportOverviewDTO();

        report.setGeneratedAt(LocalDateTime.now());

        // Courses
        report.setTotalCourses(courseRepository.count());
        report.setActiveCourses(courseRepository.countByActiveTrue());

        // People
        report.setTotalFaculty(
                userRepository.countByRole_Name(FACULTY));
        report.setTotalStudents(
                userRepository.countByRole_Name(STUDENT));

        // Enrollments
        report.setTotalEnrollments(enrollmentRepository.count());
        report.setActiveEnrollments(
                enrollmentRepository.countByStatusIgnoreCase("ACTIVE"));
        report.setInactiveEnrollments(
                enrollmentRepository.countByStatusIgnoreCase("INACTIVE"));
        report.setCompletedEnrollments(
                enrollmentRepository.countByStatusIgnoreCase("COMPLETED"));
        report.setDroppedEnrollments(
                enrollmentRepository.countByStatusIgnoreCase("DROPPED"));

        // Results
        long totalResults = resultRepository.count();
        long passed = resultRepository.countByStatusIgnoreCase("PASS");
        long failed = resultRepository.countByStatusIgnoreCase("FAIL");

        report.setTotalResults(totalResults);
        report.setPassedResults(passed);
        report.setFailedResults(failed);
        report.setPassPercentage(
                totalResults == 0
                        ? 0.0
                        : round(passed * 100.0 / totalResults));

        // Work queues
        report.setPendingExamApprovals(
                approvalRepository.countByStatus(
                        ExamApprovalRequest.Status.PENDING));
        report.setPendingExtensionRequests(
                extensionRepository.countByStatusIgnoreCase("PENDING"));
        report.setPendingCodingSubmissions(
                codingSubmissionRepository.countByStatus(
                        CodingSubmissionStatus.PENDING_REVIEW));

        // Course breakdown
        report.setCourses(
                courseRepository.findAll(Sort.by("title").ascending())
                        .stream()
                        .map(this::toCourseRow)
                        .toList());

        // Faculty workload
        report.setFaculty(
                userRepository.findByRole_Name(FACULTY)
                        .stream()
                        .sorted((a, b) -> joinName(
                                a.getFirstName(), a.getLastName())
                                .compareToIgnoreCase(joinName(
                                        b.getFirstName(), b.getLastName())))
                        .map(this::toFacultyRow)
                        .toList());

        return report;
    }

    /* ---------------- rows ---------------- */

    private AdminCourseReportRowDTO toCourseRow(Course course) {

        AdminCourseReportRowDTO row = new AdminCourseReportRowDTO();

        row.setCourseId(course.getId());
        row.setTitle(course.getTitle());
        row.setCourseCode(course.getCourseCode());
        row.setActive(course.getActive());

        if (course.getFaculty() != null) {
            row.setFacultyId(course.getFaculty().getId());
            row.setFacultyName(joinName(
                    course.getFaculty().getFirstName(),
                    course.getFaculty().getLastName()));
        }

        row.setStudents(
                enrollmentRepository.countByCourse_Id(course.getId()));
        row.setActiveStudents(
                enrollmentRepository.countByCourse_IdAndStatusIgnoreCase(
                        course.getId(), "ACTIVE"));
        row.setCompletedStudents(
                enrollmentRepository.countByCourse_IdAndStatusIgnoreCase(
                        course.getId(), "COMPLETED"));

        row.setExams(
                examRepository.countByCourse_Id(course.getId()));

        long results = resultRepository.countByExam_Course_Id(course.getId());
        long passed = resultRepository
                .countByExam_Course_IdAndStatusIgnoreCase(course.getId(), "PASS");

        row.setResults(results);
        row.setPassedResults(passed);
        row.setFailedResults(Math.max(0, results - passed));
        row.setPassPercentage(
                results == 0 ? 0.0 : round(passed * 100.0 / results));

        Double average =
                resultRepository.averagePercentageByCourse(course.getId());

        row.setAveragePercentage(average == null ? 0.0 : round(average));

        return row;
    }

    private AdminFacultyWorkloadDTO toFacultyRow(User faculty) {

        AdminFacultyWorkloadDTO row = new AdminFacultyWorkloadDTO();

        row.setFacultyId(faculty.getId());
        row.setName(joinName(faculty.getFirstName(), faculty.getLastName()));
        row.setEmail(faculty.getEmail());
        row.setActive(faculty.getActive());

        row.setCourses(
                courseRepository.findByFaculty_Id(faculty.getId()).size());
        row.setStudents(
                enrollmentRepository.countByCourse_Faculty_Id(faculty.getId()));
        row.setExams(
                examRepository.countByCourse_Faculty_IdAndActiveTrue(
                        faculty.getId()));

        row.setPendingExamApprovals(
                approvalRepository.countByCourse_Faculty_IdAndStatus(
                        faculty.getId(),
                        ExamApprovalRequest.Status.PENDING));
        row.setPendingExtensions(
                extensionRepository
                        .countByEnrollment_Course_Faculty_IdAndStatusIgnoreCase(
                                faculty.getId(), "PENDING"));
        row.setPendingCodingReviews(
                codingSubmissionRepository
                        .countByChallenge_Course_Faculty_IdAndStatus(
                                faculty.getId(),
                                CodingSubmissionStatus.PENDING_REVIEW));

        return row;
    }

    /* ---------------- helpers ---------------- */

    private String joinName(String firstName, String lastName) {
        return ((firstName == null ? "" : firstName) + " "
                + (lastName == null ? "" : lastName)).trim();
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
