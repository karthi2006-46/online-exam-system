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
import java.util.List;
import java.util.UUID;

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

    private String formatPercent(Double value) {
        return value == null
                ? ""
                : String.format(java.util.Locale.ROOT, "%.2f%%", value);
    }

    private String formatPercent(double value) {
        return String.format(java.util.Locale.ROOT, "%.2f%%", value);
    }
}
