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
    public ResponseEntity<?> requestApproval(
            @RequestParam Long studentId,
            @RequestParam Long courseId
    ) {

        try {

            ExamApprovalRequest request =
                    service.requestApproval(
                            studentId,
                            courseId
                    );

            return ResponseEntity.ok(request);

        } catch (Exception e) {

            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());
        }
    }
    // STUDENT - CHECK APPROVAL STATUS
@GetMapping("/student/{studentId}/course/{courseId}")
public ResponseEntity<?> getApprovalStatus(
        @PathVariable Long studentId,
        @PathVariable Long courseId
) {

    String status =
            service.getApprovalStatus(
                    studentId,
                    courseId
            );

    return ResponseEntity.ok(status);
}

    // FACULTY - VIEW PENDING REQUESTS
  @GetMapping("/pending")
@PreAuthorize("hasRole('FACULTY')")
public ResponseEntity<?> getPendingRequests(
        Authentication authentication) {

    List<ExamApprovalRequest> requests =
            service.getPendingRequests(authentication);

    return ResponseEntity.ok(requests);
}

    // FACULTY - APPROVE
@PutMapping("/{requestId}/approve")
@PreAuthorize("hasRole('FACULTY')")
public ResponseEntity<?> approve(
        @PathVariable Long requestId,
        @RequestParam(required = false) String comment,
        Authentication authentication) {

    return ResponseEntity.ok(
            service.approve(requestId, comment, authentication)
    );
}

    // FACULTY - REJECT
   @PutMapping("/{requestId}/reject")
@PreAuthorize("hasRole('FACULTY')")
public ResponseEntity<?> reject(
        @PathVariable Long requestId,
        @RequestParam(required = false) String comment,
        Authentication authentication) {

    return ResponseEntity.ok(
            service.reject(requestId, comment, authentication)
    );
}
}
