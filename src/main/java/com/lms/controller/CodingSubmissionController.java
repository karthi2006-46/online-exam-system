package com.lms.controller;

import com.lms.dto.CodingSubmissionRequestDTO;
import com.lms.entity.CodingSubmission;
import com.lms.entity.User;
import com.lms.repository.UserRepository;
import com.lms.service.CodingSubmissionService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import com.lms.dto.CodingSubmissionResponseDTO;
import java.util.List;

@RestController
@RequestMapping("/api/coding-submissions")
@CrossOrigin(origins = "*")
public class CodingSubmissionController {

    private final CodingSubmissionService submissionService;
    private final UserRepository userRepository;

    public CodingSubmissionController(
            CodingSubmissionService submissionService,
            UserRepository userRepository
    ) {
        this.submissionService = submissionService;
        this.userRepository = userRepository;
    }

    // ==========================================
    // STUDENT - SUBMIT CODING SOLUTION
    // ==========================================

    @PostMapping("/submit")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> submitSolution(
            @Valid @RequestBody CodingSubmissionRequestDTO request,
            Authentication authentication
    ) {

        User student = getAuthenticatedUser(authentication);

        if (!hasRole(student, "STUDENT")) {
            throw new AccessDeniedException(
                    "Only students can submit coding solutions"
            );
        }

        CodingSubmission submission =
                submissionService.submitSolution(
                        student.getId(),
                        request.getChallengeId(),
                        request.getLanguage(),
                        request.getSourceCode(),

                        // No compiler or Judge0 integration
                        null,
                        null,
                        0,
                        0,
                        null,
                        null
                );

        return ResponseEntity.ok(submission);
    }

    // ==========================================
    // STUDENT - VIEW SUBMISSION HISTORY
    // ==========================================

    @GetMapping("/student/challenge/{challengeId}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> getStudentSubmissionHistory(
            @PathVariable Long challengeId,
            Authentication authentication
    ) {

        User student = getAuthenticatedUser(authentication);

        if (!hasRole(student, "STUDENT")) {
            throw new AccessDeniedException(
                    "Only students can view their submissions"
            );
        }

        List<CodingSubmissionResponseDTO> submissions =
                submissionService.getStudentSubmissionHistory(
                        student.getId(),
                        challengeId
                );

        return ResponseEntity.ok(submissions);
    }

    // ==========================================
    // FACULTY - VIEW PENDING SUBMISSIONS
    // ==========================================

    @GetMapping("/faculty/pending")
    @PreAuthorize("hasRole('FACULTY')")
    public ResponseEntity<?> getPendingSubmissions(
            Authentication authentication
    ) {

        List<CodingSubmissionResponseDTO> submissions =
                submissionService.getFacultyPendingSubmissions(
                        authentication
                );

        return ResponseEntity.ok(submissions);
    }

    // ==========================================
    // FACULTY - VIEW INDIVIDUAL SUBMISSION
    // ==========================================

    @GetMapping("/faculty/{submissionId}")
    @PreAuthorize("hasRole('FACULTY')")
    public ResponseEntity<?> getSubmissionDetails(
            @PathVariable Long submissionId,
            Authentication authentication
    ) {

        CodingSubmissionResponseDTO submission =
                submissionService.getFacultySubmissionDetails(
                        submissionId,
                        authentication
                );

        return ResponseEntity.ok(submission);
    }

    // ==========================================
    // FACULTY - APPROVE SUBMISSION
    // ==========================================

    @PutMapping("/faculty/{submissionId}/approve")
    @PreAuthorize("hasRole('FACULTY')")
    public ResponseEntity<?> approveSubmission(
            @PathVariable Long submissionId,
            @RequestParam(required = false) String feedback,
            Authentication authentication
    ) {

        CodingSubmissionResponseDTO submission =
                submissionService.approveSubmission(
                        submissionId,
                        feedback,
                        authentication
                );

        return ResponseEntity.ok(submission);
    }

    // ==========================================
    // FACULTY - REJECT SUBMISSION
    // ==========================================

    @PutMapping("/faculty/{submissionId}/reject")
    @PreAuthorize("hasRole('FACULTY')")
    public ResponseEntity<?> rejectSubmission(
            @PathVariable Long submissionId,
            @RequestParam String feedback,
            Authentication authentication
    ) {

        CodingSubmissionResponseDTO submission =
                submissionService.rejectSubmission(
                        submissionId,
                        feedback,
                        authentication
                );

        return ResponseEntity.ok(submission);
    }

    // ==========================================
    // GET AUTHENTICATED USER
    // ==========================================

    private User getAuthenticatedUser(
            Authentication authentication
    ) {

        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof UserDetails)) {

            throw new AccessDeniedException(
                    "Authentication is required"
            );
        }

        UserDetails userDetails =
                (UserDetails) authentication.getPrincipal();

        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Authenticated user not found"
                        ));
    }

    // ==========================================
    // ROLE VALIDATION
    // ==========================================

    private boolean hasRole(User user, String roleName) {

        return user.getRole() != null
                && user.getRole().getName() != null
                && user.getRole().getName().equalsIgnoreCase(roleName);
    }
}