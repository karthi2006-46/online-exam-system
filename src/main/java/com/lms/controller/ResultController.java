package com.lms.controller;

import com.lms.dto.ResultDTO;
import com.lms.service.ResultService;
import com.lms.util.ApiResponseUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/results")
@CrossOrigin(origins = "*")
public class ResultController {

    @Autowired
    private ResultService resultService;

    @GetMapping("/{resultId}")
    public ResponseEntity<?> getResult(@PathVariable Long resultId) {
        ResultDTO resultDTO = resultService.getResult(resultId);
        return ApiResponseUtil.success("Result retrieved successfully", resultDTO);
    }

    @GetMapping("/attempt/{attemptId}")
    public ResponseEntity<?> getResultByAttempt(@PathVariable Long attemptId) {
        ResultDTO resultDTO = resultService.getResultByAttempt(attemptId);
        return ApiResponseUtil.success("Result retrieved successfully", resultDTO);
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> getStudentResults(@PathVariable Long studentId) {
        List<ResultDTO> results = resultService.getStudentResults(studentId);
        return ApiResponseUtil.success("Student results retrieved", results);
    }

    @GetMapping("/exam/{examId}")
    @PreAuthorize("hasRole('FACULTY') or hasRole('ADMIN')")
    public ResponseEntity<?> getExamResults(@PathVariable Long examId) {
        List<ResultDTO> results = resultService.getExamResults(examId);
        return ApiResponseUtil.success("Exam results retrieved", results);
    }

    @GetMapping("/student/{studentId}/course/{courseId}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> getCourseResults(@PathVariable Long studentId, @PathVariable Long courseId) {
        List<ResultDTO> results = resultService.getCourseResults(studentId, courseId);
        return ApiResponseUtil.success("Course results retrieved", results);
    }
}
