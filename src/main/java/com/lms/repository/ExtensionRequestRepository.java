package com.lms.repository;

import com.lms.entity.ExtensionRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    // Admin reporting
    long countByStatusIgnoreCase(String status);

    List<ExtensionRequest> findAllByOrderByCreatedAtDesc();

    List<ExtensionRequest>
    findByEnrollment_Course_Faculty_IdOrderByCreatedAtDesc(
            Long facultyId
    );

    long countByEnrollment_Student_Id(Long studentId);

    long countByReviewedBy_Id(Long reviewerId);

    /**
     * Paged admin search across every course extension request.
     * The search term must already be lower-case with % wildcards.
     */
    @Query(
            value = """
                    SELECT r
                    FROM ExtensionRequest r
                    JOIN FETCH r.enrollment e
                    JOIN FETCH e.student s
                    JOIN FETCH e.course c
                    LEFT JOIN FETCH c.faculty f
                    LEFT JOIN FETCH r.reviewedBy rb
                    WHERE (:status IS NULL OR UPPER(r.status) = :status)
                      AND (:courseId IS NULL OR c.id = :courseId)
                      AND (:facultyId IS NULL OR f.id = :facultyId)
                      AND (
                            :search IS NULL
                            OR LOWER(CONCAT(s.firstName, ' ', s.lastName)) LIKE :search
                            OR LOWER(s.email) LIKE :search
                            OR LOWER(c.title) LIKE :search
                      )
                    ORDER BY r.createdAt DESC
                    """,
            countQuery = """
                    SELECT COUNT(r)
                    FROM ExtensionRequest r
                    JOIN r.enrollment e
                    JOIN e.student s
                    JOIN e.course c
                    LEFT JOIN c.faculty f
                    WHERE (:status IS NULL OR UPPER(r.status) = :status)
                      AND (:courseId IS NULL OR c.id = :courseId)
                      AND (:facultyId IS NULL OR f.id = :facultyId)
                      AND (
                            :search IS NULL
                            OR LOWER(CONCAT(s.firstName, ' ', s.lastName)) LIKE :search
                            OR LOWER(s.email) LIKE :search
                            OR LOWER(c.title) LIKE :search
                      )
                    """
    )
    Page<ExtensionRequest> searchRequests(
            @Param("status") String status,
            @Param("courseId") Long courseId,
            @Param("facultyId") Long facultyId,
            @Param("search") String search,
            Pageable pageable
    );
}