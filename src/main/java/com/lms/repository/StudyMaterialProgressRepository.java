package com.lms.repository;

import com.lms.entity.StudyMaterialProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StudyMaterialProgressRepository
        extends JpaRepository<StudyMaterialProgress, Long> {

    // Find progress for a particular student and material
    Optional<StudyMaterialProgress>
    findByStudent_IdAndStudyMaterial_Id(
            Long studentId,
            Long materialId
    );

    // Get completed materials for a student
    List<StudyMaterialProgress>
    findByStudent_IdAndCompletedTrue(Long studentId);

    // Count completed active materials for a student in a course
    @Query("""
        SELECT COUNT(p)
        FROM StudyMaterialProgress p
        WHERE p.student.id = :studentId
          AND p.studyMaterial.course.id = :courseId
          AND p.completed = true
          AND p.studyMaterial.active = true
        """)
    long countCompletedActiveMaterials(
            @Param("studentId") Long studentId,
            @Param("courseId") Long courseId
    );

    // Count completed materials for a student in a course
    long countByStudent_IdAndStudyMaterial_Course_IdAndCompleted(
            Long studentId,
            Long courseId,
            Boolean completed
    );
}