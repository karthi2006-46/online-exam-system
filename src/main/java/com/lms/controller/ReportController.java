package com.lms.controller;

import com.lms.entity.Course;
import com.lms.entity.User;
import com.lms.entity.Result;
import com.lms.exception.ResourceNotFoundException;
import com.lms.repository.CourseRepository;
import com.lms.repository.ResultRepository;
import com.lms.repository.UserRepository;
import com.lms.service.ReportService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
@CrossOrigin(origins = "*")
public class ReportController {

    @Autowired
    private ReportService reportService;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ResultRepository resultRepository;

    private static final DateTimeFormatter FILE_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    // =========================================================
    // EXPORT ALL RESULTS FOR A COURSE
    // =========================================================

    @GetMapping("/export/course-results/{courseId}")
    @PreAuthorize("hasRole('FACULTY')")
    public ResponseEntity<byte[]> exportCourseResults(
            @PathVariable Long courseId,
            Authentication authentication
    ) throws IOException {

        verifyFacultyCourseAccess(courseId, authentication);

        byte[] excelData =
                reportService.generateCourseResultsExcel(courseId);

        return excelResponse(
                excelData,
                "course_results_" + courseId + ".xlsx"
        );
    }

    // =========================================================
    // EXPORT FILTERED COURSE RESULTS
    // =========================================================

    @GetMapping("/export/course-results/{courseId}/filtered")
    @PreAuthorize("hasRole('FACULTY')")
    public ResponseEntity<byte[]> exportFilteredCourseResults(
            @PathVariable Long courseId,

            @RequestParam(required = false)
            Long examId,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fromDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate toDate,

            @RequestParam(required = false)
            String status,

            Authentication authentication
    ) throws IOException {

        verifyFacultyCourseAccess(courseId, authentication);

        if (examId != null) {
            List<Result> examResults =
                    resultRepository.findByExam_Id(examId);

            boolean examBelongsToCourse = examResults.stream()
                    .anyMatch(result ->
                            result.getExam().getCourse().getId()
                                    .equals(courseId));

            // Also handle exams with no results.
            if (!examBelongsToCourse) {
                boolean examExistsInCourse =
                        resultRepository.findByExam_Course_Id(courseId)
                                .stream()
                                .anyMatch(result ->
                                        result.getExam().getId()
                                                .equals(examId));

                if (!examExistsInCourse) {
                    throw new IllegalArgumentException(
                            "The selected exam does not belong to this course."
                    );
                }
            }
        }

        LocalDateTime fromDateTime =
                fromDate == null
                        ? null
                        : fromDate.atStartOfDay();

        // Inclusive end date: include the entire selected day.
        LocalDateTime toDateTime =
                toDate == null
                        ? null
                        : toDate.plusDays(1).atStartOfDay()
                                .minusNanos(1);

        byte[] excelData =
                reportService.generateFilteredCourseResultsExcel(
                        courseId,
                        examId,
                        fromDateTime,
                        toDateTime,
                        status
                );

        return excelResponse(
                excelData,
                "filtered_course_results_" + courseId + ".xlsx"
        );
    }

    // =========================================================
    // EXPORT RESULTS FOR ONE EXAM
    // =========================================================

    @GetMapping("/export/exam-results/{examId}")
    @PreAuthorize("hasRole('FACULTY')")
    public ResponseEntity<byte[]> exportExamResults(
            @PathVariable Long examId,
            Authentication authentication
    ) throws IOException {

        List<Result> results =
                resultRepository.findByExam_Id(examId);

        if (results.isEmpty()) {
            throw new ResourceNotFoundException(
                    "No results found for this exam."
            );
        }

        Long courseId = results.get(0).getExam().getCourse().getId();

        verifyFacultyCourseAccess(courseId, authentication);

        byte[] excelData =
                reportService.generateExamResultsExcel(examId);

        return excelResponse(
                excelData,
                "exam_results_" + examId + ".xlsx"
        );
    }

    // =========================================================
    // EXPORT ONE STUDENT'S RESULTS FOR A COURSE
    // =========================================================

    @GetMapping(
            "/export/student-results/{studentId}/course/{courseId}"
    )
    @PreAuthorize("hasRole('FACULTY')")
    public ResponseEntity<byte[]> exportStudentCourseResults(
            @PathVariable Long studentId,
            @PathVariable Long courseId,
            Authentication authentication
    ) throws IOException {

        verifyFacultyCourseAccess(courseId, authentication);

        List<Result> studentResults =
                resultRepository
                        .findByStudent_IdAndExam_Course_Id(
                                studentId,
                                courseId
                        );

        byte[] excelData =
                reportService.generateResultsExcelForRecords(
                        studentResults,
                        "Student Course Results"
                );

        return excelResponse(
                excelData,
                "student_" + studentId
                        + "_course_" + courseId + "_results.xlsx"
        );
    }

    // =========================================================
    // VERIFY FACULTY OWNS THE COURSE
    // =========================================================

    private void verifyFacultyCourseAccess(
            Long courseId,
            Authentication authentication
    ) {

        if (authentication == null
                || !authentication.isAuthenticated()) {

            throw new org.springframework.security.access.AccessDeniedException(
                    "Authentication is required."
            );
        }

        User faculty = userRepository
                .findByEmail(authentication.getName())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Authenticated faculty account not found."
                        )
                );

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Course not found."
                        )
                );

        if (course.getFaculty() == null
                || !course.getFaculty().getId().equals(faculty.getId())) {

            throw new org.springframework.security.access.AccessDeniedException(
                    "You are not authorized to access this course."
            );
        }
    }

    // =========================================================
    // COMMON EXCEL RESPONSE
    // =========================================================

    private ResponseEntity<byte[]> excelResponse(
            byte[] excelData,
            String baseFileName
    ) {

        String timestamp =
                LocalDateTime.now().format(FILE_DATE_FORMAT);

        String fileName = baseFileName.replace(
                ".xlsx",
                "_" + timestamp + ".xlsx"
        );

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + fileName + "\""
                )
                .contentType(
                        MediaType.parseMediaType(
                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                        )
                )
                .body(excelData);
    }
}