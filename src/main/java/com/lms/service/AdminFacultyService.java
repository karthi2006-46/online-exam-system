package com.lms.service;

import com.lms.dto.FacultyDashboardDTO;
import com.lms.dto.UserDTO;
import com.lms.dto.admin.AdminCourseSummaryDTO;
import com.lms.dto.admin.AdminDeleteResultDTO;
import com.lms.dto.admin.AdminFacultyAccountDTO;
import com.lms.dto.admin.AdminFacultyDetailDTO;
import com.lms.dto.admin.AdminFacultyRowDTO;
import com.lms.dto.admin.AdminPageDTO;
import com.lms.dto.admin.AdminPasswordResetDTO;
import com.lms.dto.admin.AssignCoursesRequest;
import com.lms.dto.admin.CreateFacultyRequest;
import com.lms.dto.admin.ResetPasswordRequest;
import com.lms.dto.admin.UpdateFacultyRequest;
import com.lms.entity.CodingSubmissionStatus;
import com.lms.entity.Course;
import com.lms.entity.ExamApprovalRequest;
import com.lms.entity.Result;
import com.lms.entity.Role;
import com.lms.entity.User;
import com.lms.exception.BadRequestException;
import com.lms.exception.ResourceNotFoundException;
import com.lms.repository.CodingChallengeRepository;
import com.lms.repository.CodingSubmissionRepository;
import com.lms.repository.CourseRepository;
import com.lms.repository.EnrollmentRepository;
import com.lms.repository.ExamApprovalRequestRepository;
import com.lms.repository.ExamRepository;
import com.lms.repository.ExtensionRequestRepository;
import com.lms.repository.ResultRepository;
import com.lms.repository.RoleRepository;
import com.lms.repository.StudyMaterialRepository;
import com.lms.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminFacultyService {

    private static final String FACULTY = "FACULTY";
    private static final String PASSWORD_CHARS =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final ExamApprovalRequestRepository approvalRepository;
    private final ExtensionRequestRepository extensionRepository;
    private final CodingSubmissionRepository codingSubmissionRepository;
    private final ResultRepository resultRepository;
    private final ExamRepository examRepository;
    private final StudyMaterialRepository materialRepository;
    private final CodingChallengeRepository challengeRepository;
    private final FacultyDashboardService facultyDashboardService;
    private final PasswordEncoder passwordEncoder;

    private final SecureRandom secureRandom = new SecureRandom();

    // ==========================================
    // LIST
    // ==========================================

    @Transactional(readOnly = true)
    public AdminPageDTO<AdminFacultyRowDTO> list(
            String search,
            Boolean active,
            int page,
            int size) {

        int safePage = Math.max(0, page);
        int safeSize = Math.min(100, Math.max(1, size));
        String term = normalizeSearch(search);

        // Numeric search matches the faculty id exactly.
        if (term != null && term.matches("\\d+")) {
            Optional<User> byId = userRepository.findById(Long.valueOf(term));

            if (byId.isPresent()
                    && isFaculty(byId.get())
                    && (active == null || active.equals(byId.get().getActive()))) {

                AdminFacultyRowDTO row = toRow(byId.get());
                return new AdminPageDTO<>(
                        List.of(row), 0, 1, 1, 1, true, true);
            }

            return new AdminPageDTO<>(
                    List.of(), 0, 1, 0, 0, true, true);
        }

        Pageable pageable = PageRequest.of(
                safePage,
                safeSize,
                Sort.by("firstName").ascending()
                        .and(Sort.by("lastName").ascending())
        );

        Page<User> result;

        if (term == null) {
            result = active == null
                    ? userRepository.findByRole_Name(FACULTY, pageable)
                    : userRepository.findByRole_NameAndActive(
                            FACULTY, active, pageable);
        } else {
            result = userRepository.searchByRole(
                    FACULTY, active, like(term), pageable);
        }

        List<AdminFacultyRowDTO> rows = result.getContent()
                .stream()
                .map(this::toRow)
                .toList();

        return AdminPageDTO.from(result, rows);
    }

    // ==========================================
    // DETAIL
    // ==========================================

    @Transactional(readOnly = true)
    public AdminFacultyDetailDTO detail(Long facultyId) {

        User faculty = findFaculty(facultyId);

        AdminFacultyDetailDTO detail = new AdminFacultyDetailDTO();

        detail.setProfile(toUserDTO(faculty));
        detail.setDashboard(
                facultyDashboardService.buildDashboard(facultyId));

        List<Course> courses = courseRepository.findByFaculty_Id(facultyId);

        detail.setCourses(
                courses.stream()
                        .map(this::toCourseSummary)
                        .toList()
        );

        detail.setPendingCodingSubmissions(
                codingSubmissionRepository
                        .countByChallenge_Course_Faculty_IdAndStatus(
                                facultyId,
                                CodingSubmissionStatus.PENDING_REVIEW));

        detail.setApprovedCodingSubmissions(
                codingSubmissionRepository
                        .countByChallenge_Course_Faculty_IdAndStatus(
                                facultyId,
                                CodingSubmissionStatus.APPROVED));

        detail.setRejectedCodingSubmissions(
                codingSubmissionRepository
                        .countByChallenge_Course_Faculty_IdAndStatus(
                                facultyId,
                                CodingSubmissionStatus.REJECTED));

        detail.setPendingExamApprovals(
                approvalRepository.countByCourse_Faculty_IdAndStatus(
                        facultyId,
                        ExamApprovalRequest.Status.PENDING));

        detail.setApprovedExamRequests(
                approvalRepository.countByCourse_Faculty_IdAndStatus(
                        facultyId,
                        ExamApprovalRequest.Status.APPROVED));

        detail.setRejectedExamRequests(
                approvalRepository.countByCourse_Faculty_IdAndStatus(
                        facultyId,
                        ExamApprovalRequest.Status.REJECTED));

        detail.setPendingExtensionRequests(
                extensionRepository
                        .countByEnrollment_Course_Faculty_IdAndStatusIgnoreCase(
                                facultyId, "PENDING"));

        detail.setApprovedExtensionRequests(
                extensionRepository
                        .countByEnrollment_Course_Faculty_IdAndStatusIgnoreCase(
                                facultyId, "APPROVED"));

        detail.setRejectedExtensionRequests(
                extensionRepository
                        .countByEnrollment_Course_Faculty_IdAndStatusIgnoreCase(
                                facultyId, "REJECTED"));

        return detail;
    }

    // ==========================================
    // CREATE
    // ==========================================

    public AdminFacultyAccountDTO create(CreateFacultyRequest request) {

        String email = normalizeEmail(request.getEmail());

        if (userRepository.findByEmail(email).isPresent()) {
            throw new BadRequestException("Email already registered");
        }

        Role role = roleRepository.findByName(FACULTY)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Faculty role is not configured"));

        String rawPassword = request.getPassword();
        boolean generated = false;

        if (rawPassword == null || rawPassword.isBlank()) {
            if (!request.isGeneratePassword()) {
                throw new BadRequestException("Password is required");
            }
            rawPassword = generatePassword();
            generated = true;
        }

        User faculty = new User();
        faculty.setFirstName(request.getFirstName().trim());
        faculty.setLastName(request.getLastName().trim());
        faculty.setEmail(email);
        faculty.setPassword(passwordEncoder.encode(rawPassword));
        faculty.setPhone(request.getPhone() == null ? "" : request.getPhone().trim());
        faculty.setAddress(request.getAddress());
        faculty.setActive(true);
        faculty.setRole(role);

        User saved = userRepository.save(faculty);

        assignCourses(saved, request.getCourseIds());

        AdminFacultyAccountDTO account = new AdminFacultyAccountDTO();
        account.setFaculty(toUserDTO(saved));
        account.setPasswordGenerated(generated);

        if (generated) {
            account.setTemporaryPassword(rawPassword);
        }

        return account;
    }

    // ==========================================
    // UPDATE
    // ==========================================

    public AdminFacultyRowDTO update(
            Long facultyId,
            UpdateFacultyRequest request) {

        User faculty = findFaculty(facultyId);

        String email = normalizeEmail(request.getEmail());

        userRepository.findByEmail(email)
                .filter(existing -> !existing.getId().equals(facultyId))
                .ifPresent(existing -> {
                    throw new BadRequestException("Email already registered");
                });

        // Deactivation is blocked while courses are still assigned.
        if (Boolean.FALSE.equals(request.getActive())
                && !courseRepository.findByFaculty_Id(facultyId).isEmpty()) {

            throw new BadRequestException(
                    "Reassign this faculty's courses before deactivating the account");
        }

        faculty.setFirstName(request.getFirstName().trim());
        faculty.setLastName(request.getLastName().trim());
        faculty.setEmail(email);
        faculty.setPhone(request.getPhone() == null ? "" : request.getPhone().trim());
        faculty.setAddress(request.getAddress());

        if (request.getActive() != null) {
            faculty.setActive(request.getActive());
        }

        User saved = userRepository.save(faculty);

        assignCourses(saved, request.getCourseIds());

        return toRow(saved);
    }

    // ==========================================
    // ACTIVATE / DEACTIVATE
    // ==========================================

    public AdminFacultyRowDTO setActive(Long facultyId, boolean active) {

        User faculty = findFaculty(facultyId);

        if (!active) {
            List<Course> courses = courseRepository.findByFaculty_Id(facultyId);

            if (!courses.isEmpty()) {
                throw new BadRequestException(
                        "Reassign " + courses.size()
                                + " course(s) before deactivating this faculty account");
            }
        }

        faculty.setActive(active);
        return toRow(userRepository.save(faculty));
    }

    // ==========================================
    // COURSE ASSIGNMENT
    // ==========================================

    /**
     * Adds (or transfers) the listed courses to this faculty member.
     * Courses already assigned to this faculty stay assigned; no course is
     * ever left without a faculty because courses.faculty_id is NOT NULL.
     */
    public AdminFacultyDetailDTO assignCourses(
            Long facultyId,
            AssignCoursesRequest request) {

        User faculty = findFaculty(facultyId);
        assignCourses(faculty, request.getCourseIds());
        return detail(facultyId);
    }

    private void assignCourses(User faculty, List<Long> courseIds) {

        if (courseIds == null || courseIds.isEmpty()) {
            return;
        }

        List<Course> courses = new ArrayList<>();

        for (Long courseId : courseIds.stream().distinct().toList()) {

            if (courseId == null) {
                continue;
            }

            Course course = courseRepository.findById(courseId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Course not found: " + courseId));

            course.setFaculty(faculty);
            courses.add(course);
        }

        courseRepository.saveAll(courses);
    }

    // ==========================================
    // PASSWORD RESET
    // ==========================================

    public AdminPasswordResetDTO resetPassword(
            Long facultyId,
            ResetPasswordRequest request) {

        User faculty = findFaculty(facultyId);

        String rawPassword = request.getNewPassword();
        boolean generated = false;

        if (rawPassword == null || rawPassword.isBlank()) {
            if (!request.isGeneratePassword()) {
                throw new BadRequestException("Password is required");
            }
            rawPassword = generatePassword();
            generated = true;
        }

        faculty.setPassword(passwordEncoder.encode(rawPassword));
        userRepository.save(faculty);

        AdminPasswordResetDTO result = new AdminPasswordResetDTO();
        result.setUserId(facultyId);
        result.setPasswordGenerated(generated);

        if (generated) {
            result.setTemporaryPassword(rawPassword);
        }

        return result;
    }

    // ==========================================
    // DELETE
    // ==========================================

    public AdminDeleteResultDTO delete(Long facultyId) {

        User faculty = findFaculty(facultyId);

        List<Course> courses = courseRepository.findByFaculty_Id(facultyId);

        if (!courses.isEmpty()) {
            throw new BadRequestException(
                    "Reassign " + courses.size()
                            + " course(s) before deleting this faculty account");
        }

        long reviewedRecords =
                codingSubmissionRepository.countByReviewedBy_Id(facultyId)
                        + extensionRepository.countByReviewedBy_Id(facultyId);

        if (reviewedRecords > 0) {
            faculty.setActive(false);
            userRepository.save(faculty);

            return new AdminDeleteResultDTO(
                    facultyId,
                    "DEACTIVATED",
                    "Faculty account deactivated. Review history is preserved.");
        }

        userRepository.delete(faculty);

        return new AdminDeleteResultDTO(
                facultyId,
                "DELETED",
                "Faculty account deleted permanently.");
    }

    // ==========================================
    // MAPPERS
    // ==========================================

    private AdminFacultyRowDTO toRow(User faculty) {

        AdminFacultyRowDTO row = new AdminFacultyRowDTO();

        row.setFacultyId(faculty.getId());
        row.setFirstName(faculty.getFirstName());
        row.setLastName(faculty.getLastName());
        row.setName(joinName(faculty.getFirstName(), faculty.getLastName()));
        row.setEmail(faculty.getEmail());
        row.setPhone(faculty.getPhone());
        row.setActive(faculty.getActive());
        row.setCreatedAt(faculty.getCreatedAt());

        List<Course> courses = courseRepository.findByFaculty_Id(faculty.getId());

        row.setCourseIds(courses.stream().map(Course::getId).toList());
        row.setAssignedCourses(courses.stream().map(Course::getTitle).toList());

        long total = enrollmentRepository.countByCourse_Faculty_Id(faculty.getId());
        long active = enrollmentRepository
                .countByCourse_Faculty_IdAndStatusIgnoreCase(faculty.getId(), "ACTIVE");
        long completed = enrollmentRepository
                .countByCourse_Faculty_IdAndStatusIgnoreCase(faculty.getId(), "COMPLETED");

        row.setTotalStudents(total);
        row.setActiveStudents(active);
        row.setCompletedStudents(completed);
        row.setInactiveStudents(Math.max(0, total - active - completed));

        row.setPendingExamApprovals(
                approvalRepository.countByCourse_Faculty_IdAndStatus(
                        faculty.getId(),
                        ExamApprovalRequest.Status.PENDING));

        row.setPendingExtensionRequests(
                extensionRepository
                        .countByEnrollment_Course_Faculty_IdAndStatusIgnoreCase(
                                faculty.getId(), "PENDING"));

        long totalResults =
                resultRepository.countByExam_Course_Faculty_Id(faculty.getId());
        long passedResults =
                resultRepository.countByExam_Course_Faculty_IdAndStatusIgnoreCase(
                        faculty.getId(), "PASS");

        row.setTotalResults(totalResults);
        row.setPassPercentage(
                totalResults == 0
                        ? 0.0
                        : round(passedResults * 100.0 / totalResults));

        return row;
    }

    private AdminCourseSummaryDTO toCourseSummary(Course course) {

        AdminCourseSummaryDTO summary = new AdminCourseSummaryDTO();

        summary.setId(course.getId());
        summary.setTitle(course.getTitle());
        summary.setCourseCode(course.getCourseCode());
        summary.setActive(course.getActive());

        if (course.getFaculty() != null) {
            summary.setFacultyId(course.getFaculty().getId());
            summary.setFacultyName(joinName(
                    course.getFaculty().getFirstName(),
                    course.getFaculty().getLastName()));
        }

        long total = enrollmentRepository.countByCourse_Id(course.getId());
        long active = enrollmentRepository
                .countByCourse_IdAndStatusIgnoreCase(course.getId(), "ACTIVE");
        long completed = enrollmentRepository
                .countByCourse_IdAndStatusIgnoreCase(course.getId(), "COMPLETED");

        summary.setTotalStudents(total);
        summary.setActiveStudents(active);
        summary.setCompletedStudents(completed);
        summary.setInactiveStudents(Math.max(0, total - active - completed));

        summary.setTotalExams(examRepository.countByCourse_Id(course.getId()));
        summary.setTotalMaterials(
                materialRepository.countByCourse_Id(course.getId()));
        summary.setTotalChallenges(
                challengeRepository.countByCourse_Id(course.getId()));

        List<Result> results =
                resultRepository.findByExam_Course_Id(course.getId());

        double average = results.stream()
                .filter(result -> result.getPercentage() != null)
                .mapToDouble(Result::getPercentage)
                .average()
                .orElse(0.0);

        summary.setAveragePercentage(round(average));

        return summary;
    }

    private UserDTO toUserDTO(User user) {

        return new UserDTO(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getPhone(),
                user.getAddress(),
                user.getRole() == null ? null : user.getRole().getName(),
                user.getActive(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    // ==========================================
    // HELPERS
    // ==========================================

    private User findFaculty(Long facultyId) {

        User user = userRepository.findById(facultyId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Faculty not found"));

        if (!isFaculty(user)) {
            throw new ResourceNotFoundException("Faculty not found");
        }

        return user;
    }

    private boolean isFaculty(User user) {
        return user.getRole() != null
                && user.getRole().getName() != null
                && user.getRole().getName().equalsIgnoreCase(FACULTY);
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
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

    private String generatePassword() {

        StringBuilder password = new StringBuilder(12);

        for (int i = 0; i < 12; i++) {
            password.append(
                    PASSWORD_CHARS.charAt(
                            secureRandom.nextInt(PASSWORD_CHARS.length())));
        }

        return password.toString();
    }

    private String joinName(String firstName, String lastName) {
        return ((firstName == null ? "" : firstName) + " "
                + (lastName == null ? "" : lastName)).trim();
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
