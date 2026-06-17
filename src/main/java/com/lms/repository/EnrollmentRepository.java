package com.lms.repository;

import com.lms.entity.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
