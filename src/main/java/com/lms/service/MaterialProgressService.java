package com.lms.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.lms.entity.MaterialProgress;
import com.lms.entity.StudyMaterial;
import com.lms.repository.EnrollmentRepository;
import com.lms.repository.MaterialProgressRepository;
import com.lms.repository.StudyMaterialRepository;
import com.lms.repository.UserRepository;
import com.lms.entity.User;
import com.lms.entity.Enrollment;
import java.util.Optional;
import java.util.*;

@Service
public class MaterialProgressService {

    @Autowired
    private MaterialProgressRepository progressRepo;

    @Autowired
    private StudyMaterialRepository materialRepo;

    @Autowired
    private EnrollmentRepository enrollmentRepo;

    @Autowired
    private UserRepository userRepo;

   public void markCompleted(
        Long studentId,
        Long materialId
){

    Optional<MaterialProgress> existing =
        progressRepo.findByStudent_IdAndMaterial_Id(
                studentId,
                materialId
        );

    if(existing.isPresent()){
        return;
    }

    User student =
            userRepo.findById(studentId)
                    .orElseThrow();

    StudyMaterial material =
            materialRepo.findById(materialId)
                    .orElseThrow();

    MaterialProgress progress =
            new MaterialProgress();

    progress.setStudent(student);
    progress.setMaterial(material);
    progress.setCompleted(true);

    progressRepo.save(progress);

    updateEnrollmentProgress(
            studentId,
            material.getCourse().getId()
    );
}
public List<Long> getCompletedMaterialIds(
        Long studentId){

    return progressRepo
            .findByStudent_Id(studentId)
            .stream()
            .map(progress ->
                    progress.getMaterial().getId())
            .toList();
}
    

    // 👇 Step 5 code paste here
    private void updateEnrollmentProgress(
            Long studentId,
            Long courseId
    ){

        long totalMaterials =
                materialRepo.countByCourse_Id(
                        courseId
                );

        long completedMaterials =
                progressRepo
                        .countByStudent_IdAndMaterial_Course_IdAndCompleted(
                                studentId,
                                courseId,
                                true
                        );

        double percentage =
                ((double) completedMaterials
                        / totalMaterials) * 100;

        Enrollment enrollment =
                enrollmentRepo
                        .findByStudent_IdAndCourse_Id(
                                studentId,
                                courseId
                        )
                        .orElseThrow();

        enrollment.setCompletionPercentage(
                percentage
        );

        enrollmentRepo.save(
                enrollment
        );
        
    }
    
}