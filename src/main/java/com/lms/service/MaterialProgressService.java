package com.lms.service;

import com.lms.entity.Enrollment;
import com.lms.entity.MaterialProgress;
import com.lms.entity.StudyMaterial;
import com.lms.entity.User;
import com.lms.repository.EnrollmentRepository;
import com.lms.repository.MaterialProgressRepository;
import com.lms.repository.StudyMaterialRepository;
import com.lms.repository.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MaterialProgressService {

    private final MaterialProgressRepository progressRepo;
    private final StudyMaterialRepository materialRepo;
    private final EnrollmentRepository enrollmentRepo;
    private final UserRepository userRepo;

    // ======================================
    // MARK MATERIAL AS COMPLETED
    // ======================================

    @Transactional
    public MaterialProgress markCompleted(
            Long studentId,
            Long materialId) {

        User student = userRepo.findById(studentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Student not found"
                ));

        if (student.getRole() == null
                || !"STUDENT".equalsIgnoreCase(
                        student.getRole().getName())) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only students can update progress"
            );
        }

        if (Boolean.FALSE.equals(student.getActive())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Inactive students cannot update progress"
            );
        }

        StudyMaterial material = materialRepo.findById(materialId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Study material not found"
                ));

        Long courseId = material.getCourse().getId();

        // Confirm that the student is enrolled in this course.
        Enrollment enrollment = enrollmentRepo
                .findByStudent_IdAndCourse_Id(studentId, courseId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "You are not enrolled in this course"
                ));

        // Avoid duplicate progress records.
        MaterialProgress progress = progressRepo
                .findByStudent_IdAndMaterial_Id(
                        studentId,
                        materialId
                )
                .orElseGet(() -> {
                    MaterialProgress newProgress =
                            new MaterialProgress();

                    newProgress.setStudent(student);
                    newProgress.setMaterial(material);
                    newProgress.setCompleted(false);

                    return newProgress;
                });

        // Mark completed only once.
        if (!Boolean.TRUE.equals(progress.getCompleted())) {
            progress.setCompleted(true);
            progressRepo.save(progress);
        }

        // Update enrollment percentage.
        updateEnrollmentProgress(
                studentId,
                courseId,
                enrollment
        );

        return progress;
    }

    // ======================================
    // GET COMPLETED MATERIAL IDS
    // ======================================

    @Transactional(readOnly = true)
    public List<Long> getCompletedMaterialIds(Long studentId) {

        return progressRepo
                .findByStudent_Id(studentId)
                .stream()
                .filter(progress ->
                        Boolean.TRUE.equals(progress.getCompleted()))
                .map(progress -> progress.getMaterial().getId())
                .toList();
    }

    // ======================================
    // UPDATE COURSE PROGRESS
    // ======================================

    private void updateEnrollmentProgress(
            Long studentId,
            Long courseId,
            Enrollment enrollment) {

        long totalMaterials =
                materialRepo.countByCourse_Id(courseId);

        if (totalMaterials == 0) {
            enrollment.setCompletionPercentage(0.0);
            enrollmentRepo.save(enrollment);
            return;
        }

        long completedMaterials =
                progressRepo
                        .countByStudent_IdAndMaterial_Course_IdAndCompleted(
                                studentId,
                                courseId,
                                true
                        );

        double percentage =
                (completedMaterials * 100.0) / totalMaterials;

        enrollment.setCompletionPercentage(percentage);

        enrollmentRepo.save(enrollment);
    }
}