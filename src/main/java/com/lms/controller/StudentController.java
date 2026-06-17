package com.lms.controller;

import com.lms.dto.MyCourseDTO;
import com.lms.entity.Enrollment;
import com.lms.repository.EnrollmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/student")
@RequiredArgsConstructor
public class StudentController {

    private final EnrollmentRepository enrollmentRepository;

    @GetMapping("/courses/{studentId}")
    public List<MyCourseDTO> getStudentCourses(
            @PathVariable Long studentId) {

        List<Enrollment> enrollments =
                enrollmentRepository.findByStudent_Id(studentId);

        return enrollments.stream()
                .map(enrollment -> {

                    MyCourseDTO dto =
                            new MyCourseDTO();

                    dto.setCourseId(
                            enrollment.getCourse().getId());

                    dto.setCourseTitle(
                            enrollment.getCourse().getTitle());

                    dto.setDurationDays(
                            enrollment.getCourse().getDurationDays());

                    dto.setProgress(
                            enrollment.getCompletionPercentage());

                    dto.setStatus(
                            enrollment.getStatus());
                            dto.setDueDate(
    enrollment.getDueDate()
);

                    return dto;

                })
                .collect(Collectors.toList());
    }
}