package com.lms.service;

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

    private static final int MAX_EXTENSION_REQUESTS = 3;

    public ExtensionRequestDTO requestExtension(Long enrollmentId, Integer daysRequested, String reason) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found"));

        long pendingRequests = extensionRepository.countByEnrollment_IdAndStatus(enrollmentId, "PENDING");
        if (pendingRequests > 0) {
            throw new BadRequestException("You already have a pending extension request");
        }

        long totalRequests = extensionRepository.countByEnrollment_IdAndStatus(enrollmentId, "APPROVED");
        if (totalRequests >= MAX_EXTENSION_REQUESTS) {
            throw new BadRequestException("Maximum extension requests (" + MAX_EXTENSION_REQUESTS + ") already reached");
        }

        ExtensionRequest request = new ExtensionRequest();
        request.setEnrollment(enrollment);
        request.setDaysRequested(daysRequested);
        request.setReason(reason);
        request.setStatus("PENDING");

        ExtensionRequest savedRequest = extensionRepository.save(request);
        return convertToDTO(savedRequest);
    }

    public ExtensionRequestDTO getRequestById(Long requestId) {
        ExtensionRequest request = extensionRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Extension request not found"));
        return convertToDTO(request);
    }

    public List<ExtensionRequestDTO> getEnrollmentRequests(Long enrollmentId) {
        return extensionRepository.findByEnrollment_Id(enrollmentId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<ExtensionRequestDTO> getPendingRequests() {
        return extensionRepository.findByStatus("PENDING").stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<ExtensionRequestDTO> getStudentRequests(Long studentId) {
        return extensionRepository.findByEnrollment_Student_Id(studentId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public ExtensionRequestDTO approveRequest(Long requestId, Long reviewedBy) {
        ExtensionRequest request = extensionRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Extension request not found"));

        User reviewer = userRepository.findById(reviewedBy)
                .orElseThrow(() -> new ResourceNotFoundException("Reviewer not found"));

        request.setStatus("APPROVED");
        request.setReviewedBy(reviewer);
        request.setReviewedAt(LocalDateTime.now());

        // Update enrollment due date
        Enrollment enrollment = request.getEnrollment();
        enrollment.setDueDate(enrollment.getDueDate().plusDays(request.getDaysRequested()));
        enrollmentRepository.save(enrollment);

        ExtensionRequest updatedRequest = extensionRepository.save(request);
        return convertToDTO(updatedRequest);
    }

    public ExtensionRequestDTO rejectRequest(Long requestId, Long reviewedBy, String rejectionReason) {
        ExtensionRequest request = extensionRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Extension request not found"));

        User reviewer = userRepository.findById(reviewedBy)
                .orElseThrow(() -> new ResourceNotFoundException("Reviewer not found"));

        request.setStatus("REJECTED");
        request.setRejectionReason(rejectionReason);
        request.setReviewedBy(reviewer);
        request.setReviewedAt(LocalDateTime.now());

        ExtensionRequest updatedRequest = extensionRepository.save(request);
        return convertToDTO(updatedRequest);
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
