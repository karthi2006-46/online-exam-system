package com.lms.service;

import com.lms.entity.*;
import com.lms.repository.*;

import lombok.RequiredArgsConstructor;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional
public class CodingSubmissionService {

    private final CodingSubmissionRepository submissionRepository;

    private final CodingChallengeRepository challengeRepository;

    private final UserRepository userRepository;

    private final EnrollmentRepository enrollmentRepository;


    // =====================================================
    // 1. STUDENT - SUBMIT CODING SOLUTION
    // =====================================================

    public CodingSubmission submitSolution(
            Long studentId,
            Long challengeId,
            String language,
            String sourceCode,
            String executionOutput,
            String executionError,
            Integer testCasesPassed,
            Integer totalTestCases,
            Double executionTime,
            Double memoryUsed
    ) {

        User student = userRepository.findById(studentId)
                .orElseThrow(() ->
                        new RuntimeException("Student not found"));

        validateStudent(student);

        CodingChallenge challenge = challengeRepository
                .findById(challengeId)
                .orElseThrow(() ->
                        new RuntimeException("Coding challenge not found"));

        if (!Boolean.TRUE.equals(challenge.getActive())) {
            throw new RuntimeException("This coding challenge is inactive");
        }

        validateEnrollment(studentId, challenge.getCourse().getId());

        if (language == null || language.isBlank()) {
            throw new RuntimeException("Programming language is required");
        }

        if (sourceCode == null || sourceCode.isBlank()) {
            throw new RuntimeException("Source code cannot be empty");
        }

        // Check previous attempt
        var previousSubmission =
                submissionRepository
                        .findFirstByStudent_IdAndChallenge_IdOrderBySubmittedAtDesc(
                                studentId,
                                challengeId
                        );

        if (previousSubmission.isPresent()) {

            CodingSubmission previous = previousSubmission.get();

            if (previous.getStatus() ==
                    CodingSubmissionStatus.PENDING_REVIEW) {

                throw new RuntimeException(
                        "Your previous submission is waiting for faculty review"
                );
            }

            if (previous.getStatus() ==
                    CodingSubmissionStatus.APPROVED) {

                throw new RuntimeException(
                        "This coding challenge is already approved"
                );
            }
        }

        // Create new submission
        CodingSubmission submission = new CodingSubmission();

        submission.setStudent(student);
        submission.setChallenge(challenge);

        submission.setLanguage(language.trim());
        submission.setSourceCode(sourceCode);

        submission.setExecutionOutput(executionOutput);
        submission.setExecutionError(executionError);

        submission.setTestCasesPassed(
                testCasesPassed == null ? 0 : testCasesPassed
        );

        submission.setTotalTestCases(
                totalTestCases == null ? 0 : totalTestCases
        );

        submission.setExecutionTime(executionTime);
        submission.setMemoryUsed(memoryUsed);

        submission.setStatus(
                CodingSubmissionStatus.PENDING_REVIEW
        );

        return submissionRepository.save(submission);
    }


    // =====================================================
    // 2. STUDENT - VIEW SUBMISSION HISTORY
    // =====================================================

    @Transactional(readOnly = true)
    public List<CodingSubmission> getStudentSubmissionHistory(
            Long studentId,
            Long challengeId
    ) {

        User student = userRepository.findById(studentId)
                .orElseThrow(() ->
                        new RuntimeException("Student not found"));

        validateStudent(student);

        CodingChallenge challenge = challengeRepository
                .findById(challengeId)
                .orElseThrow(() ->
                        new RuntimeException("Coding challenge not found"));

        validateEnrollment(studentId, challenge.getCourse().getId());

        return submissionRepository
                .findByStudent_IdAndChallenge_IdOrderBySubmittedAtDesc(
                        studentId,
                        challengeId
                );
    }


    // =====================================================
    // 3. FACULTY - VIEW PENDING CODING SUBMISSIONS
    // =====================================================

    @Transactional(readOnly = true)
    public List<CodingSubmission> getFacultyPendingSubmissions(
            Authentication authentication
    ) {

        User faculty = getAuthenticatedFaculty(authentication);

        return submissionRepository
                .findByChallenge_Course_Faculty_IdAndStatusOrderBySubmittedAtAsc(
                        faculty.getId(),
                        CodingSubmissionStatus.PENDING_REVIEW
                );
    }


    // =====================================================
    // 4. FACULTY - APPROVE CODING SUBMISSION
    // =====================================================

