package com.lms.repository;

import com.lms.entity.Enrollment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
    Optional<Enrollment> findByStudent_IdAndCourse_Id(Long studentId, Long courseId);
    List<Enrollment> findByStudent_Id(Long studentId);
    List<Enrollment> findByCourse_Id(Long courseId);
    List<Enrollment> findByStatusAndStudent_Id(String status, Long studentId);
    List<Enrollment> findByCourse_IdAndStatus(Long courseId, String status);
    long countByStudent_Id(Long studentId);
    long countByStudent_IdAndStatusIgnoreCase(
        Long studentId,
        String status
);
long countByCourse_Faculty_Id(Long facultyId);

long countByCourse_Faculty_IdAndStatusIgnoreCase(
        Long facultyId,
        String status
);
    List<Enrollment> findByCourse_Faculty_Id(Long facultyId);

List<Enrollment> findByCourse_Faculty_IdOrderByEnrollmentDateDesc(
        Long facultyId
);

    // Admin reporting: global and course-level status counts
    long countByStatusIgnoreCase(String status);

    long countByCourse_IdAndStatusIgnoreCase(
            Long courseId,
            String status
    );

    long countByCourse_Id(Long courseId);

    /**
     * Paged student-performance search for the admin module.
     */
    @Query("""
            SELECT e
            FROM Enrollment e
            JOIN FETCH e.student s
            JOIN FETCH e.course c
            LEFT JOIN FETCH c.faculty f
            WHERE (:courseId IS NULL OR c.id = :courseId)
              AND (:facultyId IS NULL OR f.id = :facultyId)
              AND (:status IS NULL OR UPPER(e.status) = :status)
              AND (
                    :search IS NULL
                    OR LOWER(CONCAT(s.firstName, ' ', s.lastName)) LIKE :search
                    OR LOWER(s.email) LIKE :search
              )
              AND (
                    :approvalStatus IS NULL
                    OR (
                        :approvalStatus = 'NOT_REQUESTED'
                        AND NOT EXISTS (
                            SELECT 1
                            FROM ExamApprovalRequest r
                            WHERE r.student.id = s.id
                              AND r.course.id = c.id
                        )
                    )
                    OR (
                        :approvalStatus <> 'NOT_REQUESTED'
                        AND EXISTS (
                            SELECT 1
                            FROM ExamApprovalRequest r
                            WHERE r.student.id = s.id
                              AND r.course.id = c.id
                              AND CAST(r.status AS string) = :approvalStatus
                        )
                    )
              )
            """)
    Page<Enrollment> searchPerformance(
            @Param("courseId") Long courseId,
            @Param("facultyId") Long facultyId,
            @Param("status") String status,
            @Param("approvalStatus") String approvalStatus,
            @Param("search") String search,
            Pageable pageable
    );
}
