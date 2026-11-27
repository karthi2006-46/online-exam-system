package com.lms.service;

import com.lms.dto.ExtensionHistoryDTO;
import com.lms.dto.ExtensionRequestDTO;
import com.lms.entity.Enrollment;
import com.lms.entity.ExtensionRequest;
import com.lms.entity.User;
import com.lms.exception.BadRequestException;
import com.lms.exception.ResourceNotFoundException;
import com.lms.repository.EnrollmentRepository;
import com.lms.repository.ExtensionRequestRepository;
import com.lms.repository.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class ExtensionRequestService {

    @Autowired
    private ExtensionRequestRepository extensionRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudentDashboardService studentDashboardService;

    private static final int MAX_EXTENSION_REQUESTS = 3;

    private User getAuthenticatedUser() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication.getName() == null) {
            throw new BadRequestException("User is not authenticated");
        }

        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Authenticated user not found"));
    }

    public ExtensionRequestDTO requestExtension(
            Long enrollmentId,
            Integer daysRequested,
            String reason) {

        User student = getAuthenticatedUser();

        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Enrollment not found"));

        // Ensure the enrollment belongs to the logged-in student.
        if (!enrollment.getStudent().getId().equals(student.getId())) {
            throw new BadRequestException(
                    "You can request an extension only for your own enrollment");
        }

        if (daysRequested == null || daysRequested <= 0) {
            throw new BadRequestException(
                    "Extension days must be greater than zero");
        }

        if (reason == null || reason.trim().isEmpty()) {
            throw new BadRequestException(
                    "Please provide a reason for the extension");
        }

        if (enrollment.getDueDate() == null
                || LocalDateTime.now().isBefore(enrollment.getDueDate())) {
            throw new BadRequestException(
                    "You can request an extension only after the course duration expires");
        }

// Allow extension requests only for expired enrollments.
if (!"INACTIVE".equalsIgnoreCase(enrollment.getStatus())) {
    throw new BadRequestException(
            "Extension requests are allowed only for expired enrollments");
}

        long pendingRequests =
                extensionRepository.countByEnrollment_IdAndStatus(
                        enrollmentId, "PENDING");

        if (pendingRequests > 0) {
            throw new BadRequestException(
                    "You already have a pending extension request");
        }

        // Count ALL requests, including approved and rejected requests.
        long totalRequests =
                extensionRepository.countByEnrollment_Id(enrollmentId);

        if (totalRequests >= MAX_EXTENSION_REQUESTS) {
            throw new BadRequestException(
                    "Maximum of 3 extension requests reached for this enrollment");
        }

        // Extension requests are not available once the course is complete.
        double overallProgress =
                studentDashboardService.getCourseOverallProgress(
                        student.getId(),
                        enrollment.getCourse().getId());

        if (overallProgress >= 100.0) {
            throw new BadRequestException(
                    "Extension requests are not available after completing 100% of the course");
        }

        ExtensionRequest request = new ExtensionRequest();
        request.setEnrollment(enrollment);
        request.setDaysRequested(daysRequested);
        request.setReason(reason.trim());
        request.setStatus("PENDING");

        return convertToDTO(extensionRepository.save(request));
    }

    public ExtensionRequestDTO getRequestById(Long requestId) {
        ExtensionRequest request = extensionRepository.findById(requestId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Extension request not found"));

        User currentUser = getAuthenticatedUser();

        boolean isStudent = request.getEnrollment().getStudent()
                .getId().equals(currentUser.getId());

        boolean isFacultyOrAdmin =
                currentUser.getRole() != null
                && currentUser.getRole().getName() != null
                && (currentUser.getRole().getName().equalsIgnoreCase("FACULTY")
                || currentUser.getRole().getName().equalsIgnoreCase("ADMIN"));

        if (!isStudent && !isFacultyOrAdmin) {
            throw new BadRequestException(
                    "You are not authorized to view this extension request");
        }

        return convertToDTO(request);
    }

    public List<ExtensionRequestDTO> getEnrollmentRequests(Long enrollmentId) {
        User currentUser = getAuthenticatedUser();

        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Enrollment not found"));

        boolean isStudent = enrollment.getStudent().getId()
                .equals(currentUser.getId());

        boolean isFacultyOrAdmin =
                currentUser.getRole() != null
                && currentUser.getRole().getName() != null
                && (currentUser.getRole().getName().equalsIgnoreCase("FACULTY")
                || currentUser.getRole().getName().equalsIgnoreCase("ADMIN"));

        if (!isStudent && !isFacultyOrAdmin) {
            throw new BadRequestException(
                    "You are not authorized to view these extension requests");
        }

        return extensionRepository.findByEnrollment_Id(enrollmentId)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<ExtensionRequestDTO> getPendingRequests() {
        User currentUser = getAuthenticatedUser();
        validateFacultyOrAdmin(currentUser);

        return extensionRepository.findByStatus("PENDING")
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // ==========================================
    // FACULTY - FULL REQUEST HISTORY
    // ==========================================

    @Transactional(readOnly = true)
    public List<ExtensionHistoryDTO> getFacultyHistory() {

        User currentUser = getAuthenticatedUser();
        validateFacultyOrAdmin(currentUser);

        boolean isAdmin = currentUser.getRole() != null
                && currentUser.getRole().getName() != null
                && currentUser.getRole().getName().equalsIgnoreCase("ADMIN");

        List<ExtensionRequest> requests = isAdmin
                ? extensionRepository.findAllByOrderByCreatedAtDesc()
                : extensionRepository
                        .findByEnrollment_Course_Faculty_IdOrderByCreatedAtDesc(
                                currentUser.getId());

        return requests.stream()
                .map(this::toHistoryDTO)
                .collect(Collectors.toList());
    }

    public List<ExtensionRequestDTO> getStudentRequests(Long studentId) {
        User currentUser = getAuthenticatedUser();

        boolean isOwnRequests = currentUser.getId().equals(studentId);

        boolean isFacultyOrAdmin =
                currentUser.getRole() != null
                && currentUser.getRole().getName() != null
                && (currentUser.getRole().getName().equalsIgnoreCase("FACULTY")
                || currentUser.getRole().getName().equalsIgnoreCase("ADMIN"));

        if (!isOwnRequests && !isFacultyOrAdmin) {
            throw new BadRequestException(
                    "You are not authorized to view these student requests");
        }

        return extensionRepository.findByEnrollment_Student_Id(studentId)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public ExtensionRequestDTO approveRequest(Long requestId) {
        User reviewer = getAuthenticatedUser();
        validateFacultyOrAdmin(reviewer);

        ExtensionRequest request = extensionRepository.findById(requestId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Extension request not found"));

        if (!"PENDING".equalsIgnoreCase(request.getStatus())) {
            throw new BadRequestException(
                    "Only pending extension requests can be approved");
        }

        Enrollment enrollment = request.getEnrollment();

        if (enrollment.getDueDate() == null) {
            throw new BadRequestException(
                    "Enrollment due date is not configured");
        }

        int daysRequested = request.getDaysRequested() == null
                ? 0
                : request.getDaysRequested();

        if (daysRequested <= 0) {
            throw new BadRequestException(
                    "Extension days must be greater than zero");
        }

        /*
         * Extend from the later of the current due date and today.
         *
         * If the course expired some time ago, adding the requested days
         * only to the old due date can still leave it in the past. That
         * would keep the enrollment INACTIVE immediately after approval.
         * Using "now" as the base guarantees the student receives the full
         * approved extension and the enrollment becomes ACTIVE again.
         */
        LocalDateTime now = LocalDateTime.now();

        LocalDateTime baseDate = enrollment.getDueDate().isAfter(now)
                ? enrollment.getDueDate()
                : now;

        LocalDateTime newDueDate = baseDate.plusDays(daysRequested);

        enrollment.setDueDate(newDueDate);

        // Reactivate enrollment after approval
        if (!"COMPLETED".equalsIgnoreCase(enrollment.getStatus())
                && !"DROPPED".equalsIgnoreCase(enrollment.getStatus())) {
            enrollment.setStatus("ACTIVE");
        }

        enrollmentRepository.save(enrollment);

        request.setStatus("APPROVED");
        request.setReviewedBy(reviewer);
        request.setReviewedAt(LocalDateTime.now());

        return convertToDTO(extensionRepository.save(request));
    }

    public ExtensionRequestDTO rejectRequest(
            Long requestId,
            String rejectionReason) {

        User reviewer = getAuthenticatedUser();
        validateFacultyOrAdmin(reviewer);

        ExtensionRequest request = extensionRepository.findById(requestId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Extension request not found"));

        if (!"PENDING".equalsIgnoreCase(request.getStatus())) {
            throw new BadRequestException(
                    "Only pending extension requests can be rejected");
        }

        if (rejectionReason == null || rejectionReason.trim().isEmpty()) {
            throw new BadRequestException(
                    "Please provide a rejection reason");
        }

        request.setStatus("REJECTED");
        request.setRejectionReason(rejectionReason.trim());
        request.setReviewedBy(reviewer);
        request.setReviewedAt(LocalDateTime.now());

        return convertToDTO(extensionRepository.save(request));
    }

    private void validateFacultyOrAdmin(User user) {
        if (user.getRole() == null
                || user.getRole().getName() == null
                || (!user.getRole().getName().equalsIgnoreCase("FACULTY")
                && !user.getRole().getName().equalsIgnoreCase("ADMIN"))) {
            throw new BadRequestException(
                    "Only faculty or admin users can perform this action");
        }
    }

    private ExtensionHistoryDTO toHistoryDTO(ExtensionRequest request) {

        ExtensionHistoryDTO dto = new ExtensionHistoryDTO();

        Enrollment enrollment = request.getEnrollment();

        dto.setId(request.getId());
        dto.setEnrollmentId(enrollment != null ? enrollment.getId() : null);

        if (enrollment != null && enrollment.getStudent() != null) {
            dto.setStudentId(enrollment.getStudent().getId());
            dto.setStudentName(joinName(
                    enrollment.getStudent().getFirstName(),
                    enrollment.getStudent().getLastName()));
            dto.setStudentEmail(enrollment.getStudent().getEmail());
        }

        if (enrollment != null && enrollment.getCourse() != null) {
            dto.setCourseId(enrollment.getCourse().getId());
            dto.setCourseTitle(enrollment.getCourse().getTitle());
        }

        dto.setOriginalDueDate(
                enrollment != null ? enrollment.getDueDate() : null);

        dto.setDaysRequested(request.getDaysRequested());
        dto.setReason(request.getReason());
        dto.setRequestedAt(request.getCreatedAt());
        dto.setStatus(request.getStatus());
        dto.setReviewedAt(request.getReviewedAt());
        dto.setRejectionReason(request.getRejectionReason());

        if (request.getReviewedBy() != null) {
            dto.setReviewedByName(joinName(
                    request.getReviewedBy().getFirstName(),
                    request.getReviewedBy().getLastName()));
        }

        return dto;
    }

    private String joinName(String firstName, String lastName) {
        String name = ((firstName == null ? "" : firstName)
                + " "
                + (lastName == null ? "" : lastName))
                .replaceAll("\\s+", " ")
                .trim();

        return name.isEmpty() ? null : name;
    }

    private ExtensionRequestDTO convertToDTO(ExtensionRequest request) {
        return new ExtensionRequestDTO(
                request.getId(),
                request.getEnrollment().getId(),
                request.getDaysRequested(),
                request.getStatus(),
                request.getReason(),
                request.getRejectionReason()
        );
    }
}