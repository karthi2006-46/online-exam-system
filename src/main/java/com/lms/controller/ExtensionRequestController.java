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

    // Student: Request an extension for their own enrollment
    @PostMapping("/request")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> requestExtension(
            @RequestParam Long enrollmentId,
            @RequestParam Integer daysRequested,
            @RequestParam String reason) {

        ExtensionRequestDTO requestDTO =
                extensionService.requestExtension(
                        enrollmentId, daysRequested, reason);

        return ApiResponseUtil.created(
                "Extension request submitted successfully",
                requestDTO);
    }

    // Student, or authorized faculty/admin: View a request
    @GetMapping("/{requestId}")
    @PreAuthorize("hasAnyRole('STUDENT','FACULTY','ADMIN')")
    public ResponseEntity<?> getRequestById(
            @PathVariable Long requestId) {

        ExtensionRequestDTO requestDTO =
                extensionService.getRequestById(requestId);

        return ApiResponseUtil.success(
                "Extension request retrieved successfully",
                requestDTO);
    }

    // View requests for an enrollment the caller is authorized to access
    @GetMapping("/enrollment/{enrollmentId}")
    @PreAuthorize("hasAnyRole('STUDENT','FACULTY','ADMIN')")
    public ResponseEntity<?> getEnrollmentRequests(
            @PathVariable Long enrollmentId) {

        List<ExtensionRequestDTO> requests =
                extensionService.getEnrollmentRequests(enrollmentId);

        return ApiResponseUtil.success(
                "Extension requests retrieved",
                requests);
    }

    // Faculty/Admin: View pending requests
    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('FACULTY','ADMIN')")
    public ResponseEntity<?> getPendingRequests() {

        List<ExtensionRequestDTO> requests =
                extensionService.getPendingRequests();

        return ApiResponseUtil.success(
                "Pending extension requests retrieved",
                requests);
    }

    // Student: View own requests. Faculty/Admin may view a specified student's requests.
    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('STUDENT','FACULTY','ADMIN')")
    public ResponseEntity<?> getStudentRequests(
            @PathVariable Long studentId) {

        List<ExtensionRequestDTO> requests =
                extensionService.getStudentRequests(studentId);

        return ApiResponseUtil.success(
                "Student extension requests retrieved",
                requests);
    }

    // Faculty/Admin: Approve a pending extension request
    @PutMapping("/{requestId}/approve")
    @PreAuthorize("hasAnyRole('FACULTY','ADMIN')")
    public ResponseEntity<?> approveRequest(
            @PathVariable Long requestId) {

        ExtensionRequestDTO requestDTO =
                extensionService.approveRequest(requestId);

        return ApiResponseUtil.success(
                "Extension request approved successfully",
                requestDTO);
    }

    // Faculty/Admin: Reject a pending extension request
    @PutMapping("/{requestId}/reject")
    @PreAuthorize("hasAnyRole('FACULTY','ADMIN')")
    public ResponseEntity<?> rejectRequest(
            @PathVariable Long requestId,
            @RequestParam String rejectionReason) {

        ExtensionRequestDTO requestDTO =
                extensionService.rejectRequest(
                        requestId, rejectionReason);

        return ApiResponseUtil.success(
                "Extension request rejected successfully",
                requestDTO);
    }
}