package com.lms.repository;

import com.lms.entity.ProgressTracking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProgressTrackingRepository extends JpaRepository<ProgressTracking, Long> {
    List<ProgressTracking> findByEnrollment_Id(Long enrollmentId);
    Optional<ProgressTracking> findByEnrollment_IdAndMaterial_Id(Long enrollmentId, Long materialId);
    List<ProgressTracking> findByCompleted(Boolean completed);
}
