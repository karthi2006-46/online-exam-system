package com.lms.controller;

import com.lms.dto.EnrollmentDTO;
import com.lms.dto.EnrollStudentRequest;
import com.lms.service.EnrollmentService;
import com.lms.util.ApiResponseUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/enrollments")
@CrossOrigin(origins = "*")
public class EnrollmentController {

    @Autowired
    private EnrollmentService enrollmentService;

    @PostMapping
    @PreAuthorize("hasRole('FACULTY') or hasRole('ADMIN')")
    public ResponseEntity<?> enrollStudent(@Valid @RequestBody EnrollStudentRequest request) {
        EnrollmentDTO enrollmentDTO = enrollmentService.enrollStudent(request);
        return ApiResponseUtil.created("Student enrolled successfully", enrollmentDTO);
    }

    @GetMapping("/{enrollmentId}")
    public ResponseEntity<?> getEnrollmentById(@PathVariable Long enrollmentId) {
        EnrollmentDTO enrollmentDTO = enrollmentService.getEnrollmentById(enrollmentId);
        return ApiResponseUtil.success("Enrollment retrieved successfully", enrollmentDTO);
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> getStudentEnrollments(@PathVariable Long studentId) {
        List<EnrollmentDTO> enrollments = enrollmentService.getStudentEnrollments(studentId);
        return ApiResponseUtil.success("Student enrollments retrieved", enrollments);
    }

    @GetMapping("/course/{courseId}")
    @PreAuthorize("hasRole('FACULTY') or hasRole('ADMIN')")
    public ResponseEntity<?> getCourseEnrollments(@PathVariable Long courseId) {
        List<EnrollmentDTO> enrollments = enrollmentService.getCourseEnrollments(courseId);
        return ApiResponseUtil.success("Course enrollments retrieved", enrollments);
    }

    @GetMapping("/student/{studentId}/active")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> getActiveEnrollments(@PathVariable Long studentId) {
        List<EnrollmentDTO> enrollments = enrollmentService.getActiveEnrollments(studentId);
        return ApiResponseUtil.success("Active enrollments retrieved", enrollments);
    }

    @PutMapping("/{enrollmentId}/progress")
    @PreAuthorize("hasRole('FACULTY') or hasRole('ADMIN')")
    public ResponseEntity<?> updateProgressPercentage(@PathVariable Long enrollmentId, @RequestParam Double percentage) {
        EnrollmentDTO enrollmentDTO = enrollmentService.updateCompletionPercentage(enrollmentId, percentage);
        return ApiResponseUtil.success("Progress updated successfully", enrollmentDTO);
    }
}
