package com.lms.repository;

import com.lms.entity.ExamApprovalRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    // Admin reporting
    long countByStatus(ExamApprovalRequest.Status status);

    long countByCourse_Faculty_IdAndStatus(
            Long facultyId,
            ExamApprovalRequest.Status status
    );

    List<ExamApprovalRequest> findAllByOrderByRequestedAtDesc();

    List<ExamApprovalRequest>
    findByCourse_Faculty_IdOrderByRequestedAtDesc(Long facultyId);

    long countByStudent_Id(Long studentId);

    /**
     * Paged admin search across every exam approval request.
     * The search term must already be lower-case with % wildcards.
     */
    @Query(
            value = """
                    SELECT r
                    FROM ExamApprovalRequest r
                    JOIN FETCH r.student s
                    JOIN FETCH r.course c
                    LEFT JOIN FETCH c.faculty f
                    WHERE (:status IS NULL OR CAST(r.status AS string) = :status)
                      AND (:courseId IS NULL OR c.id = :courseId)
                      AND (:facultyId IS NULL OR f.id = :facultyId)
                      AND (
                            :search IS NULL
                            OR LOWER(CONCAT(s.firstName, ' ', s.lastName)) LIKE :search
                            OR LOWER(s.email) LIKE :search
                      )
                    ORDER BY r.requestedAt DESC
                    """,
            countQuery = """
                    SELECT COUNT(r)
                    FROM ExamApprovalRequest r
                    JOIN r.course c
                    LEFT JOIN c.faculty f
                    WHERE (:status IS NULL OR CAST(r.status AS string) = :status)
                      AND (:courseId IS NULL OR c.id = :courseId)
                      AND (:facultyId IS NULL OR f.id = :facultyId)
                      AND (
                            :search IS NULL
                            OR LOWER(CONCAT(r.student.firstName, ' ', r.student.lastName)) LIKE :search
                            OR LOWER(r.student.email) LIKE :search
                      )
                    """
    )
    Page<ExamApprovalRequest> searchRequests(
            @Param("status") String status,
            @Param("courseId") Long courseId,
            @Param("facultyId") Long facultyId,
            @Param("search") String search,
            Pageable pageable
    );
}