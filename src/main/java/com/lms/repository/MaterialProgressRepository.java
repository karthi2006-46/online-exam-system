package com.lms.repository;

import com.lms.entity.MaterialProgress;

import org.springframework.data.jpa.repository.JpaRepository;

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

    long countByStudent_Id(Long studentId);
}
