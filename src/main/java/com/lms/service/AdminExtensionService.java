package com.lms.service;

import com.lms.dto.admin.AdminExtensionRowDTO;
import com.lms.dto.admin.AdminExtensionStatsDTO;
import com.lms.dto.admin.AdminPageDTO;
import com.lms.entity.Enrollment;
import com.lms.entity.ExtensionRequest;
import com.lms.entity.User;
import com.lms.exception.BadRequestException;
import com.lms.exception.ResourceNotFoundException;
import com.lms.repository.EnrollmentRepository;
import com.lms.repository.ExtensionRequestRepository;
import com.lms.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Admin view of every course extension request.
 * Approving a request pushes the enrollment due date forward and, when the
 * new date is in the future, reactivates the enrollment.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class AdminExtensionService {

    private final ExtensionRequestRepository extensionRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;

    // ==========================================
    // LIST
    // ==========================================

    @Transactional(readOnly = true)
    public AdminPageDTO<AdminExtensionRowDTO> list(
            String status,
            Long courseId,
            Long facultyId,
            String search,
            int page,
            int size) {

        int safePage = Math.max(0, page);
        int safeSize = Math.min(100, Math.max(1, size));

        Pageable pageable = PageRequest.of(safePage, safeSize);
        String term = normalizeSearch(search);

        Page<ExtensionRequest> result = extensionRepository.searchRequests(
                normalizeStatus(status),
                courseId,
                facultyId,
                term == null ? null : like(term),
                pageable
        );

        List<AdminExtensionRowDTO> rows = result.getContent()
                .stream()
                .map(this::toRow)
                .toList();

        return AdminPageDTO.from(result, rows);
    }

    // ==========================================
    // STATS
    // ==========================================

    @Transactional(readOnly = true)
    public AdminExtensionStatsDTO stats() {

        AdminExtensionStatsDTO stats = new AdminExtensionStatsDTO();

        stats.setPending(
                extensionRepository.countByStatusIgnoreCase("PENDING"));
        stats.setApproved(
                extensionRepository.countByStatusIgnoreCase("APPROVED"));
        stats.setRejected(
                extensionRepository.countByStatusIgnoreCase("REJECTED"));
        stats.setTotal(extensionRepository.count());

        return stats;
    }

    // ==========================================
    // APPROVE
    // ==========================================

    public AdminExtensionRowDTO approve(
            Long requestId,
            Authentication authentication) {

        ExtensionRequest request = findPending(requestId);
        Enrollment enrollment = request.getEnrollment();

        if (enrollment.getDueDate() == null) {
            throw new BadRequestException(
                    "Enrollment due date is not configured");
        }

        LocalDateTime newDueDate = enrollment.getDueDate()
                .plusDays(request.getDaysRequested());

        enrollment.setDueDate(newDueDate);

        if (!"COMPLETED".equalsIgnoreCase(enrollment.getStatus())
                && !"DROPPED".equalsIgnoreCase(enrollment.getStatus())) {

            enrollment.setStatus(
                    newDueDate.isAfter(LocalDateTime.now())
                            ? "ACTIVE"
                            : "INACTIVE");
        }

        enrollmentRepository.save(enrollment);

        request.setStatus("APPROVED");
        request.setReviewedBy(currentUser(authentication));
        request.setReviewedAt(LocalDateTime.now());

        return toRow(extensionRepository.save(request));
    }

    // ==========================================
    // REJECT
    // ==========================================

    public AdminExtensionRowDTO reject(
            Long requestId,
            String reason,
            Authentication authentication) {

        String rejectionReason = trim(reason);

        if (rejectionReason == null || rejectionReason.isEmpty()) {
            throw new BadRequestException(
                    "Please provide a rejection reason");
        }

        ExtensionRequest request = findPending(requestId);

        request.setStatus("REJECTED");
        request.setRejectionReason(rejectionReason);
        request.setReviewedBy(currentUser(authentication));
        request.setReviewedAt(LocalDateTime.now());

        return toRow(extensionRepository.save(request));
    }

    // ==========================================
    // HELPERS
    // ==========================================

    private ExtensionRequest findPending(Long requestId) {

        ExtensionRequest request = extensionRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Extension request not found"));

        if (!"PENDING".equalsIgnoreCase(request.getStatus())) {
            throw new BadRequestException(
                    "Only pending extension requests can be reviewed");
        }

        return request;
    }

    private User currentUser(Authentication authentication) {

        if (authentication == null || authentication.getName() == null) {
            return null;
        }

        return userRepository.findByEmail(authentication.getName())
                .orElse(null);
    }

    private AdminExtensionRowDTO toRow(ExtensionRequest request) {

        AdminExtensionRowDTO row = new AdminExtensionRowDTO();

        row.setId(request.getId());
        row.setDaysRequested(request.getDaysRequested());
        row.setStatus(request.getStatus());
        row.setReason(request.getReason());
        row.setRejectionReason(request.getRejectionReason());
        row.setReviewedAt(request.getReviewedAt());
        row.setCreatedAt(request.getCreatedAt());

        Enrollment enrollment = request.getEnrollment();

        if (enrollment != null) {
            row.setEnrollmentId(enrollment.getId());
            row.setEnrollmentStatus(enrollment.getStatus());
            row.setEnrollmentDate(enrollment.getEnrollmentDate());
            row.setDueDate(enrollment.getDueDate());

            if (enrollment.getStudent() != null) {
                User student = enrollment.getStudent();
                row.setStudentId(student.getId());
                row.setStudentName(joinName(
                        student.getFirstName(), student.getLastName()));
                row.setEmail(student.getEmail());
            }

            if (enrollment.getCourse() != null) {
                row.setCourseId(enrollment.getCourse().getId());
                row.setCourseTitle(enrollment.getCourse().getTitle());
                row.setCourseCode(enrollment.getCourse().getCourseCode());

                if (enrollment.getCourse().getFaculty() != null) {
                    User faculty = enrollment.getCourse().getFaculty();
                    row.setFacultyId(faculty.getId());
                    row.setFacultyName(joinName(
                            faculty.getFirstName(), faculty.getLastName()));
                }
            }
        }

        if (request.getReviewedBy() != null) {
            row.setReviewedByName(joinName(
                    request.getReviewedBy().getFirstName(),
                    request.getReviewedBy().getLastName()));
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

    private String joinName(String firstName, String lastName) {
        return ((firstName == null ? "" : firstName) + " "
                + (lastName == null ? "" : lastName)).trim();
    }
}
