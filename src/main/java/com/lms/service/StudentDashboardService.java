package com.lms.service;

import com.lms.dto.CourseProgressDTO;
import com.lms.dto.StudentDashboardProgressDTO;
import com.lms.entity.Enrollment;
import com.lms.repository.CodingChallengeProgressRepository;
import com.lms.repository.CodingChallengeRepository;
import com.lms.repository.EnrollmentRepository;
import com.lms.repository.ExamAttemptRepository;
import com.lms.repository.ExamRepository;
import com.lms.repository.ExtensionRequestRepository;
import com.lms.repository.MaterialProgressRepository;
import com.lms.repository.StudyMaterialRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
    private final MaterialProgressRepository materialProgressRepository;
    private final ExtensionRequestRepository extensionRequestRepository;

    private static final double DURATION_WEIGHT = 20.0;
    private static final double MATERIAL_WEIGHT = 25.0;
    private static final double CHALLENGE_WEIGHT = 25.0;
    private static final double EXAM_WEIGHT = 30.0;

    public StudentDashboardProgressDTO getDashboard(Long studentId) {

        List<Enrollment> enrollments = resolveEnrollments(studentId);

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
                    examAttemptRepository.countDistinctCompletedAssignedExams(
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

        int baseDurationDays = courseDuration == null || courseDuration < 0
                ? 0
                : courseDuration;

        /*
         * Approved extension requests are part of the duration the student
         * actually has. Reading them from the enrollment (rather than only
         * the course) keeps the dashboard correct when days are granted or
         * revoked without changing the course itself.
         */
        long extensionDays = enrollment.getId() == null
                ? 0
                : Math.max(
                        0,
                        extensionRequestRepository
                                .sumApprovedDaysByEnrollmentId(
                                        enrollment.getId()
                                )
                );

        int durationDays = (int) Math.min(
                Integer.MAX_VALUE,
                (long) baseDurationDays + extensionDays
        );

        long remainingDays = Math.max(
                0,
                (long) durationDays - elapsedDays
        );

        double durationProgress = durationDays <= 0
                ? 0.0
                : percentage(Math.min(elapsedDays, durationDays), durationDays);

        dto.setCourseDurationDays(baseDurationDays);
        dto.setExtensionDays(extensionDays);
        dto.setEffectiveDurationDays(durationDays);
        dto.setElapsedDays(elapsedDays);
        dto.setRemainingDays(remainingDays);
        dto.setDurationProgress(round(durationProgress));

        // 2. Study materials
        long totalMaterials =
                studyMaterialRepository.countByCourse_IdAndActiveTrue(courseId);

        long completedMaterials =
                materialProgressRepository
                        .countCompletedActiveMaterials(
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
                        .countCompletedActiveChallenges(
                                studentId,
                                courseId
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
                examAttemptRepository.countDistinctCompletedAssignedExams(
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

    /**
     * Returns one enrollment per course for the student, ignoring dropped
     * enrollments and duplicate enrollments. When a student has more than
     * one enrollment for the same course the most relevant one wins
     * (ACTIVE, then COMPLETED, then the latest INACTIVE record), so a
     * course is never counted twice on the dashboard.
     */
    private List<Enrollment> resolveEnrollments(Long studentId) {

        List<Enrollment> enrollments =
                enrollmentRepository.findByStudent_Id(studentId);

        Map<Long, Enrollment> byCourse = new LinkedHashMap<>();

        for (Enrollment enrollment : enrollments) {

            if (enrollment.getCourse() == null
                    || enrollment.getCourse().getId() == null
                    || "DROPPED".equalsIgnoreCase(enrollment.getStatus())) {
                continue;
            }

            Long courseId = enrollment.getCourse().getId();

            Enrollment current = byCourse.get(courseId);

            if (current == null || isPreferred(enrollment, current)) {
                byCourse.put(courseId, enrollment);
            }
        }

        return new ArrayList<>(byCourse.values());
    }

    private boolean isPreferred(Enrollment candidate, Enrollment current) {

        int candidateRank = statusRank(candidate.getStatus());
        int currentRank = statusRank(current.getStatus());

        if (candidateRank != currentRank) {
            return candidateRank < currentRank;
        }

        LocalDateTime candidateDate = candidate.getEnrollmentDate();
        LocalDateTime currentDate = current.getEnrollmentDate();

        if (candidateDate == null) {
            return false;
        }

        return currentDate == null || candidateDate.isAfter(currentDate);
    }

    private int statusRank(String status) {

        if (status == null) {
            return 3;
        }

        return switch (status.toUpperCase()) {
            case "ACTIVE" -> 0;
            case "COMPLETED" -> 1;
            case "INACTIVE" -> 2;
            default -> 3;
        };
    }

    /**
     * Full material/coding/exam progress for one student in one course.
     * Reused by the admin performance view.
     */
    public CourseProgressDTO getCourseProgress(
            Long studentId,
            Long courseId) {

        Enrollment enrollment = enrollmentRepository
                .findByStudent_IdAndCourse_Id(studentId, courseId)
                .orElse(null);

        if (enrollment == null) {
            return null;
        }

        return calculateCourseProgress(enrollment);
    }

    public double getCourseOverallProgress(Long studentId, Long courseId) {

        CourseProgressDTO progress =
                getCourseProgress(studentId, courseId);

        return progress == null ? 0.0 : progress.getOverallProgress();
    }
}