package com.lms.controller;

import com.lms.dto.CourseProgressDTO;
import com.lms.dto.MyCourseDTO;
import com.lms.dto.StudentDashboardProgressDTO;
import com.lms.entity.Enrollment;
import com.lms.repository.EnrollmentRepository;
import com.lms.service.StudentDashboardService;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/student")
@RequiredArgsConstructor
public class StudentController {

    private final EnrollmentRepository enrollmentRepository;
    private final StudentDashboardService studentDashboardService;

    // STUDENT DASHBOARD
    @GetMapping("/dashboard")
    public StudentDashboardProgressDTO getDashboard(
            @RequestParam Long studentId) {

        return studentDashboardService.getDashboard(studentId);
    }

    // MY COURSES
@GetMapping("/courses/{studentId}")
public List<MyCourseDTO> getStudentCourses(
        @PathVariable Long studentId) {

    List<Enrollment> enrollments =
            enrollmentRepository.findByStudent_Id(studentId);

    StudentDashboardProgressDTO dashboard =
            studentDashboardService.getDashboard(studentId);

    return enrollments.stream()
            .map(enrollment -> {

                MyCourseDTO dto = new MyCourseDTO();

                // Include the enrollment ID
                dto.setEnrollmentId(enrollment.getId());

System.out.println("DEBUG Enrollment ID: " + enrollment.getId());
System.out.println("DEBUG DTO Enrollment ID: " + dto.getEnrollmentId());

                Long courseId = enrollment.getCourse().getId();

                dto.setCourseId(courseId);
                dto.setCourseTitle(
                        enrollment.getCourse().getTitle());
                dto.setDurationDays(
                        enrollment.getCourse().getDurationDays());

                double progress = dashboard.getCourses()
                        .stream()
                        .filter(course ->
                                course.getCourseId().equals(courseId))
                        .mapToDouble(
                                CourseProgressDTO::getOverallProgress)
                        .findFirst()
                        .orElse(0.0);

                dto.setProgress(progress);
                dto.setStatus(enrollment.getStatus());
                dto.setDueDate(enrollment.getDueDate());

                // Debug output
                System.out.println(
                        "Enrollment ID: " + enrollment.getId());
                System.out.println(
                        "DTO Enrollment ID: " + dto.getEnrollmentId());

                return dto;
            })
            .collect(Collectors.toList());
}
}