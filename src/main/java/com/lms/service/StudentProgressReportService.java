package com.lms.service;

import com.lms.dto.StudentProgressReportDTO;
import com.lms.dto.StudentProgressReportDTO.ExamPerformanceDTO;
import com.lms.entity.Course;
import com.lms.entity.Enrollment;
import com.lms.entity.ReportMetadata;
import com.lms.entity.Result;
import com.lms.entity.User;
import com.lms.repository.CourseRepository;
import com.lms.repository.EnrollmentRepository;
import com.lms.repository.ReportMetadataRepository;
import com.lms.repository.ResultRepository;
import com.lms.repository.UserRepository;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class StudentProgressReportService {

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final ResultRepository resultRepository;
    private final ReportMetadataRepository reportMetadataRepository;
    private final StudentProgressPdfGenerator pdfGenerator;
    private final StudentDashboardService studentDashboardService;

    public StudentProgressReportService(
            UserRepository userRepository,
            CourseRepository courseRepository,
            EnrollmentRepository enrollmentRepository,
            ResultRepository resultRepository,
            ReportMetadataRepository reportMetadataRepository,
            StudentProgressPdfGenerator pdfGenerator,
            StudentDashboardService studentDashboardService) {

        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.resultRepository = resultRepository;
        this.reportMetadataRepository = reportMetadataRepository;
        this.pdfGenerator = pdfGenerator;
        this.studentDashboardService = studentDashboardService;
    }

    // ==========================================
    // GENERATE INDIVIDUAL STUDENT PDF
    // ==========================================

    @Transactional
    public byte[] generateStudentReport(Long studentId, Long courseId) {

        User faculty = getAuthenticatedUser();
        Course course = getCourse(courseId);

        verifyFacultyOwnsCourse(faculty, course);

        Enrollment enrollment = enrollmentRepository
                .findByStudent_IdAndCourse_Id(studentId, courseId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Student is not enrolled in this course"));

        StudentProgressReportDTO report = buildReport(enrollment);

        return generateAndRegisterPdf(report);
    }

    // ==========================================
    // GENERATE ALL STUDENT REPORTS AS ZIP
    // ==========================================

    @Transactional
    public byte[] generateCourseReportsZip(Long courseId) {

        User faculty = getAuthenticatedUser();
        Course course = getCourse(courseId);

        verifyFacultyOwnsCourse(faculty, course);

        List<Enrollment> enrollments =
                enrollmentRepository.findByCourse_Id(courseId);

        if (enrollments.isEmpty()) {
            throw new IllegalArgumentException(
                    "No students are enrolled in this course");
        }

        try (
                ByteArrayOutputStream outputStream =
                        new ByteArrayOutputStream();

                ZipOutputStream zipOutputStream =
                        new ZipOutputStream(outputStream)
        ) {

            for (Enrollment enrollment : enrollments) {

                StudentProgressReportDTO report =
                        buildReport(enrollment);

                byte[] pdfBytes =
                        generateAndRegisterPdf(report);

                String fileName =
                        "Student_" + enrollment.getStudent().getId()
                                + "_Report_" + report.getReportId()
                                + ".pdf";

                zipOutputStream.putNextEntry(
                        new ZipEntry(fileName));

                zipOutputStream.write(pdfBytes);
                zipOutputStream.closeEntry();
            }

            zipOutputStream.finish();

            return outputStream.toByteArray();

        } catch (Exception exception) {

            throw new IllegalStateException(
                    "Failed to generate course report ZIP",
                    exception);
        }
    }

    // ==========================================
    // BUILD STUDENT PROGRESS REPORT
    // ==========================================

    private StudentProgressReportDTO buildReport(
            Enrollment enrollment) {

        User student = enrollment.getStudent();
        Course course = enrollment.getCourse();

        StudentProgressReportDTO report =
                new StudentProgressReportDTO();

        // Student details
        report.setStudentId(student.getId());

        report.setStudentName(
                joinName(
                        student.getFirstName(),
                        student.getLastName()
                )
        );

        report.setEmail(student.getEmail());
        report.setPhone(student.getPhone());
        report.setAddress(student.getAddress());

        report.setAccountStatus(
                Boolean.TRUE.equals(student.getActive())
                        ? "ACTIVE"
                        : "INACTIVE"
        );

        report.setRegistrationDate(student.getCreatedAt());

        // Enrollment details
        report.setEnrollmentId(enrollment.getId());
        report.setEnrollmentDate(enrollment.getEnrollmentDate());
        report.setDueDate(enrollment.getDueDate());
        report.setEnrollmentStatus(enrollment.getStatus());

        // Existing enrollment completion percentage
        report.setCompletionPercentage(
                enrollment.getCompletionPercentage()
        );

        // IMPORTANT:
        // Use the same overall progress calculation as the dashboard.
        report.setOverallProgress(
                studentDashboardService.getCourseOverallProgress(
                        student.getId(),
                        course.getId()
                )
        );

        // Course details
        report.setCourseId(course.getId());
        report.setCourseTitle(course.getTitle());
        report.setCourseCode(course.getCourseCode());
        report.setCourseDescription(course.getDescription());
        report.setCourseDurationDays(course.getDurationDays());
        report.setCourseFees(course.getTotalFees());

        if (course.getFaculty() != null) {

            report.setFacultyName(
                    joinName(
                            course.getFaculty().getFirstName(),
                            course.getFaculty().getLastName()
                    )
            );
        }

        // Exam performance
        List<Result> results =
                resultRepository
                        .findByStudent_IdAndExam_Course_IdOrderByCreatedAtDesc(
                                student.getId(),
                                course.getId()
                        );

        List<ExamPerformanceDTO> performances =
                results.stream()
                        .sorted(
                                Comparator.comparing(
                                        Result::getCreatedAt,
                                        Comparator.nullsLast(
                                                Comparator.naturalOrder()
                                        )
                                )
                        )
                        .map(this::toExamPerformance)
                        .toList();

        report.setExamPerformances(performances);
        report.setTotalExamsAttempted(performances.size());

        report.setExamsPassed(
                (int) performances.stream()
                        .filter(result ->
                                "PASS".equalsIgnoreCase(
                                        result.getStatus()
                                ))
                        .count()
        );

        report.setExamsFailed(
                (int) performances.stream()
                        .filter(result ->
                                "FAIL".equalsIgnoreCase(
                                        result.getStatus()
                                ))
                        .count()
        );

        double average =
                performances.stream()
                        .map(ExamPerformanceDTO::getPercentage)
                        .filter(value -> value != null)
                        .mapToDouble(Double::doubleValue)
                        .average()
                        .orElse(0.0);

        report.setAveragePercentage(
                Math.round(average * 100.0) / 100.0
        );

        return report;
    }

    // ==========================================
    // MAP EXAM RESULT
    // ==========================================

    private ExamPerformanceDTO toExamPerformance(Result result) {

        ExamPerformanceDTO dto =
                new ExamPerformanceDTO();

        dto.setResultId(result.getId());

        if (result.getExam() != null) {

            dto.setExamId(result.getExam().getId());
            dto.setExamTitle(result.getExam().getTitle());
            dto.setTotalMarks(result.getExam().getTotalMarks());
        }

        dto.setObtainedMarks(result.getObtainedMarks());
        dto.setPercentage(result.getPercentage());
        dto.setStatus(result.getStatus());
        dto.setCorrectAnswers(result.getCorrectAnswers());
        dto.setWrongAnswers(result.getWrongAnswers());
        dto.setSkippedQuestions(result.getSkippedQuestions());

        if (result.getExamAttempt() != null) {

            dto.setSubmissionDate(
                    result.getExamAttempt().getSubmissionTime()
            );
        }

        return dto;
    }

    // ==========================================
    // GENERATE AND REGISTER PDF
    // ==========================================

    private byte[] generateAndRegisterPdf(
            StudentProgressReportDTO report) {

        String reportId = UUID.randomUUID().toString();

        LocalDateTime generatedAt = LocalDateTime.now();

        report.setReportId(reportId);
        report.setGeneratedAt(generatedAt);

        report.setVerificationCode(reportId);

        byte[] pdfBytes =
                pdfGenerator.generatePdf(report);

        String sha256 =
                pdfGenerator.calculateSha256(pdfBytes);

        ReportMetadata metadata =
                new ReportMetadata();

        metadata.setReportId(reportId);
        metadata.setStudentId(report.getStudentId());
        metadata.setCourseId(report.getCourseId());
        metadata.setGeneratedAt(generatedAt);
        metadata.setPdfSha256(sha256);

        reportMetadataRepository.save(metadata);

        return pdfBytes;
    }

    // ==========================================
    // AUTHENTICATED USER
    // ==========================================

    private User getAuthenticatedUser() {

        Authentication authentication =
                SecurityContextHolder.getContext()
                        .getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication.getPrincipal() == null
                || "anonymousUser".equals(
                        authentication.getPrincipal())) {

            throw new AccessDeniedException(
                    "Authentication is required");
        }

        String email = authentication.getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new AccessDeniedException(
                                "Authenticated user was not found"));
    }

    // ==========================================
    // GET COURSE
    // ==========================================

    private Course getCourse(Long courseId) {

        return courseRepository.findById(courseId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Course not found"));
    }

    // ==========================================
    // VERIFY FACULTY OWNERSHIP
    // ==========================================

    private void verifyFacultyOwnsCourse(
            User faculty,
            Course course) {

        if (course.getFaculty() == null
                || !course.getFaculty().getId()
                        .equals(faculty.getId())) {

            throw new AccessDeniedException(
                    "You are not authorized to access reports for this course");
        }
    }

    // ==========================================
    // JOIN FIRST NAME AND LAST NAME
    // ==========================================

    private String joinName(
            String firstName,
            String lastName) {

        String first =
                firstName == null ? "" : firstName.trim();

        String last =
                lastName == null ? "" : lastName.trim();

        String fullName =
                (first + " " + last).trim();

        return fullName.isEmpty()
                ? "Not available"
                : fullName;
    }
}