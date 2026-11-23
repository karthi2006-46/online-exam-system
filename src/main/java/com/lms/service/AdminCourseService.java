package com.lms.service;

import com.lms.dto.admin.AdminCourseRequest;
import com.lms.dto.admin.AdminCourseRowDTO;
import com.lms.dto.admin.AdminDeleteResultDTO;
import com.lms.dto.admin.AdminPageDTO;
import com.lms.entity.Course;
import com.lms.entity.User;
import com.lms.exception.BadRequestException;
import com.lms.exception.ResourceNotFoundException;
import com.lms.repository.CodingChallengeRepository;
import com.lms.repository.CourseRepository;
import com.lms.repository.EnrollmentRepository;
import com.lms.repository.ExamRepository;
import com.lms.repository.ResultRepository;
import com.lms.repository.StudyMaterialRepository;
import com.lms.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Admin course management: create, edit, activate/deactivate and delete.
 * Courses that already hold academic records are deactivated instead of
 * deleted so results, attempts and materials are never lost.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class AdminCourseService {

    private static final String FACULTY = "FACULTY";

    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final ResultRepository resultRepository;
    private final ExamRepository examRepository;
    private final StudyMaterialRepository materialRepository;
    private final CodingChallengeRepository challengeRepository;
    private final AdminSettingsService settingsService;

    // ==========================================
    // LIST
    // ==========================================

    @Transactional(readOnly = true)
    public AdminPageDTO<AdminCourseRowDTO> list(
            String search,
            Boolean active,
            Long facultyId,
            int page,
            int size) {

        int safePage = Math.max(0, page);
        int safeSize = Math.min(100, Math.max(1, size));
        String term = normalizeSearch(search);

        // Numeric search matches the course id exactly.
        if (term != null && term.matches("\\d+")) {
            Optional<Course> byId = courseRepository.findById(Long.valueOf(term));

            if (byId.isPresent()
                    && (active == null || active.equals(byId.get().getActive()))
                    && (facultyId == null
                    || (byId.get().getFaculty() != null
                    && facultyId.equals(byId.get().getFaculty().getId())))) {

                return new AdminPageDTO<>(
                        List.of(toRow(byId.get())), 0, 1, 1, 1, true, true);
            }

            return new AdminPageDTO<>(
                    List.of(), 0, 1, 0, 0, true, true);
        }

        Pageable pageable = PageRequest.of(
                safePage,
                safeSize,
                Sort.by("title").ascending()
        );

        Page<Course> result = courseRepository.searchCourses(
                active,
                facultyId,
                term == null ? null : like(term),
                pageable
        );

        List<AdminCourseRowDTO> rows = result.getContent()
                .stream()
                .map(this::toRow)
                .toList();

        return AdminPageDTO.from(result, rows);
    }

    // ==========================================
    // DETAIL
    // ==========================================

    @Transactional(readOnly = true)
    public AdminCourseRowDTO detail(Long courseId) {
        return toRow(findCourse(courseId));
    }

    // ==========================================
    // CREATE
    // ==========================================

    public AdminCourseRowDTO create(AdminCourseRequest request) {

        Course course = new Course();
        applyRequest(course, request, null);

        return toRow(courseRepository.save(course));
    }

    // ==========================================
    // UPDATE
    // ==========================================

    public AdminCourseRowDTO update(Long courseId, AdminCourseRequest request) {

        Course course = findCourse(courseId);
        applyRequest(course, request, course);

        return toRow(courseRepository.save(course));
    }

    // ==========================================
    // ACTIVATE / DEACTIVATE
    // ==========================================

    public AdminCourseRowDTO setActive(Long courseId, boolean active) {

        Course course = findCourse(courseId);

        course.setActive(active);

        return toRow(courseRepository.save(course));
    }

    // ==========================================
    // DELETE
    // ==========================================

    public AdminDeleteResultDTO delete(Long courseId) {

        Course course = findCourse(courseId);

        long enrollments = enrollmentRepository.countByCourse_Id(courseId);
        long exams = examRepository.countByCourse_Id(courseId);
        long materials = materialRepository.countByCourse_Id(courseId);
        long challenges = challengeRepository.countByCourse_Id(courseId);
        long results = resultRepository.countByExam_Course_Id(courseId);

        boolean hasHistory = enrollments > 0
                || exams > 0
                || materials > 0
                || challenges > 0
                || results > 0;

        if (hasHistory) {

            course.setActive(false);
            courseRepository.save(course);

            return new AdminDeleteResultDTO(
                    courseId,
                    "DEACTIVATED",
                    "Course deactivated. Existing enrollments, exams and results were preserved.");
        }

        courseRepository.delete(course);

        return new AdminDeleteResultDTO(
                courseId,
                "DELETED",
                "Course removed permanently.");
    }

    // ==========================================
    // OPTIONS
    // ==========================================

    @Transactional(readOnly = true)
    public List<Map<String, Object>> courseOptions() {

        return courseRepository.findAll(
                        Sort.by("title").ascending())
                .stream()
                .map(course -> {
                    Map<String, Object> option = new LinkedHashMap<>();
                    option.put("id", course.getId());
                    option.put("title", course.getTitle());
                    option.put("courseCode", course.getCourseCode());
                    option.put("active", course.getActive());
                    return option;
                })
                .toList();
    }

    // ==========================================
    // HELPERS
    // ==========================================

    private void applyRequest(
            Course course,
            AdminCourseRequest request,
            Course existing) {

        if (request == null) {
            throw new BadRequestException("Course details are required");
        }

        String title = trim(request.getTitle());

        if (title == null || title.isEmpty()) {
            throw new BadRequestException("Course title is required");
        }

        String courseCode = trim(request.getCourseCode());

        if (courseCode == null || courseCode.isEmpty()) {
            throw new BadRequestException("Course code is required");
        }

        courseCode = courseCode.toUpperCase();

        Optional<Course> duplicate =
                courseRepository.findByCourseCode(courseCode);

        if (duplicate.isPresent()
                && (existing == null
                || !duplicate.get().getId().equals(existing.getId()))) {
            throw new BadRequestException("Course code already exists");
        }

        Integer duration = request.getDurationDays() == null
                ? settingsService.defaultCourseDurationDays()
                : request.getDurationDays();

        if (duration < 1) {
            throw new BadRequestException(
                    "Course duration must be at least 1 day");
        }

        Double fees = request.getTotalFees() == null
                ? settingsService.defaultCourseFees()
                : request.getTotalFees();

        if (fees < 0) {
            throw new BadRequestException("Course fees cannot be negative");
        }

        User faculty = userRepository.findById(
                        request.getFacultyId() == null ? -1L : request.getFacultyId())
                .orElseThrow(() -> new BadRequestException(
                        "Select a faculty member for this course"));

        if (faculty.getRole() == null
                || faculty.getRole().getName() == null
                || !faculty.getRole().getName().equalsIgnoreCase(FACULTY)) {
            throw new BadRequestException(
                    "Selected user is not a faculty member");
        }

        if (Boolean.FALSE.equals(faculty.getActive())) {
            throw new BadRequestException(
                    "The selected faculty account is inactive");
        }

        course.setTitle(title);
        course.setDescription(trim(request.getDescription()));
        course.setCourseCode(courseCode);
        course.setDurationDays(duration);
        course.setTotalFees(fees);
        course.setFaculty(faculty);
        course.setActive(request.getActive() == null
                ? Boolean.TRUE
                : request.getActive());
    }

    private AdminCourseRowDTO toRow(Course course) {

        AdminCourseRowDTO row = new AdminCourseRowDTO();

        row.setId(course.getId());
        row.setTitle(course.getTitle());
        row.setDescription(course.getDescription());
        row.setCourseCode(course.getCourseCode());
        row.setDurationDays(course.getDurationDays());
        row.setTotalFees(course.getTotalFees());
        row.setActive(course.getActive());
        row.setCreatedAt(course.getCreatedAt());

        if (course.getFaculty() != null) {
            row.setFacultyId(course.getFaculty().getId());
            row.setFacultyName(joinName(
                    course.getFaculty().getFirstName(),
                    course.getFaculty().getLastName()));
            row.setFacultyEmail(course.getFaculty().getEmail());
        }

        long total = enrollmentRepository.countByCourse_Id(course.getId());
        long active = enrollmentRepository
                .countByCourse_IdAndStatusIgnoreCase(course.getId(), "ACTIVE");
        long completed = enrollmentRepository
                .countByCourse_IdAndStatusIgnoreCase(course.getId(), "COMPLETED");

        row.setTotalStudents(total);
        row.setActiveStudents(active);
        row.setCompletedStudents(completed);
        row.setInactiveStudents(Math.max(0, total - active - completed));

        row.setTotalExams(examRepository.countByCourse_Id(course.getId()));
        row.setTotalMaterials(
                materialRepository.countByCourse_Id(course.getId()));
        row.setTotalChallenges(
                challengeRepository.countByCourse_Id(course.getId()));

        long results = resultRepository.countByExam_Course_Id(course.getId());
        long passed = resultRepository
                .countByExam_Course_IdAndStatusIgnoreCase(course.getId(), "PASS");

        row.setTotalResults(results);
        row.setPassedResults(passed);
        row.setPassPercentage(
                results == 0 ? 0.0 : round(passed * 100.0 / results));

        Double average =
                resultRepository.averagePercentageByCourse(course.getId());

        row.setAveragePercentage(average == null ? 0.0 : round(average));

        return row;
    }

    private Course findCourse(Long courseId) {

        if (courseId == null) {
            throw new ResourceNotFoundException("Course not found");
        }

        return courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Course not found"));
    }

    private String normalizeSearch(String search) {

        if (search == null) {
            return null;
        }

        String term = search.trim();

        return term.isEmpty() ? null : term;
    }

    private String like(String term) {
        return "%" + term.toLowerCase() + "%";
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private String joinName(String firstName, String lastName) {
        return ((firstName == null ? "" : firstName) + " "
                + (lastName == null ? "" : lastName)).trim();
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
