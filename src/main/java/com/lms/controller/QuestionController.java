package com.lms.controller;

import com.lms.dto.QuestionDTO;
import com.lms.service.QuestionService;
import com.lms.util.ApiResponseUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/questions")
@CrossOrigin(origins = "*")
public class QuestionController {

    @Autowired
    private QuestionService questionService;

    @PostMapping
    @PreAuthorize("hasRole('FACULTY') or hasRole('ADMIN')")
    public ResponseEntity<?> addQuestion(@Valid @RequestBody QuestionDTO request) {
        QuestionDTO questionDTO = questionService.addQuestion(request);
        return ApiResponseUtil.created("Question added successfully", questionDTO);
    }

    @GetMapping("/{questionId}")
    public ResponseEntity<?> getQuestionById(@PathVariable Long questionId) {
        QuestionDTO questionDTO = questionService.getQuestionById(questionId);
        return ApiResponseUtil.success("Question retrieved successfully", questionDTO);
    }

    @GetMapping("/exam/{examId}")
    public ResponseEntity<?> getExamQuestions(@PathVariable Long examId) {
        List<QuestionDTO> questions = questionService.getExamQuestions(examId);
        return ApiResponseUtil.success("Exam questions retrieved", questions);
    }

    @PutMapping("/{questionId}")
    @PreAuthorize("hasRole('FACULTY') or hasRole('ADMIN')")
    public ResponseEntity<?> updateQuestion(@PathVariable Long questionId, @Valid @RequestBody QuestionDTO request) {
        QuestionDTO questionDTO = questionService.updateQuestion(questionId, request);
        return ApiResponseUtil.success("Question updated successfully", questionDTO);
    }

    @DeleteMapping("/{questionId}")
    @PreAuthorize("hasRole('FACULTY') or hasRole('ADMIN')")
    public ResponseEntity<?> deleteQuestion(@PathVariable Long questionId) {
        questionService.deleteQuestion(questionId);
        return ApiResponseUtil.success("Question deleted successfully");
    }
}
