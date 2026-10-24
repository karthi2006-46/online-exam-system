package com.lms.controller;

import com.lms.dto.CourseDTO;
import com.lms.dto.CreateCourseRequest;
import com.lms.service.CourseService;
import com.lms.util.ApiResponseUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import com.lms.entity.User;
import com.lms.repository.UserRepository;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/courses")
@CrossOrigin(origins = "*")
public class CourseController {

    @Autowired
    private CourseService courseService;
    @Autowired
private UserRepository userRepository;

    @PostMapping
    @PreAuthorize("hasRole('FACULTY') or hasRole('ADMIN')")
    public ResponseEntity<?> createCourse(@Valid @RequestBody CreateCourseRequest request) {
        CourseDTO courseDTO = courseService.createCourse(request);
        return ApiResponseUtil.created("Course created successfully", courseDTO);
    }

    @GetMapping("/active")
    public ResponseEntity<?> getAllActiveCourses() {
        List<CourseDTO> courses = courseService.getAllActiveCourses();
        return ApiResponseUtil.success("Active courses retrieved", courses);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllCourses() {
        List<CourseDTO> courses = courseService.getAllCourses();
        return ApiResponseUtil.success("All courses retrieved", courses);
    }

    @GetMapping("/{courseId}")
    public ResponseEntity<?> getCourseById(@PathVariable Long courseId) {
        CourseDTO courseDTO = courseService.getCourseById(courseId);
        return ApiResponseUtil.success("Course retrieved successfully", courseDTO);
    }
@GetMapping("/my-courses")
@PreAuthorize("hasRole('FACULTY') or hasRole('ADMIN')")
public ResponseEntity<?> getMyCourses(Authentication authentication) {

    UserDetails userDetails =
            (UserDetails) authentication.getPrincipal();

    User user = userRepository.findByEmail(userDetails.getUsername())
            .orElseThrow(() ->
                    new RuntimeException("Authenticated user not found"));

    List<CourseDTO> courses =
            courseService.getCoursesByFaculty(user.getId());

    return ApiResponseUtil.success(
            "Assigned courses retrieved successfully",
            courses
    );
}

    @PutMapping("/{courseId}")
    @PreAuthorize("hasRole('FACULTY') or hasRole('ADMIN')")
    public ResponseEntity<?> updateCourse(@PathVariable Long courseId, @Valid @RequestBody CreateCourseRequest request) {
        CourseDTO courseDTO = courseService.updateCourse(courseId, request);
        return ApiResponseUtil.success("Course updated successfully", courseDTO);
    }

    @DeleteMapping("/{courseId}")
    @PreAuthorize("hasRole('FACULTY') or hasRole('ADMIN')")
    public ResponseEntity<?> deleteCourse(@PathVariable Long courseId) {
        courseService.deleteCourse(courseId);
        return ApiResponseUtil.success("Course deleted successfully");
    }
}
