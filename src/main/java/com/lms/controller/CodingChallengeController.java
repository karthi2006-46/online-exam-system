package com.lms.controller;

import com.lms.dto.CodingChallengeDTO;
import com.lms.dto.CodingChallengeRequest;
import com.lms.service.CodingChallengeService;
import com.lms.util.ApiResponseUtil;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/coding-challenges")
@CrossOrigin
@RequiredArgsConstructor
public class CodingChallengeController {

    private final CodingChallengeService codingChallengeService;

    // ==========================================
    // STUDENT - GET ACTIVE CHALLENGES FOR A COURSE
    // ==========================================

    @GetMapping("/course/{courseId}")
    public ResponseEntity<List<CodingChallengeDTO>> getChallengesByCourse(
            @PathVariable Long courseId) {

        return ResponseEntity.ok(
                codingChallengeService.getActiveChallengesForCourse(courseId)
        );
    }

    // ==========================================
    // STUDENT - GET A SINGLE ACTIVE CHALLENGE
    // ==========================================

    @GetMapping("/{challengeId}")
    public ResponseEntity<CodingChallengeDTO> getChallenge(
            @PathVariable Long challengeId) {

        return ResponseEntity.ok(
                codingChallengeService.getActiveChallenge(challengeId)
        );
    }

    // ==========================================
    // FACULTY - LIST ALL CHALLENGES OF OWNED COURSE
    // (includes deactivated ones)
    // ==========================================

    @GetMapping("/faculty/course/{courseId}")
    @PreAuthorize("hasRole('FACULTY') or hasRole('ADMIN')")
    public ResponseEntity<?> getCourseChallengesForFaculty(
            @PathVariable Long courseId,
            Authentication authentication) {

        List<CodingChallengeDTO> challenges =
                codingChallengeService.getCourseChallengesForFaculty(
                        authentication,
                        courseId
                );

        return ApiResponseUtil.success(
                "Course coding challenges retrieved",
                challenges
        );
    }

    // ==========================================
    // FACULTY - CREATE A CHALLENGE
    // ==========================================

    @PostMapping
    @PreAuthorize("hasRole('FACULTY') or hasRole('ADMIN')")
    public ResponseEntity<?> createChallenge(
            @RequestBody CodingChallengeRequest request,
            Authentication authentication) {

        CodingChallengeDTO created =
                codingChallengeService.create(authentication, request);

        return ApiResponseUtil.created(
                "Coding challenge created successfully",
                created
        );
    }

    // ==========================================
    // FACULTY - UPDATE A CHALLENGE
    // ==========================================

    @PutMapping("/{challengeId}")
    @PreAuthorize("hasRole('FACULTY') or hasRole('ADMIN')")
    public ResponseEntity<?> updateChallenge(
            @PathVariable Long challengeId,
            @RequestBody CodingChallengeRequest request,
            Authentication authentication) {

        CodingChallengeDTO updated =
                codingChallengeService.update(
                        authentication,
                        challengeId,
                        request
                );

        return ApiResponseUtil.success(
                "Coding challenge updated successfully",
                updated
        );
    }

    // ==========================================
    // FACULTY - ACTIVATE / DEACTIVATE A CHALLENGE
    // ==========================================

    @PatchMapping("/{challengeId}/active")
    @PreAuthorize("hasRole('FACULTY') or hasRole('ADMIN')")
    public ResponseEntity<?> setChallengeActive(
            @PathVariable Long challengeId,
            @RequestBody(required = false) Map<String, Boolean> body,
            Authentication authentication) {

        Boolean active = body != null ? body.get("active") : null;

        CodingChallengeDTO updated =
                codingChallengeService.setActive(
                        authentication,
                        challengeId,
                        active
                );

        String message = Boolean.TRUE.equals(updated.getActive())
                ? "Coding challenge activated"
                : "Coding challenge deactivated";

        return ApiResponseUtil.success(message, updated);
    }

    // ==========================================
    // FACULTY - DELETE A CHALLENGE
    // ==========================================

    @DeleteMapping("/{challengeId}")
    @PreAuthorize("hasRole('FACULTY') or hasRole('ADMIN')")
    public ResponseEntity<?> deleteChallenge(
            @PathVariable Long challengeId,
            Authentication authentication) {

        codingChallengeService.delete(authentication, challengeId);

        return ApiResponseUtil.success(
                "Coding challenge deleted successfully"
        );
    }
}
