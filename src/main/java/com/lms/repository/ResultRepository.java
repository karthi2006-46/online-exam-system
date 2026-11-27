package com.lms.repository;

import com.lms.entity.Result;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ResultRepository extends JpaRepository<Result, Long> {

    // Existing methods
    Optional<Result> findByExamAttempt_Id(Long examAttemptId);

    List<Result> findByStudent_Id(Long studentId);

    List<Result> findByExam_Id(Long examId);

    List<Result> findByStudent_IdAndExam_Course_Id(
            Long studentId,
            Long courseId
    );

    long countByExam_Id(Long examId);

    long countByExam_Course_Faculty_IdAndStatusIgnoreCase(
            Long facultyId,
            String status
    );

    List<Result> findByExam_Course_Faculty_Id(Long facultyId);


    // Get all results for students taking exams in a course
    List<Result> findByExam_Course_Id(Long courseId);


    // Get results for one exam in a course
    List<Result> findByExam_IdAndExam_Course_Id(
            Long examId,
            Long courseId
    );


    // Filter course results by exam, date range and PASS/FAIL
    @Query("""
        SELECT r
        FROM Result r
        JOIN r.exam e
        JOIN r.examAttempt a
        WHERE e.course.id = :courseId
          AND (:examId IS NULL OR e.id = :examId)
          AND (
              :status IS NULL
              OR LOWER(r.status) = LOWER(:status)
          )
          AND (
              CAST(:fromDate AS timestamp) IS NULL
              OR COALESCE(a.submissionTime, a.endTime, a.createdAt) >= :fromDate
          )
          AND (
              CAST(:toDate AS timestamp) IS NULL
              OR COALESCE(a.submissionTime, a.endTime, a.createdAt) <= :toDate
          )
        ORDER BY COALESCE(a.submissionTime, a.endTime, a.createdAt) DESC
        """)
    List<Result> findFilteredCourseResults(
            @Param("courseId") Long courseId,
            @Param("examId") Long examId,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            @Param("status") String status
    );


    // Get all results for a specific student in a course
    List<Result> findByStudent_IdAndExam_Course_IdOrderByCreatedAtDesc(
            Long studentId,
            Long courseId
    );


    // Get results for a specific student and exam
    List<Result> findByStudent_IdAndExam_Id(
            Long studentId,
            Long examId
    );


    // ==========================================
    // ADMIN REPORTING
    // ==========================================

    long countByStudent_Id(Long studentId);

    long countByStudent_IdAndStatusIgnoreCase(
            Long studentId,
            String status
    );

    long countByExam_Course_Faculty_Id(Long facultyId);

    long countByExam_Course_Id(Long courseId);

    long countByExam_Course_IdAndStatusIgnoreCase(
            Long courseId,
            String status
    );

    long countByStatusIgnoreCase(String status);

    /**
     * Average percentage for every result in a course.
     */
    @Query("""
            SELECT AVG(r.percentage)
            FROM Result r
            WHERE r.exam.course.id = :courseId
            """)
    Double averagePercentageByCourse(@Param("courseId") Long courseId);

    /**
     * Paged version of findFilteredCourseResults for the admin result pages.
     */
    @Query(
            value = """
                    SELECT r
                    FROM Result r
                    JOIN r.exam e
                    JOIN r.examAttempt a
                    WHERE e.course.id = :courseId
                      AND (:examId IS NULL OR e.id = :examId)
                      AND (
                          :status IS NULL
                          OR LOWER(r.status) = LOWER(:status)
                      )
                      AND (
                          CAST(:fromDate AS timestamp) IS NULL
                          OR COALESCE(a.submissionTime, a.endTime, a.createdAt) >= :fromDate
                      )
                      AND (
                          CAST(:toDate AS timestamp) IS NULL
                          OR COALESCE(a.submissionTime, a.endTime, a.createdAt) <= :toDate
                      )
                    """,
            countQuery = """
                    SELECT COUNT(r)
                    FROM Result r
                    JOIN r.exam e
                    JOIN r.examAttempt a
                    WHERE e.course.id = :courseId
                      AND (:examId IS NULL OR e.id = :examId)
                      AND (
                          :status IS NULL
                          OR LOWER(r.status) = LOWER(:status)
                      )
                      AND (
                          CAST(:fromDate AS timestamp) IS NULL
                          OR COALESCE(a.submissionTime, a.endTime, a.createdAt) >= :fromDate
                      )
                      AND (
                          CAST(:toDate AS timestamp) IS NULL
                          OR COALESCE(a.submissionTime, a.endTime, a.createdAt) <= :toDate
                      )
                    """
    )
    Page<Result> findFilteredCourseResultsPaged(
            @Param("courseId") Long courseId,
            @Param("examId") Long examId,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            @Param("status") String status,
            Pageable pageable
    );
}