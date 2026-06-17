package com.lms.repository;

import com.lms.entity.ExamAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExamAttemptRepository extends JpaRepository<ExamAttempt, Long> {
    Optional<ExamAttempt> findByExam_IdAndStudent_IdAndStatusNot(Long examId, Long studentId, String status);
    List<ExamAttempt> findByStudent_Id(Long studentId);
    List<ExamAttempt> findByExam_Id(Long examId);
    List<ExamAttempt> findByStudent_IdAndExam_Id(Long studentId, Long examId);
}
