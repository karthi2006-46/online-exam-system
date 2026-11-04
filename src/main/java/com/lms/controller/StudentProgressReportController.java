package com.lms.controller;

import com.lms.entity.ReportMetadata;
import com.lms.repository.ReportMetadataRepository;
import com.lms.service.StudentProgressReportService;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/reports")
public class StudentProgressReportController {

    private final StudentProgressReportService reportService;
    private final ReportMetadataRepository reportMetadataRepository;

    public StudentProgressReportController(
            StudentProgressReportService reportService,
            ReportMetadataRepository reportMetadataRepository) {

        this.reportService = reportService;
        this.reportMetadataRepository = reportMetadataRepository;
    }

    /**
     * Download one student's academic progress report.
     */
    @GetMapping(
            "/student-progress/{studentId}/course/{courseId}/pdf"
    )
    public ResponseEntity<byte[]> downloadStudentReport(
            @PathVariable Long studentId,
            @PathVariable Long courseId) {

        byte[] pdf = reportService.generateStudentReport(
                studentId,
                courseId
        );

        String filename =
                "Student_" + studentId + "_Progress_Report.pdf";

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(filename)
                                .build()
                                .toString()
                )
                .contentLength(pdf.length)
                .body(pdf);
    }

    /**
     * Download every enrolled student's report in one ZIP.
     * Includes ACTIVE, COMPLETED, and DROPPED enrollments.
     */
    @GetMapping("/course-progress/{courseId}/pdf/zip")
    public ResponseEntity<byte[]> downloadCourseReportsZip(
            @PathVariable Long courseId) {

        byte[] zip = reportService.generateCourseReportsZip(courseId);

        String filename = "Course_" + courseId + "_Student_Reports.zip";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/zip"))
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(filename)
                                .build()
                                .toString()
                )
                .contentLength(zip.length)
                .body(zip);
    }

    /**
     * Verify whether a report ID exists in the database.
     *
     * This checks report registration, not the integrity of an uploaded
     * PDF file. File integrity requires comparing its SHA-256 hash.
     */
    @GetMapping("/verify/{reportId}")
    public ResponseEntity<Map<String, Object>> verifyReport(
            @PathVariable String reportId) {

        Optional<ReportMetadata> optional =
                reportMetadataRepository.findByReportId(reportId);

        Map<String, Object> response = new LinkedHashMap<>();

        if (optional.isEmpty()) {

            response.put("valid", false);
            response.put("message", "Report ID not found");

            return ResponseEntity.status(404).body(response);
        }

        ReportMetadata metadata = optional.get();

        response.put("valid", true);
        response.put("message", "Report ID is registered");
        response.put("reportId", metadata.getReportId());
        response.put("studentId", metadata.getStudentId());
        response.put("courseId", metadata.getCourseId());
        response.put("generatedAt", metadata.getGeneratedAt());

        return ResponseEntity.ok(response);
    }
}
