package com.lms.service;

import com.lms.dto.StudentProgressReportDTO;
import com.lms.dto.StudentProgressReportDTO.ExamPerformanceDTO;
import com.lms.dto.admin.AdminStudentReportDTO;
import com.lms.dto.admin.AdminStudentReportDTO.CodingRow;
import com.lms.dto.admin.AdminStudentReportDTO.CourseSection;
import com.lms.dto.admin.AdminStudentReportDTO.ExamRow;
import com.lms.dto.admin.AdminStudentReportDTO.ExtensionRow;
import com.lms.entity.CodingSubmission;
import com.lms.entity.Enrollment;
import com.lms.entity.ExamApprovalRequest;
import com.lms.entity.ExtensionRequest;
import com.lms.entity.User;
import com.lms.exception.ResourceNotFoundException;
import com.lms.repository.CodingSubmissionRepository;
import com.lms.repository.EnrollmentRepository;
import com.lms.repository.ExamApprovalRequestRepository;
import com.lms.repository.ExtensionRequestRepository;
import com.lms.repository.MaterialProgressRepository;
import com.lms.repository.ResultRepository;
import com.lms.repository.StudyMaterialRepository;
import com.lms.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds the complete per-student report used by the admin downloads.
 * All numbers come straight from the operational tables.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminStudentReportService {

    private final UserRepository userRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final StudyMaterialRepository materialRepository;
    private final MaterialProgressRepository materialProgressRepository;
    private final CodingSubmissionRepository codingSubmissionRepository;
    private final ExamApprovalRequestRepository approvalRepository;
    private final ExtensionRequestRepository extensionRepository;
    private final ResultRepository resultRepository;
    private final StudentProgressReportService progressReportService;

    public AdminStudentReportDTO build(Long studentId) {

        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Student not found"));

        AdminStudentReportDTO report = new AdminStudentReportDTO();

        report.setStudentId(student.getId());
        report.setStudentName(joinName(
                student.getFirstName(), student.getLastName()));
        report.setEmail(student.getEmail());
        report.setPhone(student.getPhone());
        report.setAddress(student.getAddress());
        report.setAccountStatus(
                Boolean.TRUE.equals(student.getActive())
                        ? "ACTIVE" : "INACTIVE");
        report.setRegisteredAt(student.getCreatedAt());
        report.setGeneratedAt(LocalDateTime.now());

        List<Enrollment> enrollments =
                enrollmentRepository.findByStudent_Id(studentId);

        report.setEnrolledCourses(enrollments.size());

        int active = 0;
        int inactive = 0;
        int completed = 0;

        List<CourseSection> sections = new ArrayList<>();

        for (Enrollment enrollment : enrollments) {

            String status = enrollment.getStatus() == null
                    ? ""
                    : enrollment.getStatus().toUpperCase();

            if ("ACTIVE".equals(status)) {
                active++;
            } else if ("COMPLETED".equals(status)) {
                completed++;
            } else if ("INACTIVE".equals(status)) {
                inactive++;
            }

            sections.add(buildSection(studentId, enrollment));
        }

        report.setActiveCourses(active);
        report.setInactiveCourses(inactive);
        report.setCompletedCourses(completed);

        int total = (int) resultRepository.countByStudent_Id(studentId);
        int passed = (int) resultRepository
                .countByStudent_IdAndStatusIgnoreCase(studentId, "PASS");
        int failed = (int) resultRepository
                .countByStudent_IdAndStatusIgnoreCase(studentId, "FAIL");

        report.setTotalResults(total);
        report.setPassedResults(passed);
        report.setFailedResults(failed);

        double average = sections.stream()
                .map(CourseSection::getAveragePercentage)
                .filter(value -> value != null)
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);

        report.setAveragePercentage(round(average));
        report.setCourses(sections);

        return report;
    }

    private CourseSection buildSection(Long studentId, Enrollment enrollment) {

        CourseSection section = new CourseSection();

        section.setCourseId(enrollment.getCourse().getId());
        section.setCourseTitle(enrollment.getCourse().getTitle());
        section.setCourseCode(enrollment.getCourse().getCourseCode());

        if (enrollment.getCourse().getFaculty() != null) {
            section.setFacultyName(joinName(
                    enrollment.getCourse().getFaculty().getFirstName(),
                    enrollment.getCourse().getFaculty().getLastName()));
        }

        section.setEnrollmentId(enrollment.getId());
        section.setEnrollmentStatus(enrollment.getStatus());
        section.setEnrollmentDate(enrollment.getEnrollmentDate());
        section.setDueDate(enrollment.getDueDate());
        section.setCompletionPercentage(enrollment.getCompletionPercentage());

        // Materials
        section.setMaterialsTotal((int) materialRepository
                .countByCourse_IdAndActiveTrue(enrollment.getCourse().getId()));
        section.setMaterialsCompleted((int) materialProgressRepository
                .countByStudent_IdAndMaterial_Course_IdAndCompleted(
                        studentId,
                        enrollment.getCourse().getId(),
                        true));

        // Exam approval status
        section.setExamApprovalStatus(
                approvalRepository
                        .findByStudent_IdAndCourse_Id(
                                studentId,
                                enrollment.getCourse().getId())
                        .map(request -> request.getStatus() != null
                                ? request.getStatus().name()
                                : "NOT_REQUESTED")
                        .orElse("NOT_REQUESTED"));

        // Coding submissions
        List<CodingSubmission> submissions = codingSubmissionRepository
                .findByStudent_IdAndChallenge_Course_IdOrderBySubmittedAtDesc(
                        studentId,
                        enrollment.getCourse().getId());

        List<CodingRow> codingRows = submissions.stream()
                .map(this::toCodingRow)
                .toList();

        section.setCodingHistory(codingRows);
        section.setCodingSubmissions(codingRows.size());
        section.setCodingApproved((int) codingRows.stream()
                .filter(row -> "APPROVED".equalsIgnoreCase(row.getReviewStatus()))
                .count());

        // Extensions
        List<ExtensionRow> extensionRows = extensionRepository
                .findByEnrollment_Id(enrollment.getId())
                .stream()
                .map(this::toExtensionRow)
                .toList();

        section.setExtensionHistory(extensionRows);

        // Exam results (reuse the existing report builder)
        try {
            StudentProgressReportDTO progress =
                    progressReportService.buildStudentCourseReport(
                            studentId,
                            enrollment.getCourse().getId());

            section.setOverallProgress(progress.getOverallProgress());
            section.setExamsAttempted(progress.getTotalExamsAttempted());
            section.setExamsPassed(progress.getExamsPassed());
            section.setExamsFailed(progress.getExamsFailed());
            section.setAveragePercentage(progress.getAveragePercentage());

            section.setExamResults(
                    progress.getExamPerformances() == null
                            ? List.of()
                            : progress.getExamPerformances().stream()
                                    .map(this::toExamRow)
                                    .toList());

        } catch (RuntimeException exception) {
            section.setExamResults(List.of());
        }

        return section;
    }

    private CodingRow toCodingRow(CodingSubmission submission) {

        CodingRow row = new CodingRow();

        row.setSubmissionId(submission.getId());
        row.setSubmittedAt(submission.getSubmittedAt());

        if (submission.getChallenge() != null) {
            row.setChallengeTitle(submission.getChallenge().getTitle());
        }

        int passedTestCases = submission.getTestCasesPassed() == null
                ? 0 : submission.getTestCasesPassed();

        int totalTestCases = submission.getTotalTestCases() == null
                ? 0 : submission.getTotalTestCases();

        row.setTestCasesPassed(passedTestCases);
        row.setTotalTestCases(totalTestCases);
        row.setPercentage(totalTestCases > 0
                ? round((passedTestCases * 100.0) / totalTestCases)
                : 0.0);

        row.setReviewStatus(submission.getStatus() != null
                ? submission.getStatus().name()
                : "PENDING_REVIEW");
        row.setFacultyFeedback(submission.getFacultyFeedback());
        row.setReviewedAt(submission.getReviewedAt());

        if (submission.getReviewedBy() != null) {
            row.setReviewedByName(joinName(
                    submission.getReviewedBy().getFirstName(),
                    submission.getReviewedBy().getLastName()));
        }

        return row;
    }

    private ExtensionRow toExtensionRow(ExtensionRequest request) {

        ExtensionRow row = new ExtensionRow();

        row.setRequestId(request.getId());
        row.setDaysRequested(request.getDaysRequested());
        row.setReason(request.getReason());
        row.setRequestedAt(request.getCreatedAt());
        row.setStatus(request.getStatus());
        row.setReviewedAt(request.getReviewedAt());
        row.setRejectionReason(request.getRejectionReason());

        if (request.getEnrollment() != null) {
            row.setOriginalDueDate(
                    request.getEnrollment().getDueDate());
        }

        if (request.getReviewedBy() != null) {
            row.setReviewedByName(joinName(
                    request.getReviewedBy().getFirstName(),
                    request.getReviewedBy().getLastName()));
        }

        return row;
    }

    private ExamRow toExamRow(ExamPerformanceDTO performance) {

        ExamRow row = new ExamRow();

        row.setResultId(performance.getResultId());
        row.setExamTitle(performance.getExamTitle());
        row.setObtainedMarks(performance.getObtainedMarks());
        row.setTotalMarks(performance.getTotalMarks());
        row.setPercentage(performance.getPercentage());
        row.setStatus(performance.getStatus());
        row.setSubmittedAt(performance.getSubmissionDate());

        return row;
    }

    private String joinName(String firstName, String lastName) {
        String name = ((firstName == null ? "" : firstName)
                + " "
                + (lastName == null ? "" : lastName))
                .replaceAll("\\s+", " ")
                .trim();

        return name.isEmpty() ? null : name;
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
