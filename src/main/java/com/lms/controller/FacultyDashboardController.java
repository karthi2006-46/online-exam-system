package com.lms.controller;

import com.lms.dto.FacultyDashboardDTO;
import com.lms.dto.FacultyStudentDTO;
import com.lms.entity.Enrollment;
import com.lms.repository.EnrollmentRepository;
import com.lms.repository.UserRepository;
import com.lms.service.FacultyDashboardService;
import com.lms.service.StudentDashboardService;

import lombok.RequiredArgsConstructor;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/faculty/dashboard")
@RequiredArgsConstructor
public class FacultyDashboardController {

    private final FacultyDashboardService dashboardService;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final StudentDashboardService studentDashboardService;

    /**
     * Get faculty dashboard statistics.
     */
    @GetMapping
    @PreAuthorize("hasRole('FACULTY')")
    public FacultyDashboardDTO getDashboard(
            Authentication authentication) {

        return dashboardService.getDashboard(authentication);
    }

    /**
     * Get students enrolled in courses assigned to the logged-in faculty.
     * Progress is calculated using StudentDashboardService.
     */
    @GetMapping("/students")
    @PreAuthorize("hasRole('FACULTY')")
    public List<FacultyStudentDTO> getFacultyStudents(
            Authentication authentication) {

        // Validate authentication
        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof UserDetails)) {

            throw new AccessDeniedException(
                    "Authenticated faculty required");
        }

        UserDetails details =
                (UserDetails) authentication.getPrincipal();

        // Find the logged-in faculty
        var faculty = userRepository
                .findByEmail(details.getUsername())
                .orElseThrow(() ->
                        new AccessDeniedException("Faculty not found"));

        // Verify faculty role
        if (faculty.getRole() == null
                || faculty.getRole().getName() == null
                || !faculty.getRole().getName()
                        .equalsIgnoreCase("FACULTY")) {

            throw new AccessDeniedException(
                    "Faculty access required");
        }

        // Get enrollments for courses assigned to this faculty
        List<Enrollment> enrollments =
                enrollmentRepository
                        .findByCourse_Faculty_IdOrderByEnrollmentDateDesc(
                                faculty.getId());

        // Map enrollment data and calculate actual course progress
        return enrollments.stream()
                .map(enrollment -> {

                    var student = enrollment.getStudent();
                    var course = enrollment.getCourse();

                    String firstName = student.getFirstName() == null
                            ? ""
                            : student.getFirstName();

                    String lastName = student.getLastName() == null
                            ? ""
                            : student.getLastName();

                    String fullName =
                            (firstName + " " + lastName).trim();

                    Long studentId = student.getId();
                    Long courseId = course.getId();

                    // Calculate progress using the existing student
                    // dashboard progress calculation.
                    double overallProgress =
                            studentDashboardService
                                    .getCourseOverallProgress(
                                            studentId,
                                            courseId);

return new FacultyStudentDTO(
        enrollment.getId(),
        studentId,
        fullName.isEmpty() ? "Student" : fullName,
        student.getEmail(),
        course.getTitle(),
        courseId,
        enrollment.getStatus(),
        overallProgress,
        enrollment.getEnrollmentDate(),
        enrollment.getDueDate()
);
                })
                .toList();
    }
}