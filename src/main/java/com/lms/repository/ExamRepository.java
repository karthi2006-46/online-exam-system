package com.lms.repository;

import com.lms.entity.Exam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ExamRepository extends JpaRepository<Exam, Long> {
    List<Exam> findByCourse_Id(Long courseId);
    List<Exam> findByActiveTrue();
    List<Exam> findByPublishedTrue();
    List<Exam> findByCourse_IdAndPublishedTrue(Long courseId);
}
