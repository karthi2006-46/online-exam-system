package com.lms.repository;

import com.lms.entity.ExamApprovalRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ExamApprovalRequestRepository
        extends JpaRepository<ExamApprovalRequest, Long> {

    // Existing methods
    List<ExamApprovalRequest> findByStatus(
            ExamApprovalRequest.Status status
    );

    Optional<ExamApprovalRequest> findByStudent_IdAndCourse_IdAndStatus(
            Long studentId,
            Long courseId,
            ExamApprovalRequest.Status status
    );

    Optional<ExamApprovalRequest> findByStudent_IdAndCourse_Id(
            Long studentId,
            Long courseId
    );

    // New: pending requests for courses assigned to a faculty member
    List<ExamApprovalRequest>
    findByCourse_Faculty_IdAndStatus(
            Long facultyId,
            ExamApprovalRequest.Status status
    );

    // New: retrieve requests belonging to a particular student
    List<ExamApprovalRequest> findByStudent_Id(Long studentId);
}