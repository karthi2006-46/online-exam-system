package com.lms.service;

import com.lms.dto.CourseProgressDTO;
import com.lms.dto.admin.AdminPageDTO;
import com.lms.dto.admin.AdminPerformanceRowDTO;
import com.lms.dto.admin.AdminResultRowDTO;
import com.lms.entity.CodingSubmission;
import com.lms.entity.Course;
import com.lms.entity.Enrollment;
import com.lms.entity.Exam;
import com.lms.entity.ExamApprovalRequest;
import com.lms.entity.Result;
import com.lms.entity.User;
import com.lms.exception.ResourceNotFoundException;
import com.lms.repository.CodingChallengeProgressRepository;
import com.lms.repository.CodingChallengeRepository;
import com.lms.repository.CodingSubmissionRepository;
import com.lms.repository.CourseRepository;
import com.lms.repository.EnrollmentRepository;
import com.lms.repository.ExamApprovalRequestRepository;
import com.lms.repository.ExamAttemptRepository;
import com.lms.repository.ResultRepository;
import com.lms.repository.StudyMaterialRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Read-only academic reporting for the admin module:
 * student performance matrix and examination results.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminPerformanceService {

    private final EnrollmentRepository enrollmentRepository;
    private final ResultRepository resultRepository;
    private final ExamAttemptRepository examAttemptRepository;
    private final StudyMaterialRepository materialRepository;
    private final CodingChallengeRepository challengeRepository;
    private final CodingChallengeProgressRepository challengeProgressRepository;
    private final CodingSubmissionRepository codingSubmissionRepository;
    private final ExamApprovalRequestRepository approvalRepository;
    private final CourseRepository courseRepository;
    private final StudentDashboardService studentDashboardService;

    // ==========================================
    // STUDENT PERFORMANCE MATRIX
    // ==========================================

    public AdminPageDTO<AdminPerformanceRowDTO> performance(
            Long courseId,
            Long facultyId,
            String enrollmentStatus,
            String approvalStatus,
            String search,
            int page,
            int size) {

        Pageable pageable = PageRequest.of(
                Math.max(0, page),
                Math.min(100, Math.max(1, size)),
                Sort.by("student.firstName").ascending()
                        .and(Sort.by("student.lastName").ascending())
        );

        Page<Enrollment> enrollments = enrollmentRepository.searchPerformance(
                courseId,
                facultyId,
                normalizeEnum(enrollmentStatus),
                normalizeEnum(approvalStatus),
                like(search),
                pageable
        );

        List<AdminPerformanceRowDTO> rows = enrollments.getContent()
                .stream()
                .map(this::toPerformanceRow)
                .toList();

        return AdminPageDTO.from(enrollments, rows);
    }

    private AdminPerformanceRowDTO toPerformanceRow(Enrollment enrollment) {

        User student = enrollment.getStudent();
        Course course = enrollment.getCourse();

        AdminPerformanceRowDTO row = new AdminPerformanceRowDTO();

        row.setEnrollmentId(enrollment.getId());

        row.setStudentId(student.getId());
        row.setStudentName(
                joinName(student.getFirstName(), student.getLastName()));
        row.setEmail(student.getEmail());
        row.setStudentActive(student.getActive());

        row.setCourseId(course.getId());
        row.setCourseTitle(course.getTitle());
        row.setCourseCode(course.getCourseCode());

        if (course.getFaculty() != null) {
            row.setFacultyId(course.getFaculty().getId());
            row.setFacultyName(joinName(
                    course.getFaculty().getFirstName(),
                    course.getFaculty().getLastName()));
        }

        row.setEnrollmentStatus(enrollment.getStatus());
        row.setEnrollmentDate(enrollment.getEnrollmentDate());
        row.setDueDate(enrollment.getDueDate());
        row.setCompletionPercentage(enrollment.getCompletionPercentage());

        // Progress shares the exact calculation used by the student dashboard.
        CourseProgressDTO progress = studentDashboardService.getCourseProgress(
                student.getId(),
                course.getId()
        );

        if (progress != null) {
            row.setTotalMaterials(progress.getTotalMaterials() == null
                    ? 0 : progress.getTotalMaterials());
            row.setCompletedMaterials(progress.getCompletedMaterials() == null
                    ? 0 : progress.getCompletedMaterials());
            row.setMaterialsProgress(progress.getMaterialsProgress() == null
                    ? 0.0 : progress.getMaterialsProgress());

            row.setTotalChallenges(progress.getTotalChallenges() == null
                    ? 0 : progress.getTotalChallenges());
            row.setCompletedChallenges(progress.getCompletedChallenges() == null
                    ? 0 : progress.getCompletedChallenges());
            row.setChallengesProgress(progress.getChallengesProgress() == null
                    ? 0.0 : progress.getChallengesProgress());

            row.setOverallProgress(progress.getOverallProgress());
        } else {
            row.setTotalMaterials(
                    materialRepository.countByCourse_IdAndActiveTrue(course.getId()));
            row.setTotalChallenges(
                    challengeRepository.countByCourse_IdAndActive(course.getId(), true));
            row.setOverallProgress(0.0);
        }

        // Coding review status from the latest submission in this course.
        List<CodingSubmission> submissions =
                codingSubmissionRepository
                        .findByStudent_IdAndChallenge_Course_IdOrderBySubmittedAtDesc(
                                student.getId(),
                                course.getId());

        row.setCodingApprovalStatus(
                submissions.isEmpty()
                        ? "NOT_SUBMITTED"
                        : String.valueOf(submissions.get(0).getStatus()));

        // Exam approval status for this student and course.
        row.setExamApprovalStatus(
                approvalRepository
                        .findByStudent_IdAndCourse_Id(
                                student.getId(), course.getId())
                        .map(request -> request.getStatus().name())
                        .orElse("NOT_REQUESTED")
        );

        // Latest exam result in this course.
        List<Result> results =
                resultRepository.findByStudent_IdAndExam_Course_IdOrderByCreatedAtDesc(
                        student.getId(),
                        course.getId());

        if (!results.isEmpty()) {

            Result latest = results.get(0);
            Exam exam = latest.getExam();

            if (exam != null) {
                row.setLatestExamId(exam.getId());
                row.setLatestExamTitle(exam.getTitle());
                row.setLatestExamTotalMarks(exam.getTotalMarks());
            }

            row.setLatestExamMarks(latest.getObtainedMarks());
            row.setLatestExamPercentage(latest.getPercentage());
            row.setLatestExamResult(latest.getStatus());

            if (latest.getExamAttempt() != null) {
                row.setLatestExamDate(
                        latest.getExamAttempt().getSubmissionTime());
            } else {
                row.setLatestExamDate(latest.getCreatedAt());
            }
        }

        row.setExamAttempts(
                examAttemptRepository.countByStudent_IdAndExam_Course_Id(
                        student.getId(),
                        course.getId()
                )
        );

        row.setOverallResult(overallResult(results));

        return row;
    }

    private String overallResult(List<Result> results) {

        if (results.isEmpty()) {
            return "NOT_ATTEMPTED";
        }

        boolean anyFail = results.stream()
                .anyMatch(result ->
                        "FAIL".equalsIgnoreCase(result.getStatus()));

        return anyFail ? "FAIL" : "PASS";
    }

    // ==========================================
    // EXAMINATION RESULTS
    // ==========================================

    public AdminPageDTO<AdminResultRowDTO> results(
            Long courseId,
            Long examId,
            LocalDateTime fromDate,
            LocalDateTime toDate,
            String status,
            int page,
            int size) {

        if (courseId == null) {
            throw new com.lms.exception.BadRequestException(
                    "courseId is required");
        }

        Pageable pageable = PageRequest.of(
                Math.max(0, page),
                Math.min(100, Math.max(1, size)),
                Sort.by("createdAt").descending()
        );

        Page<Result> results =
                resultRepository.findFilteredCourseResultsPaged(
                        courseId,
                        examId,
                        fromDate,
                        toDate,
                        normalizeEnum(status),
                        pageable
                );

        List<AdminResultRowDTO> rows = results.getContent()
                .stream()
                .map(this::toResultRow)
                .toList();

        return AdminPageDTO.from(results, rows);
    }

    /**
     * Every result of one student across all courses.
     */
    public List<AdminResultRowDTO> studentResults(Long studentId) {

        return resultRepository.findByStudent_Id(studentId)
                .stream()
                .sorted((a, b) -> {
                    LocalDateTime left = a.getCreatedAt();
                    LocalDateTime right = b.getCreatedAt();
                    if (left == null) return 1;
                    if (right == null) return -1;
                    return right.compareTo(left);
                })
                .map(this::toResultRow)
                .toList();
    }

    private AdminResultRowDTO toResultRow(Result result) {

        AdminResultRowDTO row = new AdminResultRowDTO();

        row.setResultId(result.getId());

        if (result.getExamAttempt() != null) {
            row.setAttemptId(result.getExamAttempt().getId());
        }

        User student = result.getStudent();

        if (student != null) {
            row.setStudentId(student.getId());
            row.setStudentName(
                    joinName(student.getFirstName(), student.getLastName()));
            row.setEmail(student.getEmail());
        }

        Exam exam = result.getExam();

        if (exam != null) {
            row.setExamId(exam.getId());
            row.setExamTitle(exam.getTitle());
            row.setTotalMarks(exam.getTotalMarks());

            if (exam.getCourse() != null) {
                row.setCourseId(exam.getCourse().getId());
                row.setCourseTitle(exam.getCourse().getTitle());
            }
        }

        if (result.getExamAttempt() != null) {
            row.setExamDate(
                    result.getExamAttempt().getSubmissionTime() != null
                            ? result.getExamAttempt().getSubmissionTime()
                            : result.getExamAttempt().getEndTime());
        }

        row.setObtainedMarks(result.getObtainedMarks());
        row.setPercentage(result.getPercentage());
        row.setResultStatus(result.getStatus());
        row.setCorrectAnswers(result.getCorrectAnswers());
        row.setWrongAnswers(result.getWrongAnswers());
        row.setSkippedQuestions(result.getSkippedQuestions());

        if (student != null && exam != null && exam.getCourse() != null) {
            row.setExamAttempts(
                    examAttemptRepository.countByStudent_IdAndExam_Course_Id(
                            student.getId(),
                            exam.getCourse().getId()
                    )
            );
        }

        return row;
    }

    // ==========================================
    // FILTER OPTIONS
    // ==========================================

    /**
     * Course options for the admin filter dropdowns.
     * Includes inactive courses because historical results still reference them.
     */
    public List<Course> courseOptions() {
        return courseRepository.findAll(
                Sort.by("title").ascending());
    }

    public Course requireCourse(Long courseId) {
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Course not found"));
    }

    // ==========================================
    // HELPERS
    // ==========================================

    private String normalizeEnum(String value) {

        if (value == null) {
            return null;
        }

        String trimmed = value.trim();

        return trimmed.isEmpty() ? null : trimmed.toUpperCase();
    }

    private String like(String search) {

        if (search == null) {
            return null;
        }

        String trimmed = search.trim();

        return trimmed.isEmpty()
                ? null
                : "%" + trimmed.toLowerCase() + "%";
    }

    private String joinName(String firstName, String lastName) {
        return ((firstName == null ? "" : firstName) + " "
                + (lastName == null ? "" : lastName)).trim();
    }
}
