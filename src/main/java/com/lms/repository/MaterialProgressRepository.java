package com.lms.repository;

import com.lms.entity.MaterialProgress;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;

public interface MaterialProgressRepository
        extends JpaRepository<MaterialProgress, Long> {

    List<MaterialProgress> findByStudent_Id(Long studentId);

    Optional<MaterialProgress> findByStudent_IdAndMaterial_Id(
            Long studentId,
            Long materialId
    );

    long countByStudent_IdAndMaterial_Course_IdAndCompleted(
            Long studentId,
            Long courseId,
            Boolean completed
    );

    /**
     * Counts DISTINCT active materials a student has completed in a
     * course. Completion is recomputed against currently assigned
     * (active) materials, so a removed material can never inflate the
     * percentage, and duplicate progress rows are ignored.
     */
    @Query("""
            SELECT COUNT(DISTINCT p.material.id)
            FROM MaterialProgress p
            WHERE p.student.id = :studentId
              AND p.material.course.id = :courseId
              AND p.material.active = true
              AND p.completed = true
            """)
    long countCompletedActiveMaterials(
            @Param("studentId") Long studentId,
            @Param("courseId") Long courseId
    );

    long countByStudent_Id(Long studentId);
}
