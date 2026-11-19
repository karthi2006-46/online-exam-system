package com.lms.service;

import com.lms.dto.ExamEligibilityDTO;
import com.lms.repository.StudyMaterialRepository;
import com.lms.repository.CodingChallengeProgressRepository;
import com.lms.repository.CodingChallengeRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.lms.entity.CodingSubmissionStatus;
import com.lms.repository.CodingSubmissionRepository;
import com.lms.repository.MaterialProgressRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExamEligibilityService {

    private final StudyMaterialRepository studyMaterialRepository;

private final MaterialProgressRepository materialProgressRepository;

    private final CodingChallengeRepository codingChallengeRepository;

    private final CodingChallengeProgressRepository codingChallengeProgressRepository;
    private final CodingSubmissionRepository codingSubmissionRepository;

    // ==========================================
    // CHECK EXAM ELIGIBILITY
    // ==========================================

    public ExamEligibilityDTO checkEligibility(
            Long studentId,
            Long courseId
    ) {

        ExamEligibilityDTO dto = new ExamEligibilityDTO();

        dto.setStudentId(studentId);
        dto.setCourseId(courseId);

        // ==========================================
        // 1. STUDY MATERIALS
        // ==========================================

        long totalMaterials =
                studyMaterialRepository
                        .countByCourse_IdAndActiveTrue(courseId);

        long completedMaterials =
                materialProgressRepository
                        .countByStudent_IdAndMaterial_Course_IdAndCompleted(
                                studentId,
                                courseId,
                                true
                        );

        // Prevent completed count from exceeding total.
        completedMaterials = Math.min(
                completedMaterials,
                totalMaterials
        );

        double materialPercentage = totalMaterials == 0
                ? 0.0
                : completedMaterials * 100.0 / totalMaterials;

        boolean materialsCompleted =
                totalMaterials > 0
                && completedMaterials == totalMaterials;

// ==========================================
// 2. CODING CHALLENGES
// ==========================================

long totalCodingChallenges =
        codingChallengeRepository
                .countByCourse_IdAndActive(
                        courseId,
                        true
                );

long completedCodingChallenges =
        codingSubmissionRepository
                .countByStudent_IdAndChallenge_Course_IdAndStatus(
                        studentId,
                        courseId,
                        CodingSubmissionStatus.APPROVED
                );

completedCodingChallenges = Math.min(
        completedCodingChallenges,
        totalCodingChallenges
);

double codingPercentage = totalCodingChallenges == 0
        ? 0.0
        : completedCodingChallenges * 100.0
                / totalCodingChallenges;

boolean codingCompleted =
        totalCodingChallenges > 0
        && completedCodingChallenges == totalCodingChallenges;

        // ==========================================
        // 3. FINAL ELIGIBILITY
        // ==========================================

        boolean eligibleForExamApproval =
                materialsCompleted && codingCompleted;

        // ==========================================
        // 4. RETURN ELIGIBILITY DETAILS
        // ==========================================

        dto.setTotalMaterials(totalMaterials);
        dto.setCompletedMaterials(completedMaterials);
        dto.setMaterialPercentage(materialPercentage);
        dto.setMaterialsCompleted(materialsCompleted);

        dto.setTotalCodingChallenges(totalCodingChallenges);
        dto.setCompletedCodingChallenges(completedCodingChallenges);
        dto.setCodingPercentage(codingPercentage);
        dto.setCodingCompleted(codingCompleted);

        dto.setEligibleForExamApproval(eligibleForExamApproval);

        return dto;
    }
}