    public CodingSubmission approveSubmission(
            Long submissionId,
            String feedback,
            Authentication authentication
    ) {

        User faculty = getAuthenticatedFaculty(authentication);

        CodingSubmission submission =
                getFacultySubmission(submissionId, faculty);

        if (submission.getStatus() !=
                CodingSubmissionStatus.PENDING_REVIEW) {

            throw new RuntimeException(
                    "This submission has already been reviewed"
            );
        }

        submission.setStatus(
                CodingSubmissionStatus.APPROVED
        );

        submission.setFacultyFeedback(feedback);
        submission.setReviewedBy(faculty);
        submission.setReviewedAt(LocalDateTime.now());

        return submissionRepository.save(submission);
    }


    // =====================================================
    // 5. FACULTY - REJECT CODING SUBMISSION
    // =====================================================

    public CodingSubmission rejectSubmission(
            Long submissionId,
            String feedback,
            Authentication authentication
    ) {

        User faculty = getAuthenticatedFaculty(authentication);

        CodingSubmission submission =
                getFacultySubmission(submissionId, faculty);

        if (submission.getStatus() !=
                CodingSubmissionStatus.PENDING_REVIEW) {

            throw new RuntimeException(
                    "This submission has already been reviewed"
            );
        }

        if (feedback == null || feedback.isBlank()) {

            throw new RuntimeException(
                    "Please provide a rejection reason"
            );
        }

        submission.setStatus(
                CodingSubmissionStatus.REJECTED
        );

        submission.setFacultyFeedback(feedback);
        submission.setReviewedBy(faculty);
        submission.setReviewedAt(LocalDateTime.now());

        return submissionRepository.save(submission);
    }


    // =====================================================
    // 6. FACULTY - GET INDIVIDUAL SUBMISSION
    // =====================================================

    @Transactional(readOnly = true)
    public CodingSubmission getFacultySubmissionDetails(
            Long submissionId,
            Authentication authentication
    ) {

        User faculty = getAuthenticatedFaculty(authentication);

        return getFacultySubmission(submissionId, faculty);
    }


    // =====================================================
    // PRIVATE - VALIDATE STUDENT
    // =====================================================

    private void validateStudent(User student) {

        String role = student.getRole().getName();

        role = normalizeRole(role);

        if (!"STUDENT".equals(role)) {

            throw new RuntimeException(
                    "Only students can submit coding solutions"
            );
        }
    }


    // =====================================================
    // PRIVATE - VALIDATE ENROLLMENT
    // =====================================================

    private void validateEnrollment(
            Long studentId,
            Long courseId
    ) {

        Enrollment enrollment = enrollmentRepository
                .findByStudent_IdAndCourse_Id(studentId, courseId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "You are not enrolled in this course"
                        ));

        if ("DROPPED".equalsIgnoreCase(enrollment.getStatus())) {

            throw new RuntimeException(
                    "Your course enrollment is inactive"
            );
        }
    }


    // =====================================================
    // PRIVATE - GET AUTHENTICATED FACULTY
    // =====================================================

    private User getAuthenticatedFaculty(
            Authentication authentication
    ) {

        if (authentication == null ||
                !authentication.isAuthenticated()) {

            throw new RuntimeException("Authentication required");
        }

        String email;

        Object principal = authentication.getPrincipal();

        if (principal instanceof UserDetails userDetails) {
            email = userDetails.getUsername();
        } else {
            email = authentication.getName();
        }

        User faculty = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("Faculty account not found"));

        String role = normalizeRole(
                faculty.getRole().getName()
        );

        if (!"FACULTY".equals(role)) {

            throw new RuntimeException(
                    "Only faculty members can review submissions"
            );
        }

        return faculty;
    }


    // =====================================================
    // PRIVATE - VERIFY FACULTY COURSE ACCESS
    // =====================================================

    private CodingSubmission getFacultySubmission(
            Long submissionId,
            User faculty
    ) {

        CodingSubmission submission = submissionRepository
                .findById(submissionId)
                .orElseThrow(() ->
                        new RuntimeException("Submission not found"));

        Long assignedFacultyId = submission
                .getChallenge()
                .getCourse()
                .getFaculty()
                .getId();

        if (!assignedFacultyId.equals(faculty.getId())) {

            throw new RuntimeException(
                    "You are not authorized to review this submission"
            );
        }

        return submission;
    }


    // =====================================================
    // PRIVATE - NORMALIZE ROLE NAME
    // =====================================================

    private String normalizeRole(String role) {

        if (role == null) {
            return "";
        }

        String normalized = role.trim()
                .toUpperCase(Locale.ROOT);

        if (normalized.startsWith("ROLE_")) {
            normalized = normalized.substring(5);
        }

        return normalized;
    }
}