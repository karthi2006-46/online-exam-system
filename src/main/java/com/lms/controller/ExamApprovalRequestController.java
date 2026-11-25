package com.lms.controller;

import com.lms.entity.ExamApprovalRequest;
import com.lms.service.ExamApprovalRequestService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

@RestController
@RequestMapping("/api/exam-approval")
public class ExamApprovalRequestController {

    private final ExamApprovalRequestService service;

    public ExamApprovalRequestController(
            ExamApprovalRequestService service
    ) {
        this.service = service;
    }

    // STUDENT - REQUEST APPROVAL
    @PostMapping("/request")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> requestApproval(
            @RequestParam Long courseId,
            Authentication authentication
    ) {

        ExamApprovalRequest request =
                service.requestApproval(
                        authentication,
                        courseId
                );

        return ResponseEntity.ok(request);
    }

    // STUDENT - CHECK APPROVAL STATUS
    @GetMapping("/course/{courseId}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> getApprovalStatus(
            @PathVariable Long courseId,
            Authentication authentication
    ) {

        String status =
                service.getApprovalStatus(
                        authentication,
                        courseId
                );

        return ResponseEntity.ok(status);
    }

    // FACULTY - VIEW PENDING REQUESTS
    @GetMapping("/pending")
    @PreAuthorize("hasRole('FACULTY')")
    public ResponseEntity<?> getPendingRequests(
            Authentication authentication
    ) {

        List<ExamApprovalRequest> requests =
                service.getPendingRequests(authentication);

        return ResponseEntity.ok(requests);
    }

    // FACULTY/ADMIN - VIEW FULL APPROVAL HISTORY
    @GetMapping("/faculty/history")
    @PreAuthorize("hasAnyRole('FACULTY','ADMIN')")
    public ResponseEntity<?> getFacultyHistory(
            Authentication authentication
    ) {

        return ResponseEntity.ok(
                service.getFacultyHistory(authentication)
        );
    }

    // FACULTY - APPROVE
    @PutMapping("/{requestId}/approve")
    @PreAuthorize("hasRole('FACULTY')")
    public ResponseEntity<?> approve(
            @PathVariable Long requestId,
            @RequestParam(required = false) String comment,
            Authentication authentication
    ) {

        return ResponseEntity.ok(
                service.approve(
                        requestId,
                        comment,
                        authentication
                )
        );
    }

    // FACULTY - REJECT
    @PutMapping("/{requestId}/reject")
    @PreAuthorize("hasRole('FACULTY')")
    public ResponseEntity<?> reject(
            @PathVariable Long requestId,
            @RequestParam(required = false) String comment,
            Authentication authentication
    ) {

        return ResponseEntity.ok(
                service.reject(
                        requestId,
                        comment,
                        authentication
                )
        );
    }
}