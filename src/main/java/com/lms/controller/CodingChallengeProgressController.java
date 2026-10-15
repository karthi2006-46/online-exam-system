package com.lms.controller;

import com.lms.entity.CodingChallengeProgress;
import com.lms.service.CodingChallengeProgressService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/coding-challenge-progress")
@CrossOrigin
public class CodingChallengeProgressController {


    @Autowired
    private CodingChallengeProgressService progressService;


    // =====================================================
    // MARK CODING CHALLENGE COMPLETED
    // =====================================================

    @PostMapping("/complete/{challengeId}")
    public ResponseEntity<?> completeChallenge(

            @PathVariable Long challengeId,

            @RequestParam Long studentId

    ) {

        progressService.markCompleted(
                studentId,
                challengeId
        );

        return ResponseEntity.ok(
                "Coding Challenge Completed Successfully"
        );
    }


    // =====================================================
    // GET STUDENT CODING PROGRESS
    // =====================================================

    @GetMapping("/student/{studentId}")
    public ResponseEntity<?> getStudentProgress(

            @PathVariable Long studentId

    ) {

        List<CodingChallengeProgress> progress =
                progressService
                        .getStudentProgress(studentId);

        return ResponseEntity.ok(progress);
    }


    // =====================================================
    // GET COMPLETED CHALLENGE COUNT
    // =====================================================

    @GetMapping("/student/{studentId}/course/{courseId}/completed-count")
    public ResponseEntity<?> getCompletedCount(

            @PathVariable Long studentId,

            @PathVariable Long courseId

    ) {

        long count =
                progressService
                        .getCompletedChallengeCount(
                                studentId,
                                courseId
                        );

        return ResponseEntity.ok(count);
    }
}