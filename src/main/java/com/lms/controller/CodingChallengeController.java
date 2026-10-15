package com.lms.controller;

import com.lms.entity.CodingChallenge;
import com.lms.repository.CodingChallengeRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/coding-challenges")
@CrossOrigin
public class CodingChallengeController {

    @Autowired
    private CodingChallengeRepository codingChallengeRepository;


    // ==========================================
    // GET ALL CHALLENGES FOR A COURSE
    // ==========================================

    @GetMapping("/course/{courseId}")
    public ResponseEntity<?> getChallengesByCourse(
            @PathVariable Long courseId) {

        List<CodingChallenge> challenges =
                codingChallengeRepository
                        .findByCourse_IdAndActiveTrue(courseId);

        return ResponseEntity.ok(challenges);
    }


    // ==========================================
    // GET SINGLE CHALLENGE
    // ==========================================

    @GetMapping("/{challengeId}")
    public ResponseEntity<?> getChallenge(
            @PathVariable Long challengeId) {

        CodingChallenge challenge =
                codingChallengeRepository
                        .findById(challengeId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Coding Challenge not found"
                                )
                        );

        return ResponseEntity.ok(challenge);
    }


    // ==========================================
    // CREATE CODING CHALLENGE
    // ==========================================

    @PostMapping
    public ResponseEntity<?> createChallenge(
            @RequestBody CodingChallenge challenge) {

        CodingChallenge saved =
                codingChallengeRepository.save(challenge);

        return ResponseEntity.ok(saved);
    }


    // ==========================================
    // DELETE CODING CHALLENGE
    // ==========================================

    @DeleteMapping("/{challengeId}")
    public ResponseEntity<?> deleteChallenge(
            @PathVariable Long challengeId) {

        codingChallengeRepository.deleteById(challengeId);

        return ResponseEntity.ok(
                "Coding challenge deleted successfully"
        );
    }
}