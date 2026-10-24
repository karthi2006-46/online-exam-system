package com.lms.service;

import com.lms.dto.CourseProgressDTO;
import com.lms.dto.StudentDashboardProgressDTO;
import com.lms.entity.Enrollment;
import com.lms.repository.CodingChallengeProgressRepository;
import com.lms.repository.CodingChallengeRepository;
import com.lms.repository.EnrollmentRepository;
import com.lms.repository.ExamAttemptRepository;
import com.lms.repository.ExamRepository;
import com.lms.repository.StudyMaterialProgressRepository;
import com.lms.repository.StudyMaterialRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentDashboardService {

    private final EnrollmentRepository enrollmentRepository;
    private final StudyMaterialRepository studyMaterialRepository;
    private final ExamRepository examRepository;
    private final ExamAttemptRepository examAttemptRepository;
    private final CodingChallengeRepository codingChallengeRepository;
    private final CodingChallengeProgressRepository codingChallengeProgressRepository;
    private final StudyMaterialProgressRepository studyMaterialProgressRepository;

    private static final double DURATION_WEIGHT = 20.0;
    private static final double MATERIAL_WEIGHT = 25.0;
    private static final double CHALLENGE_WEIGHT = 25.0;
    private static final double EXAM_WEIGHT = 30.0;

    public StudentDashboardProgressDTO getDashboard(Long studentId) {

        List<Enrollment> enrollments =
                enrollmentRepository.findByStudent_Id(studentId);

        StudentDashboardProgressDTO dashboard =
                new StudentDashboardProgressDTO();

        dashboard.setTotalCourses(enrollments.size());

        dashboard.setCompletedCourses(
                enrollments.stream()
                        .filter(e -> "COMPLETED".equalsIgnoreCase(e.getStatus()))
                        .count()
        );

        List<CourseProgressDTO> courseProgressList =
                enrollments.stream()
                        .map(this::calculateCourseProgress)
                        .toList();

        dashboard.setCourses(courseProgressList);

        double averageProgress = courseProgressList.isEmpty()
                ? 0.0
                : courseProgressList.stream()
                        .mapToDouble(CourseProgressDTO::getOverallProgress)
                        .average()
                        .orElse(0.0);

        dashboard.setOverallProgress(round(averageProgress));

        long pendingExams = 0;

        for (Enrollment enrollment : enrollments) {

            Long courseId = enrollment.getCourse().getId();

            long totalExams =
                    examRepository.countByCourse_IdAndActiveTrueAndPublishedTrue(
                            courseId
                    );

            long completedExams =
                    examAttemptRepository.countDistinctCompletedExams(
                            studentId,
                            courseId,
                            List.of("SUBMITTED", "AUTO_SUBMITTED")
                    );

            pendingExams += Math.max(0, totalExams - completedExams);
        }

        dashboard.setPendingExams(pendingExams);

        return dashboard;
    }

    private CourseProgressDTO calculateCourseProgress(Enrollment enrollment) {

        Long studentId = enrollment.getStudent().getId();
        Long courseId = enrollment.getCourse().getId();

        CourseProgressDTO dto = new CourseProgressDTO();

        dto.setCourseId(courseId);
        dto.setCourseTitle(enrollment.getCourse().getTitle());
        dto.setCourseDurationDays(enrollment.getCourse().getDurationDays());
        dto.setStatus(enrollment.getStatus());

        // 1. Course duration progress
        LocalDateTime enrollmentDate = enrollment.getEnrollmentDate();

        long elapsedDays = 0;

        if (enrollmentDate != null) {
            elapsedDays = Math.max(
                    0,
                    ChronoUnit.DAYS.between(
                            enrollmentDate.toLocalDate(),
                            LocalDate.now()
                    )
            );
        }

        Integer courseDuration = enrollment.getCourse().getDurationDays();

        int durationDays = courseDuration == null ? 0 : courseDuration;

        double durationProgress = durationDays <= 0
                ? 0.0
                : percentage(Math.min(elapsedDays, durationDays), durationDays);

        dto.setElapsedDays(elapsedDays);
        dto.setDurationProgress(round(durationProgress));

        // 2. Study materials
        long totalMaterials =
                studyMaterialRepository.countByCourse_IdAndActiveTrue(courseId);

        long completedMaterials =
                studyMaterialProgressRepository.countCompletedActiveMaterials(
                        studentId,
                        courseId
                );

        completedMaterials = Math.min(completedMaterials, totalMaterials);

        double materialsProgress = totalMaterials == 0
                ? 0.0
                : percentage(completedMaterials, totalMaterials);

        dto.setTotalMaterials(totalMaterials);
        dto.setCompletedMaterials(completedMaterials);
        dto.setMaterialsProgress(round(materialsProgress));

        // 3. Coding challenges
        long totalChallenges =
                codingChallengeRepository.countByCourse_IdAndActive(
                        courseId,
                        true
                );

        long completedChallenges =
                codingChallengeProgressRepository
                        .countByStudent_IdAndChallenge_Course_IdAndCompleted(
                                studentId,
                                courseId,
                                true
                        );

        completedChallenges = Math.min(completedChallenges, totalChallenges);

        double challengesProgress = totalChallenges == 0
                ? 0.0
                : percentage(completedChallenges, totalChallenges);

        dto.setTotalChallenges(totalChallenges);
        dto.setCompletedChallenges(completedChallenges);
        dto.setChallengesProgress(round(challengesProgress));

        // 4. Exams
        long totalExams =
                examRepository.countByCourse_IdAndActiveTrueAndPublishedTrue(
                        courseId
                );

        long completedExams =
                examAttemptRepository.countDistinctCompletedExams(
                        studentId,
                        courseId,
                        List.of("SUBMITTED", "AUTO_SUBMITTED")
                );

        completedExams = Math.min(completedExams, totalExams);

        double examsProgress = totalExams == 0
                ? 0.0
                : percentage(completedExams, totalExams);

        dto.setTotalExams(totalExams);
        dto.setCompletedExams(completedExams);
        dto.setExamsProgress(round(examsProgress));

        // 5. Weighted overall progress
        double weightedTotal = 0.0;
        double availableWeight = 0.0;

        if (durationDays > 0) {
            weightedTotal += durationProgress * DURATION_WEIGHT;
            availableWeight += DURATION_WEIGHT;
        }

        if (totalMaterials > 0) {
            weightedTotal += materialsProgress * MATERIAL_WEIGHT;
            availableWeight += MATERIAL_WEIGHT;
        }

        if (totalChallenges > 0) {
            weightedTotal += challengesProgress * CHALLENGE_WEIGHT;
            availableWeight += CHALLENGE_WEIGHT;
        }

        if (totalExams > 0) {
            weightedTotal += examsProgress * EXAM_WEIGHT;
            availableWeight += EXAM_WEIGHT;
        }

        double overallProgress = availableWeight == 0
                ? 0.0
                : weightedTotal / availableWeight;

        dto.setOverallProgress(round(overallProgress));

        return dto;
    }

    private double percentage(long completed, long total) {

        if (total <= 0) {
            return 0.0;
        }

        return Math.min(
                100.0,
                Math.max(0.0, completed * 100.0 / total)
        );
    }

    private double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}