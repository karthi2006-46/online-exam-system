package com.lms.repository;

import com.lms.entity.ExtensionRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExtensionRequestRepository
        extends JpaRepository<ExtensionRequest, Long> {

    List<ExtensionRequest> findByEnrollment_Id(Long enrollmentId);

    List<ExtensionRequest> findByStatus(String status);

    List<ExtensionRequest> findByEnrollment_Student_Id(Long studentId);

    long countByEnrollment_IdAndStatus(Long enrollmentId, String status);

    long countByEnrollment_Id(Long enrollmentId);
    long countByEnrollment_Course_Faculty_IdAndStatusIgnoreCase(
        Long facultyId,
        String status
);
}