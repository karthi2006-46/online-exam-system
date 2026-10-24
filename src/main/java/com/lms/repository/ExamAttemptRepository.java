package com.lms.repository;

import com.lms.entity.ExamAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExamAttemptRepository
        extends JpaRepository<ExamAttempt, Long> {

    // Find all attempts for a specific exam, student and status.
    List<ExamAttempt> findByExam_IdAndStudent_IdAndStatus(
            Long examId,
            Long studentId,
            String status
    );

    // Find attempts matching any of the specified statuses.
    List<ExamAttempt> findByExam_IdAndStudent_IdAndStatusIn(
            Long examId,
            Long studentId,
            List<String> statuses
    );

    List<ExamAttempt> findByStudent_Id(Long studentId);

    List<ExamAttempt> findByExam_Id(Long examId);

    List<ExamAttempt> findByStudent_IdAndExam_Id(
            Long studentId,
            Long examId
    );

    long countByStudent_IdAndExam_Course_IdAndStatusIn(
            Long studentId,
            Long courseId,
            List<String> statuses
    );

    /**
     * Counts distinct exams completed by a student
     * in a particular course.
     */
    @Query("""
        SELECT COUNT(DISTINCT ea.exam.id)
        FROM ExamAttempt ea
        WHERE ea.student.id = :studentId
          AND ea.exam.course.id = :courseId
          AND ea.status IN :statuses
        """)
    long countDistinctCompletedExams(
            @Param("studentId") Long studentId,
            @Param("courseId") Long courseId,
            @Param("statuses") List<String> statuses
    );
}