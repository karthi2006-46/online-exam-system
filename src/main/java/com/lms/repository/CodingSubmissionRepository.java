package com.lms.repository;

import com.lms.entity.CodingSubmission;
import com.lms.entity.CodingSubmissionStatus;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CodingSubmissionRepository
        extends JpaRepository<CodingSubmission, Long> {

    // Get all submissions by student
    List<CodingSubmission> findByStudent_IdOrderBySubmittedAtDesc(
            Long studentId
    );

    // Get all attempts for a particular challenge
    List<CodingSubmission> findByStudent_IdAndChallenge_IdOrderBySubmittedAtDesc(
            Long studentId,
            Long challengeId
    );

    // Get latest submission for a student and challenge
    Optional<CodingSubmission> findFirstByStudent_IdAndChallenge_IdOrderBySubmittedAtDesc(
            Long studentId,
            Long challengeId
    );

    // Get submissions waiting for faculty review
    List<CodingSubmission> findByStatusOrderBySubmittedAtAsc(
            CodingSubmissionStatus status
    );

    // Get submissions for a particular course
    List<CodingSubmission> findByChallenge_Course_IdOrderBySubmittedAtDesc(
            Long courseId
    );

    // Count approved submissions (we will refine eligibility
    // to count the latest approved submission per challenge)
    long countByStudent_IdAndChallenge_Course_IdAndStatus(
            Long studentId,
            Long courseId,
            CodingSubmissionStatus status
    );// Get pending submissions belonging to courses assigned to faculty
List<CodingSubmission>
findByChallenge_Course_Faculty_IdAndStatusOrderBySubmittedAtAsc(
        Long facultyId,
        CodingSubmissionStatus status
);
boolean existsByStudent_IdAndChallenge_Id(
        Long studentId,
        Long challengeId
);

    // Used before deleting a challenge so student history is never orphaned
    long countByChallenge_Id(Long challengeId);

    // Admin and faculty detail counts
    long countByStatus(CodingSubmissionStatus status);

    long countByChallenge_Course_Faculty_IdAndStatus(
            Long facultyId,
            CodingSubmissionStatus status
    );

    long countByChallenge_Course_IdAndStatus(
            Long courseId,
            CodingSubmissionStatus status
    );

    // Admin performance view
    List<CodingSubmission>
    findByStudent_IdAndChallenge_Course_IdOrderBySubmittedAtDesc(
            Long studentId,
            Long courseId
    );

    long countByStudent_Id(Long studentId);

    long countByReviewedBy_Id(Long reviewerId);

}