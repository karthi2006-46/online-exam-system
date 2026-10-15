package com.lms.repository;

import com.lms.entity.CodingChallengeProgress;

import org.springframework.data.jpa.repository.JpaRepository;

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
}