package com.lms.repository;

import com.lms.entity.CodingChallengeProgress;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CodingChallengeProgressRepository
        extends JpaRepository<CodingChallengeProgress, Long> {


    // Find one student's progress for one challenge
    Optional<CodingChallengeProgress>
    findByStudent_IdAndChallenge_Id(
            Long studentId,
            Long challengeId
    );


    // Get all progress records of a student
    List<CodingChallengeProgress>
    findByStudent_Id(Long studentId);


    // Count completed challenges for a course
    long countByStudent_IdAndChallenge_Course_IdAndCompleted(
            Long studentId,
            Long courseId,
            Boolean completed
    );

    /**
     * Counts distinct active challenges completed by a student in a course.
     * Deactivated challenges must not inflate the completed total on the
     * dashboard.
     */
    @Query("""
            SELECT COUNT(DISTINCT p.challenge.id)
            FROM CodingChallengeProgress p
            WHERE p.student.id = :studentId
              AND p.challenge.course.id = :courseId
              AND p.challenge.active = true
              AND p.completed = true
            """)
    long countCompletedActiveChallenges(
            @Param("studentId") Long studentId,
            @Param("courseId") Long courseId
    );


    // Used before deleting a challenge so student progress is never orphaned
    long countByChallenge_Id(Long challengeId);

    long countByStudent_Id(Long studentId);
}