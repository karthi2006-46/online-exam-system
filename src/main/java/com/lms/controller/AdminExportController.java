package com.lms.controller;

import com.lms.service.AdminExportService;

import lombok.RequiredArgsConstructor;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/admin/export")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminExportController {

    private static final MediaType XLSX = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final AdminExportService exportService;

    @GetMapping("/faculty.xlsx")
    public ResponseEntity<byte[]> facultyExcel(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean active) {

        return xlsx(
                exportService.facultyExcel(search, active),
                "faculty-" + exportService.fileStamp() + ".xlsx"
        );
    }

    @GetMapping("/students.xlsx")
    public ResponseEntity<byte[]> studentsExcel(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) Long courseId) {

        return xlsx(
                exportService.studentsExcel(search, active, courseId),
                "students-" + exportService.fileStamp() + ".xlsx"
        );
    }

    @GetMapping("/performance.xlsx")
    public ResponseEntity<byte[]> performanceExcel(
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) Long facultyId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String approvalStatus,
            @RequestParam(required = false) String search) {

        return xlsx(
                exportService.performanceExcel(
                        courseId, facultyId, status, approvalStatus, search),
                "student-performance-" + exportService.fileStamp() + ".xlsx"
        );
    }

    @GetMapping("/results.xlsx")
    public ResponseEntity<byte[]> resultsExcel(
            @RequestParam Long courseId,
            @RequestParam(required = false) Long examId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) String status) {

        return xlsx(
                exportService.resultsExcel(
                        courseId,
                        examId,
                        fromDate == null ? null : fromDate.atStartOfDay(),
                        toDate == null ? null : toDate.atTime(23, 59, 59),
                        status
                ),
                "exam-results-" + exportService.fileStamp() + ".xlsx"
        );
    }

    @GetMapping("/courses.xlsx")
    public ResponseEntity<byte[]> coursesExcel(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) Long facultyId) {

        return xlsx(
                exportService.coursesExcel(search, active, facultyId),
                "courses-" + exportService.fileStamp() + ".xlsx"
        );
    }

    @GetMapping("/approvals.xlsx")
    public ResponseEntity<byte[]> approvalsExcel(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) Long facultyId,
            @RequestParam(required = false) String search) {

        return xlsx(
                exportService.approvalsExcel(
                        status, courseId, facultyId, search),
                "exam-approvals-" + exportService.fileStamp() + ".xlsx"
        );
    }

    @GetMapping("/extensions.xlsx")
    public ResponseEntity<byte[]> extensionsExcel(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) Long facultyId,
            @RequestParam(required = false) String search) {

        return xlsx(
                exportService.extensionsExcel(
                        status, courseId, facultyId, search),
                "extension-requests-" + exportService.fileStamp() + ".xlsx"
        );
    }

    @GetMapping("/report/courses.xlsx")
    public ResponseEntity<byte[]> courseReportExcel() {

        return xlsx(
                exportService.courseReportExcel(),
                "course-report-" + exportService.fileStamp() + ".xlsx"
        );
    }

    @GetMapping("/report/faculty.xlsx")
    public ResponseEntity<byte[]> facultyWorkloadExcel() {

        return xlsx(
                exportService.facultyWorkloadExcel(),
                "faculty-workload-" + exportService.fileStamp() + ".xlsx"
        );
    }

    @GetMapping("/report/platform.pdf")
    public ResponseEntity<byte[]> platformReportPdf() {

        ContentDisposition disposition = ContentDisposition.attachment()
                .filename("platform-report-"
                        + exportService.fileStamp() + ".pdf")
                .build();

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        disposition.toString())
                .body(exportService.platformReportPdf());
    }

    /**
     * One student's full progress report for one course, as a PDF.
     */
    @GetMapping("/pdf/student/{studentId}")
    public ResponseEntity<byte[]> studentProgressPdf(
            @PathVariable Long studentId,
            @RequestParam Long courseId) {

        byte[] pdf = exportService.studentProgressPdf(studentId, courseId);

        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(
                        "progress-report-" + studentId + "-" + courseId + ".pdf")
                .build();

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        disposition.toString())
                .body(pdf);
    }

    // ==========================================
    // COMPLETE STUDENT REPORTS
    // ==========================================

    @GetMapping("/student/{studentId}/report")
    public ResponseEntity<com.lms.dto.admin.AdminStudentReportDTO> studentReport(
            @PathVariable Long studentId) {

        return ResponseEntity.ok(
                exportService.studentReport(studentId));
    }

    @GetMapping("/student/{studentId}/report.xlsx")
    public ResponseEntity<byte[]> studentReportExcel(
            @PathVariable Long studentId) {

        return xlsx(
                exportService.studentFullReportExcel(studentId),
                "student-report-" + studentId + "-"
                        + exportService.fileStamp() + ".xlsx"
        );
    }

    @GetMapping("/student/{studentId}/report.pdf")
    public ResponseEntity<byte[]> studentReportPdf(
            @PathVariable Long studentId) {

        ContentDisposition disposition = ContentDisposition.attachment()
                .filename("student-report-" + studentId + "-"
                        + exportService.fileStamp() + ".pdf")
                .build();

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        disposition.toString())
                .body(exportService.studentFullReportPdf(studentId));
    }

    @GetMapping("/course/{courseId}/students.xlsx")
    public ResponseEntity<byte[]> courseStudentsExcel(
            @PathVariable Long courseId) {

        return xlsx(
                exportService.courseStudentsReportExcel(courseId),
                "course-" + courseId + "-students-"
                        + exportService.fileStamp() + ".xlsx"
        );
    }

    @GetMapping("/students/all-reports.zip")
    public ResponseEntity<byte[]> allStudentReportsZip() {

        ContentDisposition disposition = ContentDisposition.attachment()
                .filename("all-student-reports-"
                        + exportService.fileStamp() + ".zip")
                .build();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/zip"))
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        disposition.toString())
                .body(exportService.allStudentReportsZip());
    }

    private ResponseEntity<byte[]> xlsx(byte[] file, String filename) {

        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(filename)
                .build();

        return ResponseEntity.ok()
                .contentType(XLSX)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(file);
    }
}
