package com.lms.controller;

import com.lms.dto.ExamAttemptDTO;
import com.lms.service.ExamAttemptService;
import com.lms.util.ApiResponseUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/exam-attempts")
@CrossOrigin(origins = "*")
public class ExamAttemptController {

    @Autowired
    private ExamAttemptService examAttemptService;

    @PostMapping("/start")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> startExam(@RequestParam Long examId, @RequestParam Long studentId) {
        ExamAttemptDTO attemptDTO = examAttemptService.startExam(examId, studentId);
        return ApiResponseUtil.created("Exam started successfully", attemptDTO);
    }

    @GetMapping("/{attemptId}")
    public ResponseEntity<?> getAttemptById(@PathVariable Long attemptId) {
        ExamAttemptDTO attemptDTO = examAttemptService.getAttemptById(attemptId);
        return ApiResponseUtil.success("Exam attempt retrieved successfully", attemptDTO);
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> getStudentAttempts(@PathVariable Long studentId) {
        List<ExamAttemptDTO> attempts = examAttemptService.getStudentAttempts(studentId);
        return ApiResponseUtil.success("Student attempts retrieved", attempts);
    }

    @GetMapping("/exam/{examId}")
    @PreAuthorize("hasRole('FACULTY') or hasRole('ADMIN')")
    public ResponseEntity<?> getExamAttempts(@PathVariable Long examId) {
        List<ExamAttemptDTO> attempts = examAttemptService.getExamAttempts(examId);
        return ApiResponseUtil.success("Exam attempts retrieved", attempts);
    }

    @PutMapping("/{attemptId}/save-answers")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> saveAnswers(@PathVariable Long attemptId, @RequestBody String answers) {
        ExamAttemptDTO attemptDTO = examAttemptService.saveAnswers(attemptId, answers);
        return ApiResponseUtil.success("Answers saved successfully", attemptDTO);
    }

    @PutMapping("/{attemptId}/submit")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> submitExam(@PathVariable Long attemptId) {
        ExamAttemptDTO attemptDTO = examAttemptService.submitExam(attemptId);
        return ApiResponseUtil.success("Exam submitted successfully", attemptDTO);
    }

    @PutMapping("/{attemptId}/auto-submit")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> autoSubmitExam(@PathVariable Long attemptId) {
        ExamAttemptDTO attemptDTO = examAttemptService.autoSubmitExam(attemptId);
        return ApiResponseUtil.success("Exam auto-submitted successfully", attemptDTO);
    }
}
