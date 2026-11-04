package com.lms.repository;

import com.lms.entity.StudyMaterial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudyMaterialRepository extends JpaRepository<StudyMaterial, Long> {


// Retrieve active materials for a course
List<StudyMaterial> findByCourse_IdAndActiveTrue(Long courseId);

// Retrieve active materials by course and type
List<StudyMaterial> findByCourse_IdAndFileTypeAndActiveTrue(
        Long courseId, String fileType);

// Retrieve active materials across all courses
List<StudyMaterial> findByActiveTrue();

// Count all materials for a course
long countByCourse_Id(Long courseId);

// Count active materials for a course
long countByCourse_IdAndActiveTrue(Long courseId);

// Count active materials assigned to a faculty member
long countByCourse_Faculty_IdAndActiveTrue(Long facultyId);

}
