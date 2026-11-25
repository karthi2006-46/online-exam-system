package com.lms.service;

import com.lms.dto.StudentProgressReportDTO;
import com.lms.dto.admin.AdminApprovalRowDTO;
import com.lms.dto.admin.AdminCourseReportRowDTO;
import com.lms.dto.admin.AdminCourseRowDTO;
import com.lms.dto.admin.AdminExtensionRowDTO;
import com.lms.dto.admin.AdminFacultyRowDTO;
import com.lms.dto.admin.AdminFacultyWorkloadDTO;
import com.lms.dto.admin.AdminPageDTO;
import com.lms.dto.admin.AdminPerformanceRowDTO;
import com.lms.dto.admin.AdminReportOverviewDTO;
import com.lms.dto.admin.AdminResultRowDTO;
import com.lms.dto.admin.AdminStudentReportDTO;
import com.lms.dto.admin.AdminStudentReportDTO.CodingRow;
import com.lms.dto.admin.AdminStudentReportDTO.CourseSection;
import com.lms.dto.admin.AdminStudentReportDTO.ExamRow;
import com.lms.dto.admin.AdminStudentReportDTO.ExtensionRow;
import com.lms.dto.admin.AdminStudentRowDTO;

import lombok.RequiredArgsConstructor;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Excel / PDF downloads for the admin module.
 * Row data is produced by the regular admin services so exports can never
 * drift away from what the pages show.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminExportService {

    private static final int PAGE_SIZE = 100;

    private static final DateTimeFormatter FILE_STAMP =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmm");

    private final AdminFacultyService facultyService;
    private final AdminStudentService studentService;
    private final AdminCourseService courseService;
    private final AdminApprovalService approvalService;
    private final AdminExtensionService extensionService;
    private final AdminPerformanceService performanceService;
    private final AdminReportService reportService;
    private final StudentProgressReportService progressReportService;
    private final StudentProgressPdfGenerator pdfGenerator;
    private final AdminReportPdfGenerator reportPdfGenerator;
    private final AdminStudentReportService studentReportService;
    private final AdminStudentFullReportPdfGenerator fullReportPdfGenerator;

    // ==========================================
    // FACULTY
    // ==========================================

    public byte[] facultyExcel(String search, Boolean active) {

        List<AdminFacultyRowDTO> rows = new ArrayList<>();

        int page = 0;
        AdminPageDTO<AdminFacultyRowDTO> batch;

        do {
            batch = facultyService.list(search, active, page, PAGE_SIZE);
            rows.addAll(batch.getContent());
            page++;
        } while (!batch.isLast());

        List<String[]> data = rows.stream()
                .map(row -> new String[]{
                        String.valueOf(row.getFacultyId()),
                        row.getName(),
                        row.getEmail(),
                        row.getPhone(),
                        Boolean.TRUE.equals(row.getActive()) ? "Active" : "Inactive",
                        String.join(", ", row.getAssignedCourses()),
                        String.valueOf(row.getTotalStudents()),
                        String.valueOf(row.getActiveStudents()),
                        String.valueOf(row.getTotalResults()),
                        formatPercent(row.getPassPercentage())
                })
                .toList();

        return writeWorkbook(
                "Faculty",
                "Faculty accounts",
                new String[]{
                        "ID", "Name", "Email", "Phone", "Status",
                        "Assigned courses", "Students", "Active students",
                        "Results", "Pass rate"
                },
                data
        );
    }

    // ==========================================
    // STUDENTS
    // ==========================================

    public byte[] studentsExcel(
            String search,
            Boolean active,
            Long courseId) {

        List<AdminStudentRowDTO> rows = new ArrayList<>();

        int page = 0;
        AdminPageDTO<AdminStudentRowDTO> batch;

        do {
            batch = studentService.list(
                    search, active, courseId, page, PAGE_SIZE);
            rows.addAll(batch.getContent());
            page++;
        } while (!batch.isLast());

        List<String[]> data = rows.stream()
                .map(row -> new String[]{
                        String.valueOf(row.getStudentId()),
                        row.getName(),
                        row.getEmail(),
                        row.getPhone(),
                        Boolean.TRUE.equals(row.getActive()) ? "Active" : "Inactive",
                        String.valueOf(row.getCourseCount()),
                        String.valueOf(row.getActiveCourses()),
                        String.valueOf(row.getCompletedCourses()),
                        String.valueOf(row.getTotalResults()),
                        String.valueOf(row.getPassedResults()),
                        String.valueOf(row.getFailedResults())
                })
                .toList();

        return writeWorkbook(
                "Students",
                "Student accounts",
                new String[]{
                        "ID", "Name", "Email", "Phone", "Status",
                        "Courses", "Active courses", "Completed courses",
                        "Results", "Passed", "Failed"
                },
                data
        );
    }

    // ==========================================
    // COURSES
    // ==========================================

    public byte[] coursesExcel(
            String search,
            Boolean active,
            Long facultyId) {

        List<AdminCourseRowDTO> rows = new ArrayList<>();

        int page = 0;
        AdminPageDTO<AdminCourseRowDTO> batch;

        do {
            batch = courseService.list(
                    search, active, facultyId, page, PAGE_SIZE);
            rows.addAll(batch.getContent());
            page++;
        } while (!batch.isLast());

        List<String[]> data = rows.stream()
                .map(row -> new String[]{
                        String.valueOf(row.getId()),
                        row.getTitle(),
                        row.getCourseCode(),
                        Boolean.TRUE.equals(row.getActive())
                                ? "Active" : "Inactive",
                        row.getFacultyName() == null ? "" : row.getFacultyName(),
                        String.valueOf(row.getDurationDays()),
                        row.getTotalFees() == null ? "" : row.getTotalFees().toString(),
                        String.valueOf(row.getTotalStudents()),
                        String.valueOf(row.getActiveStudents()),
                        String.valueOf(row.getCompletedStudents()),
                        String.valueOf(row.getTotalExams()),
                        String.valueOf(row.getTotalMaterials()),
                        String.valueOf(row.getTotalChallenges()),
                        String.valueOf(row.getTotalResults()),
                        formatPercent(row.getPassPercentage()),
                        formatPercent(row.getAveragePercentage())
                })
                .toList();

        return writeWorkbook(
                "Courses",
                "Course management",
                new String[]{
                        "ID", "Course", "Code", "Status", "Faculty",
                        "Duration (days)", "Fees", "Students", "Active students",
                        "Completed", "Exams", "Materials", "Challenges",
                        "Results", "Pass rate", "Average score"
                },
                data
        );
    }

    // ==========================================
    // EXAM APPROVALS
    // ==========================================

    public byte[] approvalsExcel(
            String status,
            Long courseId,
            Long facultyId,
            String search) {

        List<AdminApprovalRowDTO> rows = new ArrayList<>();

        int page = 0;
        AdminPageDTO<AdminApprovalRowDTO> batch;

        do {
            batch = approvalService.list(
                    status, courseId, facultyId, search, page, PAGE_SIZE);
            rows.addAll(batch.getContent());
            page++;
        } while (!batch.isLast());

        DateTimeFormatter dateFormat =
                DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

        List<String[]> data = rows.stream()
                .map(row -> new String[]{
                        String.valueOf(row.getId()),
                        String.valueOf(row.getStudentId()),
                        row.getStudentName(),
                        row.getEmail(),
                        row.getCourseTitle(),
                        row.getFacultyName() == null ? "" : row.getFacultyName(),
                        row.getStatus(),
                        row.getRequestedAt() == null
                                ? "" : row.getRequestedAt().format(dateFormat),
                        row.getReviewedAt() == null
                                ? "" : row.getReviewedAt().format(dateFormat),
                        row.getComment() == null ? "" : row.getComment()
                })
                .toList();

        return writeWorkbook(
                "Exam approvals",
                "Exam approval requests",
                new String[]{
                        "Request ID", "Student ID", "Student", "Email",
                        "Course", "Faculty", "Status", "Requested",
                        "Reviewed", "Comment"
                },
                data
        );
    }

    // ==========================================
    // EXTENSION REQUESTS
    // ==========================================

    public byte[] extensionsExcel(
            String status,
            Long courseId,
            Long facultyId,
            String search) {

        List<AdminExtensionRowDTO> rows = new ArrayList<>();

        int page = 0;
        AdminPageDTO<AdminExtensionRowDTO> batch;

        do {
            batch = extensionService.list(
                    status, courseId, facultyId, search, page, PAGE_SIZE);
            rows.addAll(batch.getContent());
            page++;
        } while (!batch.isLast());

        DateTimeFormatter dateFormat =
                DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

        List<String[]> data = rows.stream()
                .map(row -> new String[]{
                        String.valueOf(row.getId()),
                        String.valueOf(row.getStudentId()),
                        row.getStudentName(),
                        row.getEmail(),
                        row.getCourseTitle(),
                        row.getFacultyName() == null ? "" : row.getFacultyName(),
                        String.valueOf(row.getDaysRequested()),
                        row.getDueDate() == null
                                ? "" : row.getDueDate().format(dateFormat),
                        row.getStatus(),
                        row.getReason() == null ? "" : row.getReason(),
                        row.getRejectionReason() == null
                                ? "" : row.getRejectionReason(),
                        row.getReviewedByName() == null
                                ? "" : row.getReviewedByName(),
                        row.getReviewedAt() == null
                                ? "" : row.getReviewedAt().format(dateFormat)
                })
                .toList();

        return writeWorkbook(
                "Extensions",
                "Course extension requests",
                new String[]{
                        "Request ID", "Student ID", "Student", "Email",
                        "Course", "Faculty", "Days requested", "Due date",
                        "Status", "Reason", "Rejection reason", "Reviewed by",
                        "Reviewed at"
                },
                data
        );
    }

    // ==========================================
    // PERFORMANCE MATRIX
    // ==========================================

    public byte[] performanceExcel(
            Long courseId,
            Long facultyId,
            String status,
            String approvalStatus,
            String search) {

        List<AdminPerformanceRowDTO> rows = new ArrayList<>();

        int page = 0;
        AdminPageDTO<AdminPerformanceRowDTO> batch;

        do {
            batch = performanceService.performance(
                    courseId,
                    facultyId,
                    status,
                    approvalStatus,
                    search,
                    page,
                    PAGE_SIZE
            );
            rows.addAll(batch.getContent());
            page++;
        } while (!batch.isLast());

        List<String[]> data = rows.stream()
                .map(row -> new String[]{
                        String.valueOf(row.getStudentId()),
                        row.getStudentName(),
                        row.getEmail(),
                        row.getCourseTitle(),
                        row.getFacultyName(),
                        row.getEnrollmentStatus(),
                        formatPercent(row.getMaterialsProgress()),
                        formatPercent(row.getChallengesProgress()),
                        formatPercent(row.getOverallProgress()),
                        row.getExamApprovalStatus(),
                        row.getCodingApprovalStatus(),
                        row.getLatestExamTitle(),
                        row.getLatestExamMarks() == null
                                ? "" : String.valueOf(row.getLatestExamMarks()),
                        formatPercent(row.getLatestExamPercentage()),
                        row.getOverallResult()
                })
                .toList();

        return writeWorkbook(
                "Performance",
                "Student performance",
                new String[]{
                        "Student ID", "Student", "Email", "Course", "Faculty",
                        "Enrollment", "Materials %", "Challenges %", "Overall %",
                        "Exam approval", "Coding review", "Latest exam",
                        "Marks", "Exam %", "Overall result"
                },
                data
        );
    }

    // ==========================================
    // EXAMINATION RESULTS
    // ==========================================

    public byte[] resultsExcel(
            Long courseId,
            Long examId,
            LocalDateTime fromDate,
            LocalDateTime toDate,
            String status) {

        List<AdminResultRowDTO> rows = new ArrayList<>();

        int page = 0;
        AdminPageDTO<AdminResultRowDTO> batch;

        do {
            batch = performanceService.results(
                    courseId,
                    examId,
                    fromDate,
                    toDate,
                    status,
                    page,
                    PAGE_SIZE
            );
            rows.addAll(batch.getContent());
            page++;
        } while (!batch.isLast());

        DateTimeFormatter dateFormat =
                DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

        List<String[]> data = rows.stream()
                .map(row -> new String[]{
                        String.valueOf(row.getResultId()),
                        String.valueOf(row.getStudentId()),
                        row.getStudentName(),
                        row.getEmail(),
                        row.getCourseTitle(),
                        row.getExamTitle(),
                        row.getExamDate() == null
                                ? "" : row.getExamDate().format(dateFormat),
                        row.getTotalMarks() == null
                                ? "" : String.valueOf(row.getTotalMarks()),
                        row.getObtainedMarks() == null
                                ? "" : String.valueOf(row.getObtainedMarks()),
                        formatPercent(row.getPercentage()),
                        row.getResultStatus(),
                        String.valueOf(row.getCorrectAnswers()),
                        String.valueOf(row.getWrongAnswers()),
                        String.valueOf(row.getSkippedQuestions())
                })
                .toList();

        return writeWorkbook(
                "Results",
                "Examination results",
                new String[]{
                        "Result ID", "Student ID", "Student", "Email",
                        "Course", "Exam", "Submitted", "Total marks",
                        "Obtained", "Percentage", "Status",
                        "Correct", "Wrong", "Skipped"
                },
                data
        );
    }

    // ==========================================
    // PLATFORM REPORTS
    // ==========================================

    public byte[] courseReportExcel() {

        AdminReportOverviewDTO report = reportService.overview();

        List<String[]> data = report.getCourses().stream()
                .map(row -> new String[]{
                        String.valueOf(row.getCourseId()),
                        row.getTitle(),
                        row.getCourseCode(),
                        Boolean.TRUE.equals(row.getActive())
                                ? "Active" : "Inactive",
                        row.getFacultyName() == null ? "" : row.getFacultyName(),
                        String.valueOf(row.getStudents()),
                        String.valueOf(row.getActiveStudents()),
                        String.valueOf(row.getCompletedStudents()),
                        String.valueOf(row.getExams()),
                        String.valueOf(row.getResults()),
                        String.valueOf(row.getPassedResults()),
                        String.valueOf(row.getFailedResults()),
                        formatPercent(row.getPassPercentage()),
                        formatPercent(row.getAveragePercentage())
                })
                .toList();

        return writeWorkbook(
                "Courses",
                "Course performance report",
                new String[]{
                        "Course ID", "Course", "Code", "Status", "Faculty",
                        "Students", "Active students", "Completed",
                        "Exams", "Results", "Passed", "Failed",
                        "Pass rate", "Average score"
                },
                data
        );
    }

    public byte[] facultyWorkloadExcel() {

        AdminReportOverviewDTO report = reportService.overview();

        List<String[]> data = report.getFaculty().stream()
                .map(row -> new String[]{
                        String.valueOf(row.getFacultyId()),
                        row.getName(),
                        row.getEmail(),
                        Boolean.TRUE.equals(row.getActive())
                                ? "Active" : "Inactive",
                        String.valueOf(row.getCourses()),
                        String.valueOf(row.getStudents()),
                        String.valueOf(row.getExams()),
                        String.valueOf(row.getPendingExamApprovals()),
                        String.valueOf(row.getPendingExtensions()),
                        String.valueOf(row.getPendingCodingReviews())
                })
                .toList();

        return writeWorkbook(
                "Faculty workload",
                "Faculty workload report",
                new String[]{
                        "Faculty ID", "Name", "Email", "Status",
                        "Courses", "Students", "Exams",
                        "Pending approvals", "Pending extensions",
                        "Pending coding reviews"
                },
                data
        );
    }

    public byte[] platformReportPdf() {

        AdminReportOverviewDTO report = reportService.overview();

        return reportPdfGenerator.generate(report);
    }

    // ==========================================
    // STUDENT PROGRESS PDF
    // ==========================================

    public byte[] studentProgressPdf(Long studentId, Long courseId) {

        StudentProgressReportDTO report =
                progressReportService.buildStudentCourseReport(
                        studentId, courseId);

        report.setReportId(
                "ADM-" + UUID.randomUUID()
                        .toString()
                        .substring(0, 8)
                        .toUpperCase());
        report.setGeneratedAt(LocalDateTime.now());
        report.setVerificationCode(
                pdfGenerator.calculateSha256(
                        (studentId + ":" + courseId + ":" + report.getReportId())
                                .getBytes(java.nio.charset.StandardCharsets.UTF_8)
                ).substring(0, 16).toUpperCase());

        return pdfGenerator.generatePdf(report);
    }

    public String fileStamp() {
        return LocalDateTime.now().format(FILE_STAMP);
    }

    // ==========================================
    // COMPLETE STUDENT REPORTS
    // ==========================================

    public AdminStudentReportDTO studentReport(Long studentId) {
        return studentReportService.build(studentId);
    }

    public byte[] studentFullReportPdf(Long studentId) {

        AdminStudentReportDTO report =
                studentReportService.build(studentId);

        return fullReportPdfGenerator.generate(report);
    }

    public byte[] studentFullReportExcel(Long studentId) {
        return studentWorkbook(studentReportService.build(studentId));
    }

    public byte[] courseStudentsReportExcel(Long courseId) {

        AdminCourseRowDTO course = courseService.detail(courseId);

        List<AdminStudentRowDTO> students = new ArrayList<>();

        int page = 0;
        AdminPageDTO<AdminStudentRowDTO> batch;

        do {
            batch = studentService.list(null, null, courseId, page, PAGE_SIZE);
            students.addAll(batch.getContent());
            page++;
        } while (!batch.isLast());

        List<String[]> studentRows = new ArrayList<>();

        int active = 0;
        int inactive = 0;
        int completed = 0;

        for (AdminStudentRowDTO row : students) {

            AdminStudentReportDTO report =
                    studentReportService.build(row.getStudentId());

            CourseSection section = report.getCourses().stream()
                    .filter(item -> courseId.equals(item.getCourseId()))
                    .findFirst()
                    .orElse(null);

            if (section == null) {
                continue;
            }

            String status = section.getEnrollmentStatus() == null
                    ? "" : section.getEnrollmentStatus().toUpperCase();

            if ("ACTIVE".equals(status)) {
                active++;
            } else if ("INACTIVE".equals(status)) {
                inactive++;
            } else if ("COMPLETED".equals(status)) {
                completed++;
            }

            studentRows.add(new String[]{
                    String.valueOf(row.getStudentId()),
                    row.getName(),
                    row.getEmail(),
                    section.getEnrollmentStatus(),
                    formatDate(section.getEnrollmentDate()),
                    formatDate(section.getDueDate()),
                    formatPercent(section.getCompletionPercentage()),
                    section.getMaterialsCompleted() + " / "
                            + section.getMaterialsTotal(),
                    String.valueOf(section.getCodingSubmissions()),
                    String.valueOf(section.getCodingApproved()),
                    section.getExamApprovalStatus(),
                    String.valueOf(section.getExamsAttempted()),
                    String.valueOf(section.getExamsPassed()),
                    String.valueOf(section.getExamsFailed()),
                    formatPercent(section.getAveragePercentage())
            });
        }

        List<SheetSpec> sheets = new ArrayList<>();

        sheets.add(new SheetSpec(
                "Course summary",
                new String[]{"Field", "Value"},
                List.of(
                        new String[]{"Course ID", String.valueOf(courseId)},
                        new String[]{"Course", course.getTitle()},
                        new String[]{"Code", course.getCourseCode()},
                        new String[]{"Faculty",
                                course.getFacultyName() == null
                                        ? "Not assigned"
                                        : course.getFacultyName()},
                        new String[]{"Total students",
                                String.valueOf(studentRows.size())},
                        new String[]{"Active", String.valueOf(active)},
                        new String[]{"Inactive", String.valueOf(inactive)},
                        new String[]{"Completed", String.valueOf(completed)}
                )));

        sheets.add(new SheetSpec(
                "Students",
                new String[]{
                        "Student ID", "Name", "Email", "Enrollment status",
                        "Enrolled", "Due date", "Completion %", "Materials",
                        "Coding submissions", "Coding approved",
                        "Exam approval", "Exams attempted", "Passed",
                        "Failed", "Average score"
                },
                studentRows));

        return writeSheets(
                "Course report - " + course.getTitle(),
                sheets);
    }

    public byte[] allStudentReportsZip() {

        Map<Long, String> students = new LinkedHashMap<>();

        int page = 0;
        AdminPageDTO<AdminStudentRowDTO> batch;

        do {
            batch = studentService.list(null, null, null, page, PAGE_SIZE);

            for (AdminStudentRowDTO row : batch.getContent()) {
                students.putIfAbsent(row.getStudentId(), row.getName());
            }

            page++;
        } while (!batch.isLast());

        try (ByteArrayOutputStream output = new ByteArrayOutputStream();
             ZipOutputStream zip = new ZipOutputStream(output)) {

            for (Map.Entry<Long, String> entry : students.entrySet()) {

                AdminStudentReportDTO report =
                        studentReportService.build(entry.getKey());

                String baseName = "Student_"
                        + entry.getKey()
                        + "_"
                        + safeFileName(entry.getValue());

                addZipEntry(
                        zip,
                        baseName + ".xlsx",
                        studentWorkbook(report));

                addZipEntry(
                        zip,
                        baseName + ".pdf",
                        fullReportPdfGenerator.generate(report));
            }

            zip.finish();

            return output.toByteArray();

        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Failed to build the student reports ZIP", exception);
        }
    }

    private void addZipEntry(
            ZipOutputStream zip,
            String name,
            byte[] bytes) throws IOException {

        zip.putNextEntry(new ZipEntry(name));
        zip.write(bytes);
        zip.closeEntry();
    }

    private byte[] studentWorkbook(AdminStudentReportDTO report) {

        List<SheetSpec> sheets = new ArrayList<>();

        sheets.add(new SheetSpec(
                "Student",
                new String[]{"Field", "Value"},
                List.of(
                        new String[]{"Student ID",
                                String.valueOf(report.getStudentId())},
                        new String[]{"Name",
                                report.getStudentName() == null
                                        ? "" : report.getStudentName()},
                        new String[]{"Email",
                                report.getEmail() == null
                                        ? "" : report.getEmail()},
                        new String[]{"Phone",
                                report.getPhone() == null
                                        ? "" : report.getPhone()},
                        new String[]{"Account status",
                                report.getAccountStatus()},
                        new String[]{"Enrolled courses",
                                String.valueOf(report.getEnrolledCourses())},
                        new String[]{"Active enrollments",
                                String.valueOf(report.getActiveCourses())},
                        new String[]{"Inactive enrollments",
                                String.valueOf(report.getInactiveCourses())},
                        new String[]{"Completed courses",
                                String.valueOf(report.getCompletedCourses())},
                        new String[]{"Exams attempted",
                                String.valueOf(report.getTotalResults())},
                        new String[]{"Exams passed",
                                String.valueOf(report.getPassedResults())},
                        new String[]{"Exams failed",
                                String.valueOf(report.getFailedResults())},
                        new String[]{"Average score",
                                formatPercent(report.getAveragePercentage())}
                )));

        List<String[]> courseRows = new ArrayList<>();
        List<String[]> codingRows = new ArrayList<>();
        List<String[]> examRows = new ArrayList<>();
        List<String[]> extensionRows = new ArrayList<>();

        for (CourseSection course : report.getCourses()) {

            courseRows.add(new String[]{
                    course.getCourseTitle(),
                    course.getCourseCode(),
                    course.getFacultyName() == null
                            ? "Not assigned" : course.getFacultyName(),
                    course.getEnrollmentStatus(),
                    formatDate(course.getEnrollmentDate()),
                    formatDate(course.getDueDate()),
                    formatPercent(course.getCompletionPercentage()),
                    course.getMaterialsCompleted() + " / "
                            + course.getMaterialsTotal(),
                    String.valueOf(course.getCodingSubmissions()),
                    String.valueOf(course.getCodingApproved()),
                    course.getExamApprovalStatus(),
                    String.valueOf(course.getExamsAttempted()),
                    formatPercent(course.getAveragePercentage())
            });

            for (CodingRow row : course.getCodingHistory()) {
                codingRows.add(new String[]{
                        course.getCourseTitle(),
                        row.getChallengeTitle(),
                        formatDate(row.getSubmittedAt()),
                        row.getTestCasesPassed() + " / "
                                + row.getTotalTestCases(),
                        formatPercent(row.getPercentage()),
                        row.getReviewStatus(),
                        row.getFacultyFeedback() == null
                                ? "" : row.getFacultyFeedback(),
                        row.getReviewedByName() == null
                                ? "" : row.getReviewedByName(),
                        formatDate(row.getReviewedAt())
                });
            }

            for (ExamRow row : course.getExamResults()) {
                examRows.add(new String[]{
                        course.getCourseTitle(),
                        row.getExamTitle(),
                        String.valueOf(row.getObtainedMarks()),
                        String.valueOf(row.getTotalMarks()),
                        formatPercent(row.getPercentage()),
                        row.getStatus(),
                        formatDate(row.getSubmittedAt())
                });
            }

            for (ExtensionRow row : course.getExtensionHistory()) {
                extensionRows.add(new String[]{
                        course.getCourseTitle(),
                        formatDate(row.getOriginalDueDate()),
                        String.valueOf(row.getDaysRequested()),
                        row.getReason(),
                        formatDate(row.getRequestedAt()),
                        row.getStatus(),
                        row.getReviewedByName() == null
                                ? "" : row.getReviewedByName(),
                        formatDate(row.getReviewedAt()),
                        row.getRejectionReason() == null
                                ? "" : row.getRejectionReason()
                });
            }
        }

        sheets.add(new SheetSpec(
                "Courses",
                new String[]{
                        "Course", "Code", "Faculty", "Enrollment status",
                        "Enrolled", "Due date", "Completion %", "Materials",
                        "Coding submissions", "Coding approved",
                        "Exam approval", "Exams attempted", "Average score"
                },
                courseRows));

        sheets.add(new SheetSpec(
                "Coding",
                new String[]{
                        "Course", "Challenge", "Submitted", "Test cases",
                        "Percentage", "Review status", "Feedback",
                        "Reviewed by", "Reviewed on"
                },
                codingRows));

        sheets.add(new SheetSpec(
                "Exams",
                new String[]{
                        "Course", "Exam", "Obtained", "Total",
                        "Percentage", "Status", "Submitted"
                },
                examRows));

        sheets.add(new SheetSpec(
                "Extensions",
                new String[]{
                        "Course", "Original due date", "Days", "Reason",
                        "Requested", "Status", "Reviewed by", "Reviewed on",
                        "Rejection reason"
                },
                extensionRows));

        return writeSheets(
                "Student report - "
                        + (report.getStudentName() == null
                                ? report.getStudentId()
                                : report.getStudentName()),
                sheets);
    }

    private String safeFileName(String value) {

        if (value == null || value.isBlank()) {
            return "student";
        }

        return value.replaceAll("[^A-Za-z0-9._-]+", "_");
    }

    private String formatDate(LocalDateTime value) {
        return value == null
                ? ""
                : value.format(DateTimeFormatter.ofPattern(
                        "yyyy-MM-dd HH:mm"));
    }

    // ==========================================
    // WORKBOOK WRITER
    // ==========================================

    private byte[] writeWorkbook(
            String sheetName,
            String title,
            String[] headers,
            List<String[]> rows) {

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet(sheetName);

            CellStyle titleStyle = workbook.createCellStyle();
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 15);
            titleFont.setColor(IndexedColors.DARK_BLUE.getIndex());
            titleStyle.setFont(titleFont);

            CellStyle metaStyle = workbook.createCellStyle();
            Font metaFont = workbook.createFont();
            metaFont.setFontHeightInPoints((short) 9);
            metaFont.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
            metaStyle.setFont(metaFont);

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(
                    IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setBorderBottom(BorderStyle.THIN);

            CellStyle bodyStyle = workbook.createCellStyle();
            bodyStyle.setBorderBottom(BorderStyle.HAIR);

            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue(title);
            titleCell.setCellStyle(titleStyle);

            Row metaRow = sheet.createRow(1);
            Cell metaCell = metaRow.createCell(0);
            metaCell.setCellValue(
                    "LearnHub LMS  •  Generated "
                            + LocalDateTime.now().format(
                                    DateTimeFormatter.ofPattern(
                                            "dd MMM yyyy, HH:mm")));
            metaCell.setCellStyle(metaStyle);

            Row headerRow = sheet.createRow(3);

            for (int index = 0; index < headers.length; index++) {
                Cell cell = headerRow.createCell(index);
                cell.setCellValue(headers[index]);
                cell.setCellStyle(headerStyle);
            }

            int rowIndex = 4;

            for (String[] values : rows) {

                Row row = sheet.createRow(rowIndex++);

                for (int index = 0; index < headers.length; index++) {
                    Cell cell = row.createCell(index);
                    cell.setCellValue(
                            index < values.length && values[index] != null
                                    ? values[index] : "");
                    cell.setCellStyle(bodyStyle);
                }
            }

            sheet.createFreezePane(0, 4);
            sheet.setAutoFilter(
                    new CellRangeAddress(
                            3, 3, 0, Math.max(0, headers.length - 1)));

            for (int index = 0; index < headers.length; index++) {
                sheet.autoSizeColumn(index);
                int width = sheet.getColumnWidth(index);
                sheet.setColumnWidth(index, Math.min(16000, width + 900));
            }

            workbook.write(output);

            return output.toByteArray();

        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Failed to build the Excel export", exception);
        }
    }

    private byte[] writeSheets(
            String title,
            List<SheetSpec> sheets) {

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {

            CellStyle titleStyle = workbook.createCellStyle();
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 15);
            titleFont.setColor(IndexedColors.DARK_BLUE.getIndex());
            titleStyle.setFont(titleFont);

            CellStyle metaStyle = workbook.createCellStyle();
            Font metaFont = workbook.createFont();
            metaFont.setFontHeightInPoints((short) 9);
            metaFont.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
            metaStyle.setFont(metaFont);

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(
                    IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setBorderBottom(BorderStyle.THIN);

            CellStyle bodyStyle = workbook.createCellStyle();
            bodyStyle.setBorderBottom(BorderStyle.HAIR);

            for (SheetSpec spec : sheets) {

                String name = spec.name == null || spec.name.isBlank()
                        ? "Sheet" : spec.name;

                if (name.length() > 31) {
                    name = name.substring(0, 31);
                }

                Sheet sheet = workbook.createSheet(name);

                Row titleRow = sheet.createRow(0);
                Cell titleCell = titleRow.createCell(0);
                titleCell.setCellValue(title + " - " + name);
                titleCell.setCellStyle(titleStyle);

                Row metaRow = sheet.createRow(1);
                Cell metaCell = metaRow.createCell(0);
                metaCell.setCellValue(
                        "LearnHub LMS  •  Generated "
                                + LocalDateTime.now().format(
                                        DateTimeFormatter.ofPattern(
                                                "dd MMM yyyy, HH:mm")));
                metaCell.setCellStyle(metaStyle);

                Row headerRow = sheet.createRow(3);

                for (int index = 0; index < spec.headers.length; index++) {
                    Cell cell = headerRow.createCell(index);
                    cell.setCellValue(spec.headers[index]);
                    cell.setCellStyle(headerStyle);
                }

                int rowIndex = 4;

                for (String[] values : spec.rows) {

                    Row row = sheet.createRow(rowIndex++);

                    for (int index = 0;
                            index < spec.headers.length;
                            index++) {

                        Cell cell = row.createCell(index);
                        cell.setCellValue(
                                index < values.length && values[index] != null
                                        ? values[index] : "");
                        cell.setCellStyle(bodyStyle);
                    }
                }

                sheet.createFreezePane(0, 4);
                sheet.setAutoFilter(new CellRangeAddress(
                        3, 3, 0, Math.max(0, spec.headers.length - 1)));

                for (int index = 0;
                        index < spec.headers.length;
                        index++) {

                    sheet.autoSizeColumn(index);
                    int width = sheet.getColumnWidth(index);
                    sheet.setColumnWidth(
                            index, Math.min(16000, width + 900));
                }
            }

            workbook.write(output);

            return output.toByteArray();

        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Failed to build the Excel export", exception);
        }
    }

    private static class SheetSpec {

        private final String name;
        private final String[] headers;
        private final List<String[]> rows;

        private SheetSpec(
                String name,
                String[] headers,
                List<String[]> rows) {
            this.name = name;
            this.headers = headers;
            this.rows = rows;
        }
    }

    private String formatPercent(Double value) {
        return value == null
                ? ""
                : String.format(java.util.Locale.ROOT, "%.2f%%", value);
    }

    private String formatPercent(double value) {
        return String.format(java.util.Locale.ROOT, "%.2f%%", value);
    }
}
