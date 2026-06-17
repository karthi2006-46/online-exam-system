package com.lms.service;

import com.lms.dto.CourseDTO;
import com.lms.dto.CreateCourseRequest;
import com.lms.entity.Course;
import com.lms.entity.User;
import com.lms.exception.BadRequestException;
import com.lms.exception.ResourceNotFoundException;
import com.lms.repository.CourseRepository;
import com.lms.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class CourseService {

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private UserRepository userRepository;

    public CourseDTO createCourse(CreateCourseRequest request) {
        if (courseRepository.findByCourseCode(request.getCourseCode()).isPresent()) {
            throw new BadRequestException("Course code already exists");
        }

        User faculty = userRepository.findById(request.getFacultyId())
                .orElseThrow(() -> new ResourceNotFoundException("Faculty not found"));

        if (!faculty.getRole().getName().equalsIgnoreCase("FACULTY")) {
            throw new BadRequestException("Selected user is not a faculty member");
        }

        Course course = new Course();
        course.setTitle(request.getTitle());
        course.setDescription(request.getDescription());
        course.setCourseCode(request.getCourseCode());
        course.setDurationDays(request.getDurationDays());
        course.setTotalFees(request.getTotalFees());
        course.setFaculty(faculty);
        course.setActive(true);

        Course savedCourse = courseRepository.save(course);
        return convertToDTO(savedCourse);
    }

    public CourseDTO getCourseById(Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));
        return convertToDTO(course);
    }

    public List<CourseDTO> getAllActiveCourses() {
        return courseRepository.findByActiveTrue().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<CourseDTO> getAllCourses() {
        return courseRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<CourseDTO> getCoursesByFaculty(Long facultyId) {
        return courseRepository.findByFaculty_Id(facultyId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public CourseDTO updateCourse(Long courseId, CreateCourseRequest request) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));

        course.setTitle(request.getTitle());
        course.setDescription(request.getDescription());
        course.setDurationDays(request.getDurationDays());
        course.setTotalFees(request.getTotalFees());

        Course updatedCourse = courseRepository.save(course);
        return convertToDTO(updatedCourse);
    }

    public void deleteCourse(Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));
        course.setActive(false);
        courseRepository.save(course);
    }

    private CourseDTO convertToDTO(Course course) {
        return new CourseDTO(
                course.getId(),
                course.getTitle(),
                course.getDescription(),
                course.getCourseCode(),
                course.getDurationDays(),
                course.getTotalFees(),
                course.getFaculty().getFirstName() + " " + course.getFaculty().getLastName(),
                course.getActive(),
                course.getCreatedAt()
        );
    }
}
