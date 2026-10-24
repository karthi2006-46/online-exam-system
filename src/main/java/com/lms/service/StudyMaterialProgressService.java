package com.lms.service;

import com.lms.entity.Enrollment;
import com.lms.entity.StudyMaterial;
import com.lms.entity.StudyMaterialProgress;
import com.lms.entity.User;

import com.lms.repository.EnrollmentRepository;
import com.lms.repository.StudyMaterialProgressRepository;
import com.lms.repository.StudyMaterialRepository;
import com.lms.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StudyMaterialProgressService {

    private final StudyMaterialProgressRepository progressRepository;
    private final StudyMaterialRepository materialRepository;
    private final UserRepository userRepository;
    private final EnrollmentRepository enrollmentRepository;

    // ======================================
    // MARK MATERIAL AS COMPLETED
    // ======================================

    @Transactional
    public StudyMaterialProgress markCompletedByEmail(
            String email,
            Long materialId) {

        User student = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found"
                ));

        if (student.getRole() == null
                || !"STUDENT".equalsIgnoreCase(
                        student.getRole().getName())) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only students can update study progress"
            );
        }

        if (Boolean.FALSE.equals(student.getActive())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Inactive students cannot update progress"
            );
        }

        StudyMaterial material = materialRepository.findById(materialId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Study material not found"
                ));

        if (Boolean.FALSE.equals(material.getActive())) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Study material is inactive"
            );
        }

        Long courseId = material.getCourse().getId();

        Enrollment enrollment = enrollmentRepository
                .findByStudent_IdAndCourse_Id(
                        student.getId(),
                        courseId
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "You are not enrolled in this course"
                ));

        StudyMaterialProgress progress = progressRepository
                .findByStudent_IdAndStudyMaterial_Id(
                        student.getId(),
                        materialId
                )
                .orElseGet(() -> {
                    StudyMaterialProgress newProgress =
                            new StudyMaterialProgress();

                    newProgress.setStudent(student);
                    newProgress.setStudyMaterial(material);
                    newProgress.setCompleted(false);

                    return newProgress;
                });

        if (!Boolean.TRUE.equals(progress.getCompleted())) {
            progress.setCompleted(true);
            progress.setCompletedAt(LocalDateTime.now());
        }

        StudyMaterialProgress savedProgress =
                progressRepository.save(progress);

        updateEnrollmentProgress(
                student.getId(),
                courseId,
                enrollment
        );

        return savedProgress;
    }

    // ======================================
    // GET COMPLETED MATERIAL IDS
    // ======================================

    @Transactional(readOnly = true)
    public List<Long> getCompletedMaterialIds(Long studentId) {

        return progressRepository
                .findByStudent_IdAndCompletedTrue(studentId)
                .stream()
                .map(progress ->
                        progress.getStudyMaterial().getId())
                .toList();
    }

    // ======================================
    // UPDATE ENROLLMENT PROGRESS
    // ======================================

    private void updateEnrollmentProgress(
            Long studentId,
            Long courseId,
            Enrollment enrollment) {

        long totalMaterials =
                materialRepository.countByCourse_Id(courseId);

        if (totalMaterials == 0) {
            enrollment.setCompletionPercentage(0.0);
            enrollmentRepository.save(enrollment);
            return;
        }

        long completedMaterials =
                progressRepository
                        .countByStudent_IdAndStudyMaterial_Course_IdAndCompleted(
                                studentId,
                                courseId,
                                true
                        );

        double percentage =
                completedMaterials * 100.0 / totalMaterials;

        enrollment.setCompletionPercentage(percentage);

        enrollmentRepository.save(enrollment);
    }
}