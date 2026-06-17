package com.lms.repository;

import com.lms.entity.StudyMaterial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface StudyMaterialRepository extends JpaRepository<StudyMaterial, Long> {
    List<StudyMaterial> findByCourse_Id(Long courseId);
    List<StudyMaterial> findByCourse_IdAndFileType(Long courseId, String fileType);
    List<StudyMaterial> findByActiveTrue();
}
