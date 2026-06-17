package com.lms.controller;

import com.lms.dto.ExtensionRequestDTO;
import com.lms.service.ExtensionRequestService;
import com.lms.util.ApiResponseUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/extensions")
@CrossOrigin(origins = "*")
public class ExtensionRequestController {

    @Autowired
    private ExtensionRequestService extensionService;

    @PostMapping("/request")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> requestExtension(
            @RequestParam Long enrollmentId,
            @RequestParam Integer daysRequested,
            @RequestParam String reason) {
        ExtensionRequestDTO requestDTO = extensionService.requestExtension(enrollmentId, daysRequested, reason);
        return ApiResponseUtil.created("Extension request submitted successfully", requestDTO);
    }

    @GetMapping("/{requestId}")
    public ResponseEntity<?> getRequestById(@PathVariable Long requestId) {
        ExtensionRequestDTO requestDTO = extensionService.getRequestById(requestId);
        return ApiResponseUtil.success("Extension request retrieved successfully", requestDTO);
    }

    @GetMapping("/enrollment/{enrollmentId}")
    public ResponseEntity<?> getEnrollmentRequests(@PathVariable Long enrollmentId) {
        List<ExtensionRequestDTO> requests = extensionService.getEnrollmentRequests(enrollmentId);
        return ApiResponseUtil.success("Extension requests retrieved", requests);
    }

    @GetMapping("/pending")
    @PreAuthorize("hasRole('FACULTY') or hasRole('ADMIN')")
    public ResponseEntity<?> getPendingRequests() {
        List<ExtensionRequestDTO> requests = extensionService.getPendingRequests();
        return ApiResponseUtil.success("Pending extension requests retrieved", requests);
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> getStudentRequests(@PathVariable Long studentId) {
        List<ExtensionRequestDTO> requests = extensionService.getStudentRequests(studentId);
        return ApiResponseUtil.success("Student extension requests retrieved", requests);
    }

    @PutMapping("/{requestId}/approve")
    @PreAuthorize("hasRole('FACULTY') or hasRole('ADMIN')")
    public ResponseEntity<?> approveRequest(@PathVariable Long requestId, @RequestParam Long reviewedBy) {
        ExtensionRequestDTO requestDTO = extensionService.approveRequest(requestId, reviewedBy);
        return ApiResponseUtil.success("Extension request approved successfully", requestDTO);
    }

    @PutMapping("/{requestId}/reject")
    @PreAuthorize("hasRole('FACULTY') or hasRole('ADMIN')")
    public ResponseEntity<?> rejectRequest(@PathVariable Long requestId, @RequestParam Long reviewedBy, @RequestParam String rejectionReason) {
        ExtensionRequestDTO requestDTO = extensionService.rejectRequest(requestId, reviewedBy, rejectionReason);
        return ApiResponseUtil.success("Extension request rejected successfully", requestDTO);
    }
}
