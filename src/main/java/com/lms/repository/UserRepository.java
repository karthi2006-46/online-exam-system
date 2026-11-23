package com.lms.repository;

import com.lms.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    List<User> findByRole_Name(String roleName);

    List<User> findByRole_NameAndActiveTrue(String roleName);

    List<User> findByActiveTrue();

    long countByRole_Name(String roleName);

    long countByRole_NameAndActive(String roleName, Boolean active);

    Page<User> findByRole_Name(String roleName, Pageable pageable);

    Page<User> findByRole_NameAndActive(
            String roleName,
            Boolean active,
            Pageable pageable
    );

    /**
     * Search users by role with an optional active filter.
     * The search term must already be lower-case with % wildcards.
     */
    @Query("""
            SELECT u
            FROM User u
            WHERE u.role.name = :roleName
              AND (:active IS NULL OR u.active = :active)
              AND (
                    :search IS NULL
                    OR LOWER(CONCAT(u.firstName, ' ', u.lastName)) LIKE :search
                    OR LOWER(u.email) LIKE :search
              )
            """)
    Page<User> searchByRole(
            @Param("roleName") String roleName,
            @Param("active") Boolean active,
            @Param("search") String search,
            Pageable pageable
    );

    /**
     * Same as searchByRole but restricted to users enrolled in a course.
     * Used by the admin student list course filter.
     */
    @Query("""
            SELECT u
            FROM User u
            WHERE u.role.name = :roleName
              AND (:active IS NULL OR u.active = :active)
              AND (
                    :courseId IS NULL
                    OR EXISTS (
                        SELECT 1
                        FROM Enrollment e
                        WHERE e.student.id = u.id
                          AND e.course.id = :courseId
                    )
              )
              AND (
                    :search IS NULL
                    OR LOWER(CONCAT(u.firstName, ' ', u.lastName)) LIKE :search
                    OR LOWER(u.email) LIKE :search
              )
            """)
    Page<User> searchByRoleAndCourse(
            @Param("roleName") String roleName,
            @Param("active") Boolean active,
            @Param("courseId") Long courseId,
            @Param("search") String search,
            Pageable pageable
    );
}
