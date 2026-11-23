package com.lms.service;

import com.lms.dto.StudentProgressReportDTO;
import com.lms.dto.UserDTO;
import com.lms.dto.admin.AdminDeleteResultDTO;
import com.lms.dto.admin.AdminPageDTO;
import com.lms.dto.admin.AdminStudentDetailDTO;
import com.lms.dto.admin.AdminStudentRowDTO;
import com.lms.entity.Enrollment;
import com.lms.entity.User;
import com.lms.exception.ResourceNotFoundException;
import com.lms.repository.CodingChallengeProgressRepository;
import com.lms.repository.CodingSubmissionRepository;
import com.lms.repository.EnrollmentRepository;
import com.lms.repository.ExamApprovalRequestRepository;
import com.lms.repository.ExamAttemptRepository;
import com.lms.repository.ExtensionRequestRepository;
import com.lms.repository.MaterialProgressRepository;
import com.lms.repository.ResultRepository;
import com.lms.repository.StudyMaterialProgressRepository;
import com.lms.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Admin student management.
 * Accounts are deactivated rather than deleted whenever any academic
 * record exists, so results, attempts and progress are never destroyed.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class AdminStudentService {

    private static final String STUDENT = "STUDENT";

    private final UserRepository userRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final ResultRepository resultRepository;
    private final ExamAttemptRepository examAttemptRepository;
    private final ExamApprovalRequestRepository approvalRepository;
    private final ExtensionRequestRepository extensionRepository;
    private final CodingSubmissionRepository codingSubmissionRepository;
    private final CodingChallengeProgressRepository challengeProgressRepository;
    private final MaterialProgressRepository materialProgressRepository;
    private final StudyMaterialProgressRepository studyMaterialProgressRepository;
    private final StudentProgressReportService progressReportService;

    // ==========================================
    // LIST
    // ==========================================

    @Transactional(readOnly = true)
    public AdminPageDTO<AdminStudentRowDTO> list(
            String search,
            Boolean active,
            Long courseId,
            int page,
            int size) {

        int safePage = Math.max(0, page);
        int safeSize = Math.min(100, Math.max(1, size));
        String term = normalizeSearch(search);

        // Numeric search matches the student id exactly.
        if (term != null && term.matches("\\d+")) {
            Optional<User> byId = userRepository.findById(Long.valueOf(term));

            if (byId.isPresent()
                    && isStudent(byId.get())
                    && (active == null || active.equals(byId.get().getActive()))) {

                AdminStudentRowDTO row = toRow(byId.get());
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

        Page<User> result = userRepository.searchByRoleAndCourse(
                STUDENT,
                active,
                courseId,
                term == null ? null : like(term),
                pageable
        );

        List<AdminStudentRowDTO> rows = result.getContent()
                .stream()
                .map(this::toRow)
                .toList();

        return AdminPageDTO.from(result, rows);
    }

    // ==========================================
    // DETAIL
    // ==========================================

    @Transactional(readOnly = true)
    public AdminStudentDetailDTO detail(Long studentId) {

        User student = findStudent(studentId);

        AdminStudentDetailDTO detail = new AdminStudentDetailDTO();

        detail.setProfile(toUserDTO(student));

        List<Enrollment> enrollments =
                enrollmentRepository.findByStudent_Id(studentId);

        long completed = enrollments.stream()
                .filter(enrollment -> "COMPLETED".equalsIgnoreCase(
                        enrollment.getStatus()))
                .count();

        detail.setEnrolledCourses(enrollments.size());
        detail.setCompletedCourses(completed);

        long total = resultRepository.countByStudent_Id(studentId);
        long passed = resultRepository
                .countByStudent_IdAndStatusIgnoreCase(studentId, "PASS");
        long failed = resultRepository
                .countByStudent_IdAndStatusIgnoreCase(studentId, "FAIL");

        detail.setTotalResults(total);
        detail.setPassedResults(passed);
        detail.setFailedResults(failed);

        List<StudentProgressReportDTO> reports = new ArrayList<>();

        for (Enrollment enrollment : enrollments) {
            try {
                reports.add(
                        progressReportService.buildStudentCourseReport(
                                studentId,
                                enrollment.getCourse().getId()
                        )
                );
            } catch (RuntimeException exception) {
                // A single broken enrollment must not hide the rest of
                // the student's academic history.
                continue;
            }
        }

        detail.setCourseReports(reports);

        double average = reports.stream()
                .map(StudentProgressReportDTO::getAveragePercentage)
                .filter(value -> value != null)
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);

        detail.setAveragePercentage(round(average));

        return detail;
    }

    // ==========================================
    // ACTIVATE / DEACTIVATE
    // ==========================================

    public AdminStudentRowDTO setActive(Long studentId, boolean active) {

        User student = findStudent(studentId);

        student.setActive(active);

        return toRow(userRepository.save(student));
    }

    // ==========================================
    // DELETE
    // ==========================================

    public AdminDeleteResultDTO delete(Long studentId) {

        User student = findStudent(studentId);

        if (hasAcademicHistory(studentId)) {

            student.setActive(false);
            userRepository.save(student);

            return new AdminDeleteResultDTO(
                    studentId,
                    "DEACTIVATED",
                    "Student account deactivated. Academic records are preserved.");
        }

        userRepository.delete(student);

        return new AdminDeleteResultDTO(
                studentId,
                "DELETED",
                "Student account deleted permanently.");
    }

    private boolean hasAcademicHistory(Long studentId) {

        long records =
                enrollmentRepository.countByStudent_Id(studentId)
                        + resultRepository.countByStudent_Id(studentId)
                        + examAttemptRepository.countByStudent_Id(studentId)
                        + approvalRepository.countByStudent_Id(studentId)
                        + extensionRepository.countByEnrollment_Student_Id(studentId)
                        + codingSubmissionRepository.countByStudent_Id(studentId)
                        + challengeProgressRepository.countByStudent_Id(studentId)
                        + materialProgressRepository.countByStudent_Id(studentId)
                        + studyMaterialProgressRepository.countByStudent_Id(studentId);

        return records > 0;
    }

    // ==========================================
    // MAPPERS
    // ==========================================

    private AdminStudentRowDTO toRow(User student) {

        AdminStudentRowDTO row = new AdminStudentRowDTO();

        row.setStudentId(student.getId());
        row.setFirstName(student.getFirstName());
        row.setLastName(student.getLastName());
        row.setName(joinName(student.getFirstName(), student.getLastName()));
        row.setEmail(student.getEmail());
        row.setPhone(student.getPhone());
        row.setActive(student.getActive());
        row.setCreatedAt(student.getCreatedAt());

        row.setCourseCount(
                enrollmentRepository.countByStudent_Id(student.getId()));
        row.setActiveCourses(
                enrollmentRepository.countByStudent_IdAndStatusIgnoreCase(
                        student.getId(), "ACTIVE"));
        row.setCompletedCourses(
                enrollmentRepository.countByStudent_IdAndStatusIgnoreCase(
                        student.getId(), "COMPLETED"));

        row.setTotalResults(
                resultRepository.countByStudent_Id(student.getId()));
        row.setPassedResults(
                resultRepository.countByStudent_IdAndStatusIgnoreCase(
                        student.getId(), "PASS"));
        row.setFailedResults(
                resultRepository.countByStudent_IdAndStatusIgnoreCase(
                        student.getId(), "FAIL"));

        return row;
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

    private User findStudent(Long studentId) {

        User user = userRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Student not found"));

        if (!isStudent(user)) {
            throw new ResourceNotFoundException("Student not found");
        }

        return user;
    }

    private boolean isStudent(User user) {
        return user.getRole() != null
                && user.getRole().getName() != null
                && user.getRole().getName().equalsIgnoreCase(STUDENT);
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

    private String joinName(String firstName, String lastName) {
        return ((firstName == null ? "" : firstName) + " "
                + (lastName == null ? "" : lastName)).trim();
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
