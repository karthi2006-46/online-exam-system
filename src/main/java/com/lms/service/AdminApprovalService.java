package com.lms.service;

import com.lms.dto.admin.AdminApprovalRowDTO;
import com.lms.dto.admin.AdminApprovalStatsDTO;
import com.lms.dto.admin.AdminPageDTO;
import com.lms.entity.ExamApprovalRequest;
import com.lms.entity.User;
import com.lms.exception.BadRequestException;
import com.lms.exception.ResourceNotFoundException;
import com.lms.repository.CourseRepository;
import com.lms.repository.ExamApprovalRequestRepository;
import com.lms.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Admin view of every exam approval request, across all faculties.
 * Admin decisions are final and do not need a course assignment.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class AdminApprovalService {

    private static final String FACULTY = "FACULTY";

    private final ExamApprovalRequestRepository approvalRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;

    // ==========================================
    // LIST
    // ==========================================

    @Transactional(readOnly = true)
    public AdminPageDTO<AdminApprovalRowDTO> list(
            String status,
            Long courseId,
            Long facultyId,
            String search,
            int page,
            int size) {

        int safePage = Math.max(0, page);
        int safeSize = Math.min(100, Math.max(1, size));

        Pageable pageable = PageRequest.of(safePage, safeSize);

        Page<ExamApprovalRequest> result =
                approvalRepository.searchRequests(
                        normalizeStatus(status),
                        courseId,
                        facultyId,
                        normalizeSearch(search) == null
                                ? null
                                : like(normalizeSearch(search)),
                        pageable
                );

        List<AdminApprovalRowDTO> rows = result.getContent()
                .stream()
                .map(this::toRow)
                .toList();

        return AdminPageDTO.from(result, rows);
    }

    // ==========================================
    // STATS
    // ==========================================

    @Transactional(readOnly = true)
    public AdminApprovalStatsDTO stats() {

        AdminApprovalStatsDTO stats = new AdminApprovalStatsDTO();

        stats.setPending(approvalRepository.countByStatus(
                ExamApprovalRequest.Status.PENDING));
        stats.setApproved(approvalRepository.countByStatus(
                ExamApprovalRequest.Status.APPROVED));
        stats.setRejected(approvalRepository.countByStatus(
                ExamApprovalRequest.Status.REJECTED));
        stats.setTotal(approvalRepository.count());

        return stats;
    }

    // ==========================================
    // APPROVE / REJECT
    // ==========================================

    public AdminApprovalRowDTO approve(Long requestId, String comment) {

        ExamApprovalRequest request = findPending(requestId);

        if (request.getStudent() == null
                || Boolean.FALSE.equals(request.getStudent().getActive())) {
            throw new BadRequestException(
                    "This student account is inactive. Reactivate it before approving.");
        }

        request.setStatus(ExamApprovalRequest.Status.APPROVED);
        request.setFacultyComment(trim(comment));
        request.setReviewedAt(LocalDateTime.now());

        return toRow(approvalRepository.save(request));
    }

    public AdminApprovalRowDTO reject(Long requestId, String comment) {

        ExamApprovalRequest request = findPending(requestId);

        request.setStatus(ExamApprovalRequest.Status.REJECTED);
        request.setFacultyComment(trim(comment));
        request.setReviewedAt(LocalDateTime.now());

        return toRow(approvalRepository.save(request));
    }

    // ==========================================
    // OPTIONS
    // ==========================================

    @Transactional(readOnly = true)
    public List<Map<String, Object>> courseOptions() {

        return courseRepository.findAll()
                .stream()
                .sorted((a, b) -> nullSafe(a.getTitle())
                        .compareToIgnoreCase(nullSafe(b.getTitle())))
                .map(course -> {
                    Map<String, Object> option = new LinkedHashMap<>();
                    option.put("id", course.getId());
                    option.put("title", course.getTitle());
                    option.put("courseCode", course.getCourseCode());
                    return option;
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> facultyOptions() {

        return userRepository.findByRole_Name(FACULTY)
                .stream()
                .sorted((a, b) -> joinName(a.getFirstName(), a.getLastName())
                        .compareToIgnoreCase(
                                joinName(b.getFirstName(), b.getLastName())))
                .map(faculty -> {
                    Map<String, Object> option = new LinkedHashMap<>();
                    option.put("id", faculty.getId());
                    option.put("name", joinName(
                            faculty.getFirstName(), faculty.getLastName()));
                    option.put("email", faculty.getEmail());
                    return option;
                })
                .toList();
    }

    // ==========================================
    // HELPERS
    // ==========================================

    private ExamApprovalRequest findPending(Long requestId) {

        ExamApprovalRequest request = approvalRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Approval request not found"));

        if (request.getStatus() != ExamApprovalRequest.Status.PENDING) {
            throw new BadRequestException(
                    "Only pending requests can be reviewed");
        }

        return request;
    }

    private AdminApprovalRowDTO toRow(ExamApprovalRequest request) {

        AdminApprovalRowDTO row = new AdminApprovalRowDTO();

        row.setId(request.getId());
        row.setStatus(request.getStatus() == null
                ? null
                : request.getStatus().name());
        row.setRequestedAt(request.getRequestedAt());
        row.setReviewedAt(request.getReviewedAt());
        row.setComment(request.getFacultyComment());

        if (request.getStudent() != null) {
            User student = request.getStudent();
            row.setStudentId(student.getId());
            row.setStudentName(joinName(
                    student.getFirstName(), student.getLastName()));
            row.setEmail(student.getEmail());
            row.setStudentActive(student.getActive());
        }

        if (request.getCourse() != null) {
            row.setCourseId(request.getCourse().getId());
            row.setCourseTitle(request.getCourse().getTitle());
            row.setCourseCode(request.getCourse().getCourseCode());

            if (request.getCourse().getFaculty() != null) {
                User faculty = request.getCourse().getFaculty();
                row.setFacultyId(faculty.getId());
                row.setFacultyName(joinName(
                        faculty.getFirstName(), faculty.getLastName()));
            }
        }

        return row;
    }

    private String normalizeStatus(String status) {

        if (status == null) {
            return null;
        }

        String value = status.trim().toUpperCase();

        if (value.isEmpty() || "ALL".equals(value)) {
            return null;
        }

        return value;
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

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }

    private String joinName(String firstName, String lastName) {
        return ((firstName == null ? "" : firstName) + " "
                + (lastName == null ? "" : lastName)).trim();
    }
}
