package com.lms.controller;

import com.lms.dto.ExamEligibilityDTO;
import com.lms.service.ExamEligibilityService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/exam-eligibility")
@CrossOrigin
public class ExamEligibilityController {


    @Autowired
    private ExamEligibilityService eligibilityService;


    // =====================================================
    // CHECK MATERIAL + CODING COMPLETION
    // =====================================================

    @GetMapping("/student/{studentId}/course/{courseId}")
    public ResponseEntity<?> checkEligibility(

            @PathVariable Long studentId,

            @PathVariable Long courseId

    ) {

        ExamEligibilityDTO result =
                eligibilityService.checkEligibility(
                        studentId,
                        courseId
                );

        return ResponseEntity.ok(result);
    }
}