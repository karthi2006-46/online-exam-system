package com.lms.service;

import com.lms.entity.CodingChallenge;
import com.lms.entity.CodingChallengeProgress;
import com.lms.entity.User;

import com.lms.repository.CodingChallengeProgressRepository;
import com.lms.repository.CodingChallengeRepository;
import com.lms.repository.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CodingChallengeProgressService {


    @Autowired
    private CodingChallengeProgressRepository progressRepository;


    @Autowired
    private CodingChallengeRepository challengeRepository;


    @Autowired
    private UserRepository userRepository;


    // =====================================================
    // MARK CODING CHALLENGE AS COMPLETED
    // =====================================================

    public void markCompleted(
            Long studentId,
            Long challengeId
    ) {

        // ---------------------------------------------
        // Check whether progress already exists
        // ---------------------------------------------

        var existingProgress =
                progressRepository
                        .findByStudent_IdAndChallenge_Id(
                                studentId,
                                challengeId
                        );


        // ---------------------------------------------
        // If already completed, do nothing
        // ---------------------------------------------

        if (existingProgress.isPresent()) {

            CodingChallengeProgress progress =
                    existingProgress.get();

            if (Boolean.TRUE.equals(
                    progress.getCompleted()
            )) {

                return;
            }

            progress.setCompleted(true);

            progressRepository.save(progress);

            return;
        }


        // ---------------------------------------------
        // Find Student
        // ---------------------------------------------

        User student =
                userRepository
                        .findById(studentId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Student not found"
                                )
                        );


        // ---------------------------------------------
        // Find Coding Challenge
        // ---------------------------------------------

        CodingChallenge challenge =
                challengeRepository
                        .findById(challengeId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Coding Challenge not found"
                                )
                        );


        // ---------------------------------------------
        // Create Progress
        // ---------------------------------------------

        CodingChallengeProgress progress =
                new CodingChallengeProgress();

        progress.setStudent(student);

        progress.setChallenge(challenge);

        progress.setCompleted(true);

        progressRepository.save(progress);
    }


    // =====================================================
    // GET STUDENT CODING PROGRESS
    // =====================================================

    public List<CodingChallengeProgress>
    getStudentProgress(Long studentId) {

        return progressRepository
                .findByStudent_Id(studentId);
    }


    // =====================================================
    // COUNT COMPLETED CHALLENGES
    // =====================================================

    public long getCompletedChallengeCount(
            Long studentId,
            Long courseId
    ) {

        return progressRepository
                .countByStudent_IdAndChallenge_Course_IdAndCompleted(
                        studentId,
                        courseId,
                        true
                );
    }
}