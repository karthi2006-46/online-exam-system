package com.lms.controller;

import com.lms.dto.ExamDTO;
import com.lms.service.ExamService;
import com.lms.util.ApiResponseUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import com.lms.entity.User;
import com.lms.repository.UserRepository;
import com.lms.service.ExamApprovalRequestService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
@RestController
@RequestMapping("/api/exams")
@CrossOrigin(origins = "*")
public class ExamController {

    @Autowired
    private ExamService examService;
    @Autowired
private ExamApprovalRequestService examApprovalRequestService;

@Autowired
private UserRepository userRepository;

    @PostMapping
    @PreAuthorize("hasRole('FACULTY') or hasRole('ADMIN')")
    public ResponseEntity<?> createExam(@Valid @RequestBody ExamDTO request) {
        ExamDTO examDTO = examService.createExam(request);
        return ApiResponseUtil.created("Exam created successfully", examDTO);
    }

    @GetMapping("/{examId}")
    public ResponseEntity<?> getExamById(@PathVariable Long examId) {
        ExamDTO examDTO = examService.getExamById(examId);
        return ApiResponseUtil.success("Exam retrieved successfully", examDTO);
    }

    @GetMapping("/course/{courseId}")
    @PreAuthorize("hasRole('FACULTY') or hasRole('ADMIN')")
    public ResponseEntity<?> getCourseExams(@PathVariable Long courseId) {
        List<ExamDTO> exams = examService.getCourseExams(courseId);
        return ApiResponseUtil.success("Course exams retrieved", exams);
    }

    @GetMapping("/course/{courseId}/published")
    public ResponseEntity<?> getPublishedExams(@PathVariable Long courseId) {
        List<ExamDTO> exams = examService.getPublishedExams(courseId);
        return ApiResponseUtil.success("Published exams retrieved", exams);
    }

    @PutMapping("/{examId}/publish")
    @PreAuthorize("hasRole('FACULTY') or hasRole('ADMIN')")
    public ResponseEntity<?> publishExam(@PathVariable Long examId) {
        ExamDTO examDTO = examService.publishExam(examId);
        return ApiResponseUtil.success("Exam published successfully", examDTO);
    }

    @PutMapping("/{examId}/unpublish")
    @PreAuthorize("hasRole('FACULTY') or hasRole('ADMIN')")
    public ResponseEntity<?> unpublishExam(@PathVariable Long examId) {
        ExamDTO examDTO = examService.unpublishExam(examId);
        return ApiResponseUtil.success("Exam unpublished successfully", examDTO);
    }

    @DeleteMapping("/{examId}")
    @PreAuthorize("hasRole('FACULTY') or hasRole('ADMIN')")
    public ResponseEntity<?> deleteExam(@PathVariable Long examId) {
        examService.deleteExam(examId);
        return ApiResponseUtil.success("Exam deleted successfully");
    }
    // =====================================================
// STUDENT - CHECK FACULTY APPROVAL STATUS
// =====================================================

@GetMapping("/approval-status")
@PreAuthorize("hasRole('STUDENT')")
public ResponseEntity<?> getApprovalStatus(
        @RequestParam Long courseId,
        Authentication authentication
) {

    UserDetails userDetails =
            (UserDetails) authentication.getPrincipal();

    User student = userRepository
            .findByEmail(userDetails.getUsername())
            .orElseThrow(() ->
                    new RuntimeException("Student not found")
            );

    String status =
            examApprovalRequestService.getApprovalStatus(
                    student.getId(),
                    courseId
            );

    return ApiResponseUtil.success(
            "Exam approval status retrieved successfully",
            status
    );
}
    
}
