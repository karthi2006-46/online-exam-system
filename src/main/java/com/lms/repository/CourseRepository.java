package com.lms.repository;

import com.lms.entity.Course;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {
    Optional<Course> findByCourseCode(String courseCode);
    List<Course> findByActiveTrue();
    List<Course> findByFaculty_Id(Long facultyId);
    List<Course> findByFaculty_IdAndActiveTrue(Long facultyId);
    long countByActiveTrue();

    /**
     * Paged course search for the admin course management screen.
     * The search term must already be lower-case with % wildcards.
     */
    @Query(
            value = """
                    SELECT c
                    FROM Course c
                    LEFT JOIN FETCH c.faculty f
                    WHERE (:active IS NULL OR c.active = :active)
                      AND (:facultyId IS NULL OR f.id = :facultyId)
                      AND (
                            :search IS NULL
                            OR LOWER(c.title) LIKE :search
                            OR LOWER(c.courseCode) LIKE :search
                            OR LOWER(CONCAT(f.firstName, ' ', f.lastName)) LIKE :search
                      )
                    """,
            countQuery = """
                    SELECT COUNT(c)
                    FROM Course c
                    LEFT JOIN c.faculty f
                    WHERE (:active IS NULL OR c.active = :active)
                      AND (:facultyId IS NULL OR f.id = :facultyId)
                      AND (
                            :search IS NULL
                            OR LOWER(c.title) LIKE :search
                            OR LOWER(c.courseCode) LIKE :search
                            OR LOWER(CONCAT(f.firstName, ' ', f.lastName)) LIKE :search
                      )
                    """
    )
    Page<Course> searchCourses(
            @Param("active") Boolean active,
            @Param("facultyId") Long facultyId,
            @Param("search") String search,
            Pageable pageable
    );
}
