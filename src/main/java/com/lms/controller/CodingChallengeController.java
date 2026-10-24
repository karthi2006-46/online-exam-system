package com.lms.controller;

import com.lms.entity.CodingChallenge;
import com.lms.repository.CodingChallengeRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/coding-challenges")
@CrossOrigin
public class CodingChallengeController {

    @Autowired
    private CodingChallengeRepository codingChallengeRepository;

    // ==========================================
    // GET ALL ACTIVE CHALLENGES FOR A COURSE
    // ==========================================

    @GetMapping("/course/{courseId}")
    public ResponseEntity<List<CodingChallenge>> getChallengesByCourse(
            @PathVariable Long courseId) {

        List<CodingChallenge> challenges =
                codingChallengeRepository
                        .findByCourse_IdAndActiveTrue(courseId);

        return ResponseEntity.ok(challenges);
    }

    // ==========================================
    // GET A SINGLE CHALLENGE
    // ==========================================

    @GetMapping("/{challengeId}")
    public ResponseEntity<?> getChallenge(
            @PathVariable Long challengeId) {

        return codingChallengeRepository.findById(challengeId)
                .filter(challenge ->
                        Boolean.TRUE.equals(challenge.getActive()))
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body(Map.of(
                                "message",
                                "Active coding challenge not found"
                        )));
    }

    // ==========================================
    // CREATE A CODING CHALLENGE
    // ==========================================

    @PostMapping
    public ResponseEntity<?> createChallenge(
            @RequestBody CodingChallenge challenge) {

        if (challenge.getCourse() == null
                || challenge.getCourse().getId() == null) {

            return ResponseEntity.badRequest().body(
                    Map.of("message", "Course ID is required")
            );
        }

        if (challenge.getTitle() == null
                || challenge.getTitle().isBlank()) {

            return ResponseEntity.badRequest().body(
                    Map.of("message", "Challenge title is required")
            );
        }

        if (challenge.getDifficulty() != null
                && !List.of("EASY", "MEDIUM", "HARD")
                        .contains(challenge.getDifficulty().toUpperCase())) {

            return ResponseEntity.badRequest().body(
                    Map.of(
                            "message",
                            "Difficulty must be EASY, MEDIUM, or HARD"
                    )
            );
        }

        if (challenge.getDifficulty() != null) {
            challenge.setDifficulty(
                    challenge.getDifficulty().toUpperCase()
            );
        }

        CodingChallenge saved =
                codingChallengeRepository.save(challenge);

        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // ==========================================
    // DELETE A CODING CHALLENGE
    // ==========================================

    @DeleteMapping("/{challengeId}")
    public ResponseEntity<?> deleteChallenge(
            @PathVariable Long challengeId) {

        if (!codingChallengeRepository.existsById(challengeId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of(
                            "message",
                            "Coding challenge not found"
                    ));
        }

        codingChallengeRepository.deleteById(challengeId);

        return ResponseEntity.ok(
                Map.of(
                        "message",
                        "Coding challenge deleted successfully"
                )
        );
    }
}