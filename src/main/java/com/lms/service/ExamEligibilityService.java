package com.lms.service;

import com.lms.dto.ExamEligibilityDTO;
import com.lms.repository.MaterialProgressRepository;
import com.lms.repository.StudyMaterialRepository;
import com.lms.repository.CodingChallengeProgressRepository;
import com.lms.repository.CodingChallengeRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ExamEligibilityService {


   @Autowired
private StudyMaterialRepository studyMaterialRepository;


    @Autowired
    private MaterialProgressRepository materialProgressRepository;


    @Autowired
    private CodingChallengeRepository codingChallengeRepository;


    @Autowired
    private CodingChallengeProgressRepository codingChallengeProgressRepository;


    // =====================================================
    // CHECK EXAM ELIGIBILITY
    // =====================================================

    public ExamEligibilityDTO checkEligibility(
            Long studentId,
            Long courseId
    ) {

        ExamEligibilityDTO dto =
                new ExamEligibilityDTO();


        dto.setStudentId(studentId);

        dto.setCourseId(courseId);


        // =================================================
        // MATERIALS
        // =================================================

        long totalMaterials =
              studyMaterialRepository.countByCourse_Id(courseId);


        long completedMaterials =
                materialProgressRepository
                        .countByStudent_IdAndMaterial_Course_IdAndCompleted(
                                studentId,
                                courseId,
                                true
                        );


        double materialPercentage = 0;


        if (totalMaterials > 0) {

            materialPercentage =
                    ((double) completedMaterials
                            / totalMaterials)
                            * 100;
        }


        // =================================================
        // CODING CHALLENGES
        // =================================================

        long totalCodingChallenges =
                codingChallengeRepository
                        .countByCourse_IdAndActive(
                                courseId,
                                true
                        );


        long completedCodingChallenges =
                codingChallengeProgressRepository
                        .countByStudent_IdAndChallenge_Course_IdAndCompleted(
                                studentId,
                                courseId,
                                true
                        );


        double codingPercentage = 0;


        if (totalCodingChallenges > 0) {

            codingPercentage =
                    ((double) completedCodingChallenges
                            / totalCodingChallenges)
                            * 100;
        }


        // =================================================
        // CHECK 100%
        // =================================================

        boolean materialsCompleted =
                totalMaterials > 0
                &&
                completedMaterials == totalMaterials;


        boolean codingCompleted =
                totalCodingChallenges > 0
                &&
                completedCodingChallenges
                    == totalCodingChallenges;


        // =================================================
        // FINAL ELIGIBILITY
        // =================================================

        boolean eligibleForExamApproval =
                materialsCompleted
                &&
                codingCompleted;


        // =================================================
        // SET RESPONSE
        // =================================================

        dto.setTotalMaterials(
                totalMaterials
        );

        dto.setCompletedMaterials(
                completedMaterials
        );

        dto.setMaterialPercentage(
                materialPercentage
        );

        dto.setTotalCodingChallenges(
                totalCodingChallenges
        );

        dto.setCompletedCodingChallenges(
                completedCodingChallenges
        );

        dto.setCodingPercentage(
                codingPercentage
        );

        dto.setMaterialsCompleted(
                materialsCompleted
        );

        dto.setCodingCompleted(
                codingCompleted
        );

        dto.setEligibleForExamApproval(
                eligibleForExamApproval
        );


        return dto;
    }
